# Исправление загрузки данных - Data Loading Fix

## Проблемы и решения / Problems and Solutions

### 1. Каталог автомобилей / Car Catalogue

**Проблема:** API возвращает массив, а не пагинированный ответ
**Решение:** Изменен тип ответа с `Response<PageResponse<CarListItemResponse>>` на `Response<List<CarListItemResponse>>`

**Изменения:**
- `ApiService.kt` - изменен тип ответа метода `getCatalogue()`
- `CarRepository.kt` - обновлен тип возвращаемого значения
- `CatalogueFragment.kt` - убрана обработка `.content`, добавлено подробное логирование

### 2. Профиль пользователя / User Profile

**Проблема:** Профиль не загружался с сервера, отображалось только сохраненное имя
**Решение:** Добавлена загрузка полного профиля с API

**Изменения:**
- `ProfileFragment.kt` - добавлен вызов API `getProfile()`
- `TokenManager.kt` - добавлена поддержка сохранения email
- `LoginFragment.kt` - после входа загружается профиль и сохраняется локально

### 3. Контракты / Contracts

**Проблема:** Контракты не загружались, фрагмент всегда показывал пустое состояние
**Решение:** Реализована загрузка контрактов с API

**Изменения:**
- `ApiService.kt` - изменен тип ответа с `PageResponse` на `List`
- `ContractAdapter.kt` - создан новый адаптер для отображения контрактов
- `ContractsFragment.kt` - добавлена загрузка данных с сервера

### 4. Детали автомобиля / Car Details

**Проблема:** Слабая обработка ошибок
**Решение:** Добавлено подробное логирование и Toast с ошибками

## Улучшения логирования / Logging Improvements

Добавлено подробное логирование во всех фрагментах:
- `CatalogueFragment` - логирует код ответа, количество машин, ошибки API
- `CarDetailFragment` - логирует ID машины, код ответа, ошибки
- `ProfileFragment` - логирует ошибки загрузки профиля
- `ContractsFragment` - логирует код ответа, количество контрактов

## Как проверить / How to Test

### 1. Запустите backend сервер на порту 8082

### 2. Проверьте логи в Android Studio

Откройте Logcat и фильтруйте по тегам:
- `CatalogueFragment`
- `CarDetailFragment`
- `ProfileFragment`
- `ContractsFragment`

### 3. Проверьте каталог

1. Войдите в приложение
2. Откройте вкладку "Каталог"
3. Должны загрузиться автомобили
4. Если не загружаются - проверьте логи:
   - Ошибка подключения? Проверьте, запущен ли сервер
   - Код 401? Проблема с токеном
   - Код 500? Ошибка на сервере

### 4. Проверьте профиль

1. Откройте вкладку "Профиль"
2. Должны отобразиться имя и email пользователя
3. Если показывает "Гость" - данные берутся из кэша

### 5. Проверьте контракты

1. Откройте вкладку "Контракты"
2. Должны загрузиться контракты пользователя
3. Если пусто - возможно у пользователя нет контрактов

## Типичные ошибки / Common Errors

### Ошибка 401 Unauthorized
**Причина:** Неверный или истекший токен
**Решение:** Выйдите и войдите снова

### Ошибка подключения / Connection Error
**Причина:** Backend не запущен или неверный BASE_URL
**Решение:** 
- Проверьте, что backend запущен на `localhost:8082`
- Для эмулятора используйте `http://10.0.2.2:8082/`
- Для физического устройства используйте IP компьютера

### Пустой каталог
**Причина:** В БД нет машин со статусом AVAILABLE
**Решение:** Добавьте машины через админ-панель или API

### Пустые контракты
**Причина:** У пользователя нет контрактов
**Решение:** Создайте контракт через приложение или API

## API Endpoints

```
Каталог: GET /api/car/catalogue
Деталь машины: GET /api/car/{carId}
Профиль: GET /api/profile
Контракты: GET /api/contracts
```

## Изменения в моделях / Model Changes

### До / Before:
```kotlin
suspend fun getCatalogue(...): Response<PageResponse<CarListItemResponse>>
suspend fun getUserContracts(...): Response<PageResponse<ContractResponse>>
```

### После / After:
```kotlin
suspend fun getCatalogue(...): Response<List<CarListItemResponse>>
suspend fun getUserContracts(...): Response<List<ContractResponse>>
```

## Отладка через Logcat / Debugging via Logcat

Примеры команд фильтрации:
```
adb logcat -s CatalogueFragment
adb logcat -s ProfileFragment
adb logcat -s ContractsFragment
adb logcat *:E  # Только ошибки
```

## Сетевые запросы / Network Requests

Включено полное логирование HTTP запросов через `HttpLoggingInterceptor`:
- Заголовки запросов
- Тело запросов
- Коды ответов
- Тело ответов

Проверьте логи с тегом `OkHttp` для подробной информации о запросах.
