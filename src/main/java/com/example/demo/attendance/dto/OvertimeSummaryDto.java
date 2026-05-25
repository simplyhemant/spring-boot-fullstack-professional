package com.example.demo.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeSummaryDto {
    private Long workerId;
    private String workerName;
    private String month;
    private double totalOvertimeHours;
    private BigDecimal totalPayout;
    private String settlementStatus;
    private List<OvertimeDetailDto> details;
}
