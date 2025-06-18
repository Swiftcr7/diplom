package com.example.diploma.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;

@Route("login")
@Slf4j
public class LoginPage extends VerticalLayout {

    private final AuthenticationManager authenticationManager;


    public LoginPage(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;


        H1 title = new H1("Вход");


        TextField username = new TextField("Имя пользователя");
        PasswordField password = new PasswordField("Пароль");
        Button loginButton = new Button("Войти");


        loginButton.setWidth("300px");
        username.setWidth("300px");
        password.setWidth("300px");

        Span errorMessage = new Span();
        errorMessage.getStyle().set("color", "red");

        username.addValueChangeListener(event -> errorMessage.setText(""));
        password.addValueChangeListener(event -> errorMessage.setText(""));


        loginButton.addClickListener(event -> {
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                username.getValue(),
                                password.getValue()
                        )
                );
                if (authentication != null) {
                    log.info("Аутентификация пользователя: " + authentication.getName());
                } else {
                    log.warn("Аутентификация отсутствует.");
                }

                SecurityContextHolder.getContext().setAuthentication(authentication);
                VaadinSession session = VaadinSession.getCurrent();
                if (session != null) {
                    log.info("VaadinSession ID: " + session.getSession().getId());
                    Object userInfo = session.getAttribute("userInfo");
                    if (userInfo != null) {
                        log.info("User info: " + userInfo.toString());
                    } else {
                        log.warn("User info not found in VaadinSession.");
                    }
                }
                Authentication authentication2 = SecurityContextHolder.getContext().getAuthentication();
                Object userInfo = authentication2.getPrincipal();
                log.info(userInfo.toString());
                VaadinSession.getCurrent().setAttribute("userInfo", authentication.getPrincipal());
                UI.getCurrent().navigate("main");

            } catch (AuthenticationException ex) {
                errorMessage.setText("Неверный логин или пароль.");
            }
        });

        setAlignItems(Alignment.CENTER);
        setSizeFull();

        Button registerButton = new Button("Регистрация");

        registerButton.addClickListener(e -> UI.getCurrent().navigate("authorization"));

        loginButton.getStyle().set("background-color", "#007BFF");
        loginButton.getStyle().set("color", "white");

        registerButton.getStyle().set("background-color", "#007BFF");
        registerButton.getStyle().set("color", "white");

        HorizontalLayout formLayout = new HorizontalLayout(loginButton, registerButton);
        formLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        formLayout.setAlignItems(Alignment.CENTER);
        formLayout.setSpacing(true);
        loginButton.setWidth("150px");
        registerButton.setWidth("150px");
        add(title, username, password, formLayout, errorMessage);

        username.focus();
    }
}