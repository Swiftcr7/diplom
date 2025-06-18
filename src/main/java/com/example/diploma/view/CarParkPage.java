package com.example.diploma.view;
import java.io.*;
import java.nio.file.Files;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.example.diploma.service.CarParkServiece;
import com.example.diploma.model.*;
import com.example.diploma.repository.UserRepository;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.Month;
import java.util.stream.Collectors;

@Slf4j
@PreAuthorize("hasRole('USER')")
@JavaScript("https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js")
@Route("car_park")
public class CarParkPage extends VerticalLayout {
    private final CarParkServiece server;

    private final Grid<Car> grid;

    private final VerticalLayout mainLayout = new VerticalLayout();

    private final Div chartContainer = new Div();





    @Autowired
    public CarParkPage(CarParkServiece server, UserRepository userRepository){

        this.server = server;


        H1 header = new H1("Автопарк");

        grid = new Grid<>(Car.class);
        grid.setColumns("numberCar", "make", "model", "fuelType", "status");
        grid.setItems(server.getCarsForCurrentUser());
        grid.setSortableColumns("numberCar", "make", "model", "fuelType", "status");
        grid.addComponentColumn(this::createActionButtons).setHeader("Действия").setAutoWidth(true)
                .setFlexGrow(0);

        grid.setHeight("350px");

        Button addButton = new Button("Добавить автомобиль", e -> openAddCarForm());

        UserInfo user = (UserInfo) VaadinSession.getCurrent().getAttribute("userInfo");
        Long userId = user.getId();

        chartContainer.setId("chartContainer");
        chartContainer.setWidth("100%");
        chartContainer.setHeight("400px");

        DatePicker fromDate = new DatePicker("С:");
        DatePicker toDate = new DatePicker("По:");
        fromDate.setValue(LocalDate.now().minusMonths(6).withDayOfMonth(1));
        toDate.setValue(LocalDate.now());
        Button filterButton = new Button("Показать");

        HorizontalLayout filterLayout = new HorizontalLayout(fromDate, toDate, filterButton);
        try {
            filterLayout.setAlignItems(Alignment.END);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Div filterPanel = new Div(filterLayout);
        filterPanel.getStyle()
                .set("background-color", "#ffffff")
                .set("padding", "1rem")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)")
                .set("display", "inline-block");

        Tab fuelTab = new Tab("Расход топлива");
        Tab costTab = new Tab("Затраты на топливо");
        Tabs tabs = new Tabs(fuelTab, costTab);

        VerticalLayout leftPanel = new VerticalLayout(tabs, chartContainer, filterPanel);
        leftPanel.setWidth("40%");

        Runnable updateChart = () -> {
            LocalDate from = fromDate.getValue();
            LocalDate to = toDate.getValue();
            if (from == null || to == null || from.isAfter(to)) {
                Notification.show("Укажите корректный диапазон дат");
                return;
            }

            if (tabs.getSelectedTab() == fuelTab) {
                drawFuelChart(server.getAverageFuelConsumptionByMonthForUser(userId, from, to));
            } else {
                drawFuelCostChart(server.getAverageRefuelCostByMonthForUser(userId, from, to));
            }
        };
        filterButton.addClickListener(e -> updateChart.run());
        tabs.addSelectedChangeListener(e -> updateChart.run());

        Div rightChart = new Div();
        rightChart.setId("carSpendingChart");
        rightChart.setWidth("100%");
        rightChart.setHeight("400px");

        Tab fuelPerCarTab = new Tab("Расход по авто (л)");
        Tab costPerCarTab = new Tab("Затраты по авто (₽)");
        Tabs rightTabs = new Tabs(fuelPerCarTab, costPerCarTab);
        rightTabs.setWidthFull();
        rightTabs.getStyle().set("margin-bottom", "0.5rem");

        DatePicker rightFrom = new DatePicker("С:");
        DatePicker rightTo = new DatePicker("По:");
        rightFrom.setValue(LocalDate.now().minusMonths(6).withDayOfMonth(1));
        rightTo.setValue(LocalDate.now());
        Button rightButton = new Button("Показать");

        HorizontalLayout rightFilterLayout = new HorizontalLayout(rightFrom, rightTo, rightButton);
        rightFilterLayout.setAlignItems(FlexComponent.Alignment.END);
        Div rightFilterPanel = new Div(rightFilterLayout);
        rightFilterPanel.getStyle()
                .set("background-color", "#ffffff")
                .set("padding", "1rem")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)")
                .set("display", "inline-block")
                .set("margin-left", "auto");


        VerticalLayout rightPanel = new VerticalLayout(rightTabs, rightChart, rightFilterPanel);
        rightPanel.setWidth("60%");

        Runnable updateRightChart = () -> {
            LocalDate from = rightFrom.getValue();
            LocalDate to = rightTo.getValue();
            if (from == null || to == null || from.isAfter(to)) {
                Notification.show("Укажите корректный диапазон дат");
                return;
            }

            if (rightTabs.getSelectedTab() == fuelPerCarTab) {
                drawCarFuelChart(server.getAverageFuelConsumptionPerCar(userId, from, to));
            } else {
                drawCarFuelCostChart(server.getTotalFuelCostPerCar(userId, from, to));
            }
        };

        rightButton.addClickListener(e -> updateRightChart.run());
        rightTabs.addSelectedChangeListener(e -> updateRightChart.run());

        drawFuelChart(server.getAverageFuelConsumptionByMonthForUser(userId, fromDate.getValue(), toDate.getValue()));
        drawCarFuelChart(server.getAverageFuelConsumptionPerCar(userId, rightFrom.getValue(), rightTo.getValue()));

        HorizontalLayout dualCharts = new HorizontalLayout(leftPanel, rightPanel);
        dualCharts.setWidthFull();
        dualCharts.setAlignItems(FlexComponent.Alignment.END);

        DatePicker mileageFrom = new DatePicker("С:");
        DatePicker mileageTo = new DatePicker("По:");
        mileageFrom.setValue(LocalDate.now().minusMonths(6).withDayOfMonth(1));
        mileageTo.setValue(LocalDate.now());
        Button mileageButton = new Button("Показать");
        // 2 строка 1 столбец
        Component mileageFilterPanel = createFilterPanel(mileageFrom, mileageTo, mileageButton);
        Div mileageChartDiv = createChartDiv("mileageChart", "100%", "400px");
        Span totalMileageLabel = new Span();
        totalMileageLabel.getStyle().set("font-weight", "bold").set("margin-top", "0.5rem");
        mileageButton.addClickListener(e -> {
            LocalDate from = mileageFrom.getValue();
            LocalDate to = mileageTo.getValue();
            if (from == null || to == null || from.isAfter(to)) {
                Notification.show("Укажите корректный диапазон дат");
                return;
            }
            drawCarMileageChart(
                    server.getTotalMileagePerCar(userId, from, to),
                    totalMileageLabel
            );
        });

        VerticalLayout chartMilleageLayut = new VerticalLayout(mileageChartDiv, totalMileageLabel, mileageFilterPanel);
        chartMilleageLayut.setWidth("60%");

        drawCarMileageChart(
                server.getTotalMileagePerCar(userId, mileageFrom.getValue(), mileageTo.getValue()),
                totalMileageLabel
        );

        // 2 строка 2 столбец
        Tab litersTab = new Tab("л/100 км");
        Tab rublesTab = new Tab("₽/100 км");
        Tabs efficiencyTabs = new Tabs(litersTab, rublesTab);

        Div efficiencyChart = createChartDiv("efficiencyChart", "100%", "400px");

        DatePicker efficiencyFrom = new DatePicker("С:");
        DatePicker efficiencyTo = new DatePicker("По:");
        efficiencyFrom.setValue(LocalDate.now().minusMonths(6).withDayOfMonth(1));
        efficiencyTo.setValue(LocalDate.now());

