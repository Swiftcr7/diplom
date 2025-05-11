package com.example.diploma;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ManyToOne
    private Staff staff;

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL)
    private List<MileageLog> mileageLogs;

    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }
}