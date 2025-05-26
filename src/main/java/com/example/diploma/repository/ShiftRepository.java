package com.example.diploma.repository;

import com.example.diploma.model.Shift;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    @EntityGraph(attributePaths = "mileageLogs")
    List<Shift> findByStaffId(Long staffId);

}
