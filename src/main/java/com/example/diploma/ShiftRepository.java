package com.example.diploma;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
//    List<Shift> findByStaffId(Long staffId);

    @EntityGraph(attributePaths = "mileageLogs")
    List<Shift> findByStaffId(Long staffId);

    List<Shift> findByStaff(Staff staff);
}
