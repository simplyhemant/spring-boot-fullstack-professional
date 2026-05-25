package com.example.demo.attendance.repository;

import com.example.demo.attendance.entity.AttendanceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    Optional<AttendanceLog> findFirstByWorkerIdAndClockOutIsNull(Long workerId);

    @Query(value = "SELECT a FROM AttendanceLog a JOIN FETCH a.worker JOIN FETCH a.site WHERE " +
            "(:workerId IS NULL OR a.worker.id = :workerId) AND " +
            "(:from IS NULL OR a.clockIn >= :from) AND " +
            "(:to IS NULL OR a.clockIn <= :to)",
            countQuery = "SELECT count(a) FROM AttendanceLog a WHERE " +
            "(:workerId IS NULL OR a.worker.id = :workerId) AND " +
            "(:from IS NULL OR a.clockIn >= :from) AND " +
            "(:to IS NULL OR a.clockIn <= :to)")
    Page<AttendanceLog> findLogs(@Param("workerId") Long workerId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 Pageable pageable);

    List<AttendanceLog> findByClockOutIsNullAndClockInBefore(LocalDateTime threshold);

    @Query("SELECT a FROM AttendanceLog a JOIN FETCH a.worker JOIN FETCH a.site WHERE a.clockOut IS NULL")
    List<AttendanceLog> findActiveLogs();
}
