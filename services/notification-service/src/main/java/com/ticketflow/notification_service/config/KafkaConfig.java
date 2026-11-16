package com.ticketflow.notification_service.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.ticketflow.notification_service.exceptions.InvalidEventException;

@Configuration
public class KafkaConfig {

    /**
     * Crée (si absents) les topics consommés et leurs topics de lettres mortes.
     * En production, ces topics sont normalement provisionnés par l'infrastructure.
     */
    @Bean
    public KafkaAdmin.NewTopics notificationTopics(NotificationProperties props) {
        List<NewTopic> topics = new ArrayList<>();
        String suffix = props.kafka().dltSuffix();
        for (String topic : props.kafka().topics()) {
            topics.add(TopicBuilder.name(topic).partitions(1).replicas(1).build());
            topics.add(TopicBuilder.name(topic + suffix).partitions(1).replicas(1).build());
        }
        return new KafkaAdmin.NewTopics(topics.toArray(NewTopic[]::new));
    }

    /**
     * Gestion des erreurs du consommateur :
     * - message invalide (InvalidEventException) : jamais rejoué, envoyé directement en DLT ;
     * - erreur technique (base indisponible...) : quelques reprises, puis DLT.
     * Un échec d'envoi d'email n'arrive jamais ici : il est géré par le service de livraison.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template,
                                                 NotificationProperties props) {
        String suffix = props.kafka().dltSuffix();
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + suffix, -1));
        long retries = Math.max(0, props.kafka().consumerRetryAttempts() - 1L);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(props.kafka().consumerRetryIntervalMs(), retries));
        handler.addNotRetryableExceptions(InvalidEventException.class);
        return handler;
    }
}
