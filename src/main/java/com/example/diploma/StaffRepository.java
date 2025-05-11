package com.example.diploma;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findByOwner(UserInfo owner);

    @EntityGraph(attributePaths = {
            "assignedVehicle.services",
            "assignedVehicle.refuelings"
    })
    @Query("SELECT s FROM Staff s WHERE s.hireDate IS NOT NULL AND (s.dismissedDate IS NULL OR s.dismissedDate >= :from)")
    List<Staff> findAllWithExpenses(@Param("from") LocalDate from);

    @Query("SELECT s FROM Staff s LEFT JOIN FETCH s.assignedVehicle")
    List<Staff> findAllWithAssignedVehicle();
}
