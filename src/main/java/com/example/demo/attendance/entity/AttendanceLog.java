package com.example.demo.attendance.entity;

import lombok.*;
import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

@ToString
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_log", indexes = {
    @Index(name = "idx_attendance_worker_clockin", columnList = "worker_id, clock_in"),
    @Index(name = "idx_attendance_worker_clockout", columnList = "worker_id, clock_out"),
    @Index(name = "idx_attendance_site", columnList = "site_id"),
    @Index(name = "idx_attendance_clock_in", columnList = "clock_in")
})
public class AttendanceLog {
    @Id
    @SequenceGenerator(
            name = "attendance_log_sequence",
            sequenceName = "attendance_log_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            generator = "attendance_log_sequence",
            strategy = GenerationType.SEQUENCE
    )
    private Long id;

    @NotNull(message = "Worker is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @NotNull(message = "Site is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @NotNull(message = "Clock-in time is required")
    @Column(name = "clock_in", nullable = false)
    private LocalDateTime clockIn;

    @Column(name = "clock_out")
    private LocalDateTime clockOut;

    @Column(name = "total_hours")
    private Double totalHours;

    @Column(name = "overtime_hours")
    private Double overtimeHours;

    @Column(nullable = false)
    private boolean flagged = false;

    public AttendanceLog(Worker worker, Site site, LocalDateTime clockIn) {
        this.worker = worker;
        this.site = site;
        this.clockIn = clockIn;
    }
}
