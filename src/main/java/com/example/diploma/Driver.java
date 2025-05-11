//package com.example.diploma;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

//@Entity
//@Data
//@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
//@AllArgsConstructor
//public class Driver extends Staff {
//
//    @ManyToMany
//    @JoinTable(
//            name = "driver_car",
//            joinColumns = @JoinColumn(name = "driver_id"),
//            inverseJoinColumns = @JoinColumn(name = "car_id")
//    )
//    private List<Car> cars = new ArrayList<>();
//}
