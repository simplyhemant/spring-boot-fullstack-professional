package com.example.demo.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceLogDto {
    private Long id;
    private WorkerDto worker;
    private SiteDto site;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private Double totalHours;
    private Double overtimeHours;
    private boolean flagged;
}
