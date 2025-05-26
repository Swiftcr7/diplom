package com.example.diploma.repository;

import com.example.diploma.model.Routes;
import com.example.diploma.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Routes, Long> {
    List<Routes> findAllByOwner(UserInfo owner);
}
