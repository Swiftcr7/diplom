package com.example.diploma.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Entity
@Data
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@AllArgsConstructor
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String numberCar;
    private String make;
    private String model;
    private int year;
    private double mileage;
    private String fuelType;
    private LocalDate purchaseDate;
    private String status;


    private int serviceIntervalKm;
    private double lastServiceMileage;
    private LocalDate nextServiceDate;

    private double initialCost;
    private double residualValue;
    private int usefulLifeYears;
    private double depreciationRate;
    private int totalUnits;
    private int unitsUsed;

    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL)
    private List<TechnicalMaintenance> services;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserInfo owner;

    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL)
    private List<Refueling> refuelings;


}
