package com.example.demo.attendance.service;

import com.example.demo.attendance.dto.OvertimeDetailDto;
import com.example.demo.attendance.dto.OvertimeSummaryDto;
import com.example.demo.attendance.entity.OvertimeEntry;
import com.example.demo.attendance.entity.SettlementStatus;
import com.example.demo.attendance.entity.Worker;
import com.example.demo.attendance.event.OvertimeSettledEvent;
import com.example.demo.attendance.exception.AlreadySettledException;
import com.example.demo.attendance.exception.ResourceNotFoundException;
import com.example.demo.attendance.repository.OvertimeEntryRepository;
import com.example.demo.attendance.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OvertimeSettlementTransactionHelper {

    private final OvertimeEntryRepository overtimeEntryRepository;
    private final WorkerRepository workerRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OvertimeSummaryDto executeSettlementTransaction(Long workerId, String month, BigDecimal wageMultiplier) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with ID: " + workerId));

        List<OvertimeEntry> allEntries = overtimeEntryRepository.findByWorkerIdAndMonth(workerId, month);
        if (allEntries.isEmpty()) {
            throw new ResourceNotFoundException("No overtime logs found for Worker " + workerId + " in month " + month);
        }

        List<OvertimeEntry> pendingEntries = new ArrayList<>();
        double totalHours = 0.0;
        for (OvertimeEntry entry : allEntries) {
            totalHours += entry.getOvertimeHours();
            if (entry.getSettlementStatus() == SettlementStatus.PENDING) {
                pendingEntries.add(entry);
            }
        }

        if (pendingEntries.isEmpty()) {
            throw new AlreadySettledException("Overtime is already fully settled for Worker " + workerId + " in month " + month);
        }

        BigDecimal totalSettledAmount = BigDecimal.ZERO;
        List<OvertimeDetailDto> details = new ArrayList<>();

        for (OvertimeEntry entry : pendingEntries) {
            BigDecimal originalAmount = entry.getAmount();
            BigDecimal adjustedAmount = originalAmount.multiply(wageMultiplier).setScale(2, RoundingMode.HALF_UP);
            
            entry.setAmount(adjustedAmount);
            entry.setSettlementStatus(SettlementStatus.SETTLED);
            overtimeEntryRepository.save(entry);

            totalSettledAmount = totalSettledAmount.add(adjustedAmount);

            details.add(new OvertimeDetailDto(
                    entry.getDate(),
                    entry.getOvertimeHours(),
                    entry.getOvertimeRateApplied(),
                    adjustedAmount,
                    SettlementStatus.SETTLED.name()
            ));
        }

        for (OvertimeEntry entry : allEntries) {
            if (entry.getSettlementStatus() == SettlementStatus.SETTLED && !pendingEntries.contains(entry)) {
                details.add(new OvertimeDetailDto(
                        entry.getDate(),
                        entry.getOvertimeHours(),
                        entry.getOvertimeRateApplied(),
                        entry.getAmount(),
                        SettlementStatus.SETTLED.name()
                ));
            }
        }

        OvertimeSettledEvent event = new OvertimeSettledEvent(
                this, 
                worker.getId(), 
                worker.getPhone(), 
                month, 
                totalSettledAmount
        );
        eventPublisher.publishEvent(event);

        return new OvertimeSummaryDto(
                worker.getId(),
                worker.getName(),
                month,
                totalHours,
                totalSettledAmount,
                SettlementStatus.SETTLED.name(),
                details
        );
    }
}
