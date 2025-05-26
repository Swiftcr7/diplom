package com.example.diploma.repository;

import com.example.diploma.model.TechnicalMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnicalMaintenanceRepository extends JpaRepository<TechnicalMaintenance, Long> {

    List<TechnicalMaintenance> findByCarId(Long carId);


}
