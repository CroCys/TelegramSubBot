package com.vadim.telegramsubbot.service;

import com.vadim.telegramsubbot.model.Payment;
import com.vadim.telegramsubbot.model.User;
import com.vadim.telegramsubbot.model.Subscription;
import com.vadim.telegramsubbot.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    public List<Payment> findByUserAndSubscriptionAndMonthAndYear(User user, Subscription subscription, int month, int year) {
        return paymentRepository.findByUserAndSubscriptionAndMonthAndYear(user, subscription, month, year);
    }

    public List<Payment> findBySubscriptionAndMonthAndYear(Subscription subscription, int month, int year) {
        return paymentRepository.findBySubscriptionAndMonthAndYear(subscription, month, year);
    }

    public List<Payment> findByUser(User user) {
        return paymentRepository.findByUser(user);
    }

    public List<Payment> findByMonthAndYear(int month, int year) {
        return paymentRepository.findByMonthAndYear(month, year);
    }

    public java.util.Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }
} 