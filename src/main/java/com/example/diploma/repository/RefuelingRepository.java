package com.example.diploma.repository;

import com.example.diploma.model.Refueling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefuelingRepository extends JpaRepository<Refueling, Long> {
    Optional<Refueling> findTopByCarIdOrderByRefuelDateDesc(Long carId);

    List<Refueling> findByCarId(Long carId);

}
