package com.example.notification.service.impl;

import com.example.notification.event.AppointmentBookedEvent;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private NotificationServiceImpl notificationService;

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusDays(1);

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(mailSender);
        ReflectionTestUtils.setField(notificationService, "from", "noreply@scheduler.com");
        ReflectionTestUtils.setField(notificationService, "fromName", "Scheduler App");
    }

    private AppointmentBookedEvent bookedByPatient(String clientEmail) {
        return new AppointmentBookedEvent(
                1L, "BOOKED", "PATIENT",
                clientEmail, "John Doe",
                "dr.smith@clinic.com", "Dr. Smith", "Cardiology",
                FUTURE, FUTURE.plusHours(1), "PENDING"
        );
    }

    private AppointmentBookedEvent cancelledByDoctor(String clientEmail) {
        return new AppointmentBookedEvent(
                2L, "CANCELLED", "PERSONAL",
                clientEmail, "John Doe",
                "dr.smith@clinic.com", "Dr. Smith", "Cardiology",
                FUTURE, FUTURE.plusHours(1), "CANCELLED"
        );
    }

    private AppointmentBookedEvent cancelledByPatient(String doctorEmail) {
        return new AppointmentBookedEvent(
                3L, "CANCELLED", "PATIENT",
                "john@example.com", "John Doe",
                doctorEmail, "Dr. Smith", "Cardiology",
                FUTURE, FUTURE.plusHours(1), "CANCELLED"
        );
    }

    private AppointmentBookedEvent rescheduledEvent() {
        return new AppointmentBookedEvent(
                4L, "RESCHEDULED", "PATIENT",
                "john@example.com", "John Doe",
                "dr.smith@clinic.com", "Dr. Smith", "Cardiology",
                FUTURE, FUTURE.plusHours(1), "PENDING"
        );
    }

    @Test
    void handleEvent_booked_withValidClientEmail_sendsEmailToPatient() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.handleEvent(bookedByPatient("john@example.com"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void handleEvent_booked_nullClientEmail_skipsWithoutInteractingWithMailSender() {
        notificationService.handleEvent(bookedByPatient(null));

        verifyNoInteractions(mailSender);
    }

    @Test
    void handleEvent_booked_blankClientEmail_skipsWithoutInteractingWithMailSender() {
        notificationService.handleEvent(bookedByPatient("   "));

        verifyNoInteractions(mailSender);
    }

    @Test
    void handleEvent_cancelledByDoctor_withValidClientEmail_sendsEmailToPatient() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.handleEvent(cancelledByDoctor("john@example.com"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void handleEvent_cancelledByPatient_withValidDoctorEmail_sendsEmailToDoctor() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.handleEvent(cancelledByPatient("dr.smith@clinic.com"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void handleEvent_cancelledByPatient_nullDoctorEmail_skipsWithoutInteractingWithMailSender() {
        notificationService.handleEvent(cancelledByPatient(null));

        verifyNoInteractions(mailSender);
    }

    @Test
    void handleEvent_rescheduled_withValidEmail_sendsEmail() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.handleEvent(rescheduledEvent());

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void handleEvent_booked_withNullSpecialty_sendsEmailWithoutError() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        AppointmentBookedEvent eventNoSpecialty = new AppointmentBookedEvent(
                5L, "BOOKED", "PATIENT",
                "john@example.com", "John Doe",
                "dr.smith@clinic.com", "Dr. Smith", null,
                FUTURE, FUTURE.plusHours(1), "PENDING"
        );

        notificationService.handleEvent(eventNoSpecialty);

        verify(mailSender).send(mimeMessage);
    }
}
