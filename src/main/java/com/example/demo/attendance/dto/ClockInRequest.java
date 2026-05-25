package com.example.demo.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClockInRequest {
    @NotNull(message = "workerId is required")
    private Long workerId;

    @NotNull(message = "siteId is required")
    private Long siteId;

    private String clockInTime;

}
