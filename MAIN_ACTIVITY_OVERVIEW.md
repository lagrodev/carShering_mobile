# MainActivity — полное и простое объяснение ❤️

Этот файл объясняет, что делает `MainActivity` в приложении, и отвечает на ваши вопросы: `onCreate(savedInstanceState: Bundle?)`, `onSupportNavigateUp()`, `activity_main.xml` и навигация (`nav_host_fragment`, `nav_graph`, `destination`, `popUpToInclusive`), а также почему мы делаем `tokenManager = TokenManager(this)` и зачем слушать смену destination.

---

## Кратко, что делает `MainActivity` в этом проекте

- Хостит `NavHostFragment` — контейнер, где отображаются разные экраны (Fragments).
- Настраивает `BottomNavigationView` и показывает/скрывает его в зависимости от текущего экрана.
- При старте загружает токен (через `TokenManager`) и ставит его в `RetrofitClient.authToken` → все сетевые запросы будут авторизованы.
- Реализует `onSupportNavigateUp()` для корректной работы кнопки «Up» (стрелки назад) в toolbar.
- Слушает изменения навигации (`addOnDestinationChangedListener`) и выполняет UI-правила (например, скрыть bottom bar на экранах логина).

---

## 1) `override fun onCreate(savedInstanceState: Bundle?)` — что это и зачем

- `onCreate` — **первый метод жизненного цикла Activity**, который вызывается при её создании (после конструктора). В нём вы должны выполнить *инициализацию UI и стартовые действия*.

- `savedInstanceState: Bundle?` — это контейнер с небольшим набором данных, которые Activity может сохранить в `onSaveInstanceState` перед уничтожением (например, при повороте экрана или когда ОС убивает процесс и потом восстанавливает). Если Activity создаётся впервые — `savedInstanceState` будет `null`.

  - Что хранить в `savedInstanceState`? Небольшие UI-значения (положение скролла, временные флаги). Для более серьёзного сохранения состояния используйте `SavedStateHandle` внутри `ViewModel`.

- Обязательное правило: **в начале `onCreate` вызвать `super.onCreate(savedInstanceState)`**, а затем `setContentView(R.layout.activity_main)`. До `setContentView` нельзя обращаться к view (иначе NPE).

Пример (из проекта):
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    tokenManager = TokenManager(this)

    val navHostFragment = supportFragmentManager
        .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    navController = navHostFragment.navController

    bottomNavigation = findViewById(R.id.bottomNavigation)
    bottomNavigation.setupWithNavController(navController)

    // Загружаем токен (асинхронно) и подставляем в Retrofit
    lifecycleScope.launch {
        val token = tokenManager.getToken().first()
        if (token != null) {
            RetrofitClient.authToken = token
        }
    }
}
```

**Советы:**
- `onCreate` должен быть «тонким»: инициализация view и запуск лёгких стартовых задач. Тяжёлую работу в `ViewModel` или в корутинах.
- Для устойчивого восстановления состояния используйте `SavedStateHandle` в `ViewModel`.

---

## 2) Почему `TokenManager(this)` — почему передаём `this`?

- `TokenManager` требует `Context` (он использует DataStore Preferences, который работает с контекстом приложения).
- `this` внутри `Activity` — это тот же самый `Context` (Activity наследует Context). Поэтому `TokenManager(this)` передаёт контекст Activity.

Совет: часто безопаснее передавать `applicationContext` (например, `applicationContext`), чтобы исключить риск утечек, если `TokenManager` хранит контекст в поле. Но DataStore сам по себе обычно безопасен с Activity context.

---

## 3) Что такое `activity_main.xml` и `nav_host_fragment` (в простых словах)

- `activity_main.xml` — это **layout** (XML), который описывает корневой UI Activity: контейнеры, панель навигации, toolbar и т.д.
- Внутри layout обычно есть элемент `<fragment>` или `FragmentContainerView`, который содержит `NavHostFragment`. Этот `NavHostFragment` — контейнер (placeholder), куда Navigation Component помещает фрагменты (`Fragments`) в зависимости от `nav_graph`.

Пример содержимого (упрощённо):
```xml
<CoordinatorLayout ...>
    <FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        app:navGraph="@navigation/nav_graph"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNavigation"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</CoordinatorLayout>
