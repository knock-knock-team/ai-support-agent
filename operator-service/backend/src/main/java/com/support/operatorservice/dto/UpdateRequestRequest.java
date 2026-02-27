package com.support.operatorservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateRequestRequest {
    @JsonProperty("operator_answer")
    private String operatorResponse;
    @JsonProperty("operator_notes")
    private String operatorNotes;
}
