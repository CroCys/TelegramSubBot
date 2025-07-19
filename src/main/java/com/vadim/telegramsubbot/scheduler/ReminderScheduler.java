package com.vadim.telegramsubbot.scheduler;

import com.vadim.telegramsubbot.model.Subscription;
import com.vadim.telegramsubbot.model.User;
import com.vadim.telegramsubbot.service.SubscriptionService;
import com.vadim.telegramsubbot.service.UserService;
import com.vadim.telegramsubbot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;

    // Каждый день в 10:00
    @Scheduled(cron = "0 0 10 * * *")
    public void sendReminders() {
        LocalDate today = LocalDate.now();
        int dayOfMonth = today.getDayOfMonth();
        for (Subscription sub : subscriptionService.findAll()) {
            int billingDay = sub.getBillingDay();
            int[] daysBefore = {3, 2, 1, 0};
            for (int daysLeft : daysBefore) {
                if (dayOfMonth == billingDay - daysLeft) {
                    Set<User> users = sub.getUsers();
                    if (users != null) {
                        for (User user : users) {
                            notificationService.sendReminder(user, sub, daysLeft);
                        }
                    }
                }
            }
        }
    }
} 