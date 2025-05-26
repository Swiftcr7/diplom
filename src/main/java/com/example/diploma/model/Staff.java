package com.example.diploma.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Staff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String position;
    @Enumerated(EnumType.STRING)
    private StaffStatus status = StaffStatus.ACTIVE;
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;
    private BigDecimal hourlyRate;
    private BigDecimal fixedSalary;
    private LocalDate hireDate;
    private LocalDate dismissedDate;
    @ManyToOne
    @JoinColumn(name = "car_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Car assignedVehicle;
    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL)
    private List<Shift> shifts;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserInfo owner;
}
