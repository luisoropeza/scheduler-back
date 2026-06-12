package com.example.notification.listener;

import com.example.notification.event.AppointmentBookedEvent;
import com.example.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.example.notification.config.RabbitConfig.BOOKING_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = BOOKING_QUEUE)
    public void onAppointmentBooked(AppointmentBookedEvent event) {
        log.info("Received booking event for appointment {}", event.appointmentId());
        notificationService.sendConfirmation(event);
    }
}
