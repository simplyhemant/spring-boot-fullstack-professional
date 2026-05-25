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
            "(coalesce(:workerId, a.worker.id) = a.worker.id) AND " +
            "(coalesce(:from, a.clockIn) <= a.clockIn) AND " +
            "(coalesce(:to, a.clockIn) >= a.clockIn)",
            countQuery = "SELECT count(a) FROM AttendanceLog a WHERE " +
            "(coalesce(:workerId, a.worker.id) = a.worker.id) AND " +
            "(coalesce(:from, a.clockIn) <= a.clockIn) AND " +
            "(coalesce(:to, a.clockIn) >= a.clockIn)")
    Page<AttendanceLog> findLogs(@Param("workerId") Long workerId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 Pageable pageable);


    List<AttendanceLog> findByClockOutIsNullAndClockInBefore(LocalDateTime threshold);

    @Query("SELECT a FROM AttendanceLog a JOIN FETCH a.worker JOIN FETCH a.site WHERE a.clockOut IS NULL")
    List<AttendanceLog> findActiveLogs();
}
