package com.example.diploma;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;


import java.util.List;
import java.util.Objects;

@Slf4j
@PreAuthorize("hasRole('USER')")
//@CssImport(value = "./car-park.css")
@JavaScript("https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js")
@Route("routing")
public class RoutingPage extends VerticalLayout {
    @Autowired
    private final RouteService routeService;

    @Autowired
    private final UserRepository userRepository;

    private final Grid<Routes> grid = new Grid<>(Routes.class, false);

    public RoutingPage(RouteService routeService, UserRepository userRepository) {
        this.routeService = routeService;
        this.userRepository = userRepository;

        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Маршруты");

        Button addRouteBtn = new Button("Добавить маршрут");
        addRouteBtn.addClickListener(e -> openRouteDialog());

        configureGrid();

        add(title, addRouteBtn, grid);
        refreshRoutes();
    }


    private void configureGrid() {
        grid.addColumn(Routes::getFromAddress).setHeader("Откуда");
        grid.addColumn(Routes::getToAddress).setHeader("Куда");
        grid.addColumn(r -> r.getTransportDates().size()).setHeader("Дней");
        grid.addColumn(Routes::getDepartureTime).setHeader("Отправление");
        grid.addColumn(Routes::getSeatsRequired).setHeader("Мест");
        grid.addColumn(Routes::getTripsRequired).setHeader("Рейсов");
        grid.addComponentColumn(route -> {
            Button detailsButton = new Button("Подробнее");
            detailsButton.addClickListener(event -> openDetailsDialog(route));
            return detailsButton;
        }).setHeader("Детали");
        grid.setWidthFull();
    }

    private void refreshRoutes() {
        List<Routes> routes = routeService.findByCurrentUser();
        grid.setItems(routes);
    }

