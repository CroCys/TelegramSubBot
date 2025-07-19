package com.vadim.telegramsubbot.service;

import com.vadim.telegramsubbot.model.User;
import com.vadim.telegramsubbot.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.vadim.telegramsubbot.bot.TelegramBot;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final TelegramBot telegramBot;

    public void sendReminder(User user, Subscription subscription, int daysLeft) {
        String text = daysLeft > 0
            ? String.format("Напоминание: до оплаты подписки %s осталось %d дней!", subscription.getName(), daysLeft)
            : String.format("Сегодня день оплаты подписки %s!", subscription.getName());
        telegramBot.sendMessage(user.getTelegramId(), text);
    }
} 