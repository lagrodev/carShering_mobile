# Обзор кода проекта CarSharing (полное руководство)

> Этот файл — подробная документация по проекту на Kotlin: структура, архитектура, классы, потоки данных и рекомендации по доработке.

---

## 1. Краткая идея проекта ✅

CarSharing — клиентское Android-приложение (на Kotlin), использующее архитектуру MVVM с REST API (Retrofit + OkHttp + Gson). В проекте есть модули для аутентификации, каталога автомобилей, деталей автомобиля, контрактов (аренды), а также административные endpoints.

---

## 2. Общая архитектура и паттерны 🔧

- **MVVM**: Fragments + ViewModels + Repositories.
- **Сеть**: `RetrofitClient` + `ApiService` (описание endpoint-ов).
- **Репозитории**: тонкие обёртки над `ApiService` (Auth, Car, Contract, Profile).
- **Потоки данных**: `StateFlow` (в большинстве современных ViewModel) и `LiveData` (в `AuthViewModel`).
- **Персистенcия маленьких данных**: `TokenManager` использует DataStore Preferences.
- **Асинхронность**: Coroutines + suspend-функции в репозиториях.
- **UI**: `Fragments`, `RecyclerView` с `ListAdapter` + `DiffUtil`, `BottomSheetDialogFragment` для фильтров.
- **Image loading**: Coil.

---

## 3. Пакет `data.api` — сеть 🌐

### `ApiService` (интерфейс)
- Описывает все HTTP endpoints для:
  - Auth (login/register/logout/refresh/reset)
  - Profile & Document
  - Car catalogue, filters, car details
  - Favorites
  - Contracts (user + admin)
  - Admin endpoints (cars, models, users, documents)
- Методы возвращают `Response<T>` — статус и тело доступны клиенту.

Ключи: все методы `suspend`, использованы аннотации Retrofit (`@GET, @POST, @PATCH, @DELETE, @Multipart` и др.).

### `RetrofitClient`
- Строит `Retrofit` с `OkHttpClient`.
- Важный фрагмент:
  - `var authToken: String? = null` — глобальная переменная, используемая в `authInterceptor` для добавления заголовка `Authorization: Bearer <token>` к запросам.
  - `HttpLoggingInterceptor` настроен на `Level.BODY`.
- **Замечание/рекомендация**: токен в приложении сохраняется и подставляется в заголовок, но текущая логика login в проекте НЕ сохраняет token в `TokenManager` и не пишет `RetrofitClient.authToken` сразу после логина. В `MainActivity` токен подгружается при старте из `TokenManager`, но после логина его следует сохранить явно (см. раздел "Улучшения").

---

## 4. Пакет `data.preferences` — TokenManager 🔐

`TokenManager` — тонкая оболочка над `DataStore`:
- `saveToken`, `getToken` — сохраняет/возвращает токен (Flow<String?>).
- Также сохраняются userId, userName, userEmail.
- `clearToken()` — удалить все.

Использование:
- `MainActivity` на старте читает `TokenManager.getToken().first()` и если токен есть — присваивает `RetrofitClient.authToken = token`.

---

## 5. Пакет `data.repository` — репозитории 📚

Репозитории — простые прокси к `ApiService`. Они не содержат сложной бизнес-логики, но:
- Стандартизируют вызовы и позволяют тестировать ViewModel, мокая репозиторий.
- Примеры: `AuthRepository`, `CarRepository`, `ContractRepository`, `ProfileRepository`.

Принцип: ViewModel вызывает метод репозитория, который делает запрос к `ApiService` и возвращает `Response<T>`.

---

## 6. Пакет `data.model` — DTO и domain-модели 🧾

В проекте использован набор `data class`-ов, соответствующих JSON-схемам API (часть с `@SerializedName` для Gson). Основные сущности:

- Auth & User:
  - `AuthRequest`, `RegistrationRequest`, `ChangePasswordRequest`, `ResetPasswordRequest`
  - `UserResponse`, `AllUserResponse`
- Car:
  - `CarListItemResponse` — элемент списка каталога
  - `CarDetailResponse` — подробная информация
  - `CreateCarRequest`, `UpdateCarRequest`, `UpdateCarStateRequest` и др.
  - `MinMaxCellForFilters`, `ImageResponse`
