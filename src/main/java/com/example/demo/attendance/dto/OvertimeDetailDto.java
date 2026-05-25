package com.example.demo.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeDetailDto {
    private LocalDate date;
    private double overtimeHours;
    private BigDecimal rateApplied;
    private BigDecimal amount;
    private String settlementStatus;
}
