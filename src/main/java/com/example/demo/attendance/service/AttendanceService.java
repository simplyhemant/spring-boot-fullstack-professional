package com.example.demo.attendance.service;

import com.example.demo.attendance.dto.ActiveWorkerDto;
import com.example.demo.attendance.entity.AttendanceLog;
import com.example.demo.attendance.entity.OvertimeEntry;
import com.example.demo.attendance.entity.SettlementStatus;
import com.example.demo.attendance.entity.Site;
import com.example.demo.attendance.entity.Worker;
import com.example.demo.attendance.exception.DuplicateClockInException;
import com.example.demo.attendance.exception.NotClockedInException;
import com.example.demo.attendance.exception.ResourceNotFoundException;
import com.example.demo.attendance.repository.AttendanceLogRepository;
import com.example.demo.attendance.repository.OvertimeEntryRepository;
import com.example.demo.attendance.repository.SiteRepository;
import com.example.demo.attendance.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final OvertimeEntryRepository overtimeEntryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ACTIVE_WORKER_KEY_PREFIX = "active_worker:";

    @Transactional
    public AttendanceLog clockIn(Long workerId, Long siteId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with ID: " + workerId));
        if (!worker.isActive()) {
            throw new IllegalArgumentException("Worker is inactive and cannot clock in.");
        }

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found with ID: " + siteId));
        if (!site.isActive()) {
            throw new IllegalArgumentException("Site is inactive and cannot accept clock-ins.");
        }

        // Check if already clocked in (open log)
        attendanceLogRepository.findFirstByWorkerIdAndClockOutIsNull(workerId).ifPresent(log -> {
            throw new DuplicateClockInException("Worker is already clocked in at Site: " + log.getSite().getSiteName());
        });

        LocalDateTime now = LocalDateTime.now();
        AttendanceLog attendanceLog = new AttendanceLog(worker, site, now);
        AttendanceLog savedLog = attendanceLogRepository.save(attendanceLog);

        // Cache in Redis
        saveActiveWorkerToRedis(worker, site, now);

        return savedLog;
    }

    @Transactional
    public AttendanceLog clockOut(Long workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with ID: " + workerId));

        AttendanceLog attendanceLog = attendanceLogRepository.findFirstByWorkerIdAndClockOutIsNull(workerId)
                .orElseThrow(() -> new NotClockedInException("Worker is not currently clocked in."));

        LocalDateTime now = LocalDateTime.now();
        attendanceLog.setClockOut(now);

        // Calculate hours
        double totalHours = Duration.between(attendanceLog.getClockIn(), now).toMillis() / (1000.0 * 60.0 * 60.0);
        totalHours = Math.round(totalHours * 100.0) / 100.0;
        attendanceLog.setTotalHours(totalHours);

        // Auto-flag shift exceeding 16 hours
        if (totalHours > 16.0) {
            attendanceLog.setFlagged(true);
        }

        // Calculate overtime
        double overtimeHours = 0.0;
        if (totalHours > 8.0) {
            overtimeHours = totalHours - 8.0;
        }

        // Apply monthly overtime cap of 60 hours
        String month = now.toLocalDate().toString().substring(0, 7); // Format YYYY-MM
        double accumulatedOt = overtimeEntryRepository.getAccumulatedOvertimeHours(workerId, month);
        
        double allowedOt = Math.max(0.0, 60.0 - accumulatedOt);
        double finalOt = Math.min(overtimeHours, allowedOt);
        
        attendanceLog.setOvertimeHours(finalOt);
        AttendanceLog savedLog = attendanceLogRepository.save(attendanceLog);

        if (finalOt > 0.0) {
            BigDecimal dailyWage = worker.getDailyWageRate();
            BigDecimal hourlyRate = dailyWage.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);
            BigDecimal otAmount = BigDecimal.ZERO;
            
            // 1.5x daily wage rate (hourly rate) for first 2 overtime hours, 2x beyond that
            if (finalOt <= 2.0) {
                otAmount = hourlyRate.multiply(BigDecimal.valueOf(1.5)).multiply(BigDecimal.valueOf(finalOt));
            } else {
                BigDecimal first2Hours = hourlyRate.multiply(BigDecimal.valueOf(1.5)).multiply(BigDecimal.valueOf(2.0));
                BigDecimal extraHours = hourlyRate.multiply(BigDecimal.valueOf(2.0)).multiply(BigDecimal.valueOf(finalOt - 2.0));
                otAmount = first2Hours.add(extraHours);
            }
            otAmount = otAmount.setScale(2, RoundingMode.HALF_UP);

            OvertimeEntry overtimeEntry = new OvertimeEntry();
            overtimeEntry.setWorker(worker);
            overtimeEntry.setAttendance(savedLog);
            overtimeEntry.setDate(now.toLocalDate());
            overtimeEntry.setOvertimeHours(finalOt);
            overtimeEntry.setOvertimeRateApplied(hourlyRate.multiply(BigDecimal.valueOf(1.5))); // Base overtime rate is 1.5x
            overtimeEntry.setAmount(otAmount);
            overtimeEntry.setSettlementStatus(SettlementStatus.PENDING);
            overtimeEntry.setMonth(month);
            
            overtimeEntryRepository.save(overtimeEntry);
        }

        // Remove from Redis cache
        removeActiveWorkerFromRedis(workerId);

        return savedLog;
    }

    public List<ActiveWorkerDto> getActiveWorkers() {
        try {
            Set<String> keys = redisTemplate.keys(ACTIVE_WORKER_KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return Collections.emptyList();
            }
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            List<ActiveWorkerDto> activeWorkers = new ArrayList<>();
            if (values != null) {
                for (Object value : values) {
                    if (value instanceof ActiveWorkerDto) {
                        activeWorkers.add((ActiveWorkerDto) value);
                    }
                }
            }
            return activeWorkers;
        } catch (Exception e) {
            log.error("Redis is unavailable, falling back to database query for active workers: {}", e.getMessage());
            // Degradation fallback to DB
            List<AttendanceLog> activeLogs = attendanceLogRepository.findActiveLogs();
            List<ActiveWorkerDto> dtos = new ArrayList<>();
            for (AttendanceLog logEntry : activeLogs) {
                dtos.add(new ActiveWorkerDto(
                        logEntry.getWorker().getId(),
                        logEntry.getWorker().getName(),
                        logEntry.getWorker().getPhone(),
                        logEntry.getWorker().getDesignation().name(),
                        logEntry.getWorker().getDailyWageRate(),
                        logEntry.getSite().getId(),
                        logEntry.getSite().getSiteName(),
                        logEntry.getClockIn().toString()
                ));
            }
            return dtos;
        }
    }

    public Page<AttendanceLog> getAttendanceLogs(Long workerId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return attendanceLogRepository.findLogs(workerId, from, to, pageable);
    }

    @Transactional
    public Worker updateWorkerProfile(Long workerId, Worker updatedWorkerDetails) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with ID: " + workerId));

        worker.setName(updatedWorkerDetails.getName());
        worker.setDesignation(updatedWorkerDetails.getDesignation());
        worker.setDailyWageRate(updatedWorkerDetails.getDailyWageRate());
        worker.setActive(updatedWorkerDetails.isActive());
        
        Worker savedWorker = workerRepository.save(worker);

        // Invalidate active worker cache if present, since information is stale
        invalidateActiveWorkerCache(workerId);

        return savedWorker;
    }

    @Scheduled(fixedRate = 60000) // Runs every minute
    @Transactional
    public void flagOverdueShifts() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(16);
        List<AttendanceLog> overdueLogs = attendanceLogRepository.findByClockOutIsNullAndClockInBefore(threshold);
        for (AttendanceLog logEntry : overdueLogs) {
            logEntry.setFlagged(true);
            // Auto clock-out at 16 hours
            LocalDateTime clockOutTime = logEntry.getClockIn().plusHours(16);
            logEntry.setClockOut(clockOutTime);
            logEntry.setTotalHours(16.0);
            
            // Calculate overtime
            double overtime = 8.0; // 16 - 8
            
            // Apply monthly cap
            String month = logEntry.getClockIn().toLocalDate().toString().substring(0, 7);
            double accumulated = overtimeEntryRepository.getAccumulatedOvertimeHours(logEntry.getWorker().getId(), month);
            double allowedOvertime = Math.max(0.0, 60.0 - accumulated);
            double finalOvertime = Math.min(overtime, allowedOvertime);
            
            logEntry.setOvertimeHours(finalOvertime);
            attendanceLogRepository.save(logEntry);
            
            if (finalOvertime > 0.0) {
                Worker worker = logEntry.getWorker();
                BigDecimal dailyWage = worker.getDailyWageRate();
                BigDecimal hourlyRate = dailyWage.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);
                
                BigDecimal otAmount;
                if (finalOvertime <= 2.0) {
                    otAmount = hourlyRate.multiply(BigDecimal.valueOf(1.5)).multiply(BigDecimal.valueOf(finalOvertime));
                } else {
                    BigDecimal first2 = hourlyRate.multiply(BigDecimal.valueOf(1.5)).multiply(BigDecimal.valueOf(2.0));
                    BigDecimal rest = hourlyRate.multiply(BigDecimal.valueOf(2.0)).multiply(BigDecimal.valueOf(finalOvertime - 2.0));
                    otAmount = first2.add(rest);
                }
                
                OvertimeEntry otEntry = new OvertimeEntry();
                otEntry.setWorker(worker);
                otEntry.setAttendance(logEntry);
                otEntry.setDate(logEntry.getClockIn().toLocalDate());
                otEntry.setOvertimeHours(finalOvertime);
                otEntry.setOvertimeRateApplied(hourlyRate.multiply(BigDecimal.valueOf(1.5)));
                otEntry.setAmount(otAmount.setScale(2, RoundingMode.HALF_UP));
                otEntry.setSettlementStatus(SettlementStatus.PENDING);
                otEntry.setMonth(month);
                
                overtimeEntryRepository.save(otEntry);
            }
            
            removeActiveWorkerFromRedis(logEntry.getWorker().getId());
        }
    }

    private void saveActiveWorkerToRedis(Worker worker, Site site, LocalDateTime clockIn) {
        try {
            ActiveWorkerDto dto = new ActiveWorkerDto(
                    worker.getId(),
                    worker.getName(),
                    worker.getPhone(),
                    worker.getDesignation().name(),
                    worker.getDailyWageRate(),
                    site.getId(),
                    site.getSiteName(),
                    clockIn.toString()
            );
            String key = ACTIVE_WORKER_KEY_PREFIX + worker.getId();
            redisTemplate.opsForValue().set(key, dto, Duration.ofHours(16));
        } catch (Exception e) {
            log.error("Failed to save active worker {} to Redis: {}", worker.getId(), e.getMessage());
        }
    }

    private void removeActiveWorkerFromRedis(Long workerId) {
        try {
            String key = ACTIVE_WORKER_KEY_PREFIX + workerId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to remove active worker {} from Redis: {}", workerId, e.getMessage());
        }
    }

    private void invalidateActiveWorkerCache(Long workerId) {
        try {
            String key = ACTIVE_WORKER_KEY_PREFIX + workerId;
            redisTemplate.delete(key);
            log.info("Invalidated Redis cache entry for active worker {}", workerId);
        } catch (Exception e) {
            log.error("Failed to invalidate active worker cache for worker {}: {}", workerId, e.getMessage());
        }
    }
}
