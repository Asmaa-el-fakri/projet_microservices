package com.ticketflow.notification_service.adapter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.ticketflow.notification_service.config.NotificationProperties;
import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationChannel;
import com.ticketflow.notification_service.exceptions.DeliveryException;

/** Envoi réel par SMTP. Activé uniquement avec notification.email.mode=smtp et spring.mail.host renseigné. */
@Component
@ConditionalOnProperty(name = "notification.email.mode", havingValue = "smtp")
public class SmtpEmailSender implements NotificationSender {

    public static final String PROVIDER = "smtp";

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public String send(Notification n) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(properties.email().from());
        mail.setTo(n.getRecipient());
        mail.setSubject(n.getSubject());
        mail.setText(n.getMessage());
        try {
            mailSender.send(mail);
        } catch (MailException e) {
            // Le message d'erreur peut contenir l'adresse : on ne conserve que le type d'erreur.
            throw new DeliveryException("Échec SMTP : " + e.getClass().getSimpleName(), e);
        }
        return PROVIDER;
    }
}
