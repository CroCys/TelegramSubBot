package com.vadim.telegramsubbot.bot;

import com.vadim.telegramsubbot.model.User;
import com.vadim.telegramsubbot.model.Subscription;
import com.vadim.telegramsubbot.service.UserService;
import com.vadim.telegramsubbot.service.SubscriptionService;
import com.vadim.telegramsubbot.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.util.ArrayList;
import java.util.List;

import com.vadim.telegramsubbot.model.Payment;
import java.time.LocalDate;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.username}")
    private String botUsername;

    @Value("${admin.telegram-id}")
    private Long adminTelegramId;

    // Кнопки с эмодзи
    private static final String BTN_MY_SUBS = "Мои подписки 📋";
    private static final String BTN_SUBSCRIBE = "Подписаться ➕";
    private static final String BTN_PAY = "Оплатить 💸";
    private static final String BTN_HELP = "Справка ℹ️";
    private static final String BTN_MENU = "Меню 🏠";
    private static final String BTN_ADMIN = "Админ-панель 🛠️";
    private static final String BTN_BACK = "⬅️ Назад";
    private static final String BTN_UNSUBSCRIBE = "Отписаться 🚫";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(new Locale("ru"));
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL").withLocale(new Locale("ru"));

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    private void handleMessage(Message message) {
        String text = message.getText().trim();
        Long chatId = message.getChatId();
        Long telegramId = message.getFrom().getId();
        String name = message.getFrom().getFirstName();
        User user = userService.findByTelegramId(telegramId)
            .orElseGet(() -> userService.save(User.builder()
                .telegramId(telegramId)
                .name(name)
                .isAdmin(false)
                .build()));
        boolean isAdmin = telegramId.equals(adminTelegramId) || user.isAdmin();
        try {
            if ("/start".equalsIgnoreCase(text) || BTN_MENU.equalsIgnoreCase(text)) {
                sendMainMenu(chatId, isAdmin);
            } else if (BTN_MY_SUBS.equalsIgnoreCase(text)) {
                sendUserSubscriptions(chatId, telegramId);
            } else if (BTN_SUBSCRIBE.equalsIgnoreCase(text)) {
                sendSubscribeMenu(chatId, telegramId);
            } else if (BTN_PAY.equalsIgnoreCase(text)) {
                sendPayMenu(chatId, telegramId);
            } else if (BTN_UNSUBSCRIBE.equalsIgnoreCase(text)) {
                sendUnsubscribeMenu(chatId, telegramId);
            } else if (BTN_HELP.equalsIgnoreCase(text)) {
                sendHelp(chatId);
            } else if (BTN_ADMIN.equalsIgnoreCase(text) || "/admin".equalsIgnoreCase(text)) {
                if (isAdmin) sendAdminPanel(chatId);
                else sendMessage(chatId, "⛔ Только для админа.");
            } else {
                sendMessage(chatId, "Неизвестная команда. Нажмите 'Меню 🏠'.");
            }
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка. Попробуйте ещё раз или обратитесь к администратору.");
            e.printStackTrace();
        }
    }

    public void sendMessage(Long chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId.toString()).text(text).build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendSubscribeMenu(Long chatId, Long telegramId) {
        try {
            User user = userService.findByTelegramId(telegramId).orElseThrow();
            List<Subscription> allSubs = subscriptionService.findAll();
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            for (Subscription sub : allSubs) {
                if (user.getSubscriptions() == null || !user.getSubscriptions().contains(sub)) {
                    buttons.add(InlineKeyboardButton.builder()
                            .text(sub.getName() + " ➕")
                            .callbackData("subscribe_" + sub.getId())
                            .build());
                }
            }
            if (buttons.isEmpty()) {
                sendMessage(chatId, "Вы уже подписаны на все доступные подписки.");
                return;
            }
            buttons.add(InlineKeyboardButton.builder().text(BTN_BACK).callbackData("back_to_menu").build());
            InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(buttons.stream().map(List::of).toList())
                .build();
            execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text("Выберите подписку для оформления:")
                .replyMarkup(markup)
                .build());
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при получении списка подписок.");
            e.printStackTrace();
        }
    }

    private void sendPayMenu(Long chatId, Long telegramId) {
        try {
            User user = userService.findByTelegramId(telegramId).orElseThrow();
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            if (user.getSubscriptions() != null) {
                for (Subscription sub : user.getSubscriptions()) {
                    buttons.add(InlineKeyboardButton.builder()
                            .text(sub.getName() + " 💸")
                            .callbackData("pay_" + sub.getId())
                            .build());
                }
            }
            if (buttons.isEmpty()) {
                sendMessage(chatId, "У вас нет подписок для оплаты.");
                return;
            }
            buttons.add(InlineKeyboardButton.builder().text(BTN_BACK).callbackData("back_to_menu").build());
            InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(buttons.stream().map(List::of).toList())
                .build();
            execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text("Выберите подписку для оплаты:")
                .replyMarkup(markup)
                .build());
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при получении списка для оплаты.");
            e.printStackTrace();
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Long telegramId = callbackQuery.getFrom().getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        User user = userService.findByTelegramId(telegramId).orElseThrow();
        LocalDate now = LocalDate.now();
        try {
            if (data.startsWith("subscribe_")) {
                handleSubscribeCallback(data, chatId, messageId, user);
            } else if (data.startsWith("unsubscribe_")) {
                handleUnsubscribeCallback(data, chatId, messageId, user);
            } else if (data.startsWith("pay_")) {
                handlePayCallback(data, chatId, messageId, user, now);
            } else if (data.startsWith("confirm_")) {
                handleConfirmCallback(data, chatId, messageId);
            } else {
                switch (data) {
                    case "back_to_menu" -> handleBackToMenu(chatId, messageId, telegramId, user);
                    case "admin_all_payments" -> sendAdminAllPayments(chatId, messageId);
                    case "admin_not_paid" -> sendAdminNotPaid(chatId, messageId);
                    case "admin_stats" -> sendAdminStats(chatId, messageId);
                    case "admin_confirm_payments" -> sendAdminConfirmPayments(chatId, messageId);
                    default -> editMessage(chatId, messageId, "Неизвестная команда.");
                }
            }
        } catch (Exception e) {
            editMessage(chatId, messageId, "Произошла ошибка. Попробуйте ещё раз или обратитесь к администратору.");
            e.printStackTrace();
        }
    }

    private void handleBackToMenu(Long chatId, Integer messageId, Long telegramId, User user) {
        editMessage(chatId, messageId, "🏠 Главное меню:");
        sendMainMenu(chatId, telegramId.equals(adminTelegramId) || user.isAdmin());
    }

    private void handleSubscribeCallback(String data, Long chatId, Integer messageId, User user) {
        Long subId = Long.parseLong(data.substring("subscribe_".length()));
        Subscription sub = subscriptionService.findAll().stream().filter(s -> s.getId().equals(subId)).findFirst().orElse(null);
        if (sub != null) {
            user.getSubscriptions().add(sub);
            userService.save(user);
            editMessageHtml(chatId, messageId, "➕ Вы подписались на <b>" + sub.getName() + "</b>.");
        }
    }

    private void handleUnsubscribeCallback(String data, Long chatId, Integer messageId, User user) {
        Long subId = Long.parseLong(data.substring("unsubscribe_".length()));
        Subscription sub = subscriptionService.findAll().stream().filter(s -> s.getId().equals(subId)).findFirst().orElse(null);
        if (sub != null) {
            user.getSubscriptions().remove(sub);
            userService.save(user);
            editMessageHtml(chatId, messageId, "Вы отписались от <b>" + sub.getName() + "</b>.");
        }
    }

    private void handlePayCallback(String data, Long chatId, Integer messageId, User user, LocalDate now) {
        Long subId = Long.parseLong(data.substring("pay_".length()));
        Subscription sub = subscriptionService.findAll().stream().filter(s -> s.getId().equals(subId)).findFirst().orElse(null);
        if (sub != null) {
            Payment payment = Payment.builder()
                    .user(user)
                    .subscription(sub)
                    .month(now.getMonthValue())
                    .year(now.getYear())
                    .status(Payment.Status.PENDING)
                    .confirmedByAdmin(false)
                    .build();
            paymentService.save(payment);
            String monthRus = now.format(MONTH_FORMAT);
            editMessageHtml(chatId, messageId, "💸 Ваша оплата за <b>" + sub.getName() + "</b> за <b>" + monthRus + "</b> отмечена, ожидает подтверждения админом.");
            notifyAdminAboutPayment(payment);
        }
    }

    private void handleConfirmCallback(String data, Long chatId, Integer messageId) {
        Long paymentId = Long.parseLong(data.substring("confirm_".length()));
        Payment payment = paymentService.findById(paymentId).orElse(null);
        if (payment != null) {
            payment.setStatus(Payment.Status.PAID);
            payment.setConfirmedByAdmin(true);
            paymentService.save(payment);
            editMessage(chatId, messageId, "Оплата подтверждена.");
            sendMessage(payment.getUser().getTelegramId(), "Ваша оплата подписки " + payment.getSubscription().getName() + " подтверждена админом!");
        }
    }

    private void notifyAdminAboutPayment(Payment payment) {
        String text = "Пользователь @" + payment.getUser().getName() + " оплатил подписку <b>" + payment.getSubscription().getName() + "</b>. Подтвердить?";
        InlineKeyboardButton confirmBtn = InlineKeyboardButton.builder()
                .text("Подтвердить оплату")
                .callbackData("confirm_" + payment.getId())
                .build();
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(confirmBtn)))
                .build();
        try {
            execute(SendMessage.builder()
                    .chatId(adminTelegramId.toString())
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(markup)
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMainMenu(Long chatId, boolean isAdmin) {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder keyboard = ReplyKeyboardMarkup.builder();
        if (isAdmin) {
            keyboard.keyboardRow(new KeyboardRow(List.of(new KeyboardButton(BTN_ADMIN)))).resizeKeyboard(true);
            try {
                execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("🛠️ Главное меню администратора:\n\n" +
                          "🛠️ Админ-панель")
                    .replyMarkup(keyboard.build())
                    .build());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }
        keyboard.keyboardRow(new KeyboardRow(List.of(new KeyboardButton(BTN_MY_SUBS), new KeyboardButton(BTN_SUBSCRIBE), new KeyboardButton(BTN_PAY))));
        keyboard.keyboardRow(new KeyboardRow(List.of(new KeyboardButton(BTN_UNSUBSCRIBE), new KeyboardButton(BTN_HELP))));
        keyboard.keyboardRow(new KeyboardRow(List.of(new KeyboardButton(BTN_MENU)))).resizeKeyboard(true);
        try {
            execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text("🏠 Главное меню:\n\n" +
                      "📋 Мои подписки\n" +
                      "➕ Подписаться\n" +
                      "💸 Оплатить\n" +
                      "🚫 Отписаться\n" +
                      "ℹ️ Справка")
                .replyMarkup(keyboard.build())
                .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendUnsubscribeMenu(Long chatId, Long telegramId) {
        try {
            User user = userService.findByTelegramId(telegramId).orElseThrow();
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            if (user.getSubscriptions() != null && !user.getSubscriptions().isEmpty()) {
                for (Subscription sub : user.getSubscriptions()) {
                    buttons.add(InlineKeyboardButton.builder()
                            .text("🚫 " + sub.getName())
                            .callbackData("unsubscribe_" + sub.getId())
                            .build());
                }
            }
            if (buttons.isEmpty()) {
                sendMessage(chatId, "У вас нет подписок для отписки.");
                return;
            }
            buttons.add(InlineKeyboardButton.builder().text(BTN_BACK).callbackData("back_to_menu").build());
            InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(buttons.stream().map(List::of).toList())
                .build();
            execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text("Выберите подписку для отписки:")
                .replyMarkup(markup)
                .build());
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при получении списка для отписки.");
            e.printStackTrace();
        }
    }

    private void sendHelp(Long chatId) {
        String text = "ℹ️ <b>Этот бот помогает делить подписки с друзьями.</b>\n\n" +
                "<b>Возможности:</b>\n" +
                "📋 Мои подписки — список ваших активных подписок\n" +
                "➕ Подписаться — выбрать новую подписку\n" +
                "💸 Оплатить — отметить оплату\n" +
                "🛠️ Админ-панель — управление для админа\n\n" +
                "Все действия доступны через главное меню 🏠.";
        sendMessageHtml(chatId, text);
    }

    private void sendUserSubscriptions(Long chatId, Long telegramId) {
        try {
            User user = userService.findByTelegramId(telegramId).orElseThrow();
            if (user.getSubscriptions() == null || user.getSubscriptions().isEmpty()) {
                sendMessage(chatId, "У вас нет активных подписок. Нажмите ➕ Подписаться!");
                return;
            }
            StringBuilder sb = new StringBuilder("\uD83D\uDCCB Ваши подписки:\n");
            LocalDate now = LocalDate.now();
            for (Subscription sub : user.getSubscriptions()) {
                sb.append("\n").append("<b>").append(sub.getName()).append("</b>")
                  .append(" — ").append(sub.getDescription()).append("\n")
                  .append("💰 <b>").append(sub.getPrice()).append("₽</b> | ")
                  .append("📅 Следующая оплата: <b>")
                  .append(String.format("%02d", sub.getBillingDay())).append(".")
                  .append(String.format("%02d", now.getMonthValue())).append(".")
                  .append(now.getYear()).append("</b>\n");
                boolean paid = paymentService.findByUserAndSubscriptionAndMonthAndYear(user, sub, now.getMonthValue(), now.getYear())
                    .stream().anyMatch(p -> p.getStatus() == Payment.Status.PAID);
                if (paid) sb.append("Статус: ✅ <b>Оплачено</b>\n");
                else sb.append("Статус: ⏳ <b>Ожидает оплаты</b>\n");
            }
            sendMessageHtml(chatId, sb.toString());
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при получении подписок.");
            e.printStackTrace();
        }
    }

    private void sendAdminPanel(Long chatId) {
        List<InlineKeyboardButton> buttons = List.of(
            InlineKeyboardButton.builder().text("Все оплаты 📊").callbackData("admin_all_payments").build(),
            InlineKeyboardButton.builder().text("Кто не оплатил ❌").callbackData("admin_not_paid").build(),
            InlineKeyboardButton.builder().text("Статистика 📈").callbackData("admin_stats").build(),
            InlineKeyboardButton.builder().text("Подтвердить оплату ✅").callbackData("admin_confirm_payments").build(),
            InlineKeyboardButton.builder().text(BTN_BACK).callbackData("back_to_menu").build()
        );
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
            .keyboard(buttons.stream().map(List::of).toList())
            .build();
        try {
            execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text("🛠️ Админ-панель:")
                .replyMarkup(markup)
                .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendAdminConfirmPayments(Long chatId, Integer messageId) {
        LocalDate now = LocalDate.now();
        List<Payment> payments = paymentService.findByMonthAndYear(now.getMonthValue(), now.getYear());
        List<Payment> pending = payments.stream().filter(p -> !p.isConfirmedByAdmin() && p.getStatus() == Payment.Status.PENDING).toList();
        if (pending.isEmpty()) {
            editMessageHtml(chatId, messageId, "✅ Нет ожидающих подтверждения оплат за " + now.format(DATE_FORMAT));
            return;
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Payment payment : pending) {
            String btnText = payment.getUser().getName() + " — " + payment.getSubscription().getName() + " (" + payment.getSubscription().getPrice() + "₽)";
            rows.add(List.of(InlineKeyboardButton.builder()
                .text(btnText)
                .callbackData("confirm_" + payment.getId())
                .build()));
        }
        rows.add(List.of(InlineKeyboardButton.builder().text(BTN_BACK).callbackData("back_to_menu").build()));
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(rows).build();
        editMessageHtml(chatId, messageId, "Выберите оплату для подтверждения:");
        try {
            execute(EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text("Выберите оплату для подтверждения:")
                .replyMarkup(markup)
                .parseMode("HTML")
                .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editMessage(Long chatId, Integer messageId, String newText) {
        try {
            execute(EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(newText)
                .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendAdminAllPayments(Long chatId, Integer messageId) {
        LocalDate now = LocalDate.now();
        List<Payment> payments = paymentService.findByMonthAndYear(now.getMonthValue(), now.getYear());
        if (payments.isEmpty()) {
            editMessage(chatId, messageId, "📊 Нет оплат за этот месяц.");
            return;
        }
        StringBuilder sb = new StringBuilder("📊 <b>Все оплаты за " + now.format(DATE_FORMAT) + ":</b>\n");
        for (Payment payment : payments) {
            sb.append("\n").append(payment.getUser().getName())
              .append(" — ").append(payment.getSubscription().getName())
              .append(" | ")
              .append(payment.getStatus() == Payment.Status.PAID ? "✅ Оплачено" : "⏳ Ожидает");
        }
        editMessageHtml(chatId, messageId, sb.toString());
    }

    private void sendAdminNotPaid(Long chatId, Integer messageId) {
        LocalDate now = LocalDate.now();
        List<User> users = userService.findAll();
        StringBuilder sb = new StringBuilder("❌ <b>Кто не оплатил за " + now.format(DATE_FORMAT) + ":</b>\n");
        boolean found = false;
        for (User user : users) {
            if (user.getSubscriptions() == null) continue;
            for (Subscription sub : user.getSubscriptions()) {
                boolean paid = paymentService.findByUserAndSubscriptionAndMonthAndYear(user, sub, now.getMonthValue(), now.getYear())
                    .stream().anyMatch(p -> p.getStatus() == Payment.Status.PAID);
                if (!paid) {
                    sb.append("\n").append(user.getName()).append(" — ").append(sub.getName());
                    found = true;
                }
            }
        }
        if (!found) sb.append("\nВсе оплатили!");
        editMessageHtml(chatId, messageId, sb.toString());
    }

    private void sendAdminStats(Long chatId, Integer messageId) {
        LocalDate now = LocalDate.now();
        List<Payment> payments = paymentService.findByMonthAndYear(now.getMonthValue(), now.getYear());
        long paid = payments.stream().filter(p -> p.getStatus() == Payment.Status.PAID).count();
        long pending = payments.stream().filter(p -> p.getStatus() == Payment.Status.PENDING).count();
        String text = "📈 <b>Статистика за " + now.format(DATE_FORMAT) + ":</b>\n" +
                "Всего оплат: <b>" + payments.size() + "</b>\n" +
                "✅ Оплачено: <b>" + paid + "</b>\n" +
                "⏳ Ожидает: <b>" + pending + "</b>";
        editMessageHtml(chatId, messageId, text);
    }

    private void sendMessageHtml(Long chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId.toString()).text(text).parseMode("HTML").build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editMessageHtml(Long chatId, Integer messageId, String newText) {
        try {
            execute(EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(newText)
                .parseMode("HTML")
                .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
} 