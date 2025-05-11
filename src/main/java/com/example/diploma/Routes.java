package com.example.diploma;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Routes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Адреса и координаты ---
    private String customerName;
    private String fromAddress;
    private String toAddress;
    private boolean optimized;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "from_lat")),
            @AttributeOverride(name = "lon", column = @Column(name = "from_lon"))
    })
    private LatLng fromCoordinates;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "to_lat")),
            @AttributeOverride(name = "lon", column = @Column(name = "to_lon"))
    })
    private LatLng toCoordinates;

    @ElementCollection
    @CollectionTable(name = "route_stop_coordinates", joinColumns = @JoinColumn(name = "route_id"))
    private List<LatLng> stopCoordinates = new ArrayList<>();

    // --- Расписание перевозки ---
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "route_transport_dates", joinColumns = @JoinColumn(name = "route_id"))
    @Column(name = "transport_date")
    private List<LocalDate> transportDates = new ArrayList<>();

    private LocalTime departureTime; // Время выезда в день маршрута

    private int tripsRequired;     // Сколько рейсов нужно выполнить
    private int seatsRequired;     // Сколько мест нужно для пассажиров или груза

    // --- Расчёт маршрута ---
    private double totalDistanceMeters;
    private double totalDurationSeconds;

    // --- Пользователь-владелец ---
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserInfo owner;

    // --- Автомобиль и водитель ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    private Car assignedCar;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "route_stop_addresses", joinColumns = @JoinColumn(name = "route_id"))
    @Column(name = "stop_address")
    private List<String> stopAddresses = new ArrayList<>();

    private double optimalDistanceMeters;
    private double optimalDurationSeconds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "route_optimal_addresses", joinColumns = @JoinColumn(name = "route_id"))
    @Column(name = "optimal_address")
    private List<String> optimalAddressOrder = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "route_optimal_coordinates", joinColumns = @JoinColumn(name = "route_id"))
    private List<LatLng> optimalCoordinateOrder = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff assignedDriver;

    // --- Проверка соответствия водитель ↔ автомобиль ---
    public void validateDriverCarMatch() {
        if (assignedDriver != null && assignedCar != null) {
            Car expected = assignedDriver.getAssignedVehicle();
            if (expected == null || !expected.equals(assignedCar)) {
                throw new IllegalStateException(
                        "Назначенная машина не совпадает с машиной водителя: " +
                                assignedCar.getNumberCar() + " ≠ " +
                                (expected != null ? expected.getNumberCar() : "null")
                );
            }
        }
    }
}