package com.example.demo.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerDto {
    private Long id;
    private String name;
    private String phone;
    private String designation;
    private BigDecimal dailyWageRate;
    private boolean active;
}