    private void openDetailsDialog(Routes route) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Детальная информация");

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);

        // Основная информация
        layout.add(new H3("Основная информация"));
        layout.add(new Span("Имя заказчика: " + route.getCustomerName()));
        layout.add(new Span("Адрес отправления: " + route.getFromAddress()));
        layout.add(new Span("Адрес назначения: " + route.getToAddress()));

        // Основные даты
        layout.add(new H3("Даты перевозок"));
        route.getTransportDates().forEach(date -> {
            layout.add(new Span("Дата: " + date.toString()));
        });

        // Неоптимизированный маршрут
        layout.add(new H3("Неоптимизированный маршрут"));
        layout.add(new Span("Общая дистанция: " + route.getTotalDistanceMeters() / 1000 + " км"));
        layout.add(new Span("Общее время в пути: " + (route.getTotalDurationSeconds() / 60) + " мин"));
        layout.add(new H4("Порядок следования:"));
        layout.add(new Span(route.getFromAddress()));
        route.getStopAddresses().forEach(addr -> layout.add(new Span(" → " + addr)));
        layout.add(new Span(" → " + route.getToAddress()));

        // Оптимизированный маршрут
        layout.add(new H3("Оптимизированный маршрут"));
        layout.add(new Span("Общая дистанция: " + route.getOptimalDistanceMeters() / 1000 + " км"));
        layout.add(new Span("Общее время в пути: " + (route.getOptimalDurationSeconds() / 60) + " мин"));
        layout.add(new H4("Порядок следования:"));
        route.getOptimalAddressOrder().forEach(addr -> layout.add(new Span(" → " + addr)));

        // Кнопка закрытия
        Button closeButton = new Button("Закрыть", event -> dialog.close());
        layout.add(closeButton);

        dialog.add(layout);
        dialog.setWidth("600px");
        dialog.setHeight("600px");
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.open();
    }

    private void openRouteDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Добавление маршрута");

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setPadding(true);
        formLayout.setSpacing(true);
        formLayout.setWidthFull();

        RadioButtonGroup<String> inputMode = new RadioButtonGroup<>();
        inputMode.setLabel("Режим ввода");
        inputMode.setItems("По адресу", "По координатам");
        inputMode.setValue("По адресу");

        TextField customerName = new TextField("Имя заказчика");
        customerName.setWidthFull();

        TextField fromField = new TextField("Откуда (адрес)");
        fromField.setWidthFull();
        TextField toField = new TextField("Куда (адрес)");
        toField.setWidthFull();

        VerticalLayout stopsLayout = new VerticalLayout();
        Button addStopBtn = new Button("Добавить остановку", e -> {
            TextField stop = new TextField("Адрес остановки");
            stop.setWidthFull();
            stopsLayout.add(stop);
        });

        VerticalLayout addressInputLayout = new VerticalLayout(fromField, toField, addStopBtn, stopsLayout);
        addressInputLayout.setPadding(false);

        NumberField fromLat = new NumberField("Широта (откуда)");
        NumberField fromLon = new NumberField("Долгота (откуда)");
        NumberField toLat = new NumberField("Широта (куда)");
        NumberField toLon = new NumberField("Долгота (куда)");

        HorizontalLayout fromCoordsLayout = new HorizontalLayout(fromLat, fromLon);
        HorizontalLayout toCoordsLayout = new HorizontalLayout(toLat, toLon);

        VerticalLayout stopCoordsLayout = new VerticalLayout();
        Button addStopCoordsBtn = new Button("Добавить координаты остановки", e -> {
            NumberField lat = new NumberField("Широта");
            NumberField lon = new NumberField("Долгота");
            HorizontalLayout row = new HorizontalLayout(lat, lon);
            stopCoordsLayout.add(row);
        });

        VerticalLayout coordsInputLayout = new VerticalLayout(fromCoordsLayout, toCoordsLayout, addStopCoordsBtn, stopCoordsLayout);
        coordsInputLayout.setVisible(false);

        inputMode.addValueChangeListener(event -> {
            boolean coords = "По координатам".equals(event.getValue());
            addressInputLayout.setVisible(!coords);
            coordsInputLayout.setVisible(coords);
            addStopBtn.setVisible(!coords);
            addStopCoordsBtn.setVisible(coords);
        });

        Checkbox optimizeCheckbox = new Checkbox("Оптимизировать маршрут");

        IntegerField trips = new IntegerField("Кол-во рейсов");
        trips.setMin(1);
        VerticalLayout datesLayout = new VerticalLayout();
        trips.addValueChangeListener(event -> {
            datesLayout.removeAll();
            Integer count = event.getValue();
            if (count != null && count > 0) {
                for (int i = 0; i < count; i++) {
                    datesLayout.add(new DatePicker("Дата перевозки " + (i + 1)));
                }
            }
        });

        TimePicker timePicker = new TimePicker("Время отправления");
        IntegerField seats = new IntegerField("Кол-во мест");
        seats.setMin(1);

        Button saveBtn = new Button("Сохранить", e -> {
            try {
                Routes route = new Routes();
                UserInfo userInfo = (UserInfo) VaadinSession.getCurrent().getAttribute("userInfo");
                Long id = userInfo.getId();
                UserInfo user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
                route.setOwner(user);
                route.setCustomerName(customerName.getValue());
                route.setOptimized(optimizeCheckbox.getValue());
                route.setDepartureTime(timePicker.getValue());
                route.setTripsRequired(trips.getValue() != null ? trips.getValue() : 1);
                route.setSeatsRequired(seats.getValue() != null ? seats.getValue() : 1);
                route.setTransportDates(
                        datesLayout.getChildren()
                                .filter(DatePicker.class::isInstance)
                                .map(DatePicker.class::cast)
                                .map(DatePicker::getValue)
                                .filter(Objects::nonNull)
                                .toList()
                );

                boolean manualInput = "По координатам".equals(inputMode.getValue());

                if (manualInput) {
                    route.setFromCoordinates(new LatLng(fromLat.getValue(), fromLon.getValue()));
                    route.setToCoordinates(new LatLng(toLat.getValue(), toLon.getValue()));
                    route.setFromAddress(fromLat.getValue() + ", " + fromLon.getValue());
                    route.setToAddress(toLat.getValue() + ", " + toLon.getValue());
                    route.setStopCoordinates(
                            stopCoordsLayout.getChildren()
                                    .filter(HorizontalLayout.class::isInstance)
                                    .map(HorizontalLayout.class::cast)
                                    .map(row -> {
                                        NumberField lat = (NumberField) row.getComponentAt(0);
                                        NumberField lon = (NumberField) row.getComponentAt(1);
                                        return new LatLng(lat.getValue(), lon.getValue());
                                    }).toList()
                    );
                    route.setStopAddresses(
                            route.getStopCoordinates().stream()
                                    .map(c -> c.getLat() + ", " + c.getLon())
                                    .toList()
                    );
                } else {
                    route.setFromAddress(fromField.getValue());
                    route.setToAddress(toField.getValue());
                    route.setStopAddresses(
                            stopsLayout.getChildren()
                                    .filter(TextField.class::isInstance)
                                    .map(TextField.class::cast)
                                    .map(TextField::getValue)
                                    .filter(s -> s != null && !s.isBlank())
                                    .toList()
                    );
                }

                routeService.save(route, !manualInput);
                dialog.close();
                refreshRoutes();
                Notification.show("Маршрут сохранён");
            } catch (Exception ex) {
                Notification.show("Ошибка: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        Button cancelBtn = new Button("Отмена", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        formLayout.add(
                inputMode,
                customerName,
                addressInputLayout,
                coordsInputLayout,
                optimizeCheckbox,
                trips,
                datesLayout,
                timePicker,
                seats
        );

        dialog.add(formLayout, new HorizontalLayout(cancelBtn, saveBtn));
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.open();
    }





}
