package com.vadim.telegramsubbot.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Subscription subscription;

    private int month;
    private int year;

    @Enumerated(EnumType.STRING)
    private Status status;

    private boolean confirmedByAdmin;

    public enum Status {
        PENDING, PAID, REJECTED
    }
} 