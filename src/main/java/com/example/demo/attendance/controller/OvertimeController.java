package com.example.demo.attendance.controller;

import com.example.demo.attendance.dto.OvertimeSummaryDto;
import com.example.demo.attendance.service.OvertimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    @GetMapping("/summary")
    public ResponseEntity<OvertimeSummaryDto> getSummary(
            @RequestParam Long workerId,
            @RequestParam String month
    ) {
        return ResponseEntity.ok(overtimeService.getWorkerOvertimeSummary(workerId, month));
    }

    @PostMapping("/settle")
    public ResponseEntity<OvertimeSummaryDto> settle(
            @RequestParam Long workerId,
            @RequestParam String month
    ) {
        return ResponseEntity.ok(overtimeService.settleOvertime(workerId, month));
    }
}
