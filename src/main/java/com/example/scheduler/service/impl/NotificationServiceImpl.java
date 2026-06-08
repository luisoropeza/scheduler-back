package com.example.scheduler.service.impl;

import com.example.scheduler.config.MailProperties;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy 'at' h:mm a");

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendAppointmentConfirmation(Appointment appointment) {
        if (!StringUtils.hasText(appointment.getClientEmail())) {
            log.debug("Skipping confirmation email for appointment {}: no client email", appointment.getId());
            return;
        }

        try {
            sendTo(appointment.getClientEmail(), buildSubject(appointment), buildBody(appointment));
            log.info("Confirmation email sent to {} for appointment {}",
                    appointment.getClientEmail(), appointment.getId());
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send confirmation email for appointment {}: {}",
                    appointment.getId(), e.getMessage());
        }
    }

    private void sendTo(String to, String subject, String htmlBody) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(message);
    }

    private String buildSubject(Appointment appointment) {
        return "Appointment confirmed — %s on %s".formatted(
                appointment.getSchedule().getProvider().getName(),
                appointment.getSchedule().getStartTime().format(DATE_FMT));
    }

    private String buildBody(Appointment appointment) {
        var provider  = appointment.getSchedule().getProvider();
        var startTime = appointment.getSchedule().getStartTime();
        var endTime   = appointment.getSchedule().getEndTime();

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px; }
                    .card { background: #ffffff; border-radius: 8px; padding: 32px; max-width: 520px;
                            margin: 0 auto; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
                    h1 { color: #2c3e50; font-size: 22px; margin-bottom: 4px; }
                    .subtitle { color: #7f8c8d; font-size: 14px; margin-bottom: 24px; }
                    table { width: 100%%; border-collapse: collapse; }
                    td { padding: 10px 0; border-bottom: 1px solid #ecf0f1; font-size: 15px; }
                    td:first-child { color: #7f8c8d; width: 40%%; }
                    td:last-child  { color: #2c3e50; font-weight: 600; }
                    .badge { display: inline-block; background: #27ae60; color: #fff;
                             border-radius: 4px; padding: 2px 10px; font-size: 13px; }
                    .footer { margin-top: 28px; font-size: 12px; color: #bdc3c7; text-align: center; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>Your appointment is confirmed</h1>
                    <p class="subtitle">Here are the details for your upcoming visit.</p>
                    <table>
                      <tr><td>Patient</td>     <td>%s</td></tr>
                      <tr><td>Provider</td>    <td>%s</td></tr>
                      <tr><td>Specialty</td>   <td>%s</td></tr>
                      <tr><td>Date &amp; time</td><td>%s</td></tr>
                      <tr><td>End time</td>    <td>%s</td></tr>
                      <tr><td>Status</td>      <td><span class="badge">%s</span></td></tr>
                      %s
                    </table>
                    <div class="footer">
                      To cancel or reschedule, reply to this message or contact us directly.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                appointment.getClientName(),
                provider.getName(),
                provider.getSpecialty() != null ? provider.getSpecialty() : "—",
                startTime.format(DATE_FMT),
                endTime.format(DATE_FMT),
                appointment.getStatus().name(),
                notesRow(appointment.getNotes())
        );
    }

    private String notesRow(String notes) {
        if (!StringUtils.hasText(notes)) return "";
        return "<tr><td>Notes</td><td>%s</td></tr>".formatted(notes);
    }
}
