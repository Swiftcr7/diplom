package com.example.diploma.repository;

import com.example.diploma.model.Route;
import com.example.diploma.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findAllByOwner(UserInfo owner);
}
