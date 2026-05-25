package com.example.demo.attendance.repository;

import com.example.demo.attendance.entity.OvertimeEntry;
import com.example.demo.attendance.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OvertimeEntryRepository extends JpaRepository<OvertimeEntry, Long> {

    @Query("SELECT COALESCE(SUM(o.overtimeHours), 0.0) FROM OvertimeEntry o WHERE o.worker.id = :workerId AND o.month = :month")
    double getAccumulatedOvertimeHours(@Param("workerId") Long workerId, @Param("month") String month);

    @Query("SELECT o FROM OvertimeEntry o JOIN FETCH o.worker JOIN FETCH o.attendance WHERE o.worker.id = :workerId AND o.month = :month")
    List<OvertimeEntry> findByWorkerIdAndMonth(@Param("workerId") Long workerId, @Param("month") String month);

    @Query("SELECT o FROM OvertimeEntry o WHERE o.worker.id = :workerId AND o.month = :month AND o.settlementStatus = :status")
    List<OvertimeEntry> findByWorkerIdAndMonthAndSettlementStatus(@Param("workerId") Long workerId, @Param("month") String month, @Param("status") SettlementStatus status);
}
