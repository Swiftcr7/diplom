package com.example.diploma;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByOwner(UserInfo owner);


    List<Car> findByStatus(String status);

    List<Refueling> findRefuelingsById(Long carId);

    @Query("""
    SELECT c FROM Car c
    WHERE c.owner = :owner
      AND c.id NOT IN (
        SELECT s.assignedVehicle.id FROM Staff s
        WHERE s.assignedVehicle IS NOT NULL
      )
""")
    List<Car> findUnassignedCarsByOwner(@Param("owner") UserInfo owner);

//    @EntityGraph(attributePaths = {"services"})
//    Optional<Car> findByIdWithServices(Long id);

//    @EntityGraph(attributePaths = {"services"})
//    @Query("SELECT c FROM Car c WHERE c.id = :id")
//    Optional<Car> findByIdWithServices(@Param("id") Long id);
    @Query("SELECT c FROM Car c LEFT JOIN FETCH c.services WHERE c IN :cars")
    List<Car> fetchServicesForCars(@Param("cars") List<Car> cars);

    // 🔄 Загрузка всех заправок для списка автомобилей
    @Query("SELECT c FROM Car c LEFT JOIN FETCH c.refuelings WHERE c IN :cars")
    List<Car> fetchRefuelingsForCars(@Param("cars") List<Car> cars);

}
