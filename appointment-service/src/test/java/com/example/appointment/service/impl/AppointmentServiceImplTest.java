package com.example.appointment.service.impl;

import com.example.appointment.client.ScheduleClient;
import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.dto.ScheduleResponse;
import com.example.appointment.entity.Appointment;
import com.example.appointment.enums.AppointmentStatus;
import com.example.appointment.exception.BusinessException;
import com.example.appointment.exception.ResourceNotFoundException;
import com.example.appointment.mapper.AppointmentMapper;
import com.example.appointment.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private ScheduleClient scheduleClient;

    private AppointmentServiceImpl appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository, appointmentMapper, scheduleClient
        );
    }

    private ScheduleResponse availableFutureSchedule() {
        ScheduleResponse s = new ScheduleResponse();
        s.setId(10L);
        s.setDoctorId(1L);
        s.setDoctorName("Dr. Smith");
        s.setDoctorSpecialty("Cardiology");
        s.setDoctorEmail("dr.smith@clinic.com");
        s.setStatus("AVAILABLE");
        s.setStartTime(LocalDateTime.now().plusDays(1));
        s.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        return s;
    }

    private AppointmentRequest bookingRequest() {
        AppointmentRequest r = new AppointmentRequest();
        r.setScheduleId(10L);
        r.setClientId(5L);
        r.setClientName("John Doe");
        r.setClientEmail("john@example.com");
        return r;
    }

    private Appointment savedAppointment(ScheduleResponse schedule) {
        return Appointment.builder()
                .id(100L)
                .scheduleId(schedule.getId())
                .doctorId(schedule.getDoctorId())
                .doctorName(schedule.getDoctorName())
                .doctorSpecialty(schedule.getDoctorSpecialty())
                .doctorEmail(schedule.getDoctorEmail())
                .clientId(5L)
                .clientName("John Doe")
                .clientEmail("john@example.com")
                .scheduleStart(schedule.getStartTime())
                .scheduleEnd(schedule.getEndTime())
                .status(AppointmentStatus.PENDING)
                .build();
    }

    // --- book ---

    @Test
    void book_availableFutureSlot_booksSlotAndSavesAppointment() {
        ScheduleResponse schedule = availableFutureSchedule();
        Appointment saved = savedAppointment(schedule);

        when(scheduleClient.findById(10L)).thenReturn(schedule);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);
        when(appointmentMapper.toResponse(saved)).thenReturn(new AppointmentResponse());

        appointmentService.book(bookingRequest(), 999L, "RECEPTIONIST");

        verify(scheduleClient).bookSchedule(10L);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void book_appointmentEntity_containsSnapshotFromSchedule() {
        ScheduleResponse schedule = availableFutureSchedule();
        Appointment saved = savedAppointment(schedule);

        when(scheduleClient.findById(10L)).thenReturn(schedule);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);
        when(appointmentMapper.toResponse(saved)).thenReturn(new AppointmentResponse());

        appointmentService.book(bookingRequest(), 999L, "RECEPTIONIST");

        verify(appointmentRepository).save(argThat(a ->
                a.getDoctorId().equals(1L) &&
                "Dr. Smith".equals(a.getDoctorName()) &&
                "Cardiology".equals(a.getDoctorSpecialty()) &&
                "John Doe".equals(a.getClientName()) &&
                a.getClientId().equals(5L)));
    }

    @Test
    void book_unavailableSlot_throwsBusinessExceptionWithoutBooking() {
        ScheduleResponse schedule = availableFutureSchedule();
        schedule.setStatus("BOOKED");

        when(scheduleClient.findById(10L)).thenReturn(schedule);

        assertThatThrownBy(() -> appointmentService.book(bookingRequest(), 999L, "RECEPTIONIST"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer available");

        verify(scheduleClient, never()).bookSchedule(anyLong());
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void book_pastSlot_throwsBusinessException() {
        ScheduleResponse schedule = availableFutureSchedule();
        schedule.setStartTime(LocalDateTime.now().minusHours(1));

        when(scheduleClient.findById(10L)).thenReturn(schedule);

        assertThatThrownBy(() -> appointmentService.book(bookingRequest(), 999L, "RECEPTIONIST"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("past");
    }

    // --- findById ---

    @Test
    void findById_existingId_returnsResponse() {
        Appointment appointment = Appointment.builder().id(1L).build();
        AppointmentResponse response = new AppointmentResponse();
        response.setId(1L);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentMapper.toResponse(appointment)).thenReturn(response);

        AppointmentResponse result = appointmentService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- findByClientId ---

    @Test
    void findByClientId_returnsPageOfAppointments() {
        Appointment a = Appointment.builder().id(1L).build();
        AppointmentResponse response = new AppointmentResponse();

        when(appointmentRepository.findByClientId(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a)));
        when(appointmentMapper.toResponse(a)).thenReturn(response);

        Page<AppointmentResponse> result = appointmentService.findByClientId(5L, Pageable.unpaged(), 999L, "RECEPTIONIST");

        assertThat(result.getContent()).hasSize(1);
    }

    // --- findByDoctorAndStatus ---

    @Test
    void findByDoctorAndStatus_withStatus_returnsFilteredPage() {
        when(appointmentRepository.findAllByFilters(eq(1L), eq(AppointmentStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AppointmentResponse> result = appointmentService.findByDoctorAndStatus(1L, AppointmentStatus.PENDING, Pageable.unpaged(), 999L, "RECEPTIONIST");

        assertThat(result.getContent()).isEmpty();
        verify(appointmentRepository).findAllByFilters(eq(1L), eq(AppointmentStatus.PENDING), any(Pageable.class));
    }

    @Test
    void findByDoctorAndStatus_nullStatus_returnsAllAppointments() {
        when(appointmentRepository.findAllByFilters(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AppointmentResponse> result = appointmentService.findByDoctorAndStatus(1L, null, Pageable.unpaged(), 999L, "RECEPTIONIST");

        assertThat(result.getContent()).isEmpty();
        verify(appointmentRepository).findAllByFilters(eq(1L), isNull(), any(Pageable.class));
    }

    // --- confirm ---

    @Test
    void confirm_pendingAppointment_setsStatusConfirmed() {
        Appointment appointment = Appointment.builder().id(1L).doctorId(1L).status(AppointmentStatus.PENDING).build();
        Appointment saved = Appointment.builder().id(1L).doctorId(1L).status(AppointmentStatus.CONFIRMED).build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(saved);
        when(appointmentMapper.toResponse(any())).thenReturn(new AppointmentResponse());

        appointmentService.confirm(1L, 1L, "DOCTOR");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(appointmentRepository, atLeastOnce()).save(any(Appointment.class));
    }

    @Test
    void confirm_confirmedAppointment_throwsBusinessException() {
        Appointment appointment = Appointment.builder().id(1L).doctorId(1L).status(AppointmentStatus.CONFIRMED).build();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.confirm(1L, 1L, "DOCTOR"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only pending appointments can be confirmed");
    }

    // --- cancel ---

    @Test
    void cancel_pendingAppointment_setsStatusCancelledAndReleasesSlot() {
        Appointment appointment = Appointment.builder()
                .id(1L).scheduleId(10L).doctorId(1L).clientId(5L).status(AppointmentStatus.PENDING).build();
        Appointment saved = Appointment.builder().id(1L).status(AppointmentStatus.CANCELLED).build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(saved);
        when(appointmentMapper.toResponse(saved)).thenReturn(new AppointmentResponse());

        appointmentService.cancel(1L);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(scheduleClient).releaseSchedule(10L);
    }

    @Test
    void cancel_alreadyCancelledAppointment_throwsBusinessException() {
        Appointment appointment = Appointment.builder().id(1L).doctorId(1L).clientId(5L).status(AppointmentStatus.CANCELLED).build();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancel(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already cancelled");

        verify(scheduleClient, never()).releaseSchedule(anyLong());
    }
}
