package com.example.demo.attendance.service;

import com.example.demo.attendance.dto.OvertimeSummaryDto;
import com.example.demo.attendance.entity.Designation;
import com.example.demo.attendance.entity.Worker;
import com.example.demo.attendance.exception.InvalidSettlementException;
import com.example.demo.attendance.exception.ResourceNotFoundException;
import com.example.demo.attendance.repository.OvertimeEntryRepository;
import com.example.demo.attendance.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OvertimeServiceTest {

    @Mock
    private OvertimeEntryRepository overtimeEntryRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private ExternalWageService externalWageService;
    @Mock
    private OvertimeSettlementTransactionHelper transactionHelper;

    @InjectMocks
    private OvertimeService overtimeService;

    private Worker worker;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setName("John Doe");
        worker.setPhone("9876543210");
        worker.setDesignation(Designation.MASON);
        worker.setDailyWageRate(BigDecimal.valueOf(800));
        worker.setActive(true);
    }

    @Test
    void testGetSummary_NoLogsThrowsNotFound() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(overtimeEntryRepository.findByWorkerIdAndMonth(1L, "2026-04")).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> overtimeService.getWorkerOvertimeSummary(1L, "2026-04"));
    }

    @Test
    void testSettleOvertime_OngoingMonthThrowsException() {
        String currentMonth = LocalDate.now().toString().substring(0, 7);
        assertThrows(InvalidSettlementException.class, () -> overtimeService.settleOvertime(1L, currentMonth));
    }

    @Test
    void testSettleOvertime_Success() {
        String pastMonth = "2026-04";
        BigDecimal multiplier = BigDecimal.valueOf(1.2);

        when(externalWageService.fetchMinimumWageMultiplier()).thenReturn(multiplier);

        OvertimeSummaryDto expectedDto = new OvertimeSummaryDto();
        expectedDto.setWorkerId(1L);
        expectedDto.setMonth(pastMonth);
        expectedDto.setSettlementStatus("SETTLED");

        when(transactionHelper.executeSettlementTransaction(1L, pastMonth, multiplier)).thenReturn(expectedDto);

        OvertimeSummaryDto result = overtimeService.settleOvertime(1L, pastMonth);

        assertNotNull(result);
        assertEquals(pastMonth, result.getMonth());
        assertEquals("SETTLED", result.getSettlementStatus());
        verify(externalWageService, times(1)).fetchMinimumWageMultiplier();
        verify(transactionHelper, times(1)).executeSettlementTransaction(1L, pastMonth, multiplier);
    }
}
