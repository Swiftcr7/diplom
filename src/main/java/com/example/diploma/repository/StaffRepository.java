package com.example.diploma.repository;

import com.example.diploma.model.Car;
import com.example.diploma.model.Staff;
import com.example.diploma.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findByOwner(UserInfo owner);


    @Query("SELECT s FROM Staff s LEFT JOIN FETCH s.assignedVehicle")
    List<Staff> findAllWithAssignedVehicle();

    List<Staff> findByAssignedVehicle(Car car);
}
