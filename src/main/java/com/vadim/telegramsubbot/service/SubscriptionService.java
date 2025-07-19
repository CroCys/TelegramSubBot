package com.vadim.telegramsubbot.service;

import com.vadim.telegramsubbot.model.Subscription;
import com.vadim.telegramsubbot.repository.SubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;

    public List<Subscription> findAll() {
        return subscriptionRepository.findAll();
    }

    public Optional<Subscription> findByName(String name) {
        return subscriptionRepository.findByName(name);
    }

    @PostConstruct
    public void initDefaultSubscriptions() {
        if (subscriptionRepository.count() == 0) {
            subscriptionRepository.save(Subscription.builder()
                .name("VPN")
                .description("профиль VPN")
                .price(100)
                .billingDay(21)
                .build());
            subscriptionRepository.save(Subscription.builder()
                .name("iCloud + Apple Music")
                .description("Семейная подписка на iCloud и Apple Music")
                .price(174)
                .billingDay(7)
                .build());
        }
    }
} 