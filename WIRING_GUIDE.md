# Как и что подключать (Kotlin + ViewBinding)

Этот файл — практическая карта проекта: **какой экран какой XML реально использует**, какие `id` обязательны (чтобы не ломать ViewBinding), и **куда добавить логику**.

## 1) Где лежат экраны (важно)

Проект использует **ViewBinding**, поэтому класс биндинга жестко привязан к имени XML.

- **Логин**: `FragmentLoginPremiumBinding` → XML: `app/src/main/res/layout/fragment_login_premium.xml`
- **Регистрация**: `FragmentRegisterBinding` → XML: `app/src/main/res/layout/fragment_register.xml`
- **Профиль**: `FragmentProfileBinding` → XML: `app/src/main/res/layout/fragment_profile.xml`

Если вы правите другой XML (например `fragment_login.xml`) — изменения могут **не появиться в приложении**, потому что он может вообще не использоваться.

## 2) Логин: что должно быть в XML

Файл: `app/src/main/res/layout/fragment_login_premium.xml`

Код логина ожидает следующие `id` (их нельзя переименовывать):
- `tilUsername`, `etUsername`
- `tilPassword`, `etPassword`
- `btnLogin`
- `tvRegister`
- `progressBar`

Дополнительно для UI теперь есть:
- `tvAppName` (заголовок бренда)
- `tvForgotPassword` (пока просто текст; логику можно подключить позже)

### Где включать/выключать загрузку
Обычно в `LoginFragment`:
- `binding.progressBar.isVisible = true/false`
- `binding.btnLogin.isEnabled = ...`

## 3) Регистрация: что должно быть в XML

Файл: `app/src/main/res/layout/fragment_register.xml`

Код регистрации ожидает следующие `id`:
- `tilLogin`, `etLogin`
- `tilLastName`, `etLastName`
- `tilEmail`, `etEmail`
- `tilPassword`, `etPassword`
- `tilConfirmPassword`, `etConfirmPassword`
- `btnRegister`
- `tvLogin`
- `progressBar`

Логика аналогична логину: показывайте `progressBar` и блокируйте кнопку во время запроса.

## 4) Профиль: инициалы на аватаре

Файл: `app/src/main/res/layout/fragment_profile.xml`

В аватар добавлен текстовый слой:
- `tvUserInitials` — **инициалы** (например `ИИ` для «Иван Иванов»)

Существующие `id`, которые уже использует код (и их нельзя ломать):
- `tvUserName`, `tvUserEmail`
- `cardEditProfile`, `cardDocuments`, `cardChangePassword`
- `btnLogout`
- `progressBar`

### Как посчитать инициалы (пример)
Подключайте там же, где вы подставляете имя/почту (обычно после получения пользователя).

```kotlin
val first = user.firstName?.trim().orEmpty()
val last = user.lastName?.trim().orEmpty()

val initials = buildString {
    first.firstOrNull()?.let { append(it) }
    last.firstOrNull()?.let { append(it) }
}.uppercase()

binding.tvUserInitials.text = initials
binding.tvUserInitials.isVisible = initials.isNotBlank()
```

Если у вас ФИО хранится одной строкой (например `"Иван Иванов"`), можно взять первые буквы первых двух слов.

## 5) Фильтры и поворот экрана

Про “фильтры не сбрасывались при повороте” — см. актуальный гайд: `STATE_PHENSHUI.md`.

Рекомендуемый подход:
- ViewModel + `SavedStateHandle` (лучше всего)
- или `onSaveInstanceState` как fallback

## 6) Что править, если вы хотите менять UI

UI правится в ресурсах:
- темы/цвета: `app/src/main/res/values/themes.xml`, `.../values/colors.xml`, и night-версии
- экраны: `app/src/main/res/layout/*.xml`
- фон/контейнеры: `app/src/main/res/drawable/*.xml`

Если после правок UI «не изменился» — первым делом проверьте, что вы правите **тот XML**, который реально привязан к биндингу.