```

- `Fragment` — это не отдельное приложение: это просто «часть UI», модуль, который содержит свою логику и layout. NavHostFragment заменяет содержимое этого контейнера разными фрагментами при навигации.

> Идея: `activity_main` задаёт «рамки» и общие элементы (toolbar, bottom nav), а `nav_host_fragment` показывает разные экраны (Fragments) внутри этих рамок.

---

## 4) `nav_graph` и `destination` — как они работают

- `nav_graph.xml` — файл, где вы описываете **экраны (destination)** и **пути между ними (action)**.
- Каждый `<fragment android:id="@+id/xxxFragment" ... />` в графе — это destination.
- `startDestination` — экран, с которого начинается граф.

Добавление новой страницы (примитивный алгоритм):
1. Создать layout XML для нового экрана.
2. Создать `Fragment` класс (на Kotlin) или `Fragment` + ViewModel при необходимости.
3. Прописать fragment как `destination` в `nav_graph.xml` и, при необходимости, добавить `action` из других экранов.
4. Вызвать навигацию: `findNavController().navigate(R.id.action_from_to)` или `navController.navigate(R.id.newDestination)`.

**Админские экраны:**
- Если админ должен видеть дополнительные экраны:
  - На UI-уровне: вы можете **условно** показывать/скрывать элементы навигации (кнопки, пункты меню) в зависимости от роли; например, при логине проверить роль и добавить пункты навигации или сделать `bottomNavigation.menu.add(...)`.
  - На навигационном уровне: можно иметь отдельные `nav_graph` или динамически управлять доступом (например, просто не вызывать `navigate()` на обычных users).
  - Важно: безопасность должна быть реализована на сервере. Клиентское скрытие — только UX.

---

## 5) `popUpTo` и `popUpToInclusive` — что это значит

- `popUpTo` — параметр навигации, который говорит: **удалить (pop) все destinations сверху до указанного (не включая его)**.
- `popUpToInclusive=true` — **включит сам указанный destination в удаление**.

Пример: после успешного логина вы хотите перейти на `homeFragment` и убрать `loginFragment` из стека, чтобы пользователь не вернулся назад на экран логина нажатием BACK.

```kotlin
navController.navigate(R.id.homeFragment, null,
    NavOptions.Builder()
        .setPopUpTo(R.id.loginFragment, true)
        .build())
```

В этом примере `loginFragment` будет удалён из back stack (вместе с ним, если `inclusive=true`) — при нажатии Назад пользователь не вернётся на экран логина.

---

## 6) `onSupportNavigateUp()` — что это и зачем

- `onSupportNavigateUp()` вызывается, когда пользователь нажимает кнопку Up (стрелку назад в ActionBar/Toolbar).
- Традиционная реализация:
```kotlin
override fun onSupportNavigateUp(): Boolean {
    return navController.navigateUp() || super.onSupportNavigateUp()
}
```
- Это значит: сначала попробуй выполнить навигацию вверх в `NavController` (это корректно обработает вложенные графы и back stack). Если `NavController` не обработал событие — вызови реализацию суперкласса.

**Разница между Back и Up:**
- Back (кнопка system back) — обычно идёт назад по back stack.
- Up — более «иерархическое» действие: ожидается идти к родительскому экрану (например из детали → в список). NavController моделирует и то и другое корректно.

---

## 7) `navController.addOnDestinationChangedListener` — почему скрываем `bottomNavigation` на некоторых экранах?

- Этот слушатель вызывается при переходе между destination. `destination.id` — это id фрагмента из `nav_graph`.
- В проекте мы скрываем `bottomNavigation` на экранах, где хочется дать пользователю полноэкранный UX или избежать повторяющихся навигационных элементов (login, register, car detail и т.д.).

Пример логики:
```kotlin
navController.addOnDestinationChangedListener { _, destination, _ ->
    when (destination.id) {
        R.id.loginFragment,
        R.id.registerFragment,
        R.id.carDetailFragment -> bottomNavigation.visibility = View.GONE
        else -> bottomNavigation.visibility = View.VISIBLE
    }
}
```

**Почему так:**
- Для логина/регистрации — UX: нужно убрать отвлекающие элементы.
- Для страницы детализации — часто нужно экран с большим контентом, без bottom nav, чтобы не занимать экранное пространство.

---

## 8) Короткие практические подсказки и ответы на «наиболее больные» вопросы

- Если добавляешь экран — делай Fragment + layout + добавь destination в `nav_graph`. Для работы бизнес-логики используй ViewModel и репозиторий.
- Для разных ролей (admin/user) — **контролируй доступ на сервере**, и на клиенте скрывай UI или блокируй навигацию в зависимости от роли (проверка `if (isAdmin) navigate(...)`).
- `popUpToInclusive = true` — полезно после логина/логаута, чтобы очистить back stack.
- `TokenManager(this)` — мы передаём Context; если боитесь утечек — используйте `applicationContext` или DI (Hilt/Dagger).

---

## 9) Что можно улучшить в `MainActivity`

- Перенести инициализацию `RetrofitClient.authToken` в отдельный инициализатор (например, `Application` subclass) — тогда токен будет готов ещё до создания Activity.
- Использовать DI (Hilt) для получения `TokenManager` и `NavController` — это улучшит тестируемость.
- Типовой код для показа/скрытия bottom navigation можно вынести в extension-функцию или helper.

---

## Заключение

Если хотите, могу:
- Прописать конкретный пример добавления admin-страницы с проверкой ролей (код + nav_graph), или
- Перенести загрузку токена в `Application` и показать изменения в коде (и как это влияет на тесты), или
- Добавить inline-комментарии прямо в `MainActivity.kt`.

Напишите, какой вариант предпочитаете, и я внесу правки и примеры прямо в проект.