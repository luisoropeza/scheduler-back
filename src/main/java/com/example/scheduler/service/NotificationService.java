package com.example.scheduler.service;

import com.example.scheduler.entity.Appointment;

public interface NotificationService {

    void sendAppointmentConfirmation(Appointment appointment);
}
