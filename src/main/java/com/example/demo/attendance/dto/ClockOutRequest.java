package com.example.demo.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClockOutRequest {
    @NotNull(message = "workerId is required")
    private Long workerId;

    private String clockOutTime;

}
