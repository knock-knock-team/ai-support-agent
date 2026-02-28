package com.support.operatorservice.service;

import com.support.operatorservice.dto.CreateRequestDto;
import com.support.operatorservice.entity.Request;
import com.support.operatorservice.entity.User;
import com.support.operatorservice.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RequestService {

    private static final float AUTO_SEND_THRESHOLD = 0.60f;
    
    private final RequestRepository requestRepository;
    private final EmailSenderService emailSenderService;
    
    public Request findById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));
    }
    
    public List<Request> findAll() {
        return requestRepository.findAll();
    }
    
    public List<Request> findPendingRequests() {
        return requestRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(Request.Status.NEW, Request.Status.OPERATOR_REVIEW)
        );
    }

    public List<Request> findClosedRequests() {
        return requestRepository.findByStatusOrderByCreatedAtDesc(Request.Status.CLOSED);
    }
    
    public List<Request> findByOperator(Long operatorId) {
        return requestRepository.findByOperatorId(operatorId);
    }
    
    @Transactional
    public Request createRequest(CreateRequestDto payload) {
        Request.Category category = parseCategory(payload.getCategory());

        Float confidence = payload.getConfidenceScore() != null ? payload.getConfidenceScore() : 0.55f;

        String aiAnswer = payload.getAiGeneratedAnswer();
        if (aiAnswer == null || aiAnswer.isBlank()) {
            aiAnswer = "Здравствуйте! Спасибо за обращение. Ваш запрос получен, мы подготовили рекомендации и скоро свяжемся с вами.";
        }

        String operatorAnswer = payload.getOperatorAnswer();
        if (operatorAnswer == null || operatorAnswer.isBlank()) {
            operatorAnswer = aiAnswer;
        }

        String projectTitle = payload.getProject() != null && !payload.getProject().isBlank()
                ? payload.getProject()
                : "Обращение по продукции";

        String summaryMessage = "Клиент: " + (payload.getFio() != null ? payload.getFio() : "Не указано")
                + ", организация: " + (payload.getOrganization() != null ? payload.getOrganization() : "Не указано")
                + ", тип прибора: " + (payload.getDeviceType() != null ? payload.getDeviceType() : "Не указано")
                + ", серийный номер: " + (payload.getSerialNumber() != null ? payload.getSerialNumber() : "Не указано");

        // Determine if request is from form - if isForm is explicitly set use it, otherwise true if created via API
        Boolean isFormRequest = payload.getIsForm() != null ? payload.getIsForm() : true;
        
        String userMessageText = payload.getUserMessage() != null ? payload.getUserMessage() : summaryMessage;
        
        Request request = Request.builder()
                .subject(projectTitle)
                .userMessage(userMessageText)
                .aiResponse(aiAnswer)
                .confidence((double) confidence)
                .senderEmail(payload.getEmail())
                .email(payload.getEmail())
                .organization(payload.getOrganization())
                .fio(payload.getFio())
                .phone(payload.getPhone())
                .deviceType(payload.getDeviceType())
                .serialNumber(payload.getSerialNumber())
                .category(category)
                .project(payload.getProject())
                .inn(payload.getInn())
                .countryRegion(payload.getCountryRegion())
                .file(payload.getFile())
                .confidenceScore(confidence)
                .aiGeneratedAnswer(aiAnswer)
                .operatorAnswer(operatorAnswer)
                .status(Request.Status.NEW)
                .createdAt(payload.getCreatedAt())
                .updatedAt(payload.getUpdatedAt())
                .isForm(isFormRequest)
                .build();

        if (confidence >= AUTO_SEND_THRESHOLD) {
            request.setStatus(Request.Status.AI_GENERATED);
            request.setOperatorAnswer(operatorAnswer);
            sendEmailAndClose(request, request.getAiGeneratedAnswer());
            return requestRepository.save(request);
        }

        request.setStatus(Request.Status.OPERATOR_REVIEW);
        return requestRepository.save(request);
    }

    @Transactional
    public Request createMockRequest() {
        CreateRequestDto payload = new CreateRequestDto();
        payload.setEmail("dimakol2005@mail.ru");
        payload.setOrganization("ООО Живая Сталь");
        payload.setFio("Калашников Владислав Сергеевич");
        payload.setPhone("+79826139545");
        payload.setDeviceType("газотурбинное оборудование");
        payload.setSerialNumber("123123");
        payload.setCategory("TECHNICAL");
        payload.setProject("Тестовый проект");
        payload.setInn(null);
        payload.setCountryRegion("Россия");
        payload.setConfidenceScore(0.55f);
        payload.setAiGeneratedAnswer("Здравствуйте! Для корректной эксплуатации газотурбинного оборудования рекомендуем следовать инструкции производителя и регламенту технического обслуживания. При необходимости пришлите модель и серийный номер для точных рекомендаций.");
        return createRequest(payload);
    }

    private Request.Category parseCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return Request.Category.OTHER;
        }

        String normalized = rawCategory.trim().toUpperCase();

        return switch (normalized) {
            case "TECHNICAL", "ОБРАЩЕНИЕ ПО ПРОДУКЦИИ", "ПРОДУКЦИЯ", "ТЕХНИЧЕСКОЕ" -> Request.Category.TECHNICAL;
            case "BILLING", "ОПЛАТА", "ФИНАНСЫ" -> Request.Category.BILLING;
            case "ACCOUNT", "АККАУНТ", "УЧЕТНАЯ ЗАПИСЬ" -> Request.Category.ACCOUNT;
            case "GENERAL", "ОБЩЕЕ" -> Request.Category.GENERAL;
            default -> {
                try {
                    yield Request.Category.valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield Request.Category.OTHER;
                }
            }
        };
    }
    
    @Transactional
    public Request approveAiResponse(Long requestId, User operator) {
        Request request = findById(requestId);
        request.setOperator(operator);
        if (request.getOperatorAnswer() == null || request.getOperatorAnswer().isBlank()) {
            request.setOperatorAnswer(request.getAiGeneratedAnswer());
        }
        sendEmailAndClose(request, request.getOperatorAnswer());
        return requestRepository.save(request);
    }
    
    @Transactional
    public Request updateOperatorResponse(Long requestId, String operatorResponse, 
                                         String notes, User operator) {
        Request request = findById(requestId);
        request.setOperatorAnswer(operatorResponse);
        request.setOperatorNotes(notes);
        request.setOperator(operator);
        request.setStatus(Request.Status.OPERATOR_REVIEW);
        return requestRepository.save(request);
    }
    
    @Transactional
    public Request sendResponse(Long requestId) {
        Request request = findById(requestId);
        String responseText = request.getOperatorAnswer();
        if (responseText == null || responseText.isBlank()) {
            responseText = request.getAiGeneratedAnswer();
        }
        sendEmailAndClose(request, responseText);
        return requestRepository.save(request);
    }

    private void sendEmailAndClose(Request request, String answer) {
        emailSenderService.sendResponse(
                request.getEmail(),
                "Ответ по вашему обращению",
                answer
        );
        request.setStatus(Request.Status.CLOSED);
        request.setRespondedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }
    
    public Map<String, Long> getStatsByStatus() {
        Map<String, Long> stats = new HashMap<>();
        for (Request.Status status : Request.Status.values()) {
            stats.put(status.name(), requestRepository.countByStatus(status));
        }
        return stats;
    }
    
    public Map<String, Long> getStatsByCategory() {
        Map<String, Long> stats = new HashMap<>();
        for (Request.Category category : Request.Category.values()) {
            stats.put(category.name(), requestRepository.countByCategory(category));
        }
        return stats;
    }
    
    public List<Request> getRecentRequests(int days) {
        OffsetDateTime startDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(days);
        return requestRepository.findRecentRequests(startDate);
    }
}
