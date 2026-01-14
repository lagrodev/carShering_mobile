<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" width="120" alt="ZipGo Logo"/>
</p>

<h1 align="center">ZipGo</h1>

<p align="center">
  <b>Каршеринг у тебя в кармане</b><br/>
  Современное Android-приложение для аренды автомобилей
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform"/>
  <img src="https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Material%203-Design-6750A4?logo=materialdesign&logoColor=white" alt="Material 3"/>
  <img src="https://img.shields.io/badge/Min%20SDK-24-34A853" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/License-Educational-blue" alt="License"/>
</p>

---

## О проекте

**ZipGo** — мобильный клиент каршеринг-сервиса с чистым, интуитивным UI и полным набором функций для аренды авто. Построен на современном Android-стеке с упором на UX и производительность.

---

## Возможности

| Модуль | Функционал |
|--------|------------|
| **Авторизация** | Регистрация · Вход · Безопасный logout · Хранение токена в DataStore |
| **Каталог** | Лента авто · Поиск · Multi-select фильтры (бренд, класс) · Pull-to-refresh |
| **Карточка авто** | Фото (с fallback) · Характеристики · Быстрое бронирование |
| **Избранное** | Сохранение понравившихся · Переход к деталям |
| **Аренда** | Создание брони · Список контрактов · Детали и отмена |
| **Профиль** | Статистика · Редактирование данных · Смена пароля |

---

## Технологии

```
Kotlin 1.9           —  Язык
Material 3           —  UI-дизайн
ViewBinding          —  Работа с View
Navigation Component —  Single-Activity навигация
Retrofit + OkHttp    —  Сеть
Coroutines + Flow    —  Асинхронность
Coil                 —  Загрузка изображений
DataStore            —  Локальное хранение
```

---

## Архитектура

```
┌────────────────────────────────────────────────────────┐
│                        UI Layer                        │
│   Fragments · ViewModels · Navigation · ViewBinding    │
├────────────────────────────────────────────────────────┤
│                      Domain Layer                      │
│              Repositories · Use Cases                  │
├────────────────────────────────────────────────────────┤
│                       Data Layer                       │
│    ApiService · Models · DataStore · Preferences       │
└────────────────────────────────────────────────────────┘
```

**Паттерны:** MVVM · Repository · Single-Activity

---

## Структура проекта

```
app/src/main/java/com/example/carcatalogue/
├── data/
│   ├── api/            # Retrofit ApiService, RetrofitClient
│   ├── model/          # DTO под API
│   ├── preferences/    # TokenManager, UserPreferences
│   └── repository/     # CarRepository, ContractRepository …
└── ui/
    ├── auth/           # Login, Register
    ├── catalogue/      # CatalogueFragment, FiltersBottomSheet
    ├── car_detail/     # CarDetailFragment
    ├── contracts/      # ContractsFragment, ContractDetailFragment
    ├── favorites/      # FavoritesFragment
    └── profile/        # ProfileFragment, EditProfile, ChangePassword
```

---

## Быстрый старт

```bash
# 1. Клонируй репозиторий
git clone https://github.com/user/zipgo-android.git

# 2. Открой в Android Studio (Giraffe+)

# 3. Укажи URL бэкенда в RetrofitClient.kt
BASE_URL = "https://your-api.example.com/"

# 4. Собери и запусти
./gradlew :app:assembleDebug
```

---

## API

Полная OpenAPI-спецификация лежит в `api-docs.json`. Ключевые эндпоинты:

| Группа | Endpoints |
|--------|-----------|
| Auth | `POST /auth` · `POST /registration` · `POST /logout` |
| Catalogue | `GET /car/catalogue` · `GET /car/{id}` · фильтры |
| Contracts | `GET /contracts` · `POST /contracts` · `DELETE /contracts/{id}/cancel` |
| Profile | `GET /profile` · `PATCH /profile` · `PATCH /profile/password` |
| Stats | `GET /stats/overview/client` |

---

## UI / UX

- **Material 3** — динамические цвета, современные компоненты
- **Rounded corners 16 dp** — мягкие карточки
- **Elevation & shadows** — глубина интерфейса
- **Bottom Navigation** — быстрый доступ к главным разделам
- **Skeleton & shimmer** — placeholder'ы при загрузке
- **Adaptive icons** — стильный лаунчер

---

## Диаграммы

### Use Case Diagram
Все реализованные функции системы

![Use Case](diagrams/usecase.png)

### Sequence: Авторизация и каталог
Процесс авторизации и загрузки каталога

![Auth Sequence](diagrams/sequence_auth.png)

### Sequence: Фильтрация
Multi-select фильтры каталога

![Filters Sequence](diagrams/sequence_filters.png)

### Sequence: Создание аренды
Процесс бронирования автомобиля

![Rent Sequence](diagrams/sequence_rent.png)

### Activity: Бизнес-процесс аренды
Полный цикл аренды автомобиля

![Rent Activity](diagrams/activity_rent.png)

> **Исходники:** [diagrams/*.puml](diagrams/) — PlantUML файлы для редактирования

---

## Демонстрация

<video src="NVIDIA_Overlay_sEmivQz9gY.mp4" controls width="100%"></video>

---

## Roadmap

- [ ] Тёмная тема
- [ ] Push-уведомления
- [ ] Карта с локацией авто
- [ ] Рейтинги и отзывы
- [ ] Локализация (EN / RU)

---

## Contributing

PR и issue приветствуются. Форкай, создавай ветку, пушь и открывай Pull Request.

---

## Лицензия

Проект разработан в образовательных целях.

---

<p align="center">
  Made with Kotlin
</p>
