package com.example.notification.service;

import com.example.notification.event.AppointmentBookedEvent;

public interface NotificationService {
    void sendConfirmation(AppointmentBookedEvent event);
}
