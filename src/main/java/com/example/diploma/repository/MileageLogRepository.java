package com.example.diploma.repository;

import com.example.diploma.model.Car;
import com.example.diploma.model.MileageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MileageLogRepository extends JpaRepository<MileageLog, Long> {
    List<MileageLog> findByCar(Car car);

    List<MileageLog> findByCarId(Long carId);

}