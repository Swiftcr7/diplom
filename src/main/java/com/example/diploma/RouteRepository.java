package com.example.diploma;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Routes, Long> {
    List<Routes> findAllByOwner(UserInfo owner);
}
