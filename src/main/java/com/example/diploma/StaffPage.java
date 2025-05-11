package com.example.diploma;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Route("staff")
@JavaScript("https://cdn.jsdelivr.net/npm/chart.js")
@JavaScript("https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2.2.0/dist/chartjs-plugin-datalabels.min.js")
@PreAuthorize("hasRole('USER')")
public class StaffPage extends VerticalLayout {

    private final StaffService staffService;

    private final Grid<Staff> grid = new Grid<>(Staff.class, false);

    public StaffPage(StaffService staffService) {
        this.staffService = staffService;

        configureGrid();
        //1 1
        DatePicker salaryFrom = new DatePicker("С:");
        DatePicker salaryTo = new DatePicker("По:");
        salaryFrom.setValue(LocalDate.now().minusMonths(6).withDayOfMonth(1));
        salaryTo.setValue(LocalDate.now());

// 📊 Контейнер для графика
        Div salaryChartDiv = createChartDiv("totalSalaryChart", "100%", "400px");

// 🔘 Кнопка фильтра
        Button salaryButton = new Button("Показать");
        salaryButton.addClickListener(e -> {
            LocalDate from = salaryFrom.getValue();
            LocalDate to = salaryTo.getValue();
            if (from == null || to == null || from.isAfter(to)) {
                Notification.show("Укажите корректный диапазон дат");
                return;
            }
            Map<YearMonth, BigDecimal> data = staffService.calculateMonthlySalaryExpenses(from, to);
            drawTotalSalaryChart(data);
        });

// 📦 Обёртка
        Component salaryFilterPanel = createFilterPanel(salaryFrom, salaryTo, salaryButton);
        VerticalLayout salaryLayout = new VerticalLayout(salaryChartDiv, salaryFilterPanel);
        salaryLayout.setWidth("50%");

// ⏱ Первая отрисовка
        Map<YearMonth, BigDecimal> initialData = staffService.calculateMonthlySalaryExpenses(
                salaryFrom.getValue(), salaryTo.getValue()
        );
        //1 строка 2 столбец
        Tab totalTab = new Tab("Суммарно");
        Tab salaryTab = new Tab("Зарплата");
        Tab maintenanceTab = new Tab("ТО");
        Tab fuelTab = new Tab("Топливо");
        Tabs expenseTabs = new Tabs(totalTab, salaryTab, maintenanceTab, fuelTab);

// Фильтр по дате
        DatePicker expenseFrom = new DatePicker("С:");
        DatePicker expenseTo = new DatePicker("По:");
        expenseFrom.setValue(LocalDate.now().minusMonths(6).withDayOfMonth(1));
        expenseTo.setValue(LocalDate.now());
        Button expenseButton = new Button("Показать");
        Component expenseFilterPanel = createFilterPanel(expenseFrom, expenseTo, expenseButton);

// Контейнер для графика
        Div expenseChart = createChartDiv("staffExpenseChart", "100%", "500px");
        VerticalLayout expenseLayout = new VerticalLayout(expenseTabs, expenseChart, expenseFilterPanel);
        expenseLayout.setWidth("60%");
        expenseLayout.getStyle().set("margin-bottom", "0");

        Runnable updateExpenseChart = () -> {
            LocalDate from = expenseFrom.getValue();
            LocalDate to = expenseTo.getValue();
            if (from == null || to == null || from.isAfter(to)) {
                Notification.show("Укажите корректный диапазон дат");
                return;
            }

            Map<Staff, BigDecimal[]> data = staffService.getStaffExpenses(from, to);
            drawStaffExpenseChart(data, expenseTabs.getSelectedTab());
        };

        expenseButton.addClickListener(e -> updateExpenseChart.run());
        expenseTabs.addSelectedChangeListener(e -> updateExpenseChart.run());
        updateExpenseChart.run();

        HorizontalLayout bottomRow = new HorizontalLayout(salaryLayout, expenseLayout);
        bottomRow.setWidthFull();
        bottomRow.setAlignItems(FlexComponent.Alignment.END);
        drawTotalSalaryChart(initialData);
        add(createToolbar(), grid, bottomRow);
        updateGrid();

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }



