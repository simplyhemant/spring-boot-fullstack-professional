package com.example.demo.attendance.service;

import com.example.demo.attendance.dto.OvertimeDetailDto;
import com.example.demo.attendance.dto.OvertimeSummaryDto;
import com.example.demo.attendance.entity.OvertimeEntry;
import com.example.demo.attendance.entity.SettlementStatus;
import com.example.demo.attendance.entity.Worker;
import com.example.demo.attendance.exception.InvalidSettlementException;
import com.example.demo.attendance.exception.ResourceNotFoundException;
import com.example.demo.attendance.repository.OvertimeEntryRepository;
import com.example.demo.attendance.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeEntryRepository overtimeEntryRepository;
    private final WorkerRepository workerRepository;
    private final ExternalWageService externalWageService;
    private final OvertimeSettlementTransactionHelper transactionHelper;

    public OvertimeSummaryDto getWorkerOvertimeSummary(Long workerId, String month) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with ID: " + workerId));

        List<OvertimeEntry> entries = overtimeEntryRepository.findByWorkerIdAndMonth(workerId, month);
        if (entries.isEmpty()) {
            throw new ResourceNotFoundException("No overtime logs found for Worker " + workerId + " in month " + month);
        }

        double totalHours = 0.0;
        BigDecimal totalPayout = BigDecimal.ZERO;
        boolean hasPending = false;
        List<OvertimeDetailDto> details = new ArrayList<>();

        for (OvertimeEntry entry : entries) {
            totalHours += entry.getOvertimeHours();
            totalPayout = totalPayout.add(entry.getAmount());
            if (entry.getSettlementStatus() == SettlementStatus.PENDING) {
                hasPending = true;
            }
            details.add(new OvertimeDetailDto(
                    entry.getDate(),
                    entry.getOvertimeHours(),
                    entry.getOvertimeRateApplied(),
                    entry.getAmount(),
                    entry.getSettlementStatus().name()
            ));
        }

        String overallStatus = hasPending ? SettlementStatus.PENDING.name() : SettlementStatus.SETTLED.name();

        return new OvertimeSummaryDto(
                worker.getId(),
                worker.getName(),
                month,
                totalHours,
                totalPayout,
                overallStatus,
                details
        );
    }

    public OvertimeSummaryDto settleOvertime(Long workerId, String month) {
        String currentMonth = LocalDate.now().toString().substring(0, 7);
        if (month.compareTo(currentMonth) >= 0) {
            throw new InvalidSettlementException("Cannot settle overtime for the current or future month: " + month);
        }

        log.info("Fetching minimum wage multiplier for worker {} month {} outside of database transaction...", workerId, month);
        BigDecimal wageMultiplier = externalWageService.fetchMinimumWageMultiplier();

        return transactionHelper.executeSettlementTransaction(workerId, month, wageMultiplier);
    }
}
