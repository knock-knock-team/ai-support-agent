package backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    @Value("${app.kafka.topic.requests}")
    private String requestsTopic;

    @Bean
    public NewTopic requestsTopic() {
        return TopicBuilder.name(requestsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}