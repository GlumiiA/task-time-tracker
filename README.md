# Task Time Tracker
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)
![Spring Web](https://img.shields.io/badge/Spring%20Web-blue)
![Spring Security](https://img.shields.io/badge/Spring%20Security-purple)
![MyBatis](https://img.shields.io/badge/MyBatis-lightgrey)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-orange)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-blue)
![JWT](https://img.shields.io/badge/JWT-red)
![JUnit 5](https://img.shields.io/badge/JUnit%205-blue)
![Mockito](https://img.shields.io/badge/Mockito-lightgrey)
![Testcontainers](https://img.shields.io/badge/Testcontainers-blueviolet)
![Maven](https://img.shields.io/badge/Maven-red)
![Docker](https://img.shields.io/badge/Docker-ready-blue)

Backend REST-сервис для учета рабочего времени сотрудников по задачам.

Сервис позволяет:

- авторизоваться через JWT;
- создавать, смотреть, обновлять, назначать и удалять задачи;
- менять статус задачи;
- фиксировать временные записи по задачам;
- получать список временных записей за период;
- получать суммарное количество часов сотрудника за период.

## Логика работы

- TimeRecord можно создать только для задач в статусе `IN_PROGRESS` или `REVIEW`.
- Сотрудник не может создавать или смотреть time records другого сотрудника.
- Для общего количества часов есть отдельный endpoint `GET /api/time-records/summary`.

## Быстрый запуск

```bash
docker compose up -d
./mvnw spring-boot:run
```

- API: `http://localhost:8080`
- Минимальный UI: `http://localhost:8080/ui`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Функциональность

- JWT-аутентификация.
- Роли пользователей: `ADMIN`, `EMPLOYEE`.
- CRUD-операции для задач без удаления: создание, чтение, обновление.
- Удаление задачи только для администратора.
- Фильтрация задач по `assigneeId` и `status`.
- Назначение исполнителя.
- Изменение статуса задачи.
- Создание записи рабочего времени.
- Получение списка time records по сотруднику и периоду.
- Получение суммарных часов сотрудника за период.
- Валидация входных данных.
- Единый формат ошибок API.

## Особенности реализации

- Многоуровневая архитектура: `Controller -> Service -> Repository`.
- SQL явно описан в MyBatis XML mapper'ах.
- Правило time record проверяется в сервисе и дополнительно в SQL.
- Суммарные часы считаются на основе длительности time record.
- Централизованная обработка ошибок через `@RestControllerAdvice`.

## API

Все endpoint'ы, кроме `/api/auth/**`, требуют:

```http
Authorization: Bearer <accessToken>
```

### auth-controller

| Method | Endpoint | Описание |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Получить JWT |

Тестовые пользователи:

| Логин | Пароль | Роль |
| --- | --- | --- |
| `admin` | `password` | `ADMIN` |
| `employee` | `password` | `EMPLOYEE` |

### task-controller

| Method | Endpoint | Описание |
| --- | --- | --- |
| `POST` | `/api/tasks` | Создать задачу |
| `GET` | `/api/tasks` | Получить задачи, фильтры: `assigneeId`, `status` |
| `GET` | `/api/tasks/{id}` | Получить задачу |
| `PUT` | `/api/tasks/{id}` | Обновить задачу |
| `PATCH` | `/api/tasks/{id}/status` | Изменить статус |
| `PATCH` | `/api/tasks/{id}/assignee` | Назначить исполнителя |
| `DELETE` | `/api/tasks/{id}` | Удалить задачу |

Статусы: `NEW`, `IN_PROGRESS`, `REVIEW`, `DONE`, `BLOCKED`.

### time-record-controller

| Method | Endpoint | Описание |
| --- | --- | --- |
| `POST` | `/api/time-records` | Создать запись рабочего времени |
| `GET` | `/api/time-records` | Получить записи времени, фильтры: `employeeId`, `from`, `to` |
| `GET` | `/api/time-records/summary` | Получить суммарные часы сотрудника за период |

## Пример summary

```http
GET /api/time-records/summary?employeeId=2&from=2026-05-01T09:00:00&to=2026-05-01T18:00:00
```

```json
{
  "employeeId": 2,
  "from": "2026-05-01T09:00:00",
  "to": "2026-05-01T18:00:00",
  "totalHours": 4.0
}
```

## Запуск локально

### База данных

```bash
docker compose up -d
```

### Приложение

```bash
./mvnw spring-boot:run
```

После запуска:

- UI: `http://localhost:8080/ui`
- REST API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

UI нужен только для быстрой ручной проверки: login, список задач, создание задачи, смена статуса, назначение исполнителя, создание записи времени.

## Тестовые запросы

Postman collection:

```text
docs/postman/task-time-tracker.postman_collection.json
```

## Проектирование

### Сущности

Сначала определялись ключевые сущности системы. По ТЗ было решено сделать упрощенно две роли: Employee (работник) и Admin (администратор). Со временем архитектура расширялась:

- Employee - сотрудник с логином, паролем и ролью.
- Task - задача с исполнителем, автором, статусом, датами создания и обновления.
- TimeRecord - запись о времени, затраченном на задачу.
- Role - роли пользователей: ADMIN, EMPLOYEE.
- Status - состояния задач: NEW, IN_PROGRESS, REVIEW, DONE, BLOCKED.

![ER diagram](docs/diagramms/ER.drawio.svg)

Файл: [docs/diagramms/ER.drawio.svg](docs/diagramms/ER.drawio.svg)

### Состояния задач

Изначально задачи имели базовые статусы, но для более гибкого учета работы были добавлены `IN_PROGRESS`, `REVIEW` и `BLOCKED`.

- `NEW` - задача создана, но еще не взята в работу.
- `IN_PROGRESS` - задача находится в активной разработке.
- `REVIEW` - работа по задаче завершена и ожидает проверки.
- `DONE` - задача завершена и принята.
- `BLOCKED` - работа по задаче временно остановлена из-за блокера.
![Task State diagram](docs/diagramms/task-state.drawio.svg)

Файл: [docs/diagramms/task-state.drawio.svg](docs/diagramms/task-state.drawio.svg)

### Правила создания временных записей

Решение о том, когда можно создать запись времени (TimeRecord), принималось исходя из логики работы сотрудников:

- в записи фиксируется результат выполненной работы;
- time record создается для работы над задачей, которая еще находится в процессе выполнения или на проверке;
- для упрощения системы и защиты от некорректного учета запись разрешена только для задач в `IN_PROGRESS` или `REVIEW`.

![Sequence diagram create time record](docs/diagramms/sequence-time-record.svg)

PlantUML-файл: [docs/diagramms/sequence-time-record.puml](docs/diagramms/sequence-time-record.puml)

### Архитектура

Слои проекта:

- `api` - REST-контроллеры.
- `dto` - входные и выходные модели API.
- `service` - бизнес-логика и проверки доступа.
- `repository` - MyBatis repository.
- `resources/mappers` - SQL-запросы.
- `mapper` - преобразование entity в DTO.
- `exception` - исключения и единый ответ ошибки.
- `auth` - JWT и Spring Security.
- `ui` - минимальные Thymeleaf-страницы для ручной проверки.

Валидация:

- DTO: `@NotBlank`, `@NotNull`, `@Positive`, `@Size`.
- Service: бизнес-правила, права доступа, корректность временного интервала.

## Тестирование

В рамках проекта были проведены следующие проверки:

- Интеграционные тесты DAO-слоя с взаимодействием с PostgreSQL через Testcontainers.
- Unit и интеграционные тесты, покрывающие основные сервисы и контроллеры.
- API-тестирование через Postman.
- Сценарии use case: [docs/use-case-tables.md](docs/use-case-tables.md).

### Покрытие тестами по Use Cases

| UC | Use Case | Unit tests | Integration/API tests |
| --- | --- | --- | --- |
| UC-01 | Авторизация пользователя | `AuthServiceTest`, `JwtProviderTest`, `SecurityFiltersTest` | `RestControllerMockitoIntegrationTest#loginReturnsAccessToken` |
| UC-02 | Получение списка своих задач | `TaskServiceTest` | `RestControllerMockitoIntegrationTest#getTasksReturnsFilteredTasks` |
| UC-03 | Получение деталей задачи | `TaskServiceTest` | `RestControllerMockitoIntegrationTest#getTaskByIdReturnsTask` |
| UC-04 | Создание задачи | `TaskServiceTest` | `RestControllerMockitoIntegrationTest#createTaskReturnsCreatedTask` |
| UC-05 | Обновление задачи | `TaskServiceTest` | `RestControllerMockitoIntegrationTest#updateTaskReturnsUpdatedTask` |
| UC-06 | Назначение исполнителя | `TaskServiceTest` | `RestControllerMockitoIntegrationTest#updateStatusAndAssigneeDelegateToService` |
| UC-07 | Изменение статуса задачи | `TaskServiceTest` | `RestControllerMockitoIntegrationTest#updateStatusAndAssigneeDelegateToService` |
| UC-08 | Удаление задачи | `TaskServiceTest` | `RestControllerMockitoIntegrationTest#deleteTaskReturnsNoContent` |
| UC-09 | Создание time record для `IN_PROGRESS`/`REVIEW` | `TimeRecordServiceTest`, `TimeRecordMapperTests` | `RestControllerMockitoIntegrationTest#createTimeRecordReturnsCreatedRecord` |
| UC-10 | Просмотр своих time records | `TimeRecordServiceTest` | `RestControllerMockitoIntegrationTest#getTimeRecordsReturnsFilteredRecords` |
| UC-11 | Просмотр всех time records администратором | `TimeRecordServiceTest` | `RestControllerMockitoIntegrationTest#getTimeRecordsReturnsFilteredRecords` |
| UC-12 | Получение суммарных часов сотрудника | `TimeRecordServiceTest` | `RestControllerMockitoIntegrationTest#getTimeSummaryReturnsTotals` |
| UC-13 | Проверка ошибок и валидации | `ApiExceptionHandlerTest`, `TimeRecordServiceTest`, `TaskServiceTest` | `RestControllerMockitoIntegrationTest` |

### Существующие тесты

- Service layer: `AuthServiceTest`, `TaskServiceTest`, `TimeRecordServiceTest`
- Auth/Security: `JwtProviderTest`, `SecurityFiltersTest`
- Exception handling: `ApiExceptionHandlerTest`
- Mapper: `MapperTest`, `TimeRecordMapperTests`
- REST controllers: `RestControllerMockitoIntegrationTest`
- Application context: `TaskTimeTrackerApplicationTests`
- PostgreSQL integration через Testcontainers

Запуск:

```bash
./mvnw test
```

HTML-отчёт:

```text
htmlReport/index.html
```

## Использование ИИ

ИИ использовался как помощник:

- для обсуждения архитектуры;
- для проверки edge cases;
- для идей тестовых сценариев;
- для формулировки README.
