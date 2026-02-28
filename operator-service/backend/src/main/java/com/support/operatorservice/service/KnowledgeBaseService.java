package com.support.operatorservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.operatorservice.model.dto.KnowledgeDocumentDto;
import com.support.operatorservice.model.dto.SearchResultDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeBaseService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int VECTOR_SIZE = 384;
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;
    private static final List<String> CATEGORIES = List.of(
            "General", "Technical", "Billing", "Account", "Product", "Legal"
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${qdrant.url}")
    private String qdrantUrl;

    @Value("${qdrant.api-key:}")
    private String qdrantApiKey;

    @Value("${qdrant.collection-name:knowledge_base}")
    private String collectionName;

    public Map<String, Object> uploadDocument(MultipartFile file, String category, List<String> tags) throws IOException {
        ensureCollection();

        String fullText = extractTextFromPdf(file);
        int pageCount = countPages(file);

        if (fullText == null || fullText.isBlank()) {
            throw new IllegalArgumentException("PDF does not contain extractable text");
        }

        String safeCategory = (category == null || category.isBlank()) ? "General" : category;
        List<String> safeTags = tags == null ? Collections.emptyList() : tags;

        String documentId = UUID.randomUUID().toString();
        String filename = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();
        String title = filename.replaceFirst("\\.[^.]+$", "");
        String uploadedAt = OffsetDateTime.now().toString();

        List<String> chunks = chunkText(fullText);
        List<Map<String, Object>> points = new ArrayList<>();

        for (int index = 0; index < chunks.size(); index++) {
            String chunk = chunks.get(index);
            Map<String, Object> payload = new HashMap<>();
            payload.put("chunk_id", documentId + "_chunk_" + index);
            payload.put("document_id", documentId);
            payload.put("filename", filename);
            payload.put("title", title);
            payload.put("content", chunk);
            payload.put("chunk_index", index);
            payload.put("category", safeCategory);
            payload.put("page_count", pageCount);
            payload.put("uploaded_at", uploadedAt);
            payload.put("tags", safeTags);

            Map<String, Object> point = new HashMap<>();
            point.put("id", UUID.randomUUID().toString());
            point.put("vector", generateEmbedding(chunk));
            point.put("payload", payload);
            points.add(point);
        }

        Map<String, Object> upsertBody = Map.of("points", points);
        requestQdrant("PUT", "/collections/" + encode(collectionName) + "/points?wait=true", upsertBody);

        return Map.of(
                "document_id", documentId,
                "filename", filename,
                "chunks", chunks.size(),
                "page_count", pageCount,
                "category", safeCategory
        );
    }

    public List<SearchResultDto> search(String query, int limit, String category) {
        ensureCollection();

        Map<String, Object> body = new HashMap<>();
        body.put("vector", generateEmbedding(query));
        body.put("limit", Math.max(1, limit));
        body.put("with_payload", true);

        if (category != null && !category.isBlank()) {
            body.put("filter", Map.of(
                    "must", List.of(Map.of(
                            "key", "category",
                            "match", Map.of("value", category)
                    ))
            ));
        }

        JsonNode response = requestQdrant("POST", "/collections/" + encode(collectionName) + "/points/search", body);
        JsonNode resultArray = response.path("result");

        if (!resultArray.isArray()) {
            return Collections.emptyList();
        }

        List<SearchResultDto> results = new ArrayList<>();
        for (JsonNode node : resultArray) {
            JsonNode payload = node.path("payload");
            results.add(SearchResultDto.builder()
                    .documentId(payload.path("document_id").asText(null))
                    .filename(payload.path("filename").asText(null))
                    .title(payload.path("title").asText(null))
                    .content(payload.path("content").asText(null))
                    .category(payload.path("category").asText(null))
                    .tags(readTags(payload.path("tags")))
                    .pageCount(payload.path("page_count").isNumber() ? payload.path("page_count").asInt() : null)
                    .chunkIndex(payload.path("chunk_index").isNumber() ? payload.path("chunk_index").asInt() : null)
                    .score(node.path("score").isNumber() ? node.path("score").asDouble() : null)
                    .build());
        }

        return results;
    }

    public List<KnowledgeDocumentDto> getDocuments() {
        ensureCollection();

        Map<String, Object> body = new HashMap<>();
        body.put("limit", 1000);
        body.put("with_payload", true);
        body.put("with_vector", false);

        JsonNode response = requestQdrant("POST", "/collections/" + encode(collectionName) + "/points/scroll", body);
        JsonNode resultNode = response.path("result");
        JsonNode pointsNode = resultNode.has("points") ? resultNode.path("points") : resultNode;

        if (!pointsNode.isArray()) {
            return Collections.emptyList();
        }

        Map<String, KnowledgeDocumentDto> docs = new LinkedHashMap<>();

        for (JsonNode point : pointsNode) {
            JsonNode payload = point.path("payload");
            String documentId = payload.path("document_id").asText(null);
            if (documentId == null || documentId.isBlank()) {
                continue;
            }

            KnowledgeDocumentDto existing = docs.get(documentId);
            if (existing == null) {
                OffsetDateTime uploadedAt = null;
                String uploadedRaw = payload.path("uploaded_at").asText(null);
                if (uploadedRaw != null && !uploadedRaw.isBlank()) {
                    try {
                        uploadedAt = OffsetDateTime.parse(uploadedRaw);
                    } catch (Exception ignored) {
                        // keep null
                    }
                }

                KnowledgeDocumentDto dto = KnowledgeDocumentDto.builder()
                        .documentId(documentId)
                        .filename(payload.path("filename").asText(null))
                        .title(payload.path("title").asText(null))
                        .content(payload.path("content").asText(null))
                        .category(payload.path("category").asText("General"))
                        .tags(readTags(payload.path("tags")))
                        .pageCount(payload.path("page_count").isNumber() ? payload.path("page_count").asInt() : null)
                        .uploadedAt(uploadedAt)
                        .chunkCount(1)
                        .build();
                docs.put(documentId, dto);
            } else {
                existing.setChunkCount(existing.getChunkCount() + 1);
            }
        }

        return docs.values().stream()
                .sorted(Comparator.comparing(KnowledgeDocumentDto::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Map<String, String> deleteDocument(String documentId) {
        ensureCollection();

        Map<String, Object> body = Map.of(
                "filter", Map.of(
                        "must", List.of(Map.of(
                                "key", "document_id",
                                "match", Map.of("value", documentId)
                        ))
                )
        );

        requestQdrant("POST", "/collections/" + encode(collectionName) + "/points/delete?wait=true", body);
        return Map.of("status", "deleted", "document_id", documentId);
    }

    public List<String> getCategories() {
        return CATEGORIES;
    }

    private void ensureCollection() {
        try {
            JsonNode node = requestQdrant("GET", "/collections/" + encode(collectionName), null, true);
            if (node != null && !node.path("status").isMissingNode()) {
                return;
            }
        } catch (RuntimeException ignored) {
            // create below
        }

        Map<String, Object> createBody = Map.of(
                "vectors", Map.of(
                        "size", VECTOR_SIZE,
                        "distance", "Cosine"
                )
        );
        requestQdrant("PUT", "/collections/" + encode(collectionName), createBody);
    }

    private JsonNode requestQdrant(String method, String path, Object body) {
        return requestQdrant(method, path, body, false);
    }

    private JsonNode requestQdrant(String method, String path, Object body, boolean allowNotFound) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + path));

            if (qdrantApiKey != null && !qdrantApiKey.isBlank()) {
                builder.header("api-key", qdrantApiKey);
            }

            if (body != null) {
                String json = OBJECT_MAPPER.writeValueAsString(body);
                builder.header("Content-Type", "application/json");
                builder.method(method.toUpperCase(Locale.ROOT), HttpRequest.BodyPublishers.ofString(json));
            } else {
                builder.method(method.toUpperCase(Locale.ROOT), HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (allowNotFound && response.statusCode() == 404) {
                return null;
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Qdrant request failed: " + response.statusCode() + " - " + response.body());
            }

            if (response.body() == null || response.body().isBlank()) {
                return OBJECT_MAPPER.createObjectNode();
            }

            return OBJECT_MAPPER.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant request error", e);
        }
    }

    private String normalizeBaseUrl() {
        String url = qdrantUrl == null ? "" : qdrantUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private int countPages(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return document.getNumberOfPages();
        }
    }

    private List<String> chunkText(String text) {
        String normalized = text == null ? "" : text.replace('\r', '\n');
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            if (end < normalized.length()) {
                int sentenceBreak = Math.max(normalized.lastIndexOf(". ", end), normalized.lastIndexOf('\n', end));
                if (sentenceBreak > start + CHUNK_SIZE / 2) {
                    end = sentenceBreak + 1;
                }
            }

            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end >= normalized.length()) {
                break;
            }

            start = Math.max(0, end - CHUNK_OVERLAP);
        }

        return chunks;
    }

    private List<Double> generateEmbedding(String text) {
        Random random = new Random(text == null ? 0 : text.hashCode());
        List<Double> vector = new ArrayList<>(VECTOR_SIZE);
        double normSum = 0.0;

        for (int i = 0; i < VECTOR_SIZE; i++) {
            double v = (random.nextDouble() * 2.0) - 1.0;
            vector.add(v);
            normSum += v * v;
        }

        double norm = Math.sqrt(normSum);
        if (norm == 0.0) {
            norm = 1.0;
        }

        for (int i = 0; i < vector.size(); i++) {
            vector.set(i, vector.get(i) / norm);
        }

        return vector;
    }

    private List<String> readTags(JsonNode tagsNode) {
        if (tagsNode == null || !tagsNode.isArray()) {
            return Collections.emptyList();
        }

        return OBJECT_MAPPER.convertValue(tagsNode, new TypeReference<List<String>>() {});
    }
}
