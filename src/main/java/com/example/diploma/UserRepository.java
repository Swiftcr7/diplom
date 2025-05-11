package com.example.diploma;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserInfo, Long> {
    UserInfo findByUsername(String Username);
}
