package com.example.diploma.model;

import com.example.diploma.model.Car;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@AllArgsConstructor
public class Refueling {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate refuelDate;
    private double fuelQuantity;
    private double refuelCost;

    @ManyToOne
    @JoinColumn(name = "car_id")
    private Car car;

    @Override
    public String toString() {
        return "Refueling{" +
                "id=" + id +
                ", refuelDate=" + refuelDate +
                ", fuelQuantity=" + fuelQuantity +
                ", refuelCost=" + refuelCost +
                '}';
    }
}
