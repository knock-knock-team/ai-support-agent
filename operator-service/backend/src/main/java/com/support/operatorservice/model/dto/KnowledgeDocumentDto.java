package com.support.operatorservice.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentDto {
    
    @JsonProperty("document_id")
    private String documentId;
    
    private String filename;
    
    private String title;
    
    private String content;
    
    private String category;
    
    private List<String> tags;
    
    @JsonProperty("page_count")
    private Integer pageCount;
    
    @JsonProperty("uploaded_at")
    private OffsetDateTime uploadedAt;
    
    @JsonProperty("chunk_count")
    private Integer chunkCount;
}
