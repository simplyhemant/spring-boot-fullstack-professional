package com.example.demo.attendance.entity;

import lombok.*;
import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@ToString
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "overtime_entry", indexes = {
    @Index(name = "idx_overtime_worker_month", columnList = "worker_id, month"),
    @Index(name = "idx_overtime_worker_status", columnList = "worker_id, settlement_status"),
    @Index(name = "idx_overtime_attendance", columnList = "attendance_id", unique = true)
})
public class OvertimeEntry {
    @Id
    @SequenceGenerator(
            name = "overtime_entry_sequence",
            sequenceName = "overtime_entry_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            generator = "overtime_entry_sequence",
            strategy = GenerationType.SEQUENCE
    )
    private Long id;

    @NotNull(message = "Worker is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @NotNull(message = "Attendance log reference is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_id", nullable = false, unique = true)
    private AttendanceLog attendance;

    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;

    @NotNull(message = "Overtime hours is required")
    @Column(name = "overtime_hours", nullable = false)
    private Double overtimeHours;

    @NotNull(message = "Overtime rate applied is required")
    @Column(name = "overtime_rate_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal overtimeRateApplied;

    @NotNull(message = "Amount is required")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Settlement status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false)
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    @NotBlank(message = "Month is required")
    @Column(name = "month", nullable = false, length = 7)
    private String month; // Format YYYY-MM
}
