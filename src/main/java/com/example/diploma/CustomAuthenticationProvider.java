//package com.example.diploma;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.authentication.AuthenticationProvider;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class CustomAuthenticationProvider implements AuthenticationProvider {
//
//    @Autowired
//    private CustomUserDetailsService customUserDetailsService;
//
//    @Override
//    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
//        String username = authentication.getName();
//        String password = (String) authentication.getCredentials();
//
//        // Загружаем пользователя
//        UserInfo user = (UserInfo) customUserDetailsService.loadUserByUsername(username);
//
//        // Проверка пароля
//        if (!new BCryptPasswordEncoder().matches(password, user.getPassword())) {
//            throw new BadCredentialsException("Неверный пароль");
//        }
//
//        // Возвращаем кастомный токен аутентификации с объектом UserInfo
//        CustomAuthenticationToken token = new CustomAuthenticationToken(user);
//        token.setAuthenticated(true);
//
//        return token;
//    }
//
//    @Override
//    public boolean supports(Class<?> authentication) {
//        return CustomAuthenticationToken.class.isAssignableFrom(authentication);
//    }
//}