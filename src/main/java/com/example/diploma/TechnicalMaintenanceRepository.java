package com.example.diploma;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TechnicalMaintenanceRepository extends JpaRepository<TechnicalMaintenance, Long> {
    List<TechnicalMaintenance> findByCar(Car car);


    List<TechnicalMaintenance> findByServiceDateBetween(LocalDate from, LocalDate to);

    List<TechnicalMaintenance> findByCarId(Long carId);


    List<TechnicalMaintenance> findByCarAndStatus(Car car, String status);
}
