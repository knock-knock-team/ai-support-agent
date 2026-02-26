package com.mailserver.mailprocessor.service;

import com.mailserver.mailprocessor.model.dto.RequestDto;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class EmailReceiverService {

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @Value("${app.mail.folder}")
    private String mailFolder;

    @Value("${app.mail.batch-size}")
    private int batchSize;

    private final EmailParserService emailParserService;

    public EmailReceiverService(EmailParserService emailParserService) {
        this.emailParserService = emailParserService;
    }

    /**
     * Fetch unread emails from mailbox
     */
    public List<RequestDto> fetchUnreadEmails() {
        List<RequestDto> requests = new ArrayList<>();
        Store store = null;
        Folder inbox = null;

        try {
            log.info("Connecting to mail server: {}", mailHost);
            
            // Create session
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imaps.host", mailHost);
            properties.put("mail.imaps.port", mailPort);
            properties.put("mail.imaps.ssl.enable", "true");
            properties.put("mail.imaps.ssl.trust", "*");
            properties.put("mail.imaps.timeout", "10000");
            properties.put("mail.imaps.connectiontimeout", "10000");

            Session session = Session.getInstance(properties);
            store = session.getStore("imaps");
            store.connect(mailHost, mailUsername, mailPassword);

            // Open inbox
            inbox = store.getFolder(mailFolder);
            inbox.open(Folder.READ_WRITE);

            // Get unread messages
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            
            log.info("Found {} unread messages", messages.length);

            // Process batch
            int processCount = Math.min(messages.length, batchSize);
            
            for (int i = 0; i < processCount; i++) {
                try {
                    Message message = messages[i];
                    RequestDto request = processMessage(message);
                    if (request != null) {
                        requests.add(request);
                        // Mark as read
                        message.setFlag(Flags.Flag.SEEN, true);
                        log.info("Processed email from: {}", request.getEmail());
                    }
                } catch (Exception e) {
                    log.error("Error processing message {}: {}", i, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error fetching emails: {}", e.getMessage(), e);
        } finally {
            // Close connections
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(false);
                }
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (MessagingException e) {
                log.error("Error closing connections: {}", e.getMessage());
            }
        }

        return requests;
    }

    /**
     * Process single email message
     */
    private RequestDto processMessage(Message message) throws Exception {
        String subject = message.getSubject();
        String from = message.getFrom()[0].toString();
        
        // Extract body and attachments
        String bodyText = "";
        byte[] attachmentData = null;
        String attachmentName = null;
        String attachmentType = null;

        if (message.isMimeType("text/plain")) {
            bodyText = message.getContent().toString();
        } else if (message.isMimeType("text/html")) {
            bodyText = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            bodyText = getTextFromMimeMultipart(mimeMultipart);
            
            // Get first attachment if exists
            AttachmentInfo attachmentInfo = getFirstAttachment(mimeMultipart);
            if (attachmentInfo != null) {
                attachmentData = attachmentInfo.data;
                attachmentName = attachmentInfo.name;
                attachmentType = attachmentInfo.contentType;
            }
        }

        // Parse email to RequestDto
        RequestDto request = emailParserService.parseEmailToRequest(subject, bodyText, from);
        
        // Add attachment if present
        if (attachmentData != null) {
            request.setFile(attachmentData);
            request.setFileName(attachmentName);
            request.setFileContentType(attachmentType);
        }
        
        // Store raw content for debugging
        request.setRawEmailContent(bodyText);

        return request;
    }

    /**
     * Extract text from MimeMultipart
     */
    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        
        return result.toString();
    }

    /**
     * Get first attachment from multipart message
     */
    private AttachmentInfo getFirstAttachment(MimeMultipart mimeMultipart) throws Exception {
        int count = mimeMultipart.getCount();
        
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            
            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                byte[] data = IOUtils.toByteArray(bodyPart.getInputStream());
                String name = bodyPart.getFileName();
                String contentType = bodyPart.getContentType();
                
                return new AttachmentInfo(data, name, contentType);
            }
        }
        
        return null;
    }

    /**
     * Helper class for attachment info
     */
    private static class AttachmentInfo {
        byte[] data;
        String name;
        String contentType;

        AttachmentInfo(byte[] data, String name, String contentType) {
            this.data = data;
            this.name = name;
            this.contentType = contentType;
        }
    }
}
