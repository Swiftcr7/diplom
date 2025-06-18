package com.example.diploma.service;


import com.example.diploma.model.*;
import com.example.diploma.repository.*;
import com.vaadin.flow.server.VaadinSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffServiece {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final ShiftRepository shiftRepository;
    private final MileageLogRepository mileageLogRepository;

    public List<Staff> getAllStaffForUser() {
        UserInfo user = (UserInfo) VaadinSession.getCurrent().getAttribute("userInfo");
        return staffRepository.findByOwner(user);
    }


    public void save(Staff staff) {

        if (staff.getOwner() == null) {

            UserInfo user = (UserInfo) VaadinSession.getCurrent().getAttribute("userInfo");
            staff.setOwner(user);
        }

        if (staff.getAssignedVehicle() != null && staff.getAssignedVehicle().getId() != null) {
            Long carId = staff.getAssignedVehicle().getId();
            Car car = carRepository.findById(carId).orElse(null);
            staff.setAssignedVehicle(car);
        }

        staffRepository.save(staff);
    }



    public List<Car> getAvailableCarsForUser() {
        UserInfo user = (UserInfo) VaadinSession.getCurrent().getAttribute("userInfo");
        return carRepository.findUnassignedCarsByOwner(user);
    }

    public Shift saveShift(Shift shift) {
        return shiftRepository.save(shift);
    }

    public void saveMileage(MileageLog log) {
        mileageLogRepository.save(log);
    }


    public List<Shift> getShiftsByStaff(Staff staff) {
        return shiftRepository.findByStaffId(staff.getId());
    }

    public Map<String, Double> aggregateSalaryByMode(Staff staff, String mode, LocalDate start, LocalDate end) {
        List<Shift> shifts = getShiftsByStaff(staff);
        BigDecimal rate = staff.getHourlyRate();
        Map<String, Double> result = new LinkedHashMap<>();

        for (Shift shift : shifts) {
            LocalDate date = shift.getStartTime().toLocalDate();
            if ((start != null && date.isBefore(start)) || (end != null && date.isAfter(end))) {
                continue;
            }

            long hours = Duration.between(shift.getStartTime(), shift.getEndTime()).toHours();
            double salary = hours * rate.doubleValue();

            String key;
            switch (mode) {
                case "По неделям" -> {
                    WeekFields weekFields = WeekFields.of(Locale.getDefault());
                    int week = date.get(weekFields.weekOfWeekBasedYear());
                    key = date.getYear() + "-W" + week;
                }
                case "По месяцам" -> key = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                default -> key = date.toString();
            }

            result.merge(key, salary, Double::sum);
        }

        return result;
    }

    public Map<String, Pair<Double, Double>> getWorkVsSalary(Staff staff, String mode, String salaryMode, LocalDate start, LocalDate end) {
        List<Shift> shifts = shiftRepository.findByStaffId(staff.getId()).stream()
                .filter(s -> {
                    LocalDate date = s.getStartTime().toLocalDate();
                    return (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end));
                })
                .collect(Collectors.toList());

        Map<String, Pair<Double, Double>> result = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Shift shift : shifts) {
            LocalDate date = shift.getStartTime().toLocalDate();
            String key = formatter.format(date);
            YearMonth ym = YearMonth.from(date);

            double salary;
            if ("HOURLY".equalsIgnoreCase(salaryMode)) {
                double hours = Duration.between(shift.getStartTime(), shift.getEndTime()).toHours();
                salary = hours * staff.getHourlyRate().doubleValue();
            } else if ("FIXED".equalsIgnoreCase(salaryMode)) {
                int daysInMonth = ym.lengthOfMonth();
                salary = staff.getFixedSalary().doubleValue() / daysInMonth;
            } else {
                salary = 0;
            }

            if ("shifts".equals(mode)) {
                result.merge(key, Pair.of(1.0, salary), (oldVal, newVal) ->
                        Pair.of(oldVal.getLeft() + newVal.getLeft(), oldVal.getRight() + newVal.getRight()));

            } else if ("distance".equals(mode)) {
                double distance = shift.getMileageLogs().stream()
                        .mapToDouble(MileageLog::getKilometers)
                        .sum();

                result.merge(key, Pair.of(distance, salary), (oldVal, newVal) ->
                        Pair.of(oldVal.getLeft() + newVal.getLeft(), oldVal.getRight() + newVal.getRight()));
            }
        }

        return result;
    }

    public Map<YearMonth, BigDecimal> calculateMonthlySalaryExpenses(LocalDate start, LocalDate end) {
        List<Staff> staffList = staffRepository.findAll().stream()
                .filter(s -> s.getHireDate() != null)
                .toList();

        Map<YearMonth, BigDecimal> result = new TreeMap<>();

        LocalDate from = (start != null) ? start : LocalDate.now().minusMonths(12);
        LocalDate to = (end != null) ? end : LocalDate.now();

        List<YearMonth> months = from.datesUntil(to.plusDays(1))
                .map(YearMonth::from)
                .distinct()
                .sorted()
                .toList();

        for (Staff staff : staffList) {
            Map<YearMonth, List<Shift>> shiftMap = shiftRepository.findByStaffId(staff.getId()).stream()
                    .filter(s -> {
                        LocalDate date = s.getStartTime().toLocalDate();
                        return !date.isBefore(from) && !date.isAfter(to);
                    })
                    .collect(Collectors.groupingBy(s -> YearMonth.from(s.getStartTime())));

            for (YearMonth ym : months) {
                LocalDate firstOfMonth = ym.atDay(1);

                if (staff.getHireDate().isAfter(firstOfMonth)) {
                    continue;
                }
                if (staff.getDismissedDate() != null && staff.getDismissedDate().isBefore(firstOfMonth)) {
                    continue;
                }

                BigDecimal total = BigDecimal.ZERO;

                if (staff.getPaymentType() == PaymentType.FIXED && staff.getFixedSalary() != null) {
                    total = staff.getFixedSalary();
                } else if (staff.getPaymentType() == PaymentType.HOURLY && staff.getHourlyRate() != null) {
                    List<Shift> shifts = shiftMap.getOrDefault(ym, List.of());
                    for (Shift shift : shifts) {
                        long hours = Duration.between(shift.getStartTime(), shift.getEndTime()).toHours();
                        total = total.add(staff.getHourlyRate().multiply(BigDecimal.valueOf(hours)));
                    }
                }

                result.merge(ym, total, BigDecimal::add);
            }
        }

        return result;
    }

    public Map<Staff, BigDecimal[]> getStaffExpenses(LocalDate from, LocalDate to) {
        List<Staff> staffList = staffRepository.findAllWithAssignedVehicle().stream()
                .filter(s -> s.getHireDate() != null)
                .filter(s -> s.getDismissedDate() == null || !s.getDismissedDate().isBefore(from))
                .toList();


        List<Car> assignedCars = staffList.stream()
                .map(Staff::getAssignedVehicle)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Car> carsWithServices = carRepository.fetchServicesForCars(assignedCars);
        List<Car> carsWithRefuelings = carRepository.fetchRefuelingsForCars(assignedCars);

        Map<Long, Car> servicesMap = carsWithServices.stream().collect(Collectors.toMap(Car::getId, c -> c));
        Map<Long, Car> refuelingsMap = carsWithRefuelings.stream().collect(Collectors.toMap(Car::getId, c -> c));

        Map<Staff, BigDecimal[]> result = new LinkedHashMap<>();

        for (Staff staff : staffList) {
            LocalDate hireDate = staff.getHireDate();
            LocalDate dismissedDate = staff.getDismissedDate();

            LocalDate effectiveFrom = from.isBefore(hireDate) ? hireDate : from;
            LocalDate effectiveTo = (dismissedDate != null && dismissedDate.isBefore(to)) ? dismissedDate : to;


            BigDecimal salaryTotal = BigDecimal.ZERO;
            if (staff.getPaymentType() == PaymentType.FIXED && staff.getFixedSalary() != null) {
                List<YearMonth> months = effectiveFrom.datesUntil(effectiveTo.plusDays(1))
                        .map(YearMonth::from).distinct().toList();
                salaryTotal = staff.getFixedSalary().multiply(BigDecimal.valueOf(months.size()));
            } else if (staff.getPaymentType() == PaymentType.HOURLY && staff.getHourlyRate() != null) {
                List<Shift> shifts = shiftRepository.findByStaffId(staff.getId()).stream()
                        .filter(s -> !s.getStartTime().toLocalDate().isBefore(effectiveFrom)
                                && !s.getEndTime().toLocalDate().isAfter(effectiveTo))
                        .toList();
                for (Shift shift : shifts) {
                    long hours = Duration.between(shift.getStartTime(), shift.getEndTime()).toHours();
                    salaryTotal = salaryTotal.add(staff.getHourlyRate().multiply(BigDecimal.valueOf(hours)));
                }
            }


            BigDecimal maintenanceTotal = BigDecimal.ZERO;
            BigDecimal fuelTotal = BigDecimal.ZERO;

            Car car = staff.getAssignedVehicle();
            if (car != null) {
                Car carWithServices = servicesMap.get(car.getId());
                if (carWithServices != null) {
                    maintenanceTotal = carWithServices.getServices().stream()
                            .filter(m -> m.getServiceDate() != null
                                    && !m.getServiceDate().isBefore(effectiveFrom)
                                    && !m.getServiceDate().isAfter(effectiveTo))
                            .map(tm -> tm.getCost() != null ? BigDecimal.valueOf(tm.getCost()) : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }

                Car carWithRefuelings = refuelingsMap.get(car.getId());
                if (carWithRefuelings != null) {
                    fuelTotal = carWithRefuelings.getRefuelings().stream()
                            .filter(r -> r.getRefuelDate() != null
                                    && !r.getRefuelDate().isBefore(effectiveFrom)
                                    && !r.getRefuelDate().isAfter(effectiveTo))
                            .map(r -> BigDecimal.valueOf(r.getRefuelCost()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
            }

            result.put(staff, new BigDecimal[]{salaryTotal, maintenanceTotal, fuelTotal});
        }

        return result;
    }

}