package com.example.demo.attendance.entity;

import lombok.*;
import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@ToString
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "worker", indexes = {
    @Index(name = "idx_worker_phone", columnList = "phone", unique = true),
    @Index(name = "idx_worker_active", columnList = "active")
})
public class Worker {
    @Id
    @SequenceGenerator(
            name = "worker_sequence",
            sequenceName = "worker_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            generator = "worker_sequence",
            strategy = GenerationType.SEQUENCE
    )
    private Long id;

    @NotBlank(message = "Worker name cannot be blank")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Phone number cannot be blank")
    @Column(nullable = false, unique = true)
    private String phone;

    @NotNull(message = "Designation is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Designation designation;

    @NotNull(message = "Daily wage rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Daily wage rate must be greater than zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyWageRate;

    @Column(nullable = false)
    private boolean active = true;

    public Worker(String name, String phone, Designation designation, BigDecimal dailyWageRate, boolean active) {
        this.name = name;
        this.phone = phone;
        this.designation = designation;
        this.dailyWageRate = dailyWageRate;
        this.active = active;
    }
}
