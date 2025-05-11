//package com.example.diploma;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//@Service
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final UserRepository userRepository; // Репозиторий для работы с пользователями
//
//    @Autowired
//    public CustomUserDetailsService(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        // Ищем пользователя по имени
//        UserInfo user = userRepository.findByUsername(username);
//
//        if (user == null) {
//            throw new UsernameNotFoundException("Пользователь не найден: " + username);
//        }
//
//        // Возвращаем объект UserDetails (мы возвращаем сам объект UserInfo, потому что он реализует UserDetails)
//        return user;
//    }
//}
