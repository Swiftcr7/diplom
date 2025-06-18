package com.example.diploma.service;

import com.example.diploma.model.LatLng;
import com.example.diploma.model.Route;
import com.example.diploma.model.UserInfo;
import com.example.diploma.repository.RouteRepository;
import com.vaadin.flow.server.VaadinSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiece {

    private final RouteRepository routeRepository;



    public void save(Route route, boolean manualInput) throws IOException, InterruptedException {
        log.info("Режим ввода (вручную координаты): " + manualInput);
        log.info("Имя заказчика: " + route.getCustomerName());
        log.info("Адрес откуда: " + route.getFromAddress());
        log.info("Адрес куда: " + route.getToAddress());
        log.info("Остановки: " + route.getStopAddresses());

        List<double[]> rawPoints = new ArrayList<>();
        List<String> allAddresses = new ArrayList<>();


        if (!manualInput) {
            log.info("Данные введены вручную, геокодирование не требуется.");


            rawPoints.add(new double[]{route.getFromCoordinates().getLat(), route.getFromCoordinates().getLon()});
            allAddresses.add(route.getFromCoordinates().getLat() + ", " + route.getFromCoordinates().getLon());


            for (LatLng coord : route.getStopCoordinates()) {
                rawPoints.add(new double[]{coord.getLat(), coord.getLon()});
                allAddresses.add(coord.getLat() + ", " + coord.getLon());
            }


            rawPoints.add(new double[]{route.getToCoordinates().getLat(), route.getToCoordinates().getLon()});
            allAddresses.add(route.getToCoordinates().getLat() + ", " + route.getToCoordinates().getLon());

        } else {

            log.info("Выполняется геокодирование по адресам...");

            double[] from = RouteCalculator.geocode(route.getFromAddress(), route.getFromAddress(), "Москва", "Россия");
            double[] to = RouteCalculator.geocode(route.getToAddress(), route.getToAddress(), "Москва", "Россия");

            route.setFromCoordinates(new LatLng(from[0], from[1]));
            route.setToCoordinates(new LatLng(to[0], to[1]));

            rawPoints.add(from);
            allAddresses.add(route.getFromAddress());

            List<LatLng> stopCoords = new ArrayList<>();
            for (String addr : route.getStopAddresses()) {
                double[] coord = RouteCalculator.geocode(addr, addr, "Москва", "Россия");
                stopCoords.add(new LatLng(coord[0], coord[1]));
                rawPoints.add(coord);
                allAddresses.add(addr);
            }
            rawPoints.add(to);
            allAddresses.add(route.getToAddress());
            route.setStopCoordinates(stopCoords);
        }


        log.info("Выполняется расчёт маршрута в порядке ввода...");
        RouteCalculator.RouteInfo infoNoOptimized = RouteCalculator.getOrderedRouteInfo(allAddresses, "Москва", "Россия");
        route.setTotalDistanceMeters(infoNoOptimized.distanceMeters);
        route.setTotalDurationSeconds(infoNoOptimized.durationSeconds);

        log.info("Неоптимизированный маршрут:");
        log.info("Дистанция: " + infoNoOptimized.distanceMeters + " м");
        log.info("Время в пути: " + infoNoOptimized.durationSeconds + " сек");


        log.info("Выполняется расчёт оптимизированного маршрута...");
        RouteCalculator.OptimizedRouteInfo optimizedRoute = RouteCalculator.getOptimizedRoute(allAddresses, "Москва", "Россия");

        route.setOptimalDistanceMeters(optimizedRoute.totalDistanceMeters);
        route.setOptimalDurationSeconds(optimizedRoute.totalDurationSeconds);

        List<String> optimalAddresses = new ArrayList<>();
        List<LatLng> optimalCoordinates = new ArrayList<>();

        for (int index : optimizedRoute.waypointOrder) {
            if (index == 0) {
                optimalAddresses.add(route.getFromAddress());
                optimalCoordinates.add(route.getFromCoordinates());
            } else if (index == rawPoints.size() - 1) {
                optimalAddresses.add(route.getToAddress());
                optimalCoordinates.add(route.getToCoordinates());
            } else {
                optimalAddresses.add(route.getStopAddresses().get(index - 1));
                optimalCoordinates.add(route.getStopCoordinates().get(index - 1));
            }
        }

        route.setOptimalAddressOrder(optimalAddresses);
        route.setOptimalCoordinateOrder(optimalCoordinates);

        log.info("Оптимизированный маршрут:");
        for (int i = 0; i < optimalAddresses.size(); i++) {
            log.info((i + 1) + ") " + optimalAddresses.get(i));
        }
        log.info("Общая дистанция (м): " + optimizedRoute.totalDistanceMeters);
        log.info("Общее время (сек): " + optimizedRoute.totalDurationSeconds);

        routeRepository.save(route);
        log.info("Маршрут успешно сохранён!");
    }


    public List<Route> findByCurrentUser() {
        UserInfo user = (UserInfo) VaadinSession.getCurrent().getAttribute("userInfo");
        return routeRepository.findAllByOwner(user);
    }







}
