package com.example.diploma;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MileageLogRepository extends JpaRepository<MileageLog, Long> {
    List<MileageLog> findByCar(Car car);

    List<MileageLog> findByCarId(Long carId);
}