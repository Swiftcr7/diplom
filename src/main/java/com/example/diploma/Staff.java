package com.example.diploma;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private PaymentType paymentType; // HOURLY, FIXED, PER_ORDER
    private BigDecimal hourlyRate;
    private BigDecimal fixedSalary;
    private LocalDate hireDate;
    private LocalDate dismissedDate;
    @ManyToOne
    private Car assignedVehicle;
    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL)
    private List<Shift> shifts;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserInfo owner;
}