- Contract:
  - `ContractResponse` (используется в адаптерах и UI)
  - `CreateContractRequest`, `UpdateContractRequest`, `ContractState` (enum-like)
- Page/Paged:
  - `PagedModel<T>` — общий пагинированный ответ с полем `page` и `content` (модель страницы и список)

Совет: типы и названия полей должны совпадать с API; если API изменится — обновлять модели и тестировать парсинг.

---

## 7. UI (package `ui`) — View, ViewModel и адаптеры 🖥️

Структура по экранам:
- `ui.catalogue` — каталоги:
  - `CatalogueFragment` (UI), `CatalogueViewModel` (StateFlow)
  - `CarAdapter` — `ListAdapter<CarListItemResponse>`
  - `FiltersBottomSheet` — bottom-sheet для настройки фильтров
- `ui.car_detail` — детали авто:
  - `CarDetailFragment`, `CarDetailViewModel` — загрузка деталей, расчёт стоимости, избранное
- `ui.contracts` — контракты:
  - `ContractsFragment`, `ContractAdapter`, `CreateContractFragment`, `ContractDetailFragment`
- `ui.auth` — аутентификация:
  - `LoginFragment`, `RegisterFragment`, `AuthViewModel` (использует `Result<T>` wrapper для статусов)
- `ui.profile` — профиль:
  - `ProfileFragment`, `ProfileRepository` и т.д.

Пример взаимодействия (Catalogue):
1. `CatalogueViewModel` вызывает `CarRepository.getCatalogue(...)`.
2. Результат помещается в `_uiState` (`CatalogueUiState.Success(list)`).
3. `CatalogueFragment` подписан на `uiState` и обновляет `RecyclerView` через `CarAdapter`.

Состояния UI:
- Используются sealed-классы для состояний (`Loading`, `Success`, `Error`) — это читабельно и удобно.

---

## 8. Навигация и MainActivity 🧭

- `MainActivity` находит `NavHostFragment` и подключает `BottomNavigationView` к `NavController`.
- Скрывает/показывает `BottomNavigation` на некоторых destination'ах (логин/регистрация/деталь авто).
- На старте загружает токен из `TokenManager` и присваивает `RetrofitClient.authToken`.

---

## 9. Проблемные места и предложения по улучшению (практические советы) 💡

1. Токен и логин
   - Проблема: `ApiService.login()` возвращает `Response<Unit>` и нигде не парсит или сохраняет токен.
   - Рекомендация: изменить API/клиент так, чтобы сервер возвращал тело с токеном (например `{ "token": "..." }`) и добавить модель `JwtResponse`. Затем в `AuthViewModel.login` после успешного ответа извлекать токен, сохранять через `TokenManager.saveToken(token)` и писать `RetrofitClient.authToken = token`.
   - Альтернатива (cookies): реализовать `CookieJar` в OkHttp, чтобы cookie-авторизация работала корректно.

2. Error handling
   - Сейчас ошибки частично показываются через `Result.Error` или `UiState.Error`, но стоит централизовать обработку: создать helper для преобразования `Response` в Result с подробными сообщениями.

3. Paging
   - Текущая пагинация в `CatalogueViewModel` реализована вручную. Рассмотреть `Paging 3` для более масштабируемой загрузки.

4. Date handling
   - Для точных операций с датами использовать `java.time` (API SDK 26+ или backport через ThreeTenABP), а не простые парсинги/строки.

5. Тесты
   - Добавить unit-тесты для ViewModel (мокать репозитории). Репозитории протестировать с MockWebServer.

6. Безопасность
   - Если храните токен — используйте EncryptedSharedPreferences или Encrypted DataStore для production.

---

## 10. Быстрые примеры, как править / добавлять функциональность в Kotlin ✍️

### Как правильно сохранять токен после логина (пример для `AuthViewModel`)
```kotlin
suspend fun loginAndSave(username: String, password: String) {
    val response = apiService.login(AuthRequest(username, password))
    if (response.isSuccessful) {
        // Предположим, API вернул тело с token (JwtResponse)
        val jwt = response.body() // JwtResponse
        jwt?.token?.let { token ->
            tokenManager.saveToken(token)
            RetrofitClient.authToken = token
        }
    }
}
```