    private Component createFilterPanel(DatePicker from, DatePicker to, Button apply) {
        HorizontalLayout layout = new HorizontalLayout(from, to, apply);
        layout.setAlignItems(FlexComponent.Alignment.END);
        return layout;
    }

    private Div createChartDiv(String id, String width, String height) {
        Div div = new Div();
        div.setId(id);
        div.setWidth(width);
        div.setHeight(height);
        div.getStyle().set("border", "1px solid #ccc");
        return div;
    }

    private void configureGrid() {
        grid.addColumn(Staff::getFullName).setHeader("ФИО").setAutoWidth(true)
                .setSortable(true)
                .setFlexGrow(0);
        grid.addColumn(Staff::getPosition).setHeader("Должность")
                .setAutoWidth(true)
                .setSortable(true);
        grid.addColumn(staff -> staff.getPaymentType().name()).setHeader("Тип оплаты")
                .setAutoWidth(true)
                .setSortable(true);
        grid.addColumn(staff -> staff.getAssignedVehicle() != null
                ? staff.getAssignedVehicle().getNumberCar()
                : "—").setHeader("Автомобиль");
        grid.addColumn(staff -> staff.getStatus().name()).setHeader("Статус")
                .setAutoWidth(true)
                .setSortable(true);


        grid.addComponentColumn(this::createActionButtons).setHeader("Действия").setAutoWidth(true)
                .setFlexGrow(0);
        grid.setWidthFull();           // адаптируется по ширине
        grid.setAllRowsVisible(true);
    }


    private HorizontalLayout createActionButtons(Staff staff) {
        Button editButton = new Button("Редактировать", e -> openEditStaffDialog(staff));
        Button deleteButton = new Button("Удалить", e -> confirmDelete(staff));
        Button detailsButton = new Button("Подробнее", e -> openStaffDetailsDialog(staff));
        Button shiftButton = new Button("Добавить смену", e -> openAddShiftDialog(staff));
        deleteButton.getStyle().set("color", "red");

        return new HorizontalLayout(editButton, shiftButton, detailsButton, deleteButton);
    }

    private void drawStaffExpenseChart(Map<Staff, BigDecimal[]> data, Tab selectedTab) {
        List<String> labels = data.keySet().stream()
                .map(Staff::getFullName)
                .toList();

        List<BigDecimal> values = data.values().stream()
                .map(arr -> {
                    if (selectedTab.getLabel().equals("Зарплата")) return arr[0];
                    else if (selectedTab.getLabel().equals("ТО")) return arr[1];
                    else if (selectedTab.getLabel().equals("Топливо")) return arr[2];
                    else return arr[0].add(arr[1]).add(arr[2]);
                })
                .toList();

        String jsLabels = labels.stream().map(n -> "'" + n + "'").collect(Collectors.joining(", ", "[", "]"));
        String jsValues = values.stream().map(BigDecimal::toPlainString).collect(Collectors.joining(", ", "[", "]"));

        String js = """
        const container = document.getElementById('staffExpenseChart');
        if (!container) return;
        container.innerHTML = '';

        const canvas = document.createElement('canvas');
        container.appendChild(canvas);

        if (typeof ChartDataLabels !== 'undefined') {
            Chart.register(ChartDataLabels);
        }

        new Chart(canvas.getContext('2d'), {
            type: 'bar',
            data: {
                labels: %s,
                datasets: [{
                    label: 'Расходы (₽)',
                    data: %s,
                    backgroundColor: 'rgba(75, 192, 192, 0.5)',
                    borderColor: 'rgba(75, 192, 192, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                plugins: {
                    legend: { display: false },
                    datalabels: {
                        anchor: 'end',
                        align: 'right',
                        formatter: v => v.toFixed(0) + ' ₽',
                        font: { weight: 'bold', size: 11 },
                        color: '#333'
                    }
                },
                scales: {
                    x: {
                        beginAtZero: true,
                        title: { display: true, text: '₽' }
                    },
                    y: {
                        title: { display: true, text: 'Сотрудник' }
                    }
                }
            },
            plugins: [ChartDataLabels]
        });
    """.formatted(jsLabels, jsValues);

        UI.getCurrent().getPage().executeJs(js);
    }

