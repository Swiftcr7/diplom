package com.example.diploma.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;


@Slf4j
@Route("main")
public class mainPage extends VerticalLayout {
    public mainPage(){
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UI.getCurrent().navigate("login");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            log.info("Аутентификация пользователя: " + authentication.getName());
        } else {
            log.warn("Аутентификация отсутствует.");
        }

        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            log.info("VaadinSession ID: " + session.getSession().getId());
            Object userInfo = session.getAttribute("userInfo");
            if (userInfo != null) {
                log.info("User info: " + userInfo.toString());
            } else {
                log.warn("User info не найдено в VaadinSession.");
            }
        }


        H1 header = new H1("Главная страница");
        Authentication authentication2 = SecurityContextHolder.getContext().getAuthentication();
        Object userInfo = authentication2.getPrincipal();
        log.info(userInfo.toString() + "main page");
        Button autopark = new Button("Автопарк");
        autopark.addClickListener(event -> UI.getCurrent().navigate("car_park"));

        Button staff = new Button("Сотрудники");
        staff.addClickListener(event -> UI.getCurrent().navigate("staff"));

        Button routing = new Button("Маршруты");
        routing.addClickListener(event -> UI.getCurrent().navigate("routing"));

        autopark.setWidth("200px");
        staff.setWidth("200px");
        routing.setWidth("200px");

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        add(header, autopark, staff, routing);
    }
}
