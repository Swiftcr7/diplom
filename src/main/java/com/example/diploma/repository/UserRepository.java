package com.example.diploma.repository;

import com.example.diploma.model.UserInfo;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserInfo, Long> {
    UserInfo findByUsername(String Username);
}
