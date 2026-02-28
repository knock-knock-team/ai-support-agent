package com.support.operatorservice.controller;

import com.support.operatorservice.model.dto.KnowledgeDocumentDto;
import com.support.operatorservice.model.dto.SearchRequestDto;
import com.support.operatorservice.model.dto.SearchResultDto;
import com.support.operatorservice.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy"));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "General") String category,
            @RequestParam(value = "tags", required = false) String tags
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are supported"));
            }

            List<String> tagList = tags == null || tags.isBlank()
                    ? Collections.emptyList()
                    : Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(knowledgeBaseService.uploadDocument(file, category, tagList));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to process PDF", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process PDF"));
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed"));
        }
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody SearchRequestDto request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "query is required"));
        }

        int limit = request.getLimit() == null ? 10 : request.getLimit();
        List<SearchResultDto> result = knowledgeBaseService.search(request.getQuery(), limit, request.getCategory());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/documents")
    public ResponseEntity<List<KnowledgeDocumentDto>> getDocuments() {
        return ResponseEntity.ok(knowledgeBaseService.getDocuments());
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable String documentId) {
        return ResponseEntity.ok(knowledgeBaseService.deleteDocument(documentId));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(knowledgeBaseService.getCategories());
    }
}
