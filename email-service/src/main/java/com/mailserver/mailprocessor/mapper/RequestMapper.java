package com.mailserver.mailprocessor.mapper;

import com.mailserver.mailprocessor.model.dto.RequestDto;
import com.mailserver.mailprocessor.model.entity.Request;
import org.springframework.stereotype.Component;

@Component
public class RequestMapper {

    /**
     * Convert DTO to Entity
     */
    public Request toEntity(RequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Request.builder()
                .id(dto.getId())
                .email(dto.getEmail())
                .organization(dto.getOrganization())
                .fio(dto.getFio())
                .phone(dto.getPhone())
                .deviceType(dto.getDeviceType())
                .serialNumber(dto.getSerialNumber())
                .category(dto.getCategory())
                .project(dto.getProject())
                .inn(dto.getInn())
                .countryRegion(dto.getCountryRegion())
                .file(dto.getFile())
                .fileName(dto.getFileName())
                .fileContentType(dto.getFileContentType())
                .confidenceScore(dto.getConfidenceScore())
                .status(dto.getStatus())
                .aiGeneratedAnswer(dto.getAiGeneratedAnswer())
                .operatorAnswer(dto.getOperatorAnswer())
                .emailSubject(dto.getEmailSubject())
                .emailBody(dto.getEmailBody())
                .rawEmailContent(dto.getRawEmailContent())
                .isForm(dto.getIsForm() != null ? dto.getIsForm() : false)
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    /**
     * Convert Entity to DTO
     */
    public RequestDto toDto(Request entity) {
        if (entity == null) {
            return null;
        }

        return RequestDto.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .organization(entity.getOrganization())
                .fio(entity.getFio())
                .phone(entity.getPhone())
                .deviceType(entity.getDeviceType())
                .serialNumber(entity.getSerialNumber())
                .category(entity.getCategory())
                .project(entity.getProject())
                .inn(entity.getInn())
                .countryRegion(entity.getCountryRegion())
                .file(entity.getFile())
                .fileName(entity.getFileName())
                .fileContentType(entity.getFileContentType())
                .confidenceScore(entity.getConfidenceScore())
                .status(entity.getStatus())
                .aiGeneratedAnswer(entity.getAiGeneratedAnswer())
                .operatorAnswer(entity.getOperatorAnswer())
                .emailSubject(entity.getEmailSubject())
                .emailBody(entity.getEmailBody())
                .rawEmailContent(entity.getRawEmailContent())
                .isForm(entity.getIsForm())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
