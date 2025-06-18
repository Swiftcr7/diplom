package com.example.diploma.view;
import com.example.diploma.service.CarParkServiece;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import com.vaadin.flow.component.button.Button;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.crypto.password.PasswordEncoder;


@Slf4j
@Route("authorization")
public class AuthorizationPage extends VerticalLayout {
    private final CarParkServiece server;

    private final AuthenticationManager authenticationManager;
    @Autowired
    public AuthorizationPage(CarParkServiece server, AuthenticationManager authenticationManager){
        this.server = server;

        this.authenticationManager = authenticationManager;

        H1 h1 = new H1("Регистрация");
        TextField usernameField = new TextField("Username");
        PasswordField passwordField = new PasswordField("Password");
        PasswordField confirmPasswordField = new PasswordField("Confirm Password");
        Button registerButton = new Button("Register");

        usernameField.setWidth("300px");
        passwordField.setWidth("300px");
        confirmPasswordField.setWidth("300px");
        registerButton.setWidth("300px");



        add(h1, usernameField, passwordField, confirmPasswordField, registerButton);


        setAlignItems(Alignment.CENTER);

        setSizeFull();

        registerButton.getStyle().set("background-color", "#007BFF");
        registerButton.getStyle().set("color", "white");


        registerButton.addClickListener(event -> {
            String username = usernameField.getValue();
            String password = passwordField.getValue();
            String confirmPassword = confirmPasswordField.getValue();

            if (username.isEmpty()){
                Notification.show("Ошибка авторизации: введите не пустое имя");
                return;
            }

            if (password.isEmpty()){
                Notification.show("Ошибка авторизации: введите не пустое поле пароля");
                return;
            }

            if (password.length() < 8){
                Notification.show("Ошибка авторизации: пароль должен быть больше 7 символов");
                return;
            }

            if (password.length() > 72){
                Notification.show("Ошибка авторизации: пароль должен быть меньше 72 символов");
                return;
            }

            if (!password.equals(confirmPassword)) {
                Notification.show("Ошибка авторизации: пароль не совпадает");
                return;
            }


            boolean registered = server.registerUser(username, password);
            if (registered) {

                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(username, password)
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("User registered and authenticated: " + username);

                Notification.show("Пользователь успешно зарегистрирован");
                UI.getCurrent().navigate("login");
            } else {
                Notification.show("Ошибка авторизации: пользователь с таким именем уже существует");
            }
        });
        log.info("chpo");

    }
}
