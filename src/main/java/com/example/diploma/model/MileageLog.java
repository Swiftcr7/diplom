package com.example.diploma.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MileageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double kilometers;
    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "car_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Car car;

    @ManyToOne
    @JoinColumn(name = "shift_id")
    private Shift shift;
}
