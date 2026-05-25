package com.example.demo.attendance.service;

import com.example.demo.attendance.dto.ActiveWorkerDto;
import com.example.demo.attendance.entity.AttendanceLog;
import com.example.demo.attendance.entity.Designation;
import com.example.demo.attendance.entity.OvertimeEntry;
import com.example.demo.attendance.entity.Site;
import com.example.demo.attendance.entity.Worker;
import com.example.demo.attendance.exception.DuplicateClockInException;
import com.example.demo.attendance.exception.NotClockedInException;
import com.example.demo.attendance.repository.AttendanceLogRepository;
import com.example.demo.attendance.repository.OvertimeEntryRepository;
import com.example.demo.attendance.repository.SiteRepository;
import com.example.demo.attendance.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttendanceServiceTest {

    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private SiteRepository siteRepository;
    @Mock
    private AttendanceLogRepository attendanceLogRepository;
    @Mock
    private OvertimeEntryRepository overtimeEntryRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AttendanceService attendanceService;

    private Worker worker;
    private Site site;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setName("John Doe");
        worker.setPhone("9876543210");
        worker.setDesignation(Designation.MASON);
        worker.setDailyWageRate(BigDecimal.valueOf(800)); // hourly rate = 100
        worker.setActive(true);

        site = new Site();
        site.setId(1L);
        site.setSiteName("Greenfield");
        site.setLocation("Delhi");
        site.setActive(true);
    }

    @Test
    void testClockIn_Success() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(attendanceLogRepository.findFirstByWorkerIdAndClockOutIsNull(1L)).thenReturn(Optional.empty());
        when(attendanceLogRepository.save(any(AttendanceLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AttendanceLog log = attendanceService.clockIn(1L, 1L);

        assertNotNull(log);
        assertEquals(worker, log.getWorker());
        assertEquals(site, log.getSite());
        assertNull(log.getClockOut());
        verify(attendanceLogRepository, times(1)).save(any(AttendanceLog.class));
    }

    @Test
    void testClockIn_DuplicateThrowsException() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(attendanceLogRepository.findFirstByWorkerIdAndClockOutIsNull(1L))
                .thenReturn(Optional.of(new AttendanceLog(worker, site, LocalDateTime.now())));

        assertThrows(DuplicateClockInException.class, () -> attendanceService.clockIn(1L, 1L));
    }

    @Test
    void testClockOut_Success_StandardShift() {
        AttendanceLog openLog = new AttendanceLog(worker, site, LocalDateTime.now().minusHours(6));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(attendanceLogRepository.findFirstByWorkerIdAndClockOutIsNull(1L)).thenReturn(Optional.of(openLog));
        when(attendanceLogRepository.save(any(AttendanceLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceLog log = attendanceService.clockOut(1L);

        assertNotNull(log);
        assertNotNull(log.getClockOut());
        assertEquals(6.0, log.getTotalHours(), 0.1);
        assertEquals(0.0, log.getOvertimeHours());
        verify(overtimeEntryRepository, never()).save(any(OvertimeEntry.class));
    }

    @Test
    void testClockOut_Success_WithOvertime_Capped() {
        AttendanceLog openLog = new AttendanceLog(worker, site, LocalDateTime.now().minusHours(12));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(attendanceLogRepository.findFirstByWorkerIdAndClockOutIsNull(1L)).thenReturn(Optional.of(openLog));
        when(attendanceLogRepository.save(any(AttendanceLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        String month = LocalDateTime.now().toLocalDate().toString().substring(0, 7);
        when(overtimeEntryRepository.getAccumulatedOvertimeHours(1L, month)).thenReturn(58.0);

        AttendanceLog log = attendanceService.clockOut(1L);

        assertNotNull(log);
        assertEquals(12.0, log.getTotalHours(), 0.1);
        assertEquals(2.0, log.getOvertimeHours());
        verify(overtimeEntryRepository, times(1)).save(any(OvertimeEntry.class));
    }

    @Test
    void testClockOut_NotClockedInThrowsException() {
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(attendanceLogRepository.findFirstByWorkerIdAndClockOutIsNull(1L)).thenReturn(Optional.empty());

        assertThrows(NotClockedInException.class, () -> attendanceService.clockOut(1L));
    }
}
