package com.vadim.telegramsubbot.repository;

import com.vadim.telegramsubbot.model.Payment;
import com.vadim.telegramsubbot.model.User;
import com.vadim.telegramsubbot.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserAndSubscriptionAndMonthAndYear(User user, Subscription subscription, int month, int year);
    List<Payment> findBySubscriptionAndMonthAndYear(Subscription subscription, int month, int year);
    List<Payment> findByUser(User user);
    Optional<Payment> findById(Long id);
    List<Payment> findByMonthAndYear(int month, int year);
} 