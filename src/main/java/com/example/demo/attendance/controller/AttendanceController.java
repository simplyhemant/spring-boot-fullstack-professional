package com.example.demo.attendance.controller;

import com.example.demo.attendance.dto.ActiveWorkerDto;
import com.example.demo.attendance.dto.AttendanceLogDto;
import com.example.demo.attendance.dto.ClockInRequest;
import com.example.demo.attendance.dto.ClockOutRequest;
import com.example.demo.attendance.dto.PaginatedResponse;
import com.example.demo.attendance.dto.SiteDto;
import com.example.demo.attendance.dto.WorkerDto;
import com.example.demo.attendance.entity.AttendanceLog;
import com.example.demo.attendance.entity.Worker;
import com.example.demo.attendance.entity.Site;
import com.example.demo.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceLogDto> clockIn(@Valid @RequestBody ClockInRequest request) {
        LocalDateTime customTime = null;
        if (request.getClockInTime() != null && !request.getClockInTime().isEmpty()) {
            customTime = LocalDateTime.parse(request.getClockInTime());
        }
        AttendanceLog logEntry = attendanceService.clockIn(request.getWorkerId(), request.getSiteId(), customTime);
        return ResponseEntity.ok(convertToDto(logEntry));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceLogDto> clockOut(@Valid @RequestBody ClockOutRequest request) {
        LocalDateTime customTime = null;
        if (request.getClockOutTime() != null && !request.getClockOutTime().isEmpty()) {
            customTime = LocalDateTime.parse(request.getClockOutTime());
        }
        AttendanceLog logEntry = attendanceService.clockOut(request.getWorkerId(), customTime);
        return ResponseEntity.ok(convertToDto(logEntry));
    }


    @GetMapping("/active")
    public ResponseEntity<List<ActiveWorkerDto>> getActiveWorkers() {
        return ResponseEntity.ok(attendanceService.getActiveWorkers());
    }

    @GetMapping("/logs")
    public ResponseEntity<PaginatedResponse<AttendanceLogDto>> getLogs(
            @RequestParam(required = false) Long workerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AttendanceLog> logPage = attendanceService.getAttendanceLogs(workerId, from, to, pageable);
        
        List<AttendanceLogDto> content = logPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
                
        PaginatedResponse<AttendanceLogDto> response = new PaginatedResponse<>(
                content,
                logPage.getTotalElements(),
                logPage.getTotalPages(),
                logPage.getNumber()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/workers")
    public ResponseEntity<WorkerDto> createWorker(@Valid @RequestBody Worker worker) {
        Worker saved = attendanceService.createWorker(worker);
        WorkerDto dto = new WorkerDto(
                saved.getId(),
                saved.getName(),
                saved.getPhone(),
                saved.getDesignation().name(),
                saved.getDailyWageRate(),
                saved.isActive()
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/sites")
    public ResponseEntity<SiteDto> createSite(@Valid @RequestBody Site site) {
        Site saved = attendanceService.createSite(site);
        SiteDto dto = new SiteDto(
                saved.getId(),
                saved.getSiteName(),
                saved.getLocation(),
                saved.isActive()
        );
        return ResponseEntity.ok(dto);
    }

    private AttendanceLogDto convertToDto(AttendanceLog logEntry) {

        WorkerDto workerDto = new WorkerDto(
                logEntry.getWorker().getId(),
                logEntry.getWorker().getName(),
                logEntry.getWorker().getPhone(),
                logEntry.getWorker().getDesignation().name(),
                logEntry.getWorker().getDailyWageRate(),
                logEntry.getWorker().isActive()
        );
        SiteDto siteDto = new SiteDto(
                logEntry.getSite().getId(),
                logEntry.getSite().getSiteName(),
                logEntry.getSite().getLocation(),
                logEntry.getSite().isActive()
        );
        return new AttendanceLogDto(
                logEntry.getId(),
                workerDto,
                siteDto,
                logEntry.getClockIn(),
                logEntry.getClockOut(),
                logEntry.getTotalHours(),
                logEntry.getOvertimeHours(),
                logEntry.isFlagged()
        );
    }
}
