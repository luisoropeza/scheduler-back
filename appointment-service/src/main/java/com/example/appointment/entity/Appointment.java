package com.example.appointment.entity;

import com.example.appointment.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Schedule
    @Column(nullable = false)
    private Long scheduleId;
    @Column(nullable = false)
    private LocalDateTime scheduleStart;
    @Column(nullable = false)
    private LocalDateTime scheduleEnd;

    // Doctor
    @Column(nullable = false)
    private Long doctorId;
    @Column(nullable = false)
    private String doctorName;
    private String doctorSpecialty;
    private String doctorEmail;
    @Column(nullable = false)

    // Patient
    private Long clientId;
    @Column(nullable = false)
    private String clientName;
    private String clientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
