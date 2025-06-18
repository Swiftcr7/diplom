package com.example.diploma.service;

import com.example.diploma.model.*;
import com.example.diploma.repository.*;
import com.vaadin.flow.server.VaadinSession;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CarParkServiece {

    @Autowired
    private MileageLogRepository mileageLogRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private UserRepository userRepository;

   @Autowired
   private PasswordEncoder passwordEncoder;

   @Autowired
   private TechnicalMaintenanceRepository technicalMaintenanceRepository;

   @Autowired
   private RefuelingRepository refuelingRepository;



    public boolean registerUser(String username, String password) {
        log.info("Добавление пользователя в базу данных");
        if (userRepository.findByUsername(username) != null) {
            log.info("Добавить не удалось, такой пользователь уже существует");
            return false;
        }


        String encodedPassword = passwordEncoder.encode(password);
        UserInfo newUser = new UserInfo(username, encodedPassword);
        userRepository.save(newUser);
        log.info(userRepository.findByUsername(username).getUsername());
        log.info(userRepository.findByUsername(username).getPassword());
        log.info("Добавление прошло успешно");
        return true;
    }



    public void saveCar(Car car){
        log.info("6");
        VaadinSession session = VaadinSession.getCurrent();
        UserInfo user = (UserInfo) session.getAttribute("userInfo");
        Long userId = user.getId();

        UserInfo currentUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        car.setOwner(currentUser);
        carRepository.save(car);
        log.info("7");
    }

    public List<Car> getCarsForCurrentUser() {

        VaadinSession session = VaadinSession.getCurrent();

        UserInfo user = (UserInfo) session.getAttribute("userInfo");
        Long userId = user.getId();
        log.info("42");
        UserInfo currentUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));


        log.info("5");
        return carRepository.findByOwner(currentUser);

    }

    public void addRefueling(Long carId, double fuelQuantity, double refuelCost, LocalDate date){
        Car car = carRepository.findById(carId).orElseThrow(() -> new RuntimeException("User not found"));
        Refueling refueling = new Refueling();
        refueling.setRefuelCost(refuelCost);
        refueling.setRefuelDate(date);
        refueling.setFuelQuantity(fuelQuantity);
        refueling.setCar(car);
        refuelingRepository.save(refueling);

    }

    @Transactional
    public ArrayList<Refueling> getFuelInformation(Long idCar){
        Car car = carRepository.findById(idCar)
                .orElseThrow(() -> new RuntimeException("Car not found"));
        return new ArrayList<>(car.getRefuelings());
    }

    public Optional<Refueling> getLastRefuelingByCarId(Long carId) {
        return refuelingRepository.findTopByCarIdOrderByRefuelDateDesc(carId);
    }

    @Async
    public CompletableFuture<List<RefuelingMonthlyReport>> getRefuelingMonthlyReport(Long carId) {
        List<Refueling> refuelings = refuelingRepository.findByCarId(carId);
        Map<String, Double> monthlyCosts = new HashMap<>();

        for (Refueling refueling : refuelings) {
            String monthKey = refueling.getRefuelDate().getMonth().toString() + " " + refueling.getRefuelDate().getYear();
            monthlyCosts.put(monthKey, monthlyCosts.getOrDefault(monthKey, 0.0) + refueling.getRefuelCost());
        }

        List<RefuelingMonthlyReport> monthlyReports = new ArrayList<>();
        for (Map.Entry<String, Double> entry : monthlyCosts.entrySet()) {
            monthlyReports.add(new RefuelingMonthlyReport(entry.getKey(), entry.getValue()));
        }

        return CompletableFuture.completedFuture(monthlyReports);
    }

    public static class RefuelingMonthlyReport {
        private String month;
        private Double totalCost;

        public RefuelingMonthlyReport(String month, Double totalCost) {
            this.month = month;
            this.totalCost = totalCost;
        }

        public String getMonth() {
            return month;
        }

        public Double getTotalCost() {
            return totalCost;
        }
    }

    public double calculateLinearDepreciation(double initialCost, double residualValue, int usefulLifeYears) {
        return (initialCost - residualValue) / usefulLifeYears;
    }


    public double calculateDecliningBalanceDepreciation(double initialCost, double residualValue, int usefulLifeYears, double depreciationRate) {
        double currentValue = initialCost;
        double totalDepreciation = 0;

        for (int year = 1; year <= usefulLifeYears; year++) {
            double depreciation = currentValue * depreciationRate;
            totalDepreciation += depreciation;
            currentValue -= depreciation;


            if (currentValue < residualValue) {
                currentValue = residualValue;
                break;
            }
        }

        return totalDepreciation;
    }


    public double calculateSumOfYearsDigitsDepreciation(double initialCost, double residualValue, int usefulLifeYears) {
        int sumOfYears = usefulLifeYears * (usefulLifeYears + 1) / 2;
        double totalDepreciation = 0;

        for (int year = 1; year <= usefulLifeYears; year++) {
            double yearFraction = (usefulLifeYears - (year - 1)) / (double) sumOfYears;
            double depreciationForYear = (initialCost - residualValue) * yearFraction;
            totalDepreciation += depreciationForYear;
        }

        return totalDepreciation;
    }


    public double calculateUnitsOfProductionDepreciation(double initialCost, double residualValue, int totalUnits, int unitsUsed) {
        return ((initialCost - residualValue) / totalUnits) * unitsUsed;
    }


    public double calculateDepreciation(double initialCost, double residualValue, int usefulLifeYears, double depreciationRate, int totalUnits, int unitsUsed, String depreciationMethod) {
        switch (depreciationMethod) {
            case "Линейный":
                return calculateLinearDepreciation(initialCost, residualValue, usefulLifeYears);
            case "Уменьшаемый остаток":
                return calculateDecliningBalanceDepreciation(initialCost, residualValue, usefulLifeYears, depreciationRate);
            case "Сумма чисел лет":
                return calculateSumOfYearsDigitsDepreciation(initialCost, residualValue, usefulLifeYears);
            case "Производственный":
                return calculateUnitsOfProductionDepreciation(initialCost, residualValue, totalUnits, unitsUsed);
            default:
                throw new IllegalArgumentException("Неизвестный метод амортизации: " + depreciationMethod);
        }
    }

    public double[] calculateDecliningBalance(double initialCost, double residualValue, int usefulLifeYears, double depreciationRate) {
        double[] depreciation = new double[usefulLifeYears];
        double currentValue = initialCost;

        for (int year = 0; year < usefulLifeYears; year++) {
            double depreciationYear = currentValue * depreciationRate;
            depreciation[year] = depreciationYear;
            currentValue -= depreciationYear;
            if (currentValue < residualValue) {
                currentValue = residualValue;
                break;
            }
        }

        return depreciation;
    }

    public double[] calculateSumOfYearsDigits(double initialCost, double residualValue, int usefulLifeYears) {
        double[] depreciation = new double[usefulLifeYears];
        int sumOfYears = usefulLifeYears * (usefulLifeYears + 1) / 2;

        for (int year = 0; year < usefulLifeYears; year++) {
            double yearFraction = (usefulLifeYears - year) / (double) sumOfYears;
            depreciation[year] = (initialCost - residualValue) * yearFraction;
        }

        return depreciation;
    }

    @Transactional
    public void addMileage(Long carId, double kilometers, LocalDate localDate) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Машина не найдена"));


        car.setMileage(car.getMileage() + kilometers);
        carRepository.save(car);
        MileageLog log = new MileageLog();
        log.setCar(car);
        log.setKilometers(kilometers);
        log.setDate(localDate);

        mileageLogRepository.save(log);
    }

    public List<MileageLog> getMileageLogsForCar(Long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Машина не найдена"));
        return mileageLogRepository.findByCar(car);
    }

    public Map<String, Double> getAverageMileagePerMonth(Long carId) {
        List<MileageLog> logs = getMileageLogsForCar(carId);

        return logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getDate().getMonth() + " " + log.getDate().getYear(),
                        Collectors.averagingDouble(MileageLog::getKilometers)
                ));
    }


    @Transactional
    public void deleteCar(Car car) {
        car.setStatus("Неактивен");
        carRepository.save(car);


        List<Staff> staffList = staffRepository.findByAssignedVehicle(car);


        for (Staff staff : staffList) {
            staff.setAssignedVehicle(null);
        }


        staffRepository.saveAll(staffList);
    }




    public void saveTechnicalMaintenance(Long carId, LocalDate serviceDate, String description, Double cost, String serviceProvider, String status) {
        Car car = carRepository.findById(carId).orElseThrow(() -> new RuntimeException("Машина не найдена"));


        TechnicalMaintenance technicalMaintenance = new TechnicalMaintenance();
        technicalMaintenance.setServiceDate(serviceDate);
        technicalMaintenance.setDescription(description);
        technicalMaintenance.setCost(cost);
        technicalMaintenance.setServiceProvider(serviceProvider);
        technicalMaintenance.setStatus(status);
        technicalMaintenance.setCar(car);


        technicalMaintenanceRepository.save(technicalMaintenance);
    }

    public List<TechnicalMaintenance> getTechnicalMaintenancesForCar(Long carId) {

        return technicalMaintenanceRepository.findByCarId(carId);
    }


    public Map<YearMonth, Double> getAverageFuelConsumptionByMonthForUser(Long userId, LocalDate from, LocalDate to) {
        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Car> cars = carRepository.findByOwner(user);

        Map<YearMonth, List<Refueling>> refuelingsByMonth = new HashMap<>();

        for (Car car : cars) {
            List<Refueling> allRefuelings = refuelingRepository.findByCarId(car.getId());

            List<Refueling> filteredRefuelings = allRefuelings.stream()
                    .filter(r -> {
                        LocalDate date = r.getRefuelDate();
                        return (date.isEqual(from) || date.isAfter(from)) &&
                                (date.isEqual(to) || date.isBefore(to));
                    })
                    .toList();

            for (Refueling refueling : filteredRefuelings) {
                YearMonth ym = YearMonth.from(refueling.getRefuelDate());
                refuelingsByMonth
                        .computeIfAbsent(ym, k -> new ArrayList<>())
                        .add(refueling);
            }
        }

        Map<YearMonth, Double> averageVolumeByMonth = new HashMap<>();

        for (Map.Entry<YearMonth, List<Refueling>> entry : refuelingsByMonth.entrySet()) {
            List<Refueling> refuelings = entry.getValue();

            double totalVolume = refuelings.stream()
                    .mapToDouble(Refueling::getFuelQuantity)
                    .sum();

            long uniqueCars = refuelings.stream()
                    .map(r -> r.getCar().getId())
                    .distinct()
                    .count();

            double average = uniqueCars > 0 ? totalVolume / uniqueCars : 0.0;
            averageVolumeByMonth.put(entry.getKey(), average);
        }

        return averageVolumeByMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    public Map<YearMonth, Double> getAverageRefuelCostByMonthForUser(Long userId, LocalDate from, LocalDate to) {
        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        List<Car> cars = carRepository.findByOwner(user);

        Map<YearMonth, List<Refueling>> refuelingsByMonth = new HashMap<>();

        for (Car car : cars) {
            List<Refueling> refuelings = refuelingRepository.findByCarId(car.getId()).stream()
                    .filter(r -> {
                        LocalDate date = r.getRefuelDate();
                        return (date.isEqual(from) || date.isAfter(from)) &&
                                (date.isEqual(to) || date.isBefore(to));
                    })
                    .toList();

            for (Refueling r : refuelings) {
                YearMonth ym = YearMonth.from(r.getRefuelDate());
                refuelingsByMonth.computeIfAbsent(ym, k -> new ArrayList<>()).add(r);
            }
        }

        Map<YearMonth, Double> averageCostByMonth = new HashMap<>();
        for (Map.Entry<YearMonth, List<Refueling>> entry : refuelingsByMonth.entrySet()) {
            double totalCost = entry.getValue().stream()
                    .mapToDouble(Refueling::getRefuelCost)
                    .sum();
            long uniqueCars = entry.getValue().stream()
                    .map(r -> r.getCar().getId())
                    .distinct()
                    .count();

            double average = uniqueCars > 0 ? totalCost / uniqueCars : 0.0;
            averageCostByMonth.put(entry.getKey(), average);
        }

        return averageCostByMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    public Map<String, Double> getAverageFuelConsumptionPerCar(Long userId, LocalDate from, LocalDate to) {
        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Car> cars = carRepository.findByOwner(user);
        Map<String, Double> result = new LinkedHashMap<>();

        for (Car car : cars) {
            List<Refueling> refuelings = refuelingRepository.findByCarId(car.getId()).stream()
                    .filter(r -> {
                        LocalDate date = r.getRefuelDate();
                        return (date.isEqual(from) || date.isAfter(from)) &&
                                (date.isEqual(to) || date.isBefore(to));
                    })
                    .toList();

            if (!refuelings.isEmpty()) {
                double totalFuel = refuelings.stream()
                        .mapToDouble(Refueling::getFuelQuantity)
                        .sum();
                result.put(car.getNumberCar(), totalFuel);
            }
        }

        return result;
    }

    public Map<String, Double> getTotalFuelCostPerCar(Long userId, LocalDate from, LocalDate to) {
        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Car> cars = carRepository.findByOwner(user);
        Map<String, Double> result = new LinkedHashMap<>();

        for (Car car : cars) {
            List<Refueling> refuelings = refuelingRepository.findByCarId(car.getId()).stream()
                    .filter(r -> !r.getRefuelDate().isBefore(from) && !r.getRefuelDate().isAfter(to))
                    .toList();


            if (!refuelings.isEmpty()) {
                double total = refuelings.stream()
                        .mapToDouble(Refueling::getRefuelCost)
                        .sum();
                result.put(car.getNumberCar(), total);
            }
        }

        return result;
    }

    public Map<String, Double> getTotalMileagePerCar(Long userId, LocalDate from, LocalDate to) {
        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Car> cars = carRepository.findByOwner(user);
        Map<String, Double> result = new LinkedHashMap<>();

        for (Car car : cars) {
            List<MileageLog> logs = mileageLogRepository.findByCar(car).stream()
                    .filter(log -> {
                        LocalDate date = log.getDate();
                        return (date.isEqual(from) || date.isAfter(from)) &&
                                (date.isEqual(to) || date.isBefore(to));
                    })
                    .toList();

            if (!logs.isEmpty()) {
                double totalKm = logs.stream()
                        .mapToDouble(MileageLog::getKilometers)
                        .sum();
                result.put(car.getNumberCar(), totalKm);
            }
        }

        return result;
    }

    public Map<YearMonth, Double> getFuelEfficiencyByMonthForUser(Long userId, LocalDate from, LocalDate to) {
        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Car> cars = carRepository.findByOwner(user);


        Map<YearMonth, Double> totalFuelPerMonth = new HashMap<>();

        Map<YearMonth, Double> totalDistancePerMonth = new HashMap<>();

        for (Car car : cars) {
            List<Refueling> refuelings = refuelingRepository.findByCarId(car.getId()).stream()
                    .filter(r -> {
                        LocalDate date = r.getRefuelDate();
                        return (date.isEqual(from) || date.isAfter(from)) &&
                                (date.isEqual(to) || date.isBefore(to));
                    })
                    .toList();

            List<MileageLog> mileages = mileageLogRepository.findByCarId(car.getId()).stream()
                    .filter(m -> {
                        LocalDate date = m.getDate();
                        return (date.isEqual(from) || date.isAfter(from)) &&
                                (date.isEqual(to) || date.isBefore(to));
                    })
                    .toList();

            for (Refueling r : refuelings) {
                YearMonth ym = YearMonth.from(r.getRefuelDate());
                totalFuelPerMonth.merge(ym, r.getFuelQuantity(), Double::sum);
            }

            for (MileageLog m : mileages) {
                YearMonth ym = YearMonth.from(m.getDate());
                totalDistancePerMonth.merge(ym, m.getKilometers(), Double::sum);
            }
        }

        Map<YearMonth, Double> efficiencyPerMonth = new LinkedHashMap<>();
        List<YearMonth> sortedMonths = new ArrayList<>(totalFuelPerMonth.keySet());
        sortedMonths.sort(Comparator.naturalOrder());

        for (YearMonth ym : sortedMonths) {
            double fuel = totalFuelPerMonth.getOrDefault(ym, 0.0);
            double distance = totalDistancePerMonth.getOrDefault(ym, 0.0);
            double efficiency = distance > 0 ? (fuel / distance) * 100 : 0.0;
            efficiencyPerMonth.put(ym, Math.round(efficiency * 100.0) / 100.0);
        }

        return efficiencyPerMonth;
    }

    public Map<YearMonth, Double> getFuelCostEfficiencyByMonthForUser(Long userId, LocalDate from, LocalDate to) {
        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Car> cars = carRepository.findByOwner(user);

        Map<YearMonth, Double> totalCostPerMonth = new HashMap<>();

        Map<YearMonth, Double> totalDistancePerMonth = new HashMap<>();

        for (Car car : cars) {
            List<Refueling> refuelings = refuelingRepository.findByCarId(car.getId()).stream()
                    .filter(r -> {
                        LocalDate date = r.getRefuelDate();
                        return (date.isEqual(from) || date.isAfter(from)) &&
                                (date.isEqual(to) || date.isBefore(to));
                    })
                    .toList();

            List<MileageLog> mileages = mileageLogRepository.findByCarId(car.getId()).stream()
                    .filter(m -> {
                        LocalDate date = m.getDate();
                        return (date.isEqual(from) || date.isAfter(from)) &&
                                (date.isEqual(to) || date.isBefore(to));
                    })
                    .toList();

            for (Refueling r : refuelings) {
                YearMonth ym = YearMonth.from(r.getRefuelDate());
                totalCostPerMonth.merge(ym, r.getRefuelCost(), Double::sum);
            }

            for (MileageLog m : mileages) {
                YearMonth ym = YearMonth.from(m.getDate());
                totalDistancePerMonth.merge(ym, m.getKilometers(), Double::sum);
            }
        }

        Map<YearMonth, Double> efficiencyPerMonth = new LinkedHashMap<>();
        List<YearMonth> sortedMonths = new ArrayList<>(totalCostPerMonth.keySet());
        sortedMonths.sort(Comparator.naturalOrder());

        for (YearMonth ym : sortedMonths) {
            double cost = totalCostPerMonth.getOrDefault(ym, 0.0);
            double distance = totalDistancePerMonth.getOrDefault(ym, 0.0);
            double efficiency = distance > 0 ? (cost / distance) * 100 : 0.0;
            efficiencyPerMonth.put(ym, Math.round(efficiency * 100.0) / 100.0);
        }

        return efficiencyPerMonth;
    }

    public Map<YearMonth, Double> getServiceCostsByMonthForUser(Long userId, LocalDate from, LocalDate to) {
        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Car> cars = carRepository.findByOwner(user);

        Map<YearMonth, Double> serviceCosts = new HashMap<>();

        for (Car car : cars) {
            List<TechnicalMaintenance> services = technicalMaintenanceRepository.findByCarId(car.getId()).stream()
                    .filter(s -> {
                        LocalDate date = s.getServiceDate();
                        return (date.isEqual(from) || date.isAfter(from)) &&
                                (date.isEqual(to) || date.isBefore(to)) &&
                                "Выполнено".equalsIgnoreCase(s.getStatus());
                    })
                    .toList();

            for (TechnicalMaintenance s : services) {
                YearMonth ym = YearMonth.from(s.getServiceDate());
                serviceCosts.merge(ym, s.getCost(), Double::sum);
            }
        }

        return serviceCosts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public List<Refueling> getRefuelingsForCar(Long carId) {
        return refuelingRepository.findByCarId(carId);
    }

}