        Button efficiencyButton = new Button("Показать");
        Component efficiencyFilterPanel = createFilterPanel(efficiencyFrom, efficiencyTo, efficiencyButton);

        VerticalLayout efficiencyLayout = new VerticalLayout(efficiencyTabs, efficiencyChart, efficiencyFilterPanel);
        efficiencyLayout.setWidth("40%");
        efficiencyChart.getStyle().set("margin-bottom", "0");
        efficiencyFilterPanel.getStyle().set("margin-top", "0");

        Runnable updateEfficiency = () -> {
            LocalDate from = efficiencyFrom.getValue();
            LocalDate to = efficiencyTo.getValue();
            if (from == null || to == null || from.isAfter(to)) {
                Notification.show("Укажите корректный диапазон дат");
                return;
            }

            if (efficiencyTabs.getSelectedTab() == litersTab) {
                drawEfficiencyChart(server.getFuelEfficiencyByMonthForUser(userId, from, to), "л/100 км");
            } else {
                drawEfficiencyChart(server.getFuelCostEfficiencyByMonthForUser(userId, from, to), "₽/100 км");
            }
        };
        efficiencyButton.addClickListener(e -> updateEfficiency.run());
        efficiencyTabs.addSelectedChangeListener(e -> updateEfficiency.run());

        drawEfficiencyChart(
                server.getFuelEfficiencyByMonthForUser(userId, efficiencyFrom.getValue(), efficiencyTo.getValue()),
                "л/100 км"
        );

        HorizontalLayout bottomRow = new HorizontalLayout(chartMilleageLayut, efficiencyLayout);
        bottomRow.setWidthFull();
        bottomRow.setAlignItems(FlexComponent.Alignment.END);

        //3 строка 2 столбец
        DatePicker maintenanceFrom = new DatePicker("С:");
        DatePicker maintenanceTo = new DatePicker("По:");
        maintenanceFrom.setValue(LocalDate.now().minusMonths(6).withDayOfMonth(1));
        maintenanceTo.setValue(LocalDate.now());

        Button maintenanceButton = new Button("Показать");

        Component maintenanceFilterPanel = createFilterPanel(maintenanceFrom, maintenanceTo, maintenanceButton);
        Div maintenanceChartDiv = createChartDiv("maintenanceChart", "100%", "400px");
        VerticalLayout maintenanceLayout = new VerticalLayout(maintenanceChartDiv, maintenanceFilterPanel);
        maintenanceLayout.setWidth("60%");

        maintenanceButton.addClickListener(e -> {
            LocalDate from = maintenanceFrom.getValue();
            LocalDate to = maintenanceTo.getValue();
            if (from == null || to == null || from.isAfter(to)) {
                Notification.show("Укажите корректный диапазон дат");
                return;
            }
            drawMaintenanceChart(server.getServiceCostsByMonthForUser(userId, from, to));
        });
        drawMaintenanceChart(server.getServiceCostsByMonthForUser(userId, maintenanceFrom.getValue(), maintenanceTo.getValue()));


        mainLayout.setSpacing(true);
        mainLayout.setPadding(true);
        mainLayout.setSizeFull();

