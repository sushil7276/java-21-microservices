package com.ecommerce.audit.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.ecommerce.audit.event.AuditEvent;

@Configuration
public class KafkaTopicConfig {
    @Bean
    NewTopic auditTopic() {
        return TopicBuilder.name(AuditEvent.TOPIC).partitions(1).replicas(1).build();
    }
}
