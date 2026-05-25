package com.example.demo.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteDto {
    private Long id;
    private String siteName;
    private String location;
    private boolean active;
}
