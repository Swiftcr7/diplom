package com.example.diploma.model;

import com.example.diploma.model.Car;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Slf4j
@Entity
@Data
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@AllArgsConstructor
public class TechnicalMaintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate serviceDate;
    private String description;
    private Double cost;
    private String serviceProvider;
    private String status;

    @ManyToOne
    @JoinColumn(name = "car_id")
    private Car car;
}
