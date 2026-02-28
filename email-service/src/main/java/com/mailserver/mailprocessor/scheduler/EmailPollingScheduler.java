package com.mailserver.mailprocessor.scheduler;

import com.mailserver.mailprocessor.model.dto.RequestDto;
import com.mailserver.mailprocessor.service.EmailReceiverService;
import com.mailserver.mailprocessor.service.RequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class EmailPollingScheduler {

    private final EmailReceiverService emailReceiverService;
    private final RequestService requestService;

    public EmailPollingScheduler(EmailReceiverService emailReceiverService,
                                RequestService requestService) {
        this.emailReceiverService = emailReceiverService;
        this.requestService = requestService;
    }

    /**
     * Poll emails at configured interval
     */
    @Scheduled(fixedDelayString = "${app.mail.poll-interval-ms}")
    public void pollEmails() {
        try {
            log.info("Starting email polling...");
            
            // Fetch unread emails
            List<RequestDto> requests = emailReceiverService.fetchUnreadEmails();
            
            if (requests.isEmpty()) {
                log.info("No new emails found");
                return;
            }
            
            log.info("Found {} new emails, processing...", requests.size());
            
            // Process and send to Kafka
            List<RequestDto> processedRequests = requestService.processRequests(requests);
            
            log.info("Successfully processed {} requests", processedRequests.size());
            
        } catch (Exception e) {
            log.error("Error during email polling: {}", e.getMessage(), e);
        }
    }
}