    private void drawTotalSalaryChart(Map<YearMonth, BigDecimal> data) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLL yyyy", new Locale("ru"));
        List<YearMonth> sorted = new ArrayList<>(data.keySet());
        Collections.sort(sorted);

        List<YearMonth> top = data.entrySet().stream()
                .sorted(Map.Entry.<YearMonth, BigDecimal>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        StringBuilder labels = new StringBuilder("[");
        StringBuilder values = new StringBuilder("[");
        StringBuilder colors = new StringBuilder("[");

        for (int i = 0; i < sorted.size(); i++) {
            YearMonth ym = sorted.get(i);
            labels.append("'").append(formatter.format(ym)).append("'");
            values.append(data.get(ym).setScale(2, RoundingMode.HALF_UP));

            boolean isTop = top.contains(ym);
            colors.append(isTop
                    ? "'rgba(75, 192, 192, 0.7)'"
                    : "'rgba(54, 162, 235, 0.5)'");

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
                drawTotalSalaryChart();
            };
            document.head.appendChild(script);
        } else {
            drawTotalSalaryChart();
        }

        function drawTotalSalaryChart() {
            const container = document.getElementById("totalSalaryChart");
            if (!container) return;
            container.innerHTML = '';

            const canvas = document.createElement("canvas");
            container.appendChild(canvas);

            new Chart(canvas.getContext("2d"), {
                type: 'bar',
                data: {
                    labels: %s,
                    datasets: [{
                        label: 'Зарплатные расходы (₽)',
                        data: %s,
                        backgroundColor: %s,
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    layout: {
                        padding: { bottom: 20 }
                    },
                    plugins: {
                        legend: { position: 'top' },
                        datalabels: {
                            anchor: 'end',
                            align: 'end',
                            formatter: v => v.toFixed(2) + " ₽",
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
                        },
                        x: {
                            title: {
                                display: true,
                                text: 'Месяц'
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

    private void openAddShiftDialog(Staff staff) {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        DateTimePicker startField = new DateTimePicker("Начало смены");
        DateTimePicker endField = new DateTimePicker("Конец смены");

        // поле только для пробега
        NumberField distanceField = new NumberField("Пробег (км)");

        boolean isDriver = isDriver(staff.getPosition());
        Car car = staff.getAssignedVehicle();

        // показываем пробег только водителям с машиной
        distanceField.setVisible(isDriver && car != null);

        Button save = new Button("Сохранить", e -> {
            // Создаём и сохраняем смену
            Shift shift = new Shift();
            shift.setStaff(staff);
            shift.setStartTime(startField.getValue());
            shift.setEndTime(endField.getValue());
            shift = staffService.saveShift(shift);

            // Если это водитель с авто — сохраняем пробег
            if (isDriver && car != null) {
                MileageLog mileage = new MileageLog();
                mileage.setShift(shift);
                mileage.setCar(car);
                mileage.setDate(startField.getValue().toLocalDate());
                mileage.setKilometers(distanceField.getValue());
                staffService.saveMileage(mileage);
            }

            dialog.close();
        });

        Button cancel = new Button("Отмена", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(
                startField, endField
        );

        if (isDriver && car != null) {
            layout.add(distanceField);
        }

        layout.add(new HorizontalLayout(save, cancel));
        dialog.add(layout);
        dialog.open();
    }

    private void openStaffDetailsDialog(Staff staff) {
        Dialog dialog = new Dialog();
        dialog.setWidth("900px");

        VerticalLayout layout = new VerticalLayout();
        layout.add(
                new Span("ФИО: " + staff.getFullName()),
                new Span("Должность: " + staff.getPosition()),
                new Span("Тип оплаты: " + staff.getPaymentType()),
                new Span("Дата приема: " + staff.getHireDate()),
                new Span("Дата увольнения: " + (staff.getDismissedDate() != null ? staff.getDismissedDate() : "—")),
                new Span("Ставка в час: " + (staff.getHourlyRate() != null ? staff.getHourlyRate() + " ₽" : "—")),
                new Span("Фиксированная з/п: " + (staff.getFixedSalary() != null ? staff.getFixedSalary() + " ₽" : "—")),
                new Span("Статус: " + staff.getStatus()),
                new Span("Автомобиль: " + (staff.getAssignedVehicle() != null ? staff.getAssignedVehicle().getNumberCar() : "—"))
        );

        if (staff.getHourlyRate() != null) {
            ComboBox<String> modeSelector = new ComboBox<>("Группировка");
            modeSelector.setItems("По дням", "По неделям", "По месяцам");
            modeSelector.setValue("По дням");

            DatePicker startDate = new DatePicker("Начало периода");
            DatePicker endDate = new DatePicker("Конец периода");
            startDate.setValue(LocalDate.now().minusMonths(1));
            endDate.setValue(LocalDate.now());

            Div chartDiv = new Div();
            chartDiv.setId("salaryChartContainer");
            chartDiv.setWidthFull();
            chartDiv.setHeight("400px");

            Span totalLabel = new Span();

            layout.add(modeSelector, new HorizontalLayout(startDate, endDate), chartDiv, totalLabel);

            Runnable drawChart = () -> {
                String mode = modeSelector.getValue();
                LocalDate start = startDate.getValue();
                LocalDate end = endDate.getValue();

                Map<String, Double> aggregated = staffService.aggregateSalaryByMode(staff, mode, start, end);
                List<String> sortedKeys = new ArrayList<>(aggregated.keySet());
                Collections.sort(sortedKeys);

                List<String> labelList = new ArrayList<>();
                List<String> valueList = new ArrayList<>();

                for (String label : sortedKeys) {
                    labelList.add("'" + label + "'");
                    valueList.add(String.format(Locale.US, "%.2f", aggregated.get(label)));
                }

                String labels = "[" + String.join(", ", labelList) + "]";
                String values = "[" + String.join(", ", valueList) + "]";

                double total = aggregated.values().stream().mapToDouble(Double::doubleValue).sum();
                totalLabel.setText("Итого за период: " + String.format("%.2f ₽", total));

                String js = """
                const container = document.getElementById("salaryChartContainer");
                if (!container) return;
                container.innerHTML = '';

                const canvas = document.createElement("canvas");
                canvas.style.width = "100%%";
                canvas.style.height = "100%%";
                container.appendChild(canvas);

                function drawSalaryChart() {
                    new Chart(canvas.getContext("2d"), {
                        type: 'line',
                        data: {
                            labels: %s,
                            datasets: [{
                                label: 'Начислено (₽)',
                                data: %s,
                                borderWidth: 2,
                                fill: false,
                                borderColor: 'rgba(75, 192, 192, 1)',
                                tension: 0.3,
                                pointRadius: 4,
                                pointBackgroundColor: 'rgba(75, 192, 192, 1)'
                            }]
                        },
                        options: {
                            responsive: true,
                            plugins: {
                                legend: {
                                    position: 'top'
                                }
                            },
                            scales: {
                                y: {
                                    beginAtZero: true,
                                    title: {
                                        display: true,
                                        text: 'Сумма, ₽'
                                    }
                                },
                                x: {
                                    title: {
                                        display: true,
                                        text: 'Период'
                                    }
                                }
                            }
                        }
                    });
                }

                if (!window.Chart) {
                    const chartScript = document.createElement("script");
                    chartScript.src = "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js";
                    chartScript.onload = () => drawSalaryChart();
                    document.head.appendChild(chartScript);
                } else {
                    drawSalaryChart();
                }
            """.formatted(labels, values);

                UI.getCurrent().getPage().executeJs(js);
            };

            modeSelector.addValueChangeListener(e -> drawChart.run());
            startDate.addValueChangeListener(e -> drawChart.run());
            endDate.addValueChangeListener(e -> drawChart.run());

            dialog.addOpenedChangeListener(e -> {
                if (e.isOpened()) drawChart.run();
            });
        }

        ComboBox<String> modeCombo = new ComboBox<>("Анализ по:");
        modeCombo.setItems("По сменам", "По пробегу");
        modeCombo.setValue("По сменам");

        ComboBox<String> salaryModeCombo = new ComboBox<>("Тип расчёта:");
        salaryModeCombo.setItems("POCHASOVAYA", "FIXED");
        salaryModeCombo.setValue("POCHASOVAYA");

        DatePicker startDate = new DatePicker("Начало периода");
        DatePicker endDate = new DatePicker("Конец периода");
        startDate.setValue(LocalDate.now().minusMonths(1));
        endDate.setValue(LocalDate.now());

        Div chartDiv = new Div();
        chartDiv.setId("workSalaryChart");
        chartDiv.setWidthFull();
        chartDiv.setHeight("400px");

        layout.add(new HorizontalLayout(modeCombo, salaryModeCombo), new HorizontalLayout(startDate, endDate), chartDiv);

        Runnable drawWorkChart = () -> {
            String mode = modeCombo.getValue().equals("По сменам") ? "shifts" : "distance";
            String salaryMode = salaryModeCombo.getValue().equals("FIXED") ? "FIXED" : "HOURLY";
            LocalDate start = startDate.getValue();
            LocalDate end = endDate.getValue();

            Map<String, Pair<Double, Double>> data = staffService.getWorkVsSalary(staff, mode, salaryMode, start, end);
            List<String> labels = new ArrayList<>();
            List<String> salaries = new ArrayList<>();
            List<String> tooltips = new ArrayList<>();

            for (var entry : data.entrySet()) {
                labels.add("'" + entry.getKey() + "'");
                salaries.add(String.format(Locale.US, "%.2f", entry.getValue().getRight()));
                tooltips.add(String.format(Locale.US, "%.2f", entry.getValue().getLeft()));
            }

            String labelJson = "[" + String.join(", ", labels) + "]";
            String salaryJson = "[" + String.join(", ", salaries) + "]";
            String tooltipJson = "[" + String.join(", ", tooltips) + "]";

            String js = """
        const container = document.getElementById('workSalaryChart');
        if (!container) return;
        container.innerHTML = '';

        const canvas = document.createElement("canvas");
        container.appendChild(canvas);

        new Chart(canvas.getContext("2d"), {
            type: 'bar',
            data: {
                labels: %s,
                datasets: [{
                    label: 'Начислено (₽)',
                    data: %s,
                    backgroundColor: 'rgba(75, 192, 192, 0.6)',
                    borderColor: 'rgba(75, 192, 192, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    tooltip: {
                        callbacks: {
                            afterLabel: (ctx) => {
                                const tip = %s[ctx.dataIndex];
                                return '%s: ' + tip;
                            }
                        }
                    },
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        title: { display: true, text: '₽' }
                    },
                    x: {
                        title: { display: true, text: 'Дата' }
                    }
                }
            }
        });
    """.formatted(labelJson, salaryJson, tooltipJson, modeCombo.getValue());

            UI.getCurrent().getPage().executeJs(js);
        };

        modeCombo.addValueChangeListener(e -> drawWorkChart.run());
        salaryModeCombo.addValueChangeListener(e -> drawWorkChart.run());
        startDate.addValueChangeListener(e -> drawWorkChart.run());
        endDate.addValueChangeListener(e -> drawWorkChart.run());

        dialog.addOpenedChangeListener(e -> { if (e.isOpened()) drawWorkChart.run(); });

        Button closeButton = new Button("Закрыть", e -> dialog.close());
        layout.add(closeButton);

        dialog.add(layout);
        dialog.open();
    }

    private void confirmDelete(Staff staff) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Уволить сотрудника");

        Span message = new Span("Вы уверены, что хотите уволить сотрудника \"" + staff.getFullName() + "\"?");
        DatePicker dismissedDatePicker = new DatePicker("Дата увольнения");
        dismissedDatePicker.setValue(LocalDate.now());

        Button confirm = new Button("Уволить", e -> {
            LocalDate selectedDate = dismissedDatePicker.getValue();
            if (selectedDate == null || selectedDate.isBefore(staff.getHireDate())) {
                Notification.show("Некорректная дата увольнения", 3000, Notification.Position.MIDDLE);
                return;
            }

            staff.setStatus(StaffStatus.DISMISSED);
            staff.setDismissedDate(selectedDate);
            staff.setAssignedVehicle(null);
            staffService.save(staff);
            updateGrid();
            confirmDialog.close();
        });

        Button cancel = new Button("Отмена", e -> confirmDialog.close());

        HorizontalLayout buttons = new HorizontalLayout(confirm, cancel);
        confirm.getStyle().set("color", "white");
        confirm.getStyle().set("background-color", "red");

        confirmDialog.add(new VerticalLayout(
                message,
                dismissedDatePicker,
                buttons
        ));

        confirmDialog.open();
    }

    private void openEditStaffDialog(Staff staff) {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        TextField nameField = new TextField("ФИО");
        nameField.setValue(staff.getFullName());

        TextField positionField = new TextField("Должность");
        positionField.setValue(staff.getPosition());

        ComboBox<PaymentType> paymentTypeCombo = new ComboBox<>("Тип оплаты");
        paymentTypeCombo.setItems(PaymentType.values());
        paymentTypeCombo.setValue(staff.getPaymentType());

        NumberField hourlyRateField = new NumberField("Ставка в час (₽)");
        hourlyRateField.setValue(staff.getHourlyRate() != null ? staff.getHourlyRate().doubleValue() : 0.0);

        NumberField fixedSalaryField = new NumberField("Фиксированная з/п (₽)");
        fixedSalaryField.setValue(staff.getFixedSalary() != null ? staff.getFixedSalary().doubleValue() : 0.0);

        DatePicker hireDatePicker = new DatePicker("Дата приёма");
        hireDatePicker.setValue(staff.getHireDate() != null ? staff.getHireDate() : LocalDate.now());
        ComboBox<Car> carCombo = new ComboBox<>("Автомобиль");
        carCombo.setItemLabelGenerator(Car::getNumberCar);
        carCombo.setItems(staffService.getAvailableCarsForUser());
        carCombo.setValue(staff.getAssignedVehicle());
        carCombo.setVisible(isDriver(positionField.getValue()));

        ComboBox<StaffStatus> statusCombo = new ComboBox<>("Статус");
        statusCombo.setItems(StaffStatus.values());
        statusCombo.setValue(staff.getStatus());

        // 👇 логика скрытия поля автомобиля при смене должности
        positionField.addValueChangeListener(event -> {
            boolean isDriver = isDriver(event.getValue());
            carCombo.setVisible(isDriver);
            if (!isDriver) carCombo.clear();
        });

        Button saveButton = new Button("Сохранить", event -> {
            staff.setFullName(nameField.getValue());
            staff.setPosition(positionField.getValue());
            staff.setPaymentType(paymentTypeCombo.getValue());
            if (hourlyRateField.getValue() != null)
                staff.setHourlyRate(BigDecimal.valueOf(hourlyRateField.getValue()));
            if (fixedSalaryField.getValue() != null)
                staff.setFixedSalary(BigDecimal.valueOf(fixedSalaryField.getValue()));
            staff.setHireDate(hireDatePicker.getValue());
            staff.setAssignedVehicle(carCombo.getValue());
            staff.setStatus(statusCombo.getValue());

            staffService.save(staff);
            updateGrid();
            dialog.close();
        });



        Button cancelButton = new Button("Отмена", e -> dialog.close());

        dialog.add(new VerticalLayout(
                nameField, positionField, paymentTypeCombo, hireDatePicker,
                hourlyRateField, fixedSalaryField,
                carCombo, statusCombo,
                new HorizontalLayout(saveButton, cancelButton)
        ));

        dialog.open();
    }

    private boolean isDriver(String position) {
        return position != null && position.equalsIgnoreCase("водитель");
    }
    private HorizontalLayout createToolbar() {
        Button addButton = new Button("Добавить сотрудника");
        addButton.addClickListener(e -> openAddStaffDialog());
        return new HorizontalLayout(addButton);
    }

    private void openAddStaffDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        TextField nameField = new TextField("ФИО");
        TextField positionField = new TextField("Должность");

        ComboBox<PaymentType> paymentTypeCombo = new ComboBox<>("Тип оплаты");
        paymentTypeCombo.setItems(PaymentType.values());

        DatePicker hireDatePicker = new DatePicker("Дата приёма");
        hireDatePicker.setValue(LocalDate.now());

        NumberField hourlyRateField = new NumberField("Ставка в час (₽)");
        NumberField fixedSalaryField = new NumberField("Фиксированная з/п (₽)");

        ComboBox<Car> carCombo = new ComboBox<>("Автомобиль");
        carCombo.setItemLabelGenerator(Car::getNumberCar);
        carCombo.setItems(staffService.getAvailableCarsForUser());
        carCombo.setVisible(false); // скрыто по умолчанию

// 👇 логика показа только если "Водитель"
        positionField.addValueChangeListener(event -> {
            String position = event.getValue();
            boolean isDriver = position != null && position.equalsIgnoreCase("Водитель");
            carCombo.setVisible(isDriver);

            if (!isDriver) {
                carCombo.clear();
            }
        });

        ComboBox<StaffStatus> statusCombo = new ComboBox<>("Статус");
        statusCombo.setItems(StaffStatus.values());
        statusCombo.setValue(StaffStatus.ACTIVE);

        Button saveButton = new Button("Сохранить", event -> {
            Staff staff = new Staff();
            staff.setFullName(nameField.getValue());
            staff.setPosition(positionField.getValue());
            staff.setPaymentType(paymentTypeCombo.getValue());
            staff.setHourlyRate(BigDecimal.valueOf(hourlyRateField.getValue()));
            staff.setFixedSalary(BigDecimal.valueOf(fixedSalaryField.getValue()));
            staff.setAssignedVehicle(carCombo.getValue());
            staff.setStatus(statusCombo.getValue());
            staff.setHireDate(hireDatePicker.getValue());

            staffService.save(staff); // user и car устанавливаются в сервисе
            updateGrid();
            dialog.close();
        });

        Button cancelButton = new Button("Отмена", e -> dialog.close());

        dialog.add(new VerticalLayout(
                nameField, positionField, hireDatePicker, paymentTypeCombo,
                hourlyRateField, fixedSalaryField,
                carCombo, statusCombo,
                new HorizontalLayout(saveButton, cancelButton)
        ));

        dialog.open();
    }

    private void updateGrid() {
        grid.setItems(staffService.getAllStaffForUser());
    }
}