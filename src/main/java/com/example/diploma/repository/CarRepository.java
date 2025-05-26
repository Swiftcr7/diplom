package com.example.diploma.repository;

import com.example.diploma.model.Car;
import com.example.diploma.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByOwner(UserInfo owner);

    @Query("""
    SELECT c FROM Car c
    WHERE c.owner = :owner
      AND c.id NOT IN (
        SELECT s.assignedVehicle.id FROM Staff s
        WHERE s.assignedVehicle IS NOT NULL
      )
""")
    List<Car> findUnassignedCarsByOwner(@Param("owner") UserInfo owner);

    @Query("SELECT c FROM Car c LEFT JOIN FETCH c.services WHERE c IN :cars")
    List<Car> fetchServicesForCars(@Param("cars") List<Car> cars);

    @Query("SELECT c FROM Car c LEFT JOIN FETCH c.refuelings WHERE c IN :cars")
    List<Car> fetchRefuelingsForCars(@Param("cars") List<Car> cars);

}
