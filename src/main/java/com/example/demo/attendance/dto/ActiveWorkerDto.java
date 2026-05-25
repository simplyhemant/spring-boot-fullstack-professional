package com.example.demo.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveWorkerDto {
    private Long workerId;
    private String workerName;
    private String workerPhone;
    private String designation;
    private BigDecimal dailyWageRate;
    private Long siteId;
    private String siteName;
    private String clockIn; // Store as ISO String for easy Redis serialization
}
