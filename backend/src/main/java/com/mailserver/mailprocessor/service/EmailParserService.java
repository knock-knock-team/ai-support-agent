package com.mailserver.mailprocessor.service;

import com.mailserver.mailprocessor.model.dto.RequestDto;
import com.mailserver.mailprocessor.model.enums.RequestCategory;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class EmailParserService {

    // Regex patterns for parsing email content
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?[0-9]{1,4}?[-.\\s]?\\(?[0-9]{1,3}?\\)?[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,9}");
    private static final Pattern INN_PATTERN = Pattern.compile("\\b\\d{10,12}\\b");
    private static final Pattern SERIAL_NUMBER_PATTERN = Pattern.compile("\\b[A-Z0-9]{5,20}\\b");

    /**
     * Parse email content to RequestDto
     */
    public RequestDto parseEmailToRequest(String subject, String bodyText, String from) {
        log.debug("Parsing email from: {}, subject: {}", from, subject);
        
        RequestDto request = RequestDto.builder()
                .emailSubject(subject)
                .emailBody(bodyText)
                .email(extractEmail(from, bodyText))
                .build();

        // Parse fields from email body
        parseEmailBody(request, bodyText);
        
        // Try to determine category from subject
        request.setCategory(determineCategory(subject, bodyText));
        
        log.info("Parsed request from email: {}", request.getEmail());
        return request;
    }

    /**
     * Parse email body and extract structured data
     */
    private void parseEmailBody(RequestDto request, String body) {
        String cleanBody = cleanHtmlContent(body);
        
        // Extract phone
        Matcher phoneMatcher = PHONE_PATTERN.matcher(cleanBody);
        if (phoneMatcher.find()) {
            request.setPhone(phoneMatcher.group());
        }
        
        // Extract INN
        Matcher innMatcher = INN_PATTERN.matcher(cleanBody);
        if (innMatcher.find()) {
            request.setInn(innMatcher.group());
        }
        
        // Look for specific keywords and extract data
        extractFieldByKeyword(cleanBody, "организация", "organization", request);
        extractFieldByKeyword(cleanBody, "фио", "fio", request);
        extractFieldByKeyword(cleanBody, "имя", "fio", request);
        extractFieldByKeyword(cleanBody, "тип прибора", "deviceType", request);
        extractFieldByKeyword(cleanBody, "серийный номер", "serialNumber", request);
        extractFieldByKeyword(cleanBody, "заводской номер", "serialNumber", request);
        extractFieldByKeyword(cleanBody, "проект", "project", request);
        extractFieldByKeyword(cleanBody, "страна", "countryRegion", request);
        extractFieldByKeyword(cleanBody, "регион", "countryRegion", request);
    }

    /**
     * Extract field by keyword from text
     */
    private void extractFieldByKeyword(String text, String keyword, String fieldName, RequestDto request) {
        try {
            String lowerText = text.toLowerCase();
            int index = lowerText.indexOf(keyword.toLowerCase());
            
            if (index != -1) {
                // Look for value after keyword (find next line or after colon)
                String afterKeyword = text.substring(index + keyword.length());
                String value = extractValueAfterKeyword(afterKeyword);
                
                if (value != null && !value.isEmpty()) {
                    setRequestField(request, fieldName, value);
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting field {}: {}", fieldName, e.getMessage());
        }
    }

    /**
     * Extract value after keyword (after : or on new line)
     */
    private String extractValueAfterKeyword(String text) {
        text = text.trim();
        
        // Skip colon or equals sign
        if (text.startsWith(":") || text.startsWith("=")) {
            text = text.substring(1).trim();
        }
        
        // Get first line or until next field marker
        int endIndex = text.indexOf('\n');
        if (endIndex == -1) {
            endIndex = Math.min(text.length(), 100); // Max 100 chars
        }
        
        String value = text.substring(0, endIndex).trim();
        
        // Clean up common artifacts
        value = value.replaceAll("[<>\\[\\]{}]", "").trim();
        
        return value.length() > 1 ? value : null;
    }

    /**
     * Set request field using reflection-like approach
     */
    private void setRequestField(RequestDto request, String fieldName, String value) {
        switch (fieldName) {
            case "organization" -> request.setOrganization(value);
            case "fio" -> {
                if (request.getFio() == null) request.setFio(value);
            }
            case "deviceType" -> request.setDeviceType(value);
            case "serialNumber" -> request.setSerialNumber(value);
            case "project" -> request.setProject(value);
            case "countryRegion" -> request.setCountryRegion(value);
        }
    }

    /**
     * Determine category from subject and body keywords
     */
    private RequestCategory determineCategory(String subject, String body) {
        String combined = (subject + " " + body).toLowerCase();
        
        if (containsAny(combined, "гарантия", "warranty")) {
            return RequestCategory.WARRANTY;
        } else if (containsAny(combined, "ремонт", "repair", "неисправн")) {
            return RequestCategory.REPAIR;
        } else if (containsAny(combined, "установка", "монтаж", "installation")) {
            return RequestCategory.INSTALLATION;
        } else if (containsAny(combined, "настройка", "конфигурация", "configuration")) {
            return RequestCategory.CONFIGURATION;
        } else if (containsAny(combined, "консультация", "вопрос", "помощь", "consultation")) {
            return RequestCategory.CONSULTATION;
        } else if (containsAny(combined, "техподдержка", "тех поддержка", "support")) {
            return RequestCategory.TECHNICAL_SUPPORT;
        }
        
        return RequestCategory.OTHER;
    }

    /**
     * Check if text contains any of the keywords
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract email from string
     */
    private String extractEmail(String from, String bodyText) {
        // First try from address
        Matcher matcher = EMAIL_PATTERN.matcher(from);
        if (matcher.find()) {
            return matcher.group();
        }
        
        // Try body
        matcher = EMAIL_PATTERN.matcher(bodyText);
        if (matcher.find()) {
            return matcher.group();
        }
        
        return "unknown@example.com";
    }

    /**
     * Clean HTML content and extract plain text
     */
    private String cleanHtmlContent(String html) {
        if (html == null) {
            return "";
        }
        
        try {
            return Jsoup.parse(html).text();
        } catch (Exception e) {
            log.warn("Error cleaning HTML: {}", e.getMessage());
            return html;
        }
    }
}
