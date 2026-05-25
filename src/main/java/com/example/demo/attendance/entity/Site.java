package com.example.demo.attendance.entity;

import lombok.*;
import javax.persistence.*;
import javax.validation.constraints.*;

@ToString
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "site", indexes = {
    @Index(name = "idx_site_active", columnList = "active")
})
public class Site {
    @Id
    @SequenceGenerator(
            name = "site_sequence",
            sequenceName = "site_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            generator = "site_sequence",
            strategy = GenerationType.SEQUENCE
    )
    private Long id;

    @NotBlank(message = "Site name cannot be blank")
    @Column(nullable = false)
    private String siteName;

    @NotBlank(message = "Location cannot be blank")
    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private boolean active = true;

    public Site(String siteName, String location, boolean active) {
        this.siteName = siteName;
        this.location = location;
        this.active = active;
    }
}
