package com.example.notification.service.impl;

import com.example.notification.enums.ActionEvent;
import com.example.notification.enums.ERole;
import com.example.notification.event.AppointmentBookedEvent;
import com.example.notification.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy 'at' h:mm a");

    private final JavaMailSender mailSender;
    @Value("${app.mail.from}") private String from;
    @Value("${app.mail.from-name}") private String fromName;

    public NotificationServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void handleEvent(AppointmentBookedEvent event) {
        boolean notifyPatient = ActionEvent.BOOKED.name().equals(event.eventType()) || ERole.DOCTOR.name().equals(event.actorType());
        String recipientEmail = notifyPatient ? event.clientEmail() : event.doctorEmail();
        String recipientName  = notifyPatient ? event.clientName()  : event.doctorName();
        if (!StringUtils.hasText(recipientEmail)) {
            log.debug("Skipping notification for appointment {}: no recipient email", event.appointmentId());
            return;
        }
        try {
            sendTo(recipientEmail, buildSubject(event), buildBody(event, recipientName));
            log.info("Notification ({}) sent to {} for appointment {}", event.eventType(), recipientEmail, event.appointmentId());
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send notification for appointment {}: {}", event.appointmentId(), e.getMessage());
        }
    }

    private void sendTo(String to, String subject, String htmlBody) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    private String buildSubject(AppointmentBookedEvent event) {
        return switch (event.eventType()) {
            case "BOOKED"       -> "Appointment confirmed — %s on %s".formatted(event.doctorName(), event.scheduleStart().format(DATE_FMT));
            case "CANCELLED"    -> "Appointment cancelled — %s on %s".formatted(event.doctorName(), event.scheduleStart().format(DATE_FMT));
            case "RESCHEDULED"  -> "Appointment rescheduled — %s on %s".formatted(event.doctorName(), event.scheduleStart().format(DATE_FMT));
            default             -> "Appointment update";
        };
    }

    private String buildBody(AppointmentBookedEvent event, String recipientName) {
        String title = switch (event.eventType()) {
            case "BOOKED"       -> "Your appointment is confirmed";
            case "CANCELLED"    -> "An appointment has been cancelled";
            case "RESCHEDULED"  -> "An appointment has been rescheduled";
            default             -> "Appointment update";
        };
        String badgeColor = "CANCELLED".equals(event.eventType()) ? "#e74c3c"
                : "RESCHEDULED".equals(event.eventType()) ? "#f39c12" : "#27ae60";
        return """
                <!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"/>
                <style>
                  body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px}
                  .card{background:#fff;border-radius:8px;padding:32px;max-width:520px;margin:0 auto;box-shadow:0 2px 8px rgba(0,0,0,.08)}
                  h1{color:#2c3e50;font-size:22px;margin-bottom:4px}
                  .subtitle{color:#7f8c8d;font-size:14px;margin-bottom:24px}
                  table{width:100%%;border-collapse:collapse}
                  td{padding:10px 0;border-bottom:1px solid #ecf0f1;font-size:15px}
                  td:first-child{color:#7f8c8d;width:40%%}td:last-child{color:#2c3e50;font-weight:600}
                  .badge{display:inline-block;background:%s;color:#fff;border-radius:4px;padding:2px 10px;font-size:13px}
                  .footer{margin-top:28px;font-size:12px;color:#bdc3c7;text-align:center}
                </style></head><body>
                <div class="card"><h1>%s</h1><p class="subtitle">Details below.</p>
                <table>
                  <tr><td>Recipient</td><td>%s</td></tr>
                  <tr><td>Doctor</td><td>%s</td></tr>
                  <tr><td>Specialty</td><td>%s</td></tr>
                  <tr><td>Date &amp; time</td><td>%s</td></tr>
                  <tr><td>End time</td><td>%s</td></tr>
                  <tr><td>Status</td><td><span class="badge">%s</span></td></tr>
                </table>
                <div class="footer">Contact us to make any changes.</div>
                </div></body></html>
                """.formatted(
                badgeColor, title, recipientName,
                event.doctorName(),
                event.doctorSpecialty() != null ? event.doctorSpecialty() : "—",
                event.scheduleStart().format(DATE_FMT),
                event.scheduleEnd().format(DATE_FMT),
                event.status());
    }
}
