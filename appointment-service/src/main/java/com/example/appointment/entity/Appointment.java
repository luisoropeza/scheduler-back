package com.example.appointment.entity;

import com.example.appointment.entity.enums.AppointmentStatus;
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

    // Cross-service references stored as plain IDs (no JPA foreign keys across services).
    @Column(nullable = false)
    private Long scheduleId;

    @Column(nullable = false)
    private Long providerId;

    // Denormalized schedule/provider snapshot captured at booking time.
    @Column(nullable = false)
    private LocalDateTime scheduleStart;

    @Column(nullable = false)
    private LocalDateTime scheduleEnd;

    @Column(nullable = false)
    private String providerName;

    private String providerSpecialty;

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private String clientPhone;

    private String clientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
