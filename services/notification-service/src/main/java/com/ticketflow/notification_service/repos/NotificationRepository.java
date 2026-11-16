package com.ticketflow.notification_service.repos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationChannel;
import com.ticketflow.notification_service.entities.NotificationStatus;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByEventIdAndChannelAndRecipient(UUID eventId, NotificationChannel channel, String recipient);

    @Query("select n from Notification n where n.status = :status and n.nextAttemptAt <= :now order by n.nextAttemptAt")
    List<Notification> findDue(@Param("status") NotificationStatus status, @Param("now") Instant now, Pageable pageable);

    Page<Notification> findByRecipient(String recipient, Pageable pageable);

    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);
}
