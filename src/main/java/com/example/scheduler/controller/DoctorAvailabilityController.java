package com.example.scheduler.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctorAvailability")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account Controller")
public class DoctorAvailabilityController {
}
