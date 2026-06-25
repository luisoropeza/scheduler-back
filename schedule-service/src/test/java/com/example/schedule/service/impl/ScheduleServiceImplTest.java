package com.example.schedule.service.impl;

import com.example.schedule.client.PersonalClient;
import com.example.schedule.dto.PersonalResponse;
import com.example.schedule.dto.ScheduleRequest;
import com.example.schedule.dto.ScheduleResponse;
import com.example.schedule.entity.Schedule;
import com.example.schedule.enums.ScheduleStatus;
import com.example.schedule.exception.BusinessException;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.mapper.ScheduleMapper;
import com.example.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class ScheduleServiceImplTest {

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private PersonalClient personalClient;
    @Mock private ScheduleMapper scheduleMapper;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private PersonalResponse activeDoctor() {
        PersonalResponse p = new PersonalResponse();
        p.setId(1L);
        p.setName("Dr. Smith");
        p.setEmail("dr.smith@clinic.com");
        p.setSpecialtyName("Cardiology");
        p.setActive(true);
        return p;
    }

    private PersonalResponse inactiveDoctor() {
        PersonalResponse p = new PersonalResponse();
        p.setId(2L);
        p.setActive(false);
        return p;
    }

    private ScheduleRequest futureSlot() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        ScheduleRequest r = new ScheduleRequest();
        r.setStartTime(start);
        r.setEndTime(start.plusHours(1));
        return r;
    }

    // --- findAll ---

    @Test
    void findAll_activeDoctorNoFilters_returnsAvailableSlots() {
        Schedule slot = Schedule.builder().id(1L).status(ScheduleStatus.AVAILABLE).build();
        ScheduleResponse response = new ScheduleResponse();

        when(personalClient.findById(1L)).thenReturn(activeDoctor());
        when(scheduleRepository.findAllByFilters(
                eq(1L), isNull(), eq(ScheduleStatus.AVAILABLE), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(slot)));
        when(scheduleMapper.toResponse(slot)).thenReturn(response);

        Page<ScheduleResponse> result = scheduleService.findAll(1L, null, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findAll_inactiveDoctor_throwsBusinessException() {
        when(personalClient.findById(2L)).thenReturn(inactiveDoctor());

        assertThatThrownBy(() -> scheduleService.findAll(2L, null, null, null, Pageable.unpaged()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void findAll_nullDoctorId_doesNotValidateDoctor() {
        when(scheduleRepository.findAllByFilters(
                isNull(), isNull(), eq(ScheduleStatus.AVAILABLE), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        scheduleService.findAll(null, null, null, null, Pageable.unpaged());

        verifyNoInteractions(personalClient);
    }

    @Test
    void findAll_withExplicitStatus_passesStatusToRepository() {
        when(personalClient.findById(1L)).thenReturn(activeDoctor());
        when(scheduleRepository.findAllByFilters(
                eq(1L), isNull(), eq(ScheduleStatus.BOOKED), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        scheduleService.findAll(1L, null, ScheduleStatus.BOOKED, null, Pageable.unpaged());

        verify(scheduleRepository).findAllByFilters(
                eq(1L), isNull(), eq(ScheduleStatus.BOOKED), any(LocalDateTime.class), any(Pageable.class));
    }

    // --- getById ---

    @Test
    void getById_existingId_returnsResponse() {
        Schedule schedule = Schedule.builder().id(1L).build();
        ScheduleResponse response = new ScheduleResponse();
        response.setId(1L);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleMapper.toResponse(schedule)).thenReturn(response);

        ScheduleResponse result = scheduleService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- create ---

    @Test
    void create_validRequest_savesScheduleWithDoctorSnapshot() {
        ScheduleRequest request = futureSlot();
        Schedule saved = Schedule.builder().id(1L).doctorName("Dr. Smith").build();
        ScheduleResponse response = new ScheduleResponse();

        when(personalClient.findById(1L)).thenReturn(activeDoctor());
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(saved);
        when(scheduleMapper.toResponse(saved)).thenReturn(response);

        scheduleService.create(1L, request);

        verify(scheduleRepository).save(argThat(s ->
                "Dr. Smith".equals(s.getDoctorName()) &&
                "Cardiology".equals(s.getDoctorSpecialty()) &&
                "dr.smith@clinic.com".equals(s.getDoctorEmail()) &&
                Long.valueOf(1L).equals(s.getDoctorId())));
    }

    @Test
    void create_endTimeEqualsStartTime_throwsBusinessException() {
        LocalDateTime time = LocalDateTime.now().plusDays(1);
        ScheduleRequest request = new ScheduleRequest();
        request.setStartTime(time);
        request.setEndTime(time);

        when(personalClient.findById(1L)).thenReturn(activeDoctor());

        assertThatThrownBy(() -> scheduleService.create(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void create_endTimeBeforeStartTime_throwsBusinessException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        ScheduleRequest request = new ScheduleRequest();
        request.setStartTime(start);
        request.setEndTime(start.minusMinutes(30));

        when(personalClient.findById(1L)).thenReturn(activeDoctor());

        assertThatThrownBy(() -> scheduleService.create(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("End time must be after start time");
    }

    // --- createBatch ---

    @Test
    void createBatch_validRequests_savesAllSlots() {
        LocalDateTime base = LocalDateTime.now().plusDays(1);
        ScheduleRequest r1 = new ScheduleRequest();
        r1.setStartTime(base);
        r1.setEndTime(base.plusHours(1));
        ScheduleRequest r2 = new ScheduleRequest();
        r2.setStartTime(base.plusHours(2));
        r2.setEndTime(base.plusHours(3));

        when(personalClient.findById(1L)).thenReturn(activeDoctor());
        when(scheduleRepository.saveAll(anyList())).thenReturn(List.of());
        when(scheduleMapper.toResponseList(anyList())).thenReturn(List.of());

        scheduleService.createBatch(1L, List.of(r1, r2));

        verify(scheduleRepository).saveAll(argThat(iter -> {
            int count = 0;
            for (Object ignored : iter) count++;
            return count == 2;
        }));
    }

    @Test
    void createBatch_oneInvalidSlot_throwsBusinessExceptionWithoutSaving() {
        LocalDateTime base = LocalDateTime.now().plusDays(1);
        ScheduleRequest valid = new ScheduleRequest();
        valid.setStartTime(base);
        valid.setEndTime(base.plusHours(1));
        ScheduleRequest invalid = new ScheduleRequest();
        invalid.setStartTime(base.plusHours(3));
        invalid.setEndTime(base.plusHours(2));

        when(personalClient.findById(1L)).thenReturn(activeDoctor());

        assertThatThrownBy(() -> scheduleService.createBatch(1L, List.of(valid, invalid)))
                .isInstanceOf(BusinessException.class);

        verify(scheduleRepository, never()).saveAll(any());
    }

    // --- book ---

    @Test
    void book_availableSlot_changesStatusToBooked() {
        Schedule schedule = Schedule.builder().id(1L).status(ScheduleStatus.AVAILABLE).build();
        Schedule saved = Schedule.builder().id(1L).status(ScheduleStatus.BOOKED).build();

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(schedule)).thenReturn(saved);
        when(scheduleMapper.toResponse(saved)).thenReturn(new ScheduleResponse());

        scheduleService.book(1L);

        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.BOOKED);
        verify(scheduleRepository).save(schedule);
    }

    @Test
    void book_alreadyBookedSlot_throwsBusinessException() {
        Schedule schedule = Schedule.builder().id(1L).status(ScheduleStatus.BOOKED).build();
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.book(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available");
    }

    // --- release ---

    @Test
    void release_bookedSlot_changesStatusToAvailable() {
        Schedule schedule = Schedule.builder().id(1L).status(ScheduleStatus.BOOKED).build();
        Schedule saved = Schedule.builder().id(1L).status(ScheduleStatus.AVAILABLE).build();

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(schedule)).thenReturn(saved);
        when(scheduleMapper.toResponse(saved)).thenReturn(new ScheduleResponse());

        scheduleService.release(1L);

        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.AVAILABLE);
    }

    // --- delete ---

    @Test
    void delete_availableSlot_deletesSuccessfully() {
        Schedule schedule = Schedule.builder().id(1L).doctorId(1L).status(ScheduleStatus.AVAILABLE).build();
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        scheduleService.delete(1L, 1L);

        verify(scheduleRepository).delete(schedule);
    }

    @Test
    void delete_bookedSlot_throwsBusinessException() {
        Schedule schedule = Schedule.builder().id(1L).doctorId(1L).status(ScheduleStatus.BOOKED).build();
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.delete(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete a booked schedule slot");

        verify(scheduleRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.delete(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
