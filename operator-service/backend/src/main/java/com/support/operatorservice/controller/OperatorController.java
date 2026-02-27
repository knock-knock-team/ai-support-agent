package com.support.operatorservice.controller;

import com.support.operatorservice.dto.RequestDto;
import com.support.operatorservice.dto.UpdateRequestRequest;
import com.support.operatorservice.entity.Request;
import com.support.operatorservice.entity.User;
import com.support.operatorservice.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/operator/requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
public class OperatorController {
    
    private final RequestService requestService;
    
    @GetMapping
    public ResponseEntity<List<RequestDto>> getAllRequests() {
        List<RequestDto> requests = requestService.findAll().stream()
                .map(RequestDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(requests);
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<RequestDto>> getPendingRequests() {
        List<RequestDto> requests = requestService.findPendingRequests().stream()
                .map(RequestDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(requests);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RequestDto> getRequestById(@PathVariable Long id) {
        Request request = requestService.findById(id);
        return ResponseEntity.ok(RequestDto.fromEntity(request));
    }
    
    @PostMapping("/{id}/approve")
    public ResponseEntity<RequestDto> approveAiResponse(
            @PathVariable Long id,
            @AuthenticationPrincipal User operator
    ) {
        Request request = requestService.approveAiResponse(id, operator);
        return ResponseEntity.ok(RequestDto.fromEntity(request));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<RequestDto> updateRequest(
            @PathVariable Long id,
            @RequestBody UpdateRequestRequest request,
            @AuthenticationPrincipal User operator
    ) {
        Request updatedRequest = requestService.updateOperatorResponse(
                id,
                request.getOperatorResponse(),
                request.getOperatorNotes(),
                operator
        );
        return ResponseEntity.ok(RequestDto.fromEntity(updatedRequest));
    }
    
    @PostMapping("/{id}/send")
    public ResponseEntity<RequestDto> sendResponse(@PathVariable Long id) {
        Request request = requestService.sendResponse(id);
        return ResponseEntity.ok(RequestDto.fromEntity(request));
    }
    
    @GetMapping("/stats/status")
    public ResponseEntity<Map<String, Long>> getStatsByStatus() {
        return ResponseEntity.ok(requestService.getStatsByStatus());
    }
    
    @GetMapping("/stats/category")
    public ResponseEntity<Map<String, Long>> getStatsByCategory() {
        return ResponseEntity.ok(requestService.getStatsByCategory());
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<RequestDto>> getRecentRequests(@RequestParam(defaultValue = "7") int days) {
        List<RequestDto> requests = requestService.getRecentRequests(days).stream()
                .map(RequestDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(requests);
    }
}