        mainLayout.add(header, addButton, grid, new Hr(), dualCharts, new Hr(), bottomRow, new Hr(), maintenanceLayout);
        add(mainLayout);

    }

    private HorizontalLayout createActionButtons(Car car) {
        Button editButton = new Button("Редактировать", e -> openEditCarForm(car));

        Button deactivateButton = new Button("Деактивировать", e -> deleteCar(car));
        deactivateButton.getStyle().set("color", "red");

        Button detailsButton = new Button("Подробнее", e -> {
            try {
                openCarDetailsDialog(car);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        Button mileageButton = new Button("Заправка", e -> openAddRefuelForm(car));

        if ("Неактивен".equals(car.getStatus())) {
            mileageButton.setEnabled(false);
            mileageButton.getStyle().set("opacity", "0.5");
        }

        return new HorizontalLayout(editButton, mileageButton, detailsButton, deactivateButton);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        List<Car> cars = server.getCarsForCurrentUser();

        for (Car car : cars) {
            if (needsMaintenance(car)) {
                Notification.show("Автомобилю " + car.getNumberCar() + " требуется ТО!", 5000, Notification.Position.TOP_CENTER);
            }
        }
        grid.addComponentColumn(car -> {
                    if (needsMaintenance(car)) {
                        Icon icon = VaadinIcon.WARNING.create();
                        icon.setColor("red");
                        icon.getElement().setProperty("title", "Требуется ТО");
                        return icon;
                    } else {
                        return new Span();
                    }
                }).setHeader("ТО")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    private boolean needsMaintenance(Car car) {
        LocalDate today = LocalDate.now();

        boolean dateExceeded = car.getNextServiceDate() != null && !car.getNextServiceDate().isAfter(today);
        boolean mileageExceeded = car.getMileage() - car.getLastServiceMileage() >= car.getServiceIntervalKm();

        return dateExceeded || mileageExceeded;
    }


    private Component createFilterPanel(DatePicker fromDate, DatePicker toDate, Button filterButton) {
        HorizontalLayout filterLayout = new HorizontalLayout(fromDate, toDate, filterButton);
        filterLayout.setAlignItems(FlexComponent.Alignment.END);

        Div filterPanel = new Div(filterLayout);
        filterPanel.getStyle()
                .set("background-color", "#ffffff")
                .set("padding", "1rem")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)")
                .set("display", "inline-block");

        return filterPanel;
    }

    private Div createChartDiv(String id, String width, String height) {
        Div div = new Div();
        div.setId(id);
        div.setWidth(width);
        div.setHeight(height);
        return div;
    }

    private void drawMaintenanceChart(Map<YearMonth, Double> data) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLL yyyy", new Locale("ru"));
        List<YearMonth> sorted = new ArrayList<>(data.keySet());
        Collections.sort(sorted);

        List<YearMonth> top = data.entrySet().stream()
                .sorted(Map.Entry.<YearMonth, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        StringBuilder labels = new StringBuilder("[");
        StringBuilder values = new StringBuilder("[");
        StringBuilder colors = new StringBuilder("[");

        for (int i = 0; i < sorted.size(); i++) {
            YearMonth ym = sorted.get(i);
            labels.append("'").append(formatter.format(ym)).append("'");
            values.append(String.format(Locale.US, "%.2f", data.get(ym)));

            boolean isTop = top.contains(ym);
            colors.append(isTop
                    ? "'rgba(255, 159, 64, 0.6)'"
                    : "'rgba(153, 102, 255, 0.5)'");

            if (i < sorted.size() - 1) {
                labels.append(", ");
                values.append(", ");
                colors.append(", ");
            }
        }

        labels.append("]");
        values.append("]");
        colors.append("]");

        String js = """
        if (!window.ChartDataLabels) {
            const script = document.createElement("script");
            script.src = "https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2";
            script.onload = () => {
                Chart.register(ChartDataLabels);
                drawMaintenanceChart();
            };
            document.head.appendChild(script);
        } else {
            drawMaintenanceChart();
        }

        function drawMaintenanceChart() {
            const container = document.getElementById("maintenanceChart");
            if (!container) return;
            container.innerHTML = '';

            const canvas = document.createElement("canvas");
            container.appendChild(canvas);

            new Chart(canvas.getContext("2d"), {
                type: 'bar',
                data: {
                    labels: %s,
                    datasets: [{
                        label: 'Расходы на ТО (₽)',
                        data: %s,
                        backgroundColor: %s,
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    layout: {
                        padding: {
                            bottom: 20
                        }
                    },
                    plugins: {
                        legend: { position: 'top' },
                        datalabels: {
                            anchor: 'end',
                            align: 'end',
                            formatter: v => v.toFixed(0) + " ₽",
                            font: { weight: 'bold', size: 11 },
                            color: '#333'
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            title: {
                                display: true,
                                text: '₽'
                            }
                        }
                    }
                },
                plugins: [ChartDataLabels]
            });
        }
        """.formatted(labels, values, colors);

        UI.getCurrent().getPage().executeJs(js);
    }

    private void drawEfficiencyChart(Map<YearMonth, Double> data, String unitLabel) {
        List<String> labelsList = new ArrayList<>();
        List<String> valuesList = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

        for (Map.Entry<YearMonth, Double> entry : data.entrySet()) {
            labelsList.add("'" + entry.getKey().format(formatter) + "'");
            valuesList.add(String.format(Locale.US, "%.1f", entry.getValue()));
        }

        String labels = "[" + String.join(", ", labelsList) + "]";
        String values = "[" + String.join(", ", valuesList) + "]";

        String js = """
    const container = document.getElementById("efficiencyChart");
    if (!container) return;
    container.innerHTML = '';

    const canvas = document.createElement("canvas");
    canvas.style.width = "100%%";
    canvas.style.height = "100%%";
    container.appendChild(canvas);

    function drawEfficiencyChart() {
        new Chart(canvas.getContext("2d"), {
            type: 'line',
            data: {
                labels: %s,
                datasets: [{
                    label: 'Расход топлива (%s)',
                    data: %s,
                    backgroundColor: 'rgba(255, 99, 132, 0.2)',
                    borderColor: 'rgba(255, 99, 132, 1)',
                    borderWidth: 2,
                    fill: true,
                    tension: 0,
                    pointRadius: 4,
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'top' },
                    datalabels: {
                        anchor: 'end',
                        align: 'top',
                        formatter: v => v.toFixed(1) + ' %s',
                        font: { weight: 'bold' },
                        color: '#333'
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: '%s'
                        }
                    }
                }
            },
            plugins: [ChartDataLabels]
        });
    }

    if (!window.Chart || !window.ChartDataLabels) {
        const chartScript = document.createElement("script");
        chartScript.src = "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js";
        chartScript.onload = () => {
            const pluginScript = document.createElement("script");
            pluginScript.src = "https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2";
            pluginScript.onload = () => {
                Chart.register(ChartDataLabels);
                drawEfficiencyChart();
            };
            document.head.appendChild(pluginScript);
        };
        document.head.appendChild(chartScript);
    } else {
        drawEfficiencyChart();
    }
""".formatted(labels, unitLabel, values, unitLabel, unitLabel);

        UI.getCurrent().getPage().executeJs(js);
    }

    private void drawCarMileageChart(Map<String, Double> data, Span label) {
        double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
        label.setText("Общий пробег за период: " + String.format(Locale.US, "%.0f", total) + " км");
        List<Map.Entry<String, Double>> top = data.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .toList();

        StringBuilder labels = new StringBuilder("[");
        StringBuilder values = new StringBuilder("[");
        StringBuilder colors = new StringBuilder("[");

        int i = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            labels.append("'").append(entry.getKey()).append("'");
            values.append(String.format(Locale.US, "%.0f", entry.getValue()));

            boolean isTop = top.stream().anyMatch(e -> e.getKey().equals(entry.getKey()));
            colors.append(isTop
                    ? "'rgba(255, 206, 86, 0.6)'"
                    : "'rgba(75, 192, 192, 0.5)'");

            if (++i < data.size()) {
                labels.append(", ");
                values.append(", ");
                colors.append(", ");
            }
        }

        labels.append("]");
        values.append("]");
        colors.append("]");

        String js = """
    const container = document.getElementById("mileageChart");
    if (!container) return;
    container.innerHTML = '';

    const canvas = document.createElement("canvas");
    container.appendChild(canvas);

    function drawChart() {
        const ctx = canvas.getContext("2d");
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: %s,
                datasets: [{
                    label: 'Суммарный пробег (км)',
                    data: %s,
                    backgroundColor: %s,
                    borderWidth: 1
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                plugins: {
                    legend: { position: 'top' },
                    datalabels: {
                        anchor: 'end',
                        align: 'left',
                        offset: -10,
                        formatter: v => v.toFixed(0) + " км",
                        font: { weight: 'bold', size: 11 },
                        color: '#444'
                    }
                },
                scales: {
                    x: { beginAtZero: true }
                }
            },
            plugins: [ChartDataLabels]
        });
    }

    if (!window.Chart || !window.ChartDataLabels) {
        const script1 = document.createElement("script");
        script1.src = "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js";
        document.head.appendChild(script1);
        script1.onload = function () {
            const script2 = document.createElement("script");
            script2.src = "https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2";
            document.head.appendChild(script2);
            script2.onload = function () {
                Chart.register(ChartDataLabels);
                drawChart();
            };
        };
    } else {
        drawChart();
    }
""".formatted(labels, values, colors);

        UI.getCurrent().getPage().executeJs(js);
    }


    private void drawCarFuelCostChart(Map<String, Double> data) {
        List<Map.Entry<String, Double>> top = data.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .toList();

        StringBuilder labels = new StringBuilder("[");
        StringBuilder values = new StringBuilder("[");
        StringBuilder colors = new StringBuilder("[");

        int i = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            labels.append("'").append(entry.getKey()).append("'");
            values.append(String.format(Locale.US, "%.2f", entry.getValue()));

            boolean isTop = top.stream().anyMatch(e -> e.getKey().equals(entry.getKey()));
            colors.append(isTop
                    ? "'rgba(255, 159, 64, 0.6)'"
                    : "'rgba(75, 192, 192, 0.5)'");

            if (++i < data.size()) {
                labels.append(", ");
                values.append(", ");
                colors.append(", ");
            }
        }

        labels.append("]");
        values.append("]");
        colors.append("]");

        String js = """
        const container = document.getElementById("carSpendingChart");
        if (!container) return;
        container.innerHTML = '';

        const canvas = document.createElement("canvas");
        container.appendChild(canvas);

        new Chart(canvas.getContext("2d"), {
            type: 'bar',
            data: {
                labels: %s,
                datasets: [{
                    label: 'Суммарные затраты на топливо (₽)',
                    data: %s,
                    backgroundColor: %s,
                    borderWidth: 1
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                layout: {
                    padding: { right: 30 }
                },
                plugins: {
                    legend: { position: 'top' },
                    datalabels: {
                        anchor: 'end',
                        align: 'right',
                        formatter: v => v.toFixed(0) + " ₽",
                        font: { weight: 'bold', size: 11 }
                    }
                },
                scales: {
                    x: { beginAtZero: true }
                }
            },
            plugins: [ChartDataLabels]
        });
    """.formatted(labels, values, colors);

        UI.getCurrent().getPage().executeJs(js);
    }
    private void drawCarFuelChart(Map<String, Double> data) {
        List<Map.Entry<String, Double>> top = data.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .toList();

        StringBuilder labels = new StringBuilder("[");
        StringBuilder values = new StringBuilder("[");
        StringBuilder colors = new StringBuilder("[");

        int i = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            labels.append("'").append(entry.getKey()).append("'");
            values.append(String.format(Locale.US, "%.2f", entry.getValue()));

            boolean isTop = top.stream().anyMatch(e -> e.getKey().equals(entry.getKey()));
            colors.append(isTop
                    ? "'rgba(255, 99, 132, 0.6)'"
                    : "'rgba(54, 162, 235, 0.5)'");

            if (++i < data.size()) {
                labels.append(", ");
                values.append(", ");
                colors.append(", ");
            }
        }

        labels.append("]");
        values.append("]");
        colors.append("]");

        String js = """
                if (!window.ChartDataLabels) {
                    const script = document.createElement("script");
                    script.src = "https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2";
                    script.onload = () => {
                        Chart.register(ChartDataLabels);
                        drawCarChart();
                    };
                    document.head.appendChild(script);
                } else {
                    drawCarChart();
                }

                function drawCarChart() {
                    const container = document.getElementById("carSpendingChart");
                    if (!container) return;
                    container.innerHTML = '';

                    const canvas = document.createElement("canvas");
                    container.appendChild(canvas);

                    new Chart(canvas.getContext("2d"), {
                        type: 'bar',
                        data: {
                            labels: %s,
                            datasets: [{
                                label: 'Средний расход по авто (л)',
                                data: %s,
                                backgroundColor: %s,
                                borderWidth: 1
                            }]
                        },
                        options: {
                            indexAxis: 'y',
                            responsive: true,
                            layout: {
                                padding: {
                                    right: 40
                                }
                            },
                            plugins: {
                                legend: { position: 'top' },
                                datalabels: {
                                    anchor: 'end',
                                    align: 'right',
                                    formatter: v => v.toFixed(1) + " л",
                                    font: { weight: 'bold' }
                                }
                            },
                            scales: {
                                x: { beginAtZero: true }
                            }
                        },
                        plugins: [ChartDataLabels]
                    });
                }
                """.formatted(labels, values, colors);

        UI.getCurrent().getPage().executeJs(js);
    }



    private void drawFuelChart(Map<YearMonth, Double> averageConsumption) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        List<YearMonth> sortedMonths = new ArrayList<>(averageConsumption.keySet());
        Collections.sort(sortedMonths);
        List<Map.Entry<YearMonth, Double>> topMonths = averageConsumption.entrySet().stream()
                .sorted(Map.Entry.<YearMonth, Double>comparingByValue().reversed())
                .limit(3)
                .toList();

        StringBuilder labels = new StringBuilder("[");
        StringBuilder data = new StringBuilder("[");
        StringBuilder backgroundColors = new StringBuilder("[");

        for (int i = 0; i < sortedMonths.size(); i++) {
            YearMonth ym = sortedMonths.get(i);
            String label = formatter.format(ym);
            double value = averageConsumption.get(ym);

            labels.append("'").append(label).append("'");
            data.append(String.format(Locale.US, "%.2f", value));

            boolean isTop = topMonths.stream().anyMatch(e -> e.getKey().equals(ym));
            backgroundColors.append(isTop
                    ? "'rgba(255, 99, 132, 0.6)'"
                    : "'rgba(75, 192, 192, 0.5)'");

            if (i < sortedMonths.size() - 1) {
                labels.append(", ");
                data.append(", ");
                backgroundColors.append(", ");
            }
        }

        labels.append("]");
        data.append("]");
        backgroundColors.append("]");

        String jsCode = """
    const div = document.getElementById("chartContainer");
    if (!div) return;

    const oldCanvas = div.querySelector("canvas");
    if (oldCanvas) div.removeChild(oldCanvas);

    const canvas = document.createElement("canvas");
    canvas.id = "fuelCanvas";
    canvas.style.width = "100%%";
    canvas.style.height = "100%%";
    div.appendChild(canvas);

    function drawChart() {
        const ctx = canvas.getContext("2d");
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: %s,
                datasets: [{
                    label: 'Средний расход топлива (л)',
                    data: %s,
                    backgroundColor: %s,
                    borderColor: 'rgba(75, 192, 192, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'top' },
                    datalabels: {
                        anchor: 'end',
                        align: 'end',
                        formatter: function(value) {
                            return value.toFixed(1) + " л";
                        },
                        font: {
                            weight: 'bold'
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            },
            plugins: [ChartDataLabels]
        });
    }

    
    if (!window.Chart || !window.ChartDataLabels) {
        const script1 = document.createElement("script");
        script1.src = "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js";
        document.head.appendChild(script1);
        script1.onload = function () {
            const script2 = document.createElement("script");
            script2.src = "https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2";
            document.head.appendChild(script2);
            script2.onload = function () {
                Chart.register(ChartDataLabels);
                drawChart();
            };
        };
    } else {
        drawChart();
    }
""".formatted(labels, data, backgroundColors);

        UI.getCurrent().getPage().executeJs(jsCode);
    }

    private void drawFuelCostChart(Map<YearMonth, Double> costData) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        List<YearMonth> months = new ArrayList<>(costData.keySet());
        Collections.sort(months);

        List<Map.Entry<YearMonth, Double>> top = costData.entrySet().stream()
                .sorted(Map.Entry.<YearMonth, Double>comparingByValue().reversed())
                .limit(3)
                .toList();

        StringBuilder labels = new StringBuilder("[");
        StringBuilder data = new StringBuilder("[");
        StringBuilder colors = new StringBuilder("[");

        for (int i = 0; i < months.size(); i++) {
            YearMonth ym = months.get(i);
            labels.append("'").append(formatter.format(ym)).append("'");
            data.append(String.format(Locale.US, "%.2f", costData.get(ym)));
            boolean isTop = top.stream().anyMatch(e -> e.getKey().equals(ym));
            colors.append(isTop
                    ? "'rgba(255, 205, 86, 0.6)'"
                    : "'rgba(100, 149, 237, 0.5)'");
            if (i < months.size() - 1) {
                labels.append(", ");
                data.append(", ");
                colors.append(", ");
            }
        }

        labels.append("]");
        data.append("]");
        colors.append("]");

        String js = """
        const container = document.getElementById("chartContainer");
        if (!container) return;
        container.innerHTML = '';

        const canvas = document.createElement("canvas");
        canvas.style.width = "100%%";
        canvas.style.height = "100%%";
        container.appendChild(canvas);

        new Chart(canvas.getContext("2d"), {
            type: 'bar',
            data: {
                labels: %s,
                datasets: [{
                    label: 'Средние затраты на топливо (₽)',
                    data: %s,
                    backgroundColor: %s,
                    borderColor: 'rgba(255, 205, 86, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'top' },
                    datalabels: {
                        anchor: 'end',
                        align: 'end',
                        formatter: v => v.toFixed(0) + " ₽",
                        font: { weight: 'bold' }
                    }
                },
                scales: {
                    y: { beginAtZero: true }
                }
            },
            plugins: [ChartDataLabels]
        });
    """.formatted(labels, data, colors);

        UI.getCurrent().getPage().executeJs(js);
    }

    private void deleteCar(Car car) {
        try {
            server.deleteCar(car);

            Notification.show("Статус автомобиля изменен на 'Неактивен'");


            grid.setItems(server.getCarsForCurrentUser());

            grid.getDataProvider().refreshItem(car);
        } catch (Exception e) {
            Notification.show("Ошибка при удалении автомобиля");
            e.printStackTrace();
        }
    }

    private void openAddCarForm() {

        Dialog dialog = new Dialog();


        TextField numberCar = new TextField("Номер");
        TextField makeField = new TextField("Марка");
        TextField modelField = new TextField("Модель");
        TextField yearField = new TextField("Год выпуска");
        TextField mileageField = new TextField("Пробег");
        TextField fuelTypeField = new TextField("Тип топлива");
        DatePicker datePurchase = new DatePicker("Дата покупки");
        ComboBox<String> statusComboBox = new ComboBox<>("Статус");
        statusComboBox.setItems("Активен", "Техническое обслуживание");
        statusComboBox.setPlaceholder("Выберите статус");


        TextField serviceIntervalKmField = new TextField("Интервал между ТО (в км)");
        TextField lastServiceMileageField = new TextField("Пробег на момент последнего ТО");
        DatePicker nextServiceDateField = new DatePicker("Дата следующего ТО");


        TextField initialCostField = new TextField("Стоимость покупки");
        TextField residualValueField = new TextField("Ликвидационная стоимость");
        TextField usefulLifeYearsField = new TextField("Срок полезного использования (в годах)");
        TextField depreciationRateField = new TextField("Коэффициент амортизации");
        TextField totalUnitsField = new TextField("Общий ресурс (например, пробег)");
        TextField unitsUsedField = new TextField("Использованные единицы");


        Button saveButton = new Button("Сохранить", event -> {
            double initialCost = Double.parseDouble(initialCostField.getValue());
            double residualValue = Double.parseDouble(residualValueField.getValue());
            int usefulLife = Integer.parseInt(usefulLifeYearsField.getValue());
            double depreciationRate = Double.parseDouble(depreciationRateField.getValue());
            int totalUnits = Integer.parseInt(totalUnitsField.getValue());
            int unitsUsed = Integer.parseInt(unitsUsedField.getValue());
            Car newCar = new Car();
            newCar.setNumberCar(numberCar.getValue());
            newCar.setMake(makeField.getValue());
            newCar.setModel(modelField.getValue());
            newCar.setYear(Integer.parseInt(yearField.getValue()));
            newCar.setMileage(Double.parseDouble(mileageField.getValue()));
            newCar.setFuelType(fuelTypeField.getValue());
            newCar.setStatus(statusComboBox.getValue());
            newCar.setPurchaseDate(datePurchase.getValue());


            newCar.setServiceIntervalKm(Integer.parseInt(serviceIntervalKmField.getValue()));
            newCar.setLastServiceMileage(Double.parseDouble(lastServiceMileageField.getValue()));
            newCar.setNextServiceDate(nextServiceDateField.getValue());


            newCar.setInitialCost(initialCost);
            newCar.setResidualValue(residualValue);
            newCar.setUsefulLifeYears(usefulLife);
            newCar.setDepreciationRate(depreciationRate);
            newCar.setTotalUnits(totalUnits);
            newCar.setUnitsUsed(unitsUsed);


            server.saveCar(newCar);
            grid.setItems(server.getCarsForCurrentUser());


            dialog.close();
        });


        Button cancelButton = new Button("Отмена", event -> dialog.close());


        VerticalLayout layout = new VerticalLayout(
                numberCar, makeField, modelField, yearField, mileageField, fuelTypeField,
                statusComboBox, serviceIntervalKmField, lastServiceMileageField, nextServiceDateField,
                initialCostField, residualValueField, usefulLifeYearsField, depreciationRateField, totalUnitsField, unitsUsedField, saveButton, cancelButton
        );
        dialog.add(layout);


        dialog.open();
    }

    private void openEditCarForm(Car car) {

        Dialog dialog = new Dialog();


        TextField numberCar = new TextField("Номер", car.getNumberCar());
        numberCar.setValue(car.getNumberCar());
        TextField makeField = new TextField("Марка", car.getMake());
        makeField.setValue(car.getMake());
        TextField modelField = new TextField("Модель", car.getModel());
        modelField.setValue(car.getModel());
        TextField yearField = new TextField("Год выпуска", String.valueOf(car.getYear()));
        yearField.setValue(String.valueOf(car.getYear()));
        TextField mileageField = new TextField("Пробег", String.valueOf(car.getMileage()));
        mileageField.setValue(String.valueOf(car.getMileage()));
        TextField fuelTypeField = new TextField("Тип топлива", car.getFuelType());
        fuelTypeField.setValue(car.getFuelType());
        ComboBox<String> statusComboBox = new ComboBox<>("Статус", "Активен", "Техническое обслуживание");
        statusComboBox.setValue(car.getStatus());

        TextField initialCostField = new TextField("Стоимость покупки", String.valueOf(car.getInitialCost()));
        initialCostField.setValue(String.valueOf(car.getInitialCost()));

        TextField residualValueField = new TextField("Ликвидационная стоимость", String.valueOf(car.getResidualValue()));
        residualValueField.setValue(String.valueOf(car.getResidualValue()));

        TextField usefulLifeYearsField = new TextField("Срок полезного использования (в годах)", String.valueOf(car.getUsefulLifeYears()));
        usefulLifeYearsField.setValue(String.valueOf(car.getUsefulLifeYears()));

        TextField depreciationRateField = new TextField("Коэффициент амортизации", String.valueOf(car.getDepreciationRate()));
        depreciationRateField.setValue(String.valueOf(car.getDepreciationRate()));

        TextField totalUnitsField = new TextField("Общий ресурс (например, пробег)", String.valueOf(car.getTotalUnits()));
        totalUnitsField.setValue(String.valueOf(car.getTotalUnits()));

        TextField unitsUsedField = new TextField("Использованные единицы (например, пробег за год)", String.valueOf(car.getUnitsUsed()));
        unitsUsedField.setValue(String.valueOf(car.getUnitsUsed()));

        TextField serviceIntervalKmField = new TextField("Интервал между ТО (в км)", String.valueOf(car.getServiceIntervalKm()));
        serviceIntervalKmField.setValue(String.valueOf(car.getServiceIntervalKm()));

        TextField lastServiceMileageField = new TextField("Пробег на момент последнего ТО", String.valueOf(car.getLastServiceMileage()));
        lastServiceMileageField.setValue(String.valueOf(car.getLastServiceMileage()));

        DatePicker nextServiceDateField = new DatePicker("Дата следующего ТО");
        nextServiceDateField.setValue(car.getNextServiceDate());

        Button saveButton = new Button("Сохранить", event -> {
            car.setNumberCar(numberCar.getValue());
            car.setMake(makeField.getValue());
            car.setModel(modelField.getValue());
            car.setYear(Integer.parseInt(yearField.getValue()));
            car.setMileage(Double.parseDouble(mileageField.getValue()));
            car.setFuelType(fuelTypeField.getValue());
            car.setStatus(statusComboBox.getValue());

            car.setInitialCost(Double.parseDouble(initialCostField.getValue()));
            car.setResidualValue(Double.parseDouble(residualValueField.getValue()));
            car.setUsefulLifeYears(Integer.parseInt(usefulLifeYearsField.getValue()));
            car.setDepreciationRate(Double.parseDouble(depreciationRateField.getValue()));
            car.setTotalUnits(Integer.parseInt(totalUnitsField.getValue()));
            car.setUnitsUsed(Integer.parseInt(unitsUsedField.getValue()));


            car.setServiceIntervalKm(Integer.parseInt(serviceIntervalKmField.getValue()));
            car.setLastServiceMileage(Double.parseDouble(lastServiceMileageField.getValue()));
            car.setNextServiceDate(nextServiceDateField.getValue());

            server.saveCar(car);
            dialog.close();
            grid.setItems(server.getCarsForCurrentUser());
            Notification.show("Автомобиль обновлён");
        });

        Button cancelButton = new Button("Отмена", event -> dialog.close());

        VerticalLayout layout = new VerticalLayout(numberCar, makeField, modelField, yearField, mileageField,
                fuelTypeField, statusComboBox, initialCostField, residualValueField, usefulLifeYearsField,
                depreciationRateField, totalUnitsField, unitsUsedField,
                serviceIntervalKmField, lastServiceMileageField, nextServiceDateField,saveButton, cancelButton);

        dialog.add(layout);
        dialog.open();
    }

    private void openCarDetailsDialog(Car car) throws IOException {
        Dialog dialog = new Dialog();


        H1 title = new H1("Подробности автомобиля");


        VerticalLayout layout = new VerticalLayout();
        layout.add(new Span("id автомобиля: " + car.getId()));
        layout.add(new Span("Номер автомобиля: " + car.getNumberCar() + " "));
        layout.add(new Span("Марка: " + car.getMake() + " "));
        layout.add(new Span("Модель: " + car.getModel() + " "));
        layout.add(new Span("Год выпуска: " + car.getYear() + " "));
        layout.add(new Span("Пробег: " + car.getMileage() + " км"));
        layout.add(new Span("Тип топлива: " + car.getFuelType() + " "));
        layout.add(new Span("Статус: " + car.getStatus() + " "));
        layout.add(new H4("Добавить пройденную дистанцию"));

        TextField kmField = new TextField("Сколько проехали (км)");
        DatePicker addKmField = new DatePicker("Дата изменения");

        Button save = new Button("Сохранить", e -> {
            try {
                double km = Double.parseDouble(kmField.getValue());
                if (km <= 0) {
                    Notification.show("Введите положительное число");
                    return;
                }

                server.addMileage(car.getId(), km, addKmField.getValue());
                Notification.show("Пробег обновлён");
            } catch (NumberFormatException ex) {
                Notification.show("Введите число");
            }
        });
        if ("Неактивен".equals(car.getStatus())) {
            kmField.setVisible(false);
            addKmField.setVisible(false);
            save.setVisible(false);
            Notification.show("Автомобиль неактивен. Изменение пробега невозможно");
        }
        save.getStyle().set("margin-top", "40px");
        HorizontalLayout newLayout = new HorizontalLayout();
        newLayout.add(kmField, addKmField, save);
        layout.add(newLayout);


        server.getRefuelingMonthlyReport(car.getId()).thenAccept(monthlyReports -> {

            Div chartDiv = new Div();
            chartDiv.setId("chartDiv");
            chartDiv.setWidth("100%");
            chartDiv.setHeight("400px");


            layout.add(chartDiv);


            StringBuilder labels = new StringBuilder();
            StringBuilder data = new StringBuilder();


            List<CarParkServiece.RefuelingMonthlyReport> sortedReports = monthlyReports.stream()
                    .sorted((r1, r2) -> {

                        String[] month1 = r1.getMonth().split(" ");
                        String[] month2 = r2.getMonth().split(" ");
                        int year1 = Integer.parseInt(month1[1]);
                        int year2 = Integer.parseInt(month2[1]);
                        int monthIndex1 = Month.valueOf(month1[0].toUpperCase()).getValue();
                        int monthIndex2 = Month.valueOf(month2[0].toUpperCase()).getValue();


                        if (year1 != year2) {
                            return Integer.compare(year1, year2);
                        }
                        return Integer.compare(monthIndex1, monthIndex2);
                    })
                    .toList();


            for (CarParkServiece.RefuelingMonthlyReport report : sortedReports) {
                if (labels.length() > 0) {
                    labels.append(", ");
                    data.append(", ");
                }
                labels.append("'").append(report.getMonth()).append("'");
                data.append(report.getTotalCost());
            }


            String chartJsCode = "var ctx = document.createElement('canvas');" +
                    "ctx.id = 'myChart1111';" +
                    "document.getElementById('chartDiv').appendChild(ctx);" +
                    "var chart = new Chart(ctx, {" +
                    "    type: 'bar'," +
                    "    data: {" +
                    "        labels: [" + labels.toString() + "]," +
                    "        datasets: [{" +
                    "            label: 'Затраты на заправки'," +
                    "            data: [" + data.toString() + "]," +
                    "            backgroundColor: 'rgba(54, 162, 235, 0.2)'," +
                    "            borderColor: 'rgba(54, 162, 235, 1)'," +
                    "            borderWidth: 1" +
                    "}]" +
                    "    }," +
                    "    options: {" +
                    "        scales: {" +
                    "            y: {" +
                    "                beginAtZero: true" +
                    "            }" +
                    "        }" +
                    "    }" +
                    "});";


            getUI().ifPresent(ui -> ui.getPage().executeJs(chartJsCode));
        }).exceptionally(ex -> {

            Notification.show("Ошибка при загрузке данных заправок: " + ex.getMessage());
            return null;
        });

        double initialCost = car.getInitialCost();
        double residualValue = car.getResidualValue();
        int usefulLifeYears = car.getUsefulLifeYears();
        double depreciationRate = car.getDepreciationRate();
        int totalUnits = car.getTotalUnits();
        int unitsUsed = car.getUnitsUsed();

        double linearDepreciation = server.calculateDepreciation(initialCost, residualValue, usefulLifeYears,
                depreciationRate, totalUnits, unitsUsed, "Линейный");
        double decliningBalanceDepreciation = server.calculateDepreciation(initialCost, residualValue, usefulLifeYears,
                depreciationRate, totalUnits, unitsUsed, "Уменьшаемый остаток");
        double sumOfYearsDigitsDepreciation = server.calculateDepreciation(initialCost, residualValue, usefulLifeYears,
                depreciationRate, totalUnits, unitsUsed, "Сумма чисел лет");
        double unitsOfProductionDepreciation = server.calculateDepreciation(initialCost, residualValue, usefulLifeYears,
                depreciationRate, totalUnits, unitsUsed, "Производственный");

        layout.add(new Span("Линейная амортизация: " + linearDepreciation + " руб."));
        layout.add(new Span("Амортизация по уменьшаемому остатку: "
                + decliningBalanceDepreciation + " руб."));
        layout.add( new Span("Амортизация по сумме чисел лет: "
                + sumOfYearsDigitsDepreciation + " руб."));
        layout.add( new Span("Производственная амортизация: "
                + unitsOfProductionDepreciation + " руб."));

        double[] decliningBalanceDepreciationOnYear = server.calculateDecliningBalance(initialCost, residualValue, usefulLifeYears, depreciationRate);
        double[] sumOfYearsDigitsDepreciationOnYear = server.calculateSumOfYearsDigits(initialCost, residualValue, usefulLifeYears);


        StringBuilder decliningBalanceData = new StringBuilder();
        StringBuilder sumOfYearsDigitsData = new StringBuilder();
        StringBuilder yearsData = new StringBuilder();

        for (int i = 0; i < usefulLifeYears; i++) {
            if (i > 0) {
                decliningBalanceData.append(", ");
                sumOfYearsDigitsData.append(", ");
                yearsData.append(", ");
            }
            decliningBalanceData.append(decliningBalanceDepreciationOnYear[i]);
            sumOfYearsDigitsData.append(sumOfYearsDigitsDepreciationOnYear[i]);
            yearsData.append(i + 1);
        }

        Div chartDiv1 = new Div();
        chartDiv1.setId("chartDiv1");
        chartDiv1.setWidth("100%");
        chartDiv1.setHeight("400px");
        chartDiv1.getStyle().set("margin-top", "20px");
        chartDiv1.getStyle().set("margin-bottom", "80px");
        layout.add(chartDiv1);


        String chartJsCode = "var ctx = document.createElement('canvas');" +
                "ctx.id = 'depreciationChart';" +
                "var chartDiv = document.getElementById('chartDiv1');" +
                "if (chartDiv) {" +
                "    chartDiv.appendChild(ctx);" +
                "    var chart = new Chart(ctx, {" +
                "        type: 'line'," +
                "        data: {" +
                "            labels: [" + yearsData.toString() + "]," +
                "            datasets: [{" +
                "                label: 'Уменьшаемый остаток'," +
                "                data: [" + decliningBalanceData.toString() + "]," +
                "                borderColor: 'rgba(54, 162, 235, 1)'," +
                "                borderWidth: 1," +
                "                fill: false" +
                "            }]" +
                "        }," +
                "        options: {" +
                "            scales: {" +
                "                y: {" +
                "                    beginAtZero: true" +
                "                }" +
                "            }" +
                "        }" +
                "    });" +
                "}" +
                "var ctx2 = document.createElement('canvas');" +
                "ctx2.id = 'sumOfYearsDigitsChart';" +
                "var chartDiv2 = document.getElementById('chartDiv1');" +
                "if (chartDiv2) {" +
                "    chartDiv2.appendChild(ctx2);" +
                "    var chart2 = new Chart(ctx2, {" +
                "        type: 'line'," +
                "        data: {" +
                "            labels: [" + yearsData.toString() + "]," +
                "            datasets: [{" +
                "                label: 'Сумма чисел лет'," +
                "                data: [" + sumOfYearsDigitsData.toString() + "]," +
                "                borderColor: 'rgba(153, 102, 255, 1)'," +
                "                borderWidth: 1," +
                "                fill: false" +
                "            }]" +
                "        }," +
                "        options: {" +
                "            scales: {" +
                "                y: {" +
                "                    beginAtZero: true" +
                "                }" +
                "            }" +
                "        }" +
                "    });" +
                "}";


        getUI().ifPresent(ui -> ui.getPage().executeJs(chartJsCode));

        DatePicker fromDate = new DatePicker("С");
        DatePicker toDate   = new DatePicker("По");
        Button     filterButton = new Button("Применить фильтр");


        fromDate.setWidth("150px");
        toDate.setWidth(  "150px");


        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.setSpacing(true);
        filterLayout.setPadding(true);
        filterLayout.setAlignItems(FlexComponent.Alignment.END);
        filterLayout.setJustifyContentMode(JustifyContentMode.START);
        filterLayout.expand(toDate);
        filterLayout.getStyle().set("margin-top", "280px");
        filterLayout.getStyle().set("margin-bottom", "40px");
        layout.add(filterLayout);

        filterLayout.add(fromDate, toDate, filterButton);
        filterLayout.expand(toDate);

        layout.add(filterLayout);




        Div historyChartDiv = new Div();
        historyChartDiv.getStyle().set("margin-top", "20px");
        historyChartDiv.getStyle().set("margin-bottom", "40px");
        historyChartDiv.setId("historyChart");
        historyChartDiv.setWidth("100%");
        historyChartDiv.setHeight("400px");

        Div averageChartDiv = new Div();
        averageChartDiv.getStyle().set("margin-top", "10px");
        averageChartDiv.getStyle().set("margin-bottom", "40px");
        averageChartDiv.setId("averageChart");
        averageChartDiv.setWidth("100%");
        averageChartDiv.setHeight("400px");

        H4 historyTitle = new H4("История поездок");
        historyTitle.getStyle().set("margin-top", "30px");
        H4 averageTitle = new H4("Средняя дистанция по месяцам");
        averageTitle.getStyle().set("margin-top", "30px");

        historyChartDiv.getStyle().set("margin-bottom", "40px");
        averageChartDiv.getStyle().set("margin-bottom", "40px");

        layout.add(historyTitle, historyChartDiv);
        layout.add(averageTitle, averageChartDiv);




        Runnable updateCharts = () -> {
            List<MileageLog> allLogs = server.getMileageLogsForCar(car.getId());


            List<MileageLog> filteredLogs = allLogs.stream()
                    .filter(log -> {
                        LocalDate date = log.getDate();
                        return (fromDate.getValue() == null || !date.isBefore(fromDate.getValue())) &&
                                (toDate.getValue() == null || !date.isAfter(toDate.getValue()));
                    })
                    .sorted(Comparator.comparing(MileageLog::getDate))
                    .toList();


            Map<String, Double> monthlyAverages = filteredLogs.stream()
                    .collect(Collectors.groupingBy(
                            log -> log.getDate().getMonth() + " " + log.getDate().getYear(),
                            LinkedHashMap::new,
                            Collectors.averagingDouble(MileageLog::getKilometers)
                    ));


            StringBuilder labels1 = new StringBuilder();
            StringBuilder data1 = new StringBuilder();

            for (MileageLog log : filteredLogs) {
                if (labels1.length() > 0) {
                    labels1.append(", ");
                    data1.append(", ");
                }
                labels1.append("'").append(log.getDate()).append("'");
                data1.append(log.getKilometers());
            }

            StringBuilder labels2 = new StringBuilder();
            StringBuilder data2 = new StringBuilder();
            for (Map.Entry<String, Double> entry : monthlyAverages.entrySet()) {
                if (labels2.length() > 0) {
                    labels2.append(", ");
                    data2.append(", ");
                }
                labels2.append("'").append(entry.getKey()).append("'");
                data2.append(String.format("%.2f", entry.getValue()));
            }


            getUI().ifPresent(ui -> ui.getPage().executeJs("""
        document.getElementById('historyChart').innerHTML = '';
        document.getElementById('averageChart').innerHTML = '';

        const createChart = (id, label, labels, data, color) => {
            const ctx = document.createElement('canvas');
            document.getElementById(id).appendChild(ctx);
            new Chart(ctx, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        label: label,
                        data: data,
                        borderColor: color,
                        borderWidth: 2,
                        fill: false
                    }]
                },
                options: {
                    responsive: true,
                    scales: {
                        y: { beginAtZero: true }
                    }
                }
            });
        };

        createChart('historyChart', 'Поездки (км)', [%s], [%s], 'rgba(255, 99, 132, 1)');
        createChart('averageChart', 'Среднее по месяцам (км)', [%s], [%s], 'rgba(54, 162, 235, 1)');
        """.formatted(labels1, data1, labels2, data2)));
        };


        filterButton.addClickListener(e -> updateCharts.run());


        updateCharts.run();
        Button addTechnicalMaintenanceButton = new Button("Добавить ТО", e -> openAddTechnicalMaintenanceForm(car));
        if ("Неактивен".equals(car.getStatus())) {
            addTechnicalMaintenanceButton.setVisible(false);
            Notification.show("Автомобиль неактивен. Добавление ТО невозможно");
        }
        layout.add(addTechnicalMaintenanceButton);


        List<TechnicalMaintenance> technicalMaintenances = server.getTechnicalMaintenancesForCar(car.getId());
        VerticalLayout archerLayoutTO = new VerticalLayout();
        archerLayoutTO.setPadding(false);
        archerLayoutTO.setSpacing(true);
        Div file = new Div();
        RadioButtonGroup<String> formatSelector = new RadioButtonGroup<>();
        formatSelector.setLabel("Выберите формат:");
        formatSelector.setItems("TXT", "XLSX");
        formatSelector.setValue("TXT");

        Anchor anchor = new Anchor();

        Button downloadButton = new Button("Скачать отчет", event -> {
            try {
                String format = formatSelector.getValue();
                StreamResource resource = format.equals("TXT")
                        ? generateTxtFile(technicalMaintenances)
                        : generateExcelFile(technicalMaintenances);
                anchor.setHref(resource);
                anchor.getElement().callJsFunction("click");
            } catch (IOException e) {
                Notification.show("Ошибка при генерации отчета");
            }
        });

        anchor.getElement().setAttribute("download", true);
        file.add(downloadButton, anchor);
        archerLayoutTO.add(formatSelector, file);

        List<Refueling> refuelings = server.getRefuelingsForCar(car.getId());
        List<MileageLog> mileages = server.getMileageLogsForCar(car.getId());
        RadioButtonGroup<String> formatSelectorRefuelingsAndMileages = new RadioButtonGroup<>();
        formatSelectorRefuelingsAndMileages.setItems("TXT", "XLSX");
        formatSelectorRefuelingsAndMileages .setLabel("Формат отчета");
        formatSelectorRefuelingsAndMileages .setValue("TXT");

        Anchor anchorRefuelingsAndMileages = new Anchor("", "");
        anchorRefuelingsAndMileages.getElement().setAttribute("download", true);

        Button downloadButtonRefuelingsAndMileages  = new Button("Скачать отчет", event -> {
            try {
                StreamResource resource = formatSelectorRefuelingsAndMileages .getValue().equals("TXT")
                        ? generateFuelAndMileageTxt(refuelings, mileages)
                        : generateFuelAndMileageExcel(refuelings, mileages);
                anchorRefuelingsAndMileages.setHref(resource);
                anchorRefuelingsAndMileages.getElement().callJsFunction("click");
            } catch (IOException e) {
                Notification.show("Ошибка при создании отчета");
            }
        });

        VerticalLayout downloadLayoutRefuelingsAndMileages = new VerticalLayout(formatSelectorRefuelingsAndMileages , downloadButtonRefuelingsAndMileages, anchorRefuelingsAndMileages);


        layout.add(new H1("Сохранить данные ТО"), archerLayoutTO, new H1("Сохранить данные о пробеге и заправках"), downloadLayoutRefuelingsAndMileages);




        Button closeButton = new Button("Закрыть", event -> dialog.close());
        layout.add(closeButton);

        dialog.add(title, layout);
        dialog.open();
    }

    private StreamResource generateFuelAndMileageTxt(List<Refueling> refuelings, List<MileageLog> mileages) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append("Отчет по заправкам:\n");
        sb.append("===================\n\n");
        for (Refueling r : refuelings) {
            sb.append("Дата: ").append(r.getRefuelDate()).append("\n");
            sb.append("Количество топлива: ").append(r.getFuelQuantity()).append(" л\n");
            sb.append("Стоимость: ").append(r.getRefuelCost()).append(" руб\n");
            sb.append("-----------------------------\n");
        }

        sb.append("\nОтчет по пробегу:\n");
        sb.append("=================\n\n");
        for (MileageLog m : mileages) {
            sb.append("Дата: ").append(m.getDate()).append("\n");
            sb.append("Пробег: ").append(m.getKilometers()).append(" км\n");
            sb.append("-----------------------------\n");
        }

        File file = new File("fuel_mileage_report.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sb.toString());
        }

        byte[] bytes = Files.readAllBytes(file.toPath());
        return new StreamResource(file.getName(), () -> new ByteArrayInputStream(bytes));
    }


    private StreamResource generateFuelAndMileageExcel(List<Refueling> refuelings, List<MileageLog> mileages) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        Sheet refuelSheet = workbook.createSheet("Заправки");
        Row refuelHeader = refuelSheet.createRow(0);
        refuelHeader.createCell(0).setCellValue("Дата");
        refuelHeader.createCell(1).setCellValue("Топливо (л)");
        refuelHeader.createCell(2).setCellValue("Стоимость (руб)");

        int r = 1;
        for (Refueling refuel : refuelings) {
            Row row = refuelSheet.createRow(r++);
            row.createCell(0).setCellValue(refuel.getRefuelDate().toString());
            row.createCell(1).setCellValue(refuel.getFuelQuantity());
            row.createCell(2).setCellValue(refuel.getRefuelCost());
        }

        Sheet mileageSheet = workbook.createSheet("Пробег");
        Row mileageHeader = mileageSheet.createRow(0);
        mileageHeader.createCell(0).setCellValue("Дата");
        mileageHeader.createCell(1).setCellValue("Пробег (км)");

        int m = 1;
        for (MileageLog mileage : mileages) {
            Row row = mileageSheet.createRow(m++);
            row.createCell(0).setCellValue(mileage.getDate().toString());
            row.createCell(1).setCellValue(mileage.getKilometers());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        byte[] bytes = out.toByteArray();
        return new StreamResource("fuel_mileage_report.xlsx", () -> new ByteArrayInputStream(bytes));
    }


    private StreamResource generateTxtFile(List<TechnicalMaintenance> technicalMaintenances) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append("Отчет о техническом обслуживании\n");
        sb.append("=================================\n\n");


        for (TechnicalMaintenance maintenance : technicalMaintenances) {
            sb.append("Дата ТО: ").append(maintenance.getServiceDate()).append("\n");
            sb.append("Описание работ: ").append(maintenance.getDescription()).append("\n");
            sb.append("Стоимость: ").append(maintenance.getCost()).append(" руб.\n");
            sb.append("Поставщик услуги: ").append(maintenance.getServiceProvider()).append("\n");
            sb.append("Статус: ").append(maintenance.getStatus()).append("\n");
            sb.append("---------------------------------\n");
        }


        File file = new File("technical_maintenance_report.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sb.toString());
        }
        byte[] fileBytes = Files.readAllBytes(file.toPath());


        return new StreamResource(file.getName(), () -> new ByteArrayInputStream(fileBytes));
    }

    private StreamResource generateExcelFile(List<TechnicalMaintenance> list) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("ТО");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Дата");
        header.createCell(1).setCellValue("Описание");
        header.createCell(2).setCellValue("Стоимость");
        header.createCell(3).setCellValue("Поставщик");
        header.createCell(4).setCellValue("Статус");

        int rowNum = 1;
        for (TechnicalMaintenance item : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getServiceDate().toString());
            row.createCell(1).setCellValue(item.getDescription());
            row.createCell(2).setCellValue(item.getCost());
            row.createCell(3).setCellValue(item.getServiceProvider());
            row.createCell(4).setCellValue(item.getStatus());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        byte[] bytes = out.toByteArray();
        return new StreamResource("technical_maintenance_report.xlsx", () -> new ByteArrayInputStream(bytes));
    }


    private void sendFileForDownload(File file) {
        try {

            byte[] fileBytes = Files.readAllBytes(file.toPath());


            StreamResource resource = new StreamResource(file.getName(), () -> new ByteArrayInputStream(fileBytes));


            Anchor downloadLink = new Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);


            downloadLink.getElement().executeJs("this.click()");


            Notification.show("Файл с отчетом о ТО готов для скачивания.");
        } catch (IOException e) {
            Notification.show("Ошибка при отправке файла для скачивания: " + e.getMessage());
        }
    }

    private void openAddTechnicalMaintenanceForm(Car car) {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");


        DatePicker serviceDateField = new DatePicker("Дата проведения ТО");
        serviceDateField.setValue(LocalDate.now());

        TextField descriptionField = new TextField("Описание работ");
        TextField costField = new TextField("Стоимость ТО");
        TextField serviceProviderField = new TextField("СТО или механик");
        ComboBox<String> statusComboBox = new ComboBox<>("Статус");
        statusComboBox.setItems("Выполнено", "Запланировано", "Отменено");
        statusComboBox.setPlaceholder("Выберите статус");


        Button saveButton = new Button("Сохранить", event -> {
            try {

                Double cost = Double.parseDouble(costField.getValue());


                server.saveTechnicalMaintenance(
                        car.getId(),
                        serviceDateField.getValue(),
                        descriptionField.getValue(),
                        cost,
                        serviceProviderField.getValue(),
                        statusComboBox.getValue()
                );


                grid.setItems(server.getCarsForCurrentUser());

                Notification.show("Отчёт о ТО сохранён");
                dialog.close();

            } catch (NumberFormatException ex) {
                Notification.show("Ошибка: Пожалуйста, введите правильную стоимость");
            }
        });


        Button cancelButton = new Button("Отмена", event -> dialog.close());


        dialog.add(new VerticalLayout(
                serviceDateField,
                descriptionField,
                costField,
                serviceProviderField,
                statusComboBox,
                saveButton,
                cancelButton
        ));

        dialog.open();
    }

    private void openAddRefuelForm(Car car) {
        Dialog dialog = new Dialog();


        TextField fuelQuantityField = new TextField("Количество топлива (литры)");
        TextField refuelCostField = new TextField("Стоимость заправки");
        DatePicker fuelingDateField = new DatePicker("Дата заправки");

        Button saveButton = new Button("Сохранить", event -> {
            double fuelQuantity = Double.parseDouble(fuelQuantityField.getValue());
            double refuelCost = Double.parseDouble(refuelCostField.getValue());
            LocalDate fuelingDate = fuelingDateField.getValue();


            server.addRefueling(car.getId(), fuelQuantity, refuelCost, fuelingDate);


            dialog.close();
        });

        Button cancelButton = new Button("Отмена", event -> dialog.close());


        VerticalLayout layout = new VerticalLayout(fuelQuantityField, refuelCostField, fuelingDateField, saveButton, cancelButton);
        dialog.add(layout);

        dialog.open();
    }
}
