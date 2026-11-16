package com.ticketflow.notification_service.adapter;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ticketflow.notification_service.entities.NotificationChannel;
import com.ticketflow.notification_service.exceptions.DeliveryException;

/** Retrouve l'adaptateur d'un canal. Ajouter un canal = ajouter un NotificationSender. */
@Component
public class SenderRegistry {

    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);

    public SenderRegistry(List<NotificationSender> available) {
        available.forEach(s -> senders.put(s.channel(), s));
    }

    public NotificationSender forChannel(NotificationChannel channel) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new DeliveryException("Aucun adaptateur configuré pour le canal " + channel);
        }
        return sender;
    }
}
