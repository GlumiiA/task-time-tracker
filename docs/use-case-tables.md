# Таблицы прецедентов использования

Ниже собраны основные use case проекта `Task Time Tracker`. Эти сценарии используются как ориентир для тестов и проверки поведения системы.

## Сводная таблица

| UC | Прецедент | Краткое описание | Основные тесты |
| --- | --- | --- | --- |
| UC-01 | Авторизация пользователя | Пользователь входит в систему и получает JWT. | `AuthServiceTest`, `JwtProviderTest`, `SecurityFiltersTest`, `RestControllerMockitoIntegrationTest#loginReturnsAccessToken` |
| UC-02 | Получение списка своих задач | Сотрудник видит только назначенные ему задачи. Администратор может видеть все или отфильтрованные задачи. | `TaskServiceTest`, `RestControllerMockitoIntegrationTest#getTasksReturnsFilteredTasks` |
| UC-03 | Получение деталей задачи | Пользователь открывает карточку задачи по ID. | `TaskServiceTest`, `RestControllerMockitoIntegrationTest#getTaskByIdReturnsTask` |
| UC-04 | Создание задачи | Администратор создает новую задачу со статусом `NEW`. | `TaskServiceTest`, `RestControllerMockitoIntegrationTest#createTaskReturnsCreatedTask` |
| UC-05 | Обновление задачи | Администратор редактирует название и описание задачи. | `TaskServiceTest`, `RestControllerMockitoIntegrationTest#updateTaskReturnsUpdatedTask` |
| UC-06 | Назначение исполнителя | Администратор назначает сотрудника на задачу. | `TaskServiceTest`, `RestControllerMockitoIntegrationTest#updateStatusAndAssigneeDelegateToService` |
| UC-07 | Изменение статуса задачи | Исполнитель или администратор меняет статус задачи. | `TaskServiceTest`, `RestControllerMockitoIntegrationTest#updateStatusAndAssigneeDelegateToService` |
| UC-08 | Удаление задачи | Администратор удаляет задачу из системы. | `TaskServiceTest`, `RestControllerMockitoIntegrationTest#deleteTaskReturnsNoContent` |
| UC-09 | Создание time record | Сотрудник фиксирует рабочее время по задаче, если она в статусе `IN_PROGRESS` или `REVIEW`. | `TimeRecordServiceTest`, `TimeRecordMapperTests`, `RestControllerMockitoIntegrationTest#createTimeRecordReturnsCreatedRecord` |
| UC-10 | Просмотр своих time records | Сотрудник смотрит свои записи времени за период. | `TimeRecordServiceTest`, `RestControllerMockitoIntegrationTest#getTimeRecordsReturnsFilteredRecords` |
| UC-11 | Просмотр всех time records | Администратор может видеть все записи времени либо записи конкретного сотрудника. | `TimeRecordServiceTest`, `RestControllerMockitoIntegrationTest#getTimeRecordsReturnsFilteredRecords` |
| UC-12 | Получение суммарных часов сотрудника | Система считает общее количество часов сотрудника за период. | `TimeRecordServiceTest`, `RestControllerMockitoIntegrationTest#getTimeSummaryReturnsTotals` |
| UC-13 | Проверка ошибок и валидации | Система возвращает понятные ошибки для невалидных данных и нарушений прав доступа. | `ApiExceptionHandlerTest`, `TimeRecordServiceTest`, `TaskServiceTest`, `RestControllerMockitoIntegrationTest` |

## Подробности по time record

### UC-08: Создание time record

| Поле | Значение |
| --- | --- |
| Название | Создание time record |
| Основные акторы | Сотрудник, администратор |
| Предусловия | Пользователь авторизован, задача существует и находится в статусе `IN_PROGRESS` или `REVIEW`. |
| Основной поток | 1. Пользователь заполняет время начала и окончания работы.<br>2. Система проверяет права доступа и валидность интервала.<br>3. Система сохраняет запись времени.<br>4. Система возвращает созданную запись. |
| Альтернативный поток | A1. Если задача не в `IN_PROGRESS` или `REVIEW`, система возвращает `409 Conflict`.<br>A2. Если пользователь пытается создать запись за другого сотрудника, система возвращает `403 Forbidden`. |
| Постусловия | Запись времени сохранена в системе. |

### UC-11: Получение суммарных часов

| Поле | Значение |
| --- | --- |
| Название | Получение суммарных часов сотрудника |
| Основные акторы | Сотрудник, администратор |
| Предусловия | Пользователь авторизован, задан сотрудник и период. |
| Основной поток | 1. Пользователь запрашивает summary по сотруднику и периоду.<br>2. Система проверяет права доступа.<br>3. Система находит time records за указанный период.<br>4. Система суммирует длительность всех записей.<br>5. Система возвращает итоговое число часов. |
| Альтернативный поток | A1. Если сотрудник чужой для не-администратора, система возвращает `403 Forbidden`.<br>A2. Если период задан некорректно, система возвращает `400 Bad Request`. |
| Постусловия | Пользователь получает суммарное количество часов за период. |

## Связанные тесты

- Unit: `AuthServiceTest`, `TaskServiceTest`, `TimeRecordServiceTest`, `ApiExceptionHandlerTest`, `JwtProviderTest`, `SecurityFiltersTest`, `MapperTest`, `TimeRecordMapperTests`
- API / integration: `RestControllerMockitoIntegrationTest`
- Интеграция с PostgreSQL: `TimeRecordMapperTests`

## Связь с README

Описание проекта и таблица покрытия тестами приведены в [README.md](../README.md).
