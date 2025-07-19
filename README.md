# TelegramSubBot

## Локальный запуск

1. Убедитесь, что установлен Java 21 и PostgreSQL.
2. Настройте параметры подключения к БД в `src/main/resources/application.yml`.
3. Запустите приложение:
   ```bash
   ./mvnw spring-boot:run
   ```

## Сборка и запуск в Docker

1. Соберите образ:
   ```bash
   docker build -t telegramsubbot .
   ```
2. Запустите контейнер (укажите переменные окружения для БД, если нужно):
   ```bash
   docker run -d --name telegramsubbot -p 8080:8080 telegramsubbot
   ```

## Переменные окружения (опционально)
- Для production рекомендуется передавать параметры БД и токены через переменные окружения или внешний volume с `application.yml`.

---

**Рекомендация:**
- Для VPS используйте docker-compose или systemd для автозапуска. 