Если API возвращает токен в заголовке `Authorization`, можно получить его через `response.headers()["Authorization"]`.

### Как вызвать endpoint в ViewModel (с обработкой ошибок)
```kotlin
viewModelScope.launch {
    _uiState.value = Loading
    try {
        val resp = repository.getCatalogue(page = 0)
        if (resp.isSuccessful) {
            _uiState.value = Success(resp.body()?.content ?: emptyList())
        } else {
            _uiState.value = Error("Ошибка: ${resp.code()}")
        }
    } catch (e: IOException) {
        _uiState.value = Error("Нет соединения")
    } catch (e: Exception) {
        _uiState.value = Error(e.message ?: "Неизвестная ошибка")
    }
}
```

---

## 11. Краткий список всех основных классов (справочник) 🗂️

- `data.api`:
  - `ApiService.kt` — интерфейс API
  - `RetrofitClient.kt` — singleton Retrofit, auth interceptor
- `data.preferences`:
  - `TokenManager.kt` — DataStore wrapper
- `data.repository`:
  - `AuthRepository.kt`, `CarRepository.kt`, `ContractRepository.kt`, `ProfileRepository.kt`
- `data.model`:
  - `User.kt`, `Car.kt`, `AuthModels.kt`, `Contract.kt`, `Document.kt`, `PagedModel.kt`, `PageResponse.kt`, `Result.kt`
- `ui`:
  - `Catalogu e` — `CatalogueViewModel.kt`, `CatalogueFragment.kt`, `CarAdapter.kt`, `FiltersBottomSheet.kt`
  - `car_detail` — `CarDetailFragment.kt`, `CarDetailViewModel.kt`
  - `contracts` — `ContractsFragment.kt`, `ContractAdapter.kt`, `CreateContractFragment.kt`, `ContractDetailFragment.kt`
  - `auth` — `AuthViewModel.kt`, `LoginFragment.kt`, `RegisterFragment.kt`
  - `profile` — `ProfileFragment.kt`
- `MainActivity.kt`

---

## 12. Как начать править — практический чеклист для новичка 👶➡️🧑‍💻

1. Настройте IDE (Android Studio) — импортируйте проект Gradle.
2. Запустите локальный backend (если есть) или используйте MockWebServer.
3. Для внесения изменений в API-модели: обновите `data.model`, затем запустите сборку и проверьте parsing.
4. Для добавления нового endpoint: допишите метод в `ApiService`, добавьте wrapper в соответствующий репозиторий и используйте в ViewModel.
5. Для UI: обновите layout XML, измените Fragment/ViewModel и напишите unit-тест для ViewModel.

---

## 13. Ресурсы для быстрого обучения Kotlin & Android MVVM (советы) 📚
- Официальная документация Kotlin — https://kotlinlang.org
- Coroutines & Flow — https://kotlinlang.org/docs/coroutines-overview.html
- Android Architecture Components (ViewModel, LiveData, SavedStateHandle)
- Retrofit + OkHttp + Gson
- Jetpack Navigation, Paging 3, DataStore

---

## 14. Заключение ✅

Этот проект — хороший пример MVVM-архитектуры на Kotlin с аккуратным разделением ответственности (API ↔ Repository ↔ ViewModel ↔ View). Чтобы вы могли не просто понять, но и активно править кодом, рекомендую начать с правки простых мест (сохранение токена, улучшение обработки ошибок) и по мере роста добавлять тесты и более масштабируемые механизмы загрузки (Paging).

Если хотите, могу:
- сгенерировать подробные комментарии непосредственно в коде (Pull Request-стиле),
- добавить unit-тесты для одного ViewModel (например, `CatalogueViewModel`),
- или исправить текущую логику сохранения токена (внести изменения прямо в код).

---

*Файл сгенерирован автоматически. При желании расширю разделы (пошаговые примеры для новичка, UML-диаграммы, карта вызовов) — скажите, что вам важнее сначала.*
