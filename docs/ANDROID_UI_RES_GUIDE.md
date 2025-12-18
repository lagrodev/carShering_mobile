# CarSharing Android: UI и `/res` без боли (для backend-разработчика)

Если ты backend-чик и смотришь на Android-проект как на джунгли — это файл-карта.
Он не пытается «научить Android вообще», он объясняет *этот* проект через знакомые аналогии (router/controller/template/state) + реальные примеры из кода.

Ограничение (как ты просил): я **не лезу глубоко** в `ApiService` / `data.model` / `data.preferences` / `data.repository`. Упоминаю их ровно настолько, чтобы понимать, *как UI зовёт сеть*.

Как читать (быстро и запоминаемо):

1) Сначала пробеги раздел “TL;DR (ментальная модель)” — это «легенда карты».
2) Потом раздел “Навигация” — это «router» и объясняет, почему клики вообще куда-то ведут.
3) Потом выбери 1 фичу и пройди её цепочку: layout → fragment → viewmodel → adapter.

---

## TL;DR (ментальная модель)

Если перевести на бекенд-термины:

- **Activity** = один “shell/server process”, который живёт всё время.
- **Fragment** = “страница/handler/controller” (экран) внутри shell.
- **nav_graph.xml** = “router” (маршрутизация + аргументы).
- **layout XML** = “template” (как HTML), но без логики.
- **ViewBinding** = сгенерированный “typed DOM” для layout (типобезопасные ссылки на `View`).
- **ViewModel** = “state holder + use-case” для экрана. Он переживает поворот экрана.

Запоминалка (5 правил, которые реально помогают):

1) Экран = Fragment + layout + (часто) ViewModel.
2) Любой клик «перехода» почти всегда уходит в Navigation (router).
3) Binding живёт ровно пока живёт View у Fragment.
4) RecyclerView = список, Adapter = «как рисовать 1 строку», DiffUtil = «как обновлять быстро».
5) Всё красивое (цвета/отступы/иконки) живёт в `res/`, а код — только связывает и управляет состоянием.

Мини-сниппет «как это выглядит вживую» (ресурсы + navigation):

```kotlin
// Fragment = экран. Layout = шаблон. Navigation = роутинг.
class AnyFragment : Fragment(R.layout.fragment_any) {
  fun onSomeClick() {
    findNavController().navigate(R.id.someDestinationFragment)
  }
}
```

---

## Точка входа: запуск приложения

### 1) Манифест

- Приложение стартует с Activity, указанной в [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml).
- Важно:
  - `android:usesCleartextTraffic="true"` и `android:networkSecurityConfig="@xml/network_security_config"` — значит мы разрешаем HTTP (не HTTPS). Это нужно для локального бэка `10.0.2.2`.

Мини-пример из манифеста этого проекта:

```xml
<!-- app/src/main/AndroidManifest.xml -->
<application
  android:usesCleartextTraffic="true"
  android:networkSecurityConfig="@xml/network_security_config"
  android:theme="@style/Theme.CarSharing">

  <activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
      <action android:name="android.intent.action.MAIN" />
      <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
  </activity>
</application>
```

### 2) MainActivity = контейнер всего UI

Файл: [app/src/main/java/com/example/carcatalogue/MainActivity.kt](../app/src/main/java/com/example/carcatalogue/MainActivity.kt)

Что делает:

- `setContentView(R.layout.activity_main)` — загружает “каркас” экрана.
- Находит `NavHostFragment` и достаёт из него `NavController`.
- Подключает `BottomNavigationView` к `NavController` через `setupWithNavController`.
- Прячет нижнюю навигацию на некоторых экранах (логин/регистрация/детали/детали аренды).

Важно: в `onCreate` есть чтение токена из `TokenManager` и присваивание `RetrofitClient.authToken`. Это “старый” путь с Bearer-токеном; при cookie-аутентификации он может быть не нужен.

Мини-пример из этого проекта (идея в 15 строк):

```kotlin
// app/src/main/java/com/example/carcatalogue/MainActivity.kt
val navHostFragment = supportFragmentManager
  .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
navController = navHostFragment.navController

bottomNavigation = findViewById(R.id.bottomNavigation)
bottomNavigation.setupWithNavController(navController)

navController.addOnDestinationChangedListener { _, destination, _ ->
  bottomNavigation.visibility = when (destination.id) {
    R.id.loginFragment,
    R.id.registerFragment,
    R.id.carDetailFragment,
    R.id.contractDetailFragment -> View.GONE
    else -> View.VISIBLE
  }
}
```

### 3) activity_main.xml = “shell layout”

Файл: [app/src/main/res/layout/activity_main.xml](../app/src/main/res/layout/activity_main.xml)

Состоит из двух ключевых частей:

- `FragmentContainerView` с `android:name="NavHostFragment"` — это контейнер, в который Navigation будет подставлять фрагменты (экраны).
- `BottomNavigationView` снизу — навигация по основным разделам.

Отсюда очень важная связка:

- `app:navGraph="@navigation/nav_graph"` у `NavHostFragment` → все маршруты и стартовый экран задаются в navigation XML.
- `app:menu="@menu/bottom_nav_menu"` у `BottomNavigationView` → пункты нижнего меню.

Мини-пример: NavHost + BottomNav выглядят так (сильно сокращено):

```xml
<!-- app/src/main/res/layout/activity_main.xml -->
<FragmentContainerView
  android:id="@+id/nav_host_fragment"
  android:name="androidx.navigation.fragment.NavHostFragment"
  app:defaultNavHost="true"
  app:navGraph="@navigation/nav_graph" />

<com.google.android.material.bottomnavigation.BottomNavigationView
  android:id="@+id/bottomNavigation"
  app:menu="@menu/bottom_nav_menu" />
```

---

## Навигация: как экраны “роутятся”

Файл: [app/src/main/res/navigation/nav_graph.xml](../app/src/main/res/navigation/nav_graph.xml)

Ключевые понятия:

- **destination** = экран (fragment).
- **action** = переход между экранами.
- **argument** = параметры маршрута (типа `carId`, `contractId`).

### Стартовый экран

`app:startDestination="@id/loginFragment"` — при запуске всегда показываем логин.

Вот как это реально задано в проекте:

```xml
<!-- app/src/main/res/navigation/nav_graph.xml -->
<navigation
  android:id="@+id/nav_graph"
  app:startDestination="@id/loginFragment">

  <fragment
    android:id="@+id/loginFragment"
    android:name="com.example.carcatalogue.ui.auth.LoginFragment" />
</navigation>
```

### Bottom nav

Файл: [app/src/main/res/menu/bottom_nav_menu.xml](../app/src/main/res/menu/bottom_nav_menu.xml)

Там item’ы имеют `android:id` **совпадающий** с destination id в nav_graph:

- `@id/catalogueFragment`
- `@id/contractsFragment`
- `@id/profileFragment`

Когда в [app/src/main/java/com/example/carcatalogue/MainActivity.kt](../app/src/main/java/com/example/carcatalogue/MainActivity.kt) вызывается `bottomNavigation.setupWithNavController(navController)`, Navigation сам делает:

- клик по пункту меню → переход на соответствующий destination
- поддержка back stack (в разумных пределах)

Мини-пример (важно: `android:id` совпадает с destination id в `nav_graph.xml`):

```xml
<!-- app/src/main/res/menu/bottom_nav_menu.xml -->
<item
  android:id="@+id/contractsFragment"
  android:icon="@drawable/ic_contracts"
  android:title="@string/nav_contracts" />
```

### Аргументы экранов

В nav_graph есть:

- `carDetailFragment` принимает `carId: long`
- `contractDetailFragment` принимает `contractId: long`
- `createContractFragment` принимает `carId: long` и `dailyRate: float`

Передавать аргументы можно 2 способами:

1) “вручную” (Bundle). Пример из деталей авто: уходим на создание аренды и передаём `carId` и `dailyRate`.

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/car_detail/CarDetailFragment.kt
findNavController().navigate(
  R.id.action_carDetailFragment_to_createContractFragment,
  bundleOf(
    "carId" to carId,
    "dailyRate" to dailyRate
  )
)
```

2) через SafeArgs (генерация directions), как сделано в [app/src/main/java/com/example/carcatalogue/ui/catalogue/CatalogueFragment.kt](../app/src/main/java/com/example/carcatalogue/ui/catalogue/CatalogueFragment.kt):

- `CatalogueFragmentDirections.actionCatalogueFragmentToCarDetailFragment(carId)`

SafeArgs безопаснее: меньше шансов ошибиться в ключах и типах.

Запоминалка: SafeArgs = “typed routes”, как если бы в backend у тебя был типизированный клиент к router’у.

---

## ViewBinding: почему в коде “магические” Binding-классы

Пример: [app/src/main/java/com/example/carcatalogue/ui/auth/LoginFragment.kt](../app/src/main/java/com/example/carcatalogue/ui/auth/LoginFragment.kt) использует `FragmentLoginPremiumBinding`.

Откуда он берётся:

- Есть layout-файл [app/src/main/res/layout/fragment_login_premium.xml](../app/src/main/res/layout/fragment_login_premium.xml)
- Gradle генерирует класс `FragmentLoginPremiumBinding`, где есть поля под `id` из XML.

Правило генерации:

- `fragment_login_premium.xml` → `FragmentLoginPremiumBinding`
- `item_car_premium.xml` → `ItemCarPremiumBinding`

Это даёт:

- типобезопасные ссылки вместо `findViewById`
- минимум NPE (если правильно чистить binding в `onDestroyView`)

**Шаблон во всех Fragment’ах проекта:**

- `_binding: XBinding? = null`
- `binding get() = _binding!!`
- `inflate(...)` в `onCreateView`
- `_binding = null` в `onDestroyView`

Это важно: View у Fragment может уничтожаться отдельно от самого Fragment.

Мини-шаблон (реально используется в проекте почти везде):

```kotlin
private var _binding: FragmentLoginPremiumBinding? = null
private val binding get() = _binding!!

override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
  _binding = FragmentLoginPremiumBinding.inflate(inflater, container, false)
  return binding.root
}

override fun onDestroyView() {
  super.onDestroyView()
  _binding = null
}
```

Почему это надо помнить: Fragment живёт дольше, чем его View. Если держать ссылку на старый View — будут утечки/краши.

---

## Пакет UI: что где лежит

Папка: [app/src/main/java/com/example/carcatalogue/ui](../app/src/main/java/com/example/carcatalogue/ui)

Внутри всё разложено “по фичам”:

- `auth/` — вход/регистрация
- `catalogue/` — каталог + фильтры
- `car_detail/` — детальная страница авто
- `contracts/` — аренды (список + создание)
- `profile/` — профиль

Если представить это как “роуты по папкам”:

```text
app/src/main/java/com/example/carcatalogue/ui/
  auth/         -> login/register
  catalogue/    -> list + filters
  car_detail/   -> car details
  contracts/    -> list + create
  profile/      -> profile + logout
```

Ниже — подробно по каждому экрану.

---

## ui/auth: логин и регистрация

Мини-навигация (как “router actions”):

```xml
<!-- app/src/main/res/navigation/nav_graph.xml -->
<fragment
  android:id="@+id/loginFragment"
  android:name="com.example.carcatalogue.ui.auth.LoginFragment">
  <action
    android:id="@+id/action_loginFragment_to_registerFragment"
    app:destination="@id/registerFragment" />
</fragment>
```

### LoginFragment

Файл: [app/src/main/java/com/example/carcatalogue/ui/auth/LoginFragment.kt](../app/src/main/java/com/example/carcatalogue/ui/auth/LoginFragment.kt)

Связанный layout:

- [app/src/main/res/layout/fragment_login_premium.xml](../app/src/main/res/layout/fragment_login_premium.xml)

Логика экрана:

- Считывает `username/password` из `EditText`.
- Валидирует (пусто/минимальная длина).
- Вызывает `viewModel.login(username, password)`.
- Подписывается на `loginResult` (LiveData) и:
  - `Loading` → показывает прогресс, дизейблит кнопку
  - `Success` → пробует запросить профиль и сохранить имя/почту, затем навигация в каталог
  - `Error` → показывает Toast

Примечание по cookie-auth:

- Здесь логин успешный, потому что сервер ставит cookies (`Set-Cookie`).
- Токен из тела ответа не ждём.

Мини-пример из реального экрана: валидируем ввод → зовём `viewModel.login()`:

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/auth/LoginFragment.kt
binding.btnLogin.setOnClickListener {
  val username = binding.etUsername.text.toString().trim()
  val password = binding.etPassword.text.toString().trim()
  if (validateInput(username, password)) {
    viewModel.login(username, password)
  }
}
```

### AuthViewModel

Файл: [app/src/main/java/com/example/carcatalogue/ui/auth/AuthViewModel.kt](../app/src/main/java/com/example/carcatalogue/ui/auth/AuthViewModel.kt)

Что делает:

- Держит `loginResult: LiveData<Result<Unit>>`
- Держит `registerResult: LiveData<Result<UserResponse>>`
- В `login()` дергает репозиторий, и если `response.isSuccessful` → `Success(Unit)`

Тут намеренно мало логики: UI-слой просто понимает “ок/не ок”.

Сниппет «ViewModel как state machine для UI»:

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/auth/AuthViewModel.kt
fun login(username: String, password: String) {
  viewModelScope.launch {
    _loginResult.value = Result.Loading
    val response = repository.authenticate(username, password)
    _loginResult.value = if (response.isSuccessful) Result.Success(Unit)
    else Result.Error(response.errorBody()?.string() ?: "Ошибка авторизации")
  }
}
```

---

## ui/catalogue: каталог и фильтры

Мини-пример “рендерим экран из StateFlow”:

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/catalogue/CatalogueFragment.kt
viewLifecycleOwner.lifecycleScope.launch {
  viewModel.uiState.collectLatest { state ->
    when (state) {
      is CatalogueUiState.Loading -> binding.progressBar.isVisible = true
      is CatalogueUiState.Success -> adapter.submitList(state.cars)
      is CatalogueUiState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
    }
  }
}
```

### CatalogueFragment

Файл: [app/src/main/java/com/example/carcatalogue/ui/catalogue/CatalogueFragment.kt](../app/src/main/java/com/example/carcatalogue/ui/catalogue/CatalogueFragment.kt)

Связанный layout (фактически используемый):

- [app/src/main/res/layout/fragment_catalogue_vibrant.xml](../app/src/main/res/layout/fragment_catalogue_vibrant.xml)

Что делает:

- Настраивает `RecyclerView` с `LinearLayoutManager`.
- Подключает `CarAdapter`.
- Добавляет infinite scroll: когда дошли до конца → `viewModel.loadNextPage()`.
- Подключает `SwipeRefreshLayout` → `viewModel.loadCars()`.
- Кнопка фильтров открывает `FiltersBottomSheet`.
- Подписывается на `viewModel.uiState` (StateFlow) и рендерит:
  - `Loading` → прогресс
  - `Success` → `adapter.submitList(state.cars)`
  - `Error` → Toast

Обрати внимание: тут используется Flow (`collectLatest`) вместо LiveData.

Пример (как открывается bottom sheet фильтров):

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/catalogue/CatalogueFragment.kt
binding.btnFilters.setOnClickListener {
  val currentFilters = viewModel.getCurrentCarFilters()
  val bottomSheet = FiltersBottomSheet.newInstance(currentFilters) { filters ->
    viewModel.applyCarFilters(filters)
  }
  bottomSheet.show(parentFragmentManager, "filters")
}
```

Запоминалка: это как `GET /cars?filters=...` — только вместо URL ты прокидываешь объект фильтров.

### CatalogueViewModel

Файл: [app/src/main/java/com/example/carcatalogue/ui/catalogue/CatalogueViewModel.kt](../app/src/main/java/com/example/carcatalogue/ui/catalogue/CatalogueViewModel.kt)

Фишки:

- Фильтры хранятся в `SavedStateHandle` → не слетают на повороте экрана.
- Пагинация руками:
  - `currentPage`, `isLastPage`, `isLoadingMore`
  - `allCars` копит общий список
- Есть два типа фильтров:
  - `CarFilters` — удобный для UI (год/цена как Int, даты как `Date`)
  - `CatalogueFilters` — удобный для запроса в API (строки/Double)

Также ViewModel подгружает варианты для фильтров:

- бренды, модели, типы кузова, классы, min/max цену

Мини-пример “фильтры живут в SavedStateHandle + триггерят перезагрузку”:

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/catalogue/CatalogueViewModel.kt
fun applyCarFilters(carFilters: CarFilters) {
  val catalogueFilters = CatalogueFilters(
    brand = carFilters.brand,
    model = carFilters.model,
    minYear = carFilters.minYear,
    maxYear = carFilters.maxYear,
    minPrice = carFilters.minCell?.toDouble(),
    maxPrice = carFilters.maxCell?.toDouble()
  )
  _filters.value = catalogueFilters
  savedStateHandle[KEY_FILTERS] = catalogueFilters
  loadCars()
}
```

### FiltersBottomSheet

Файл: [app/src/main/java/com/example/carcatalogue/ui/catalogue/FiltersBottomSheet.kt](../app/src/main/java/com/example/carcatalogue/ui/catalogue/FiltersBottomSheet.kt)

Layout:

- [app/src/main/res/layout/bottom_sheet_filters.xml](../app/src/main/res/layout/bottom_sheet_filters.xml)

Что важно понять:

- Это `BottomSheetDialogFragment` — модальный “лист” снизу.
- Берёт `CatalogueViewModel` через `activityViewModels()` → это **общая** ViewModel на Activity, т.е. каталог и боттомшит видят одно состояние.
- Рисует `ChipGroup` на основе Flow’ов `carClasses`/`bodyTypes`.
- Слайдеры:
  - `sliderYear`: 2000..2025
  - `sliderPrice`: подстраивается под min/max из API
- Кнопка Apply собирает `CarFilters` и отдаёт callback’ом обратно в `CatalogueFragment`.

Пример (что делает кнопка “Применить”):

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/catalogue/FiltersBottomSheet.kt
binding.btnApply.setOnClickListener {
  val filters = collectFilters()
  onFiltersApplied?.invoke(filters)
  dismiss()
}
```

А вот “ядро” — как bottom sheet заполняет чипы из Flow’ов ViewModel:

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
  viewModel.carClasses.collect { classes ->
    binding.chipGroupClass.removeAllViews()
    classes.forEach { carClass ->
      binding.chipGroupClass.addView(createChip(carClass))
    }
  }
}
```

### CarAdapter

Файл: [app/src/main/java/com/example/carcatalogue/ui/catalogue/CarAdapter.kt](../app/src/main/java/com/example/carcatalogue/ui/catalogue/CarAdapter.kt)

Layout:

- [app/src/main/res/layout/item_car_premium.xml](../app/src/main/res/layout/item_car_premium.xml)

Паттерн:

- `ListAdapter + DiffUtil` → эффективно обновляет список.
- `onItemClick(car.id)` используется и на всей карточке, и на кнопке “Забронировать”.

Мини-пример клика (суть адаптера):

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/catalogue/CarAdapter.kt
binding.btnBookNow.setOnClickListener { onItemClick(car.id) }
binding.root.setOnClickListener { onItemClick(car.id) }
```

Запоминалка: Adapter не «грузит данные» — он просто рендерит 1 элемент списка.

---

## ui/car_detail: детали авто

Мини-пример аргумента в `nav_graph.xml` (как параметр маршрута):

```xml
<!-- app/src/main/res/navigation/nav_graph.xml -->
<fragment
  android:id="@+id/carDetailFragment"
  android:name="com.example.carcatalogue.ui.car_detail.CarDetailFragment">
  <argument
    android:name="carId"
    app:argType="long" />
</fragment>
```

### CarDetailFragment

Файл: [app/src/main/java/com/example/carcatalogue/ui/car_detail/CarDetailFragment.kt](../app/src/main/java/com/example/carcatalogue/ui/car_detail/CarDetailFragment.kt)

Layout:

- [app/src/main/res/layout/fragment_car_detail_vibrant.xml](../app/src/main/res/layout/fragment_car_detail_vibrant.xml)

Как работает:

- Берёт `carId` из аргументов (`arguments?.getLong("carId")`).
- `viewModel.loadCarDetails(carId)`.
- Подписывается на `uiState`:
  - `Loading` → показывает `ProgressBar`
  - `Success` → `displayCar(car)` заполняет UI
  - `Error` → Toast
- Кнопка “Забронировать” делает переход на экран создания аренды:
  - передаёт `carId` и `dailyRate`.

Мини-пример из этого проекта: читаем `carId` и уходим на бронирование через SafeArgs:

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/car_detail/CarDetailFragment.kt
val carId = arguments?.getLong("carId") ?: return

binding.btnBook.setOnClickListener {
  val dailyRate = (currentCar?.rent ?: 0.0).toFloat()
  val action = CarDetailFragmentDirections
    .actionCarDetailFragmentToCreateContractFragment(carId, dailyRate)
  findNavController().navigate(action)
}
```

### CarDetailViewModel

Файл: [app/src/main/java/com/example/carcatalogue/ui/car_detail/CarDetailViewModel.kt](../app/src/main/java/com/example/carcatalogue/ui/car_detail/CarDetailViewModel.kt)

Что есть:

- `uiState` (Flow) с загрузкой деталей.
- `isFavorite` (Flow) + метод `toggleFavorite`.

Примечание: внутри есть методы `setDateStart/setDateEnd/calculateTotalCost/createContract`, но фактическое создание аренды у нас сейчас делается через отдельный экран `CreateContractFragment`.

Мини-пример (Flow-state + загрузка деталей):

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/car_detail/CarDetailViewModel.kt
fun loadCarDetails(carId: Long) {
  viewModelScope.launch {
    _uiState.value = CarDetailUiState.Loading
    val response = repository.getCarDetail(carId)
    _uiState.value = if (response.isSuccessful && response.body() != null)
      CarDetailUiState.Success(response.body()!!)
    else CarDetailUiState.Error("Автомобиль не найден")
  }
}
```

---

## ui/contracts: “Аренды”

Мини-роутинг контрактов (аргумент + action):

```xml
<!-- app/src/main/res/navigation/nav_graph.xml -->
<fragment
  android:id="@+id/contractsFragment"
  android:name="com.example.carcatalogue.ui.contracts.ContractsFragment">
  <action
    android:id="@+id/action_contractsFragment_to_contractDetailFragment"
    app:destination="@id/contractDetailFragment" />
</fragment>

<fragment
  android:id="@+id/contractDetailFragment"
  android:name="com.example.carcatalogue.ui.contracts.ContractDetailFragment">
  <argument
    android:name="contractId"
    app:argType="long" />
</fragment>
```

### ContractsFragment

Файл: [app/src/main/java/com/example/carcatalogue/ui/contracts/ContractsFragment.kt](../app/src/main/java/com/example/carcatalogue/ui/contracts/ContractsFragment.kt)

Layout:

- [app/src/main/res/layout/fragment_contracts.xml](../app/src/main/res/layout/fragment_contracts.xml)

Сценарий:

- Настраивает список контрактов (RecyclerView).
- Подключает SwipeRefresh.
- Делает CTA “Выбрать авто” (кнопки сверху и в empty-state) → ведёт в каталог.
- `loadContracts()` делает запрос, и:
  - если список пуст → показывает empty-state
  - если 401 → говорит “Нужно войти” и кидает на логин

Мини-пример: загрузка + 401-ветка (как “auth required”):

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/contracts/ContractsFragment.kt
val response = RetrofitClient.apiService.getAllContracts(page = 0, size = 50)
if (response.code() == 401) {
  Toast.makeText(requireContext(), "Нужно войти", Toast.LENGTH_SHORT).show()
  findNavController().navigate(R.id.loginFragment)
}
```

### CreateContractFragment

Файл: [app/src/main/java/com/example/carcatalogue/ui/contracts/CreateContractFragment.kt](../app/src/main/java/com/example/carcatalogue/ui/contracts/CreateContractFragment.kt)

Layout:

- [app/src/main/res/layout/fragment_create_contract.xml](../app/src/main/res/layout/fragment_create_contract.xml)

Это самый важный экран с точки зрения “дата+время”:

- Выбор диапазона дат через `MaterialDatePicker.dateRangePicker()`.
- Выбор времени начала/окончания через два `MaterialTimePicker`.
- Формирует `LocalDateTime` и отправляет в API в ISO-формате:

- `2025-12-01T10:00:00`

Требование API (из OpenAPI): `dataStart/dataEnd` имеют формат `date-time`.

Пример: формируем строку ISO и шлём CreateContractRequest:

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/contracts/CreateContractFragment.kt
val request = CreateContractRequest(
  carId = carId,
  dataStart = startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
  dataEnd = endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
  dailyRate = dailyRate
)

val response = RetrofitClient.apiService.createContract(request)
```

### ContractAdapter

Файл: [app/src/main/java/com/example/carcatalogue/ui/contracts/ContractAdapter.kt](../app/src/main/java/com/example/carcatalogue/ui/contracts/ContractAdapter.kt)

Layout:

- [app/src/main/res/layout/item_contract.xml](../app/src/main/res/layout/item_contract.xml)

Что делает:

- Биндит поля контракта в карточку.
- Красит status chip в зависимости от `ContractState`.
- Аккуратно форматирует даты, пытаясь распарсить либо `yyyy-MM-dd`, либо `yyyy-MM-dd'T'HH:mm:ss`.

Мини-пример: маппинг статуса → текст + цвет:

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/contracts/ContractAdapter.kt
binding.chipStatus.text = getStatusText(contract.state)
val statusColor = when (contract.state) {
  ContractState.CONFIRMED, ContractState.ACTIVE, ContractState.COMPLETED -> R.color.success
  ContractState.PENDING, ContractState.CANCELLATION_REQUESTED, ContractState.AWAITING_CANCELLATION -> R.color.warning
  ContractState.CANCELLED -> R.color.error
  null -> R.color.divider
}
binding.chipStatus.setChipBackgroundColorResource(statusColor)
```

### ContractDetailFragment (пока заглушка)

Файл: [app/src/main/java/com/example/carcatalogue/ui/contracts/ContractDetailFragment.kt](../app/src/main/java/com/example/carcatalogue/ui/contracts/ContractDetailFragment.kt)

Сейчас это placeholder:

- Он инфлейтит [app/src/main/res/layout/fragment_contracts.xml](../app/src/main/res/layout/fragment_contracts.xml) через `FragmentContractsBinding` (это не “detail”).
- По nav_graph он должен принимать `contractId`.

То есть экран “детали аренды” логически заявлен, но ещё не реализован.

Мини-шаблон будущего detail-экрана (как достать `contractId` из аргументов):

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/contracts/ContractDetailFragment.kt
val contractId = requireArguments().getLong("contractId")
// TODO: RetrofitClient.apiService.getContractById(contractId)
```

---

## ui/profile: профиль

Файл: [app/src/main/java/com/example/carcatalogue/ui/profile/ProfileFragment.kt](../app/src/main/java/com/example/carcatalogue/ui/profile/ProfileFragment.kt)

Layout:

- [app/src/main/res/layout/fragment_profile.xml](../app/src/main/res/layout/fragment_profile.xml)

Логика:

- Пытается загрузить профиль с API.
- Если не получилось — берёт кэшированное имя/почту из `TokenManager`.
- Logout:
  - чистит `TokenManager`
  - сбрасывает `RetrofitClient.authToken`
  - навигация на логин (через action из nav_graph)

Мини-пример logout (очистка локального состояния + переход):

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/profile/ProfileFragment.kt
binding.btnLogout.setOnClickListener {
  lifecycleScope.launch {
    tokenManager.clearToken()
    RetrofitClient.authToken = null
    findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
  }
}
```

---

## Cookie-auth: почему “просто работает” после логина

Файл: [app/src/main/java/com/example/carcatalogue/data/api/InMemoryCookieJar.kt](../app/src/main/java/com/example/carcatalogue/data/api/InMemoryCookieJar.kt)

Коротко:

- Сервер на логине шлёт `Set-Cookie: access_token=...; HttpOnly` и `refresh_token=...; HttpOnly`.
- OkHttp сам не хранит cookies между запросами без `CookieJar`.
- `InMemoryCookieJar` делает две вещи:
  - сохраняет cookies из ответа
  - подставляет подходящие cookies в следующий запрос

Ограничение:

- Это **in-memory**. После убийства приложения cookies исчезнут.

Если захочешь “как в браузере” (переживать перезапуск), нужен persistent cookie jar (на диске).

Мини-пример CookieJar (из проекта):

```kotlin
// app/src/main/java/com/example/carcatalogue/data/api/InMemoryCookieJar.kt
override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
  cookies.forEach { newCookie ->
    cookieStore.removeAll { old ->
      old.name == newCookie.name && old.domain == newCookie.domain && old.path == newCookie.path
    }
    cookieStore.add(newCookie)
  }
}

override fun loadForRequest(url: HttpUrl): List<Cookie> {
  val now = System.currentTimeMillis()
  cookieStore.removeAll { it.expiresAt < now }
  return cookieStore.filter { it.matches(url) }
}
```

Запоминалка: это ровно как “браузерная cookie jar”, только без диска.

---

# `/res`: что это за лес и как в нём жить

Папка: [app/src/main/res](../app/src/main/res)

Ключевое правило Android:

- Всё, что не Kotlin-код, а “описание UI/конфигов” — живёт в `res/`.
- Ресурсы имеют тип (`layout`, `string`, `drawable`, …) и имя.
- В коде ты обращаешься к ним через `R.<type>.<name>`.

## layout/

Папка: [app/src/main/res/layout](../app/src/main/res/layout)

Это XML-шаблоны экранов и элементов списка.

Пример: как Kotlin привязывается к layout.

1) Есть layout [app/src/main/res/layout/fragment_contracts.xml](../app/src/main/res/layout/fragment_contracts.xml)

2) В коде Fragment использует Binding, соответствующий имени файла:

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/contracts/ContractsFragment.kt
private var _binding: FragmentContractsBinding? = null
private val binding get() = _binding!!

override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
  _binding = FragmentContractsBinding.inflate(inflater, container, false)
  return binding.root
}
```

Супер-правило: имя `FragmentContractsBinding` = `fragment_contracts.xml`.

В этом проекте есть “варианты” одного и того же экрана:

- `fragment_login.xml` и `fragment_login_premium.xml`
- `fragment_catalogue.xml`, `fragment_catalogue_modern.xml`, `fragment_catalogue_vibrant.xml`
- `fragment_car_detail.xml`, `fragment_car_detail_modern.xml`, `fragment_car_detail_vibrant.xml`
- `item_car.xml`, `item_car_modern.xml`, `item_car_premium.xml`

Как понять, какой реально используется:

- Смотри импорт binding в Fragment/Adapter.
  - `FragmentCatalogueVibrantBinding` → используется `fragment_catalogue_vibrant.xml`
  - `ItemCarPremiumBinding` → используется `item_car_premium.xml`

Системно важные layouts:

- [app/src/main/res/layout/activity_main.xml](../app/src/main/res/layout/activity_main.xml) — каркас, где живёт NavHost и bottom nav
- [app/src/main/res/layout/item_contract.xml](../app/src/main/res/layout/item_contract.xml) — элемент списка аренд

## menu/

Папка: [app/src/main/res/menu](../app/src/main/res/menu)

Меню — это XML-описание пунктов для тулбара/боттомнава/контекстных меню.

Что реально используется сейчас:

- [app/src/main/res/menu/bottom_nav_menu.xml](../app/src/main/res/menu/bottom_nav_menu.xml) подключён в `activity_main.xml`.

Мини-пример (один пункт меню = один destination):

```xml
<!-- app/src/main/res/menu/bottom_nav_menu.xml -->
<item
  android:id="@+id/profileFragment"
  android:icon="@drawable/ic_profile"
  android:title="@string/nav_profile" />
```

Остальные:

- [app/src/main/res/menu/menu_bottom_navigation.xml](../app/src/main/res/menu/menu_bottom_navigation.xml)
- [app/src/main/res/menu/menu_toolbar.xml](../app/src/main/res/menu/menu_toolbar.xml)

могут быть заделом/старой версией. Это нормально в прототипах, но если хочешь “чистый” проект — такие вещи обычно либо подключают, либо удаляют.

## values/ и values-night/

Папки:

- [app/src/main/res/values](../app/src/main/res/values)
- [app/src/main/res/values-night](../app/src/main/res/values-night)

Это “таблицы констант”. Android сам выбирает нужный набор в зависимости от режима (день/ночь).

Что внутри:

- [app/src/main/res/values/strings.xml](../app/src/main/res/values/strings.xml) — все тексты
- [app/src/main/res/values/colors.xml](../app/src/main/res/values/colors.xml) — палитра
- [app/src/main/res/values/themes.xml](../app/src/main/res/values/themes.xml) — тема приложения (Material3)
- [app/src/main/res/values-night/themes.xml](../app/src/main/res/values-night/themes.xml) — ночная версия темы
- [app/src/main/res/values/styles.xml](../app/src/main/res/values/styles.xml) — переиспользуемые стили/типографика
- [app/src/main/res/values/arrays.xml](../app/src/main/res/values/arrays.xml) — списки/диапазоны (если используются)

Как это связано с UI:

- В манифесте у приложения `android:theme="@style/Theme.CarSharing"`.
- `Theme.CarSharing` определён в `themes.xml`.
- Там настроены базовые цвета + дефолтные стили кнопок/карточек.

Практический смысл:

- Хочешь сделать “везде одинаковые” кнопки → правишь `Widget.CarSharing.Button`.
- Хочешь изменить стиль карточек → `Widget.CarSharing.Card`.

Пример: как тема подключается и «течёт» по всему приложению.

1) В манифесте:

```xml
<!-- app/src/main/AndroidManifest.xml -->
<application
  android:theme="@style/Theme.CarSharing" />
```

2) В теме (упрощённо):

```xml
<!-- app/src/main/res/values/themes.xml -->
<style name="Base.Theme.CarSharing" parent="Theme.Material3.Light.NoActionBar">
  <item name="colorPrimary">@color/primary</item>
  <item name="materialButtonStyle">@style/Widget.CarSharing.Button</item>
</style>
```

3) И теперь любая `MaterialButton` автоматически подхватывает стиль.

Запоминалка: `Theme` = глобальный конфиг UI-компонентов (как “design system config”).

## xml/

Папка: [app/src/main/res/xml](../app/src/main/res/xml)

Тут лежат системные конфиги.

Конкретно в проекте:

- [app/src/main/res/xml/network_security_config.xml](../app/src/main/res/xml/network_security_config.xml)
  - разрешает cleartext HTTP (иначе Android будет резать запросы на `http://...`)

Также в манифесте упомянуты `backup_rules` и `data_extraction_rules` — они обычно тоже лежат в `res/xml`.

Мини-пример (разрешаем HTTP):

```xml
<!-- app/src/main/res/xml/network_security_config.xml -->
<network-security-config>
  <base-config cleartextTrafficPermitted="true" />
</network-security-config>
```

## mipmap-*/

Это иконки приложения для разных плотностей экрана.

- `@mipmap/ic_launcher` и `@mipmap/ic_launcher_round` подключены в манифесте.

Вот где именно это подключено:

```xml
<!-- app/src/main/AndroidManifest.xml -->
<application
  android:icon="@mipmap/ic_launcher"
  android:roundIcon="@mipmap/ic_launcher_round" />
```

---

# Как добавить новый экран (пошагово, без магии)

Пример: хотим добавить экран `SettingsFragment`.

1) Создать layout

- создаёшь [app/src/main/res/layout/fragment_settings.xml](../app/src/main/res/layout/fragment_settings.xml)
- добавляешь нужные View с `@+id/...`

2) Создать Fragment

- создаёшь класс `SettingsFragment.kt` в папке UI, например:
  - `app/src/main/java/com/example/carcatalogue/ui/settings/SettingsFragment.kt`
- подключаешь binding: `FragmentSettingsBinding`

3) Добавить destination в nav_graph

- правишь [app/src/main/res/navigation/nav_graph.xml](../app/src/main/res/navigation/nav_graph.xml)
- добавляешь новый `<fragment ... android:id="@+id/settingsFragment" ... />`

4) Добавить action из места, откуда идёшь

- например, из `profileFragment` добавить `<action app:destination="@id/settingsFragment" />`

5) Вызвать навигацию из кода

- `findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)`

6) Если нужен в bottom nav

- добавить item в [app/src/main/res/menu/bottom_nav_menu.xml](../app/src/main/res/menu/bottom_nav_menu.xml) с `android:id` равным id destination.

7) Подумать про “скрывать ли bottom nav”

- обновить список destination’ов в [app/src/main/java/com/example/carcatalogue/MainActivity.kt](../app/src/main/java/com/example/carcatalogue/MainActivity.kt), если экран должен быть без нижнего меню.

---

# Частые грабли (и почему они возникают)

- `binding` NPE → забыли `_binding = null` в `onDestroyView` или используете `binding` после уничтожения view.
- “Не передались аргументы” → ключи в Bundle не совпали с именем аргумента в nav_graph.
- “UI не обновляется” → собираете Flow не на `viewLifecycleOwner.lifecycleScope`.
- “HTTP не работает на Android 9+” → нет `network_security_config` или `usesCleartextTraffic`.

И ещё 2 супер-типичных:

- “Открыл bottom sheet, а там только кусочек” → у BottomSheetDialogFragment collapsed-state/peekHeight. В проекте это решается расширением поведения при старте.
- “В списке кликаю, но ничего не происходит” → обработчик клика должен быть в Adapter (на root/button) и делегировать наружу через callback.

---

## Что дальше

Если хочешь, я могу:

- довести до конца `ContractDetailFragment` (с отдельным layout и загрузкой по `contractId`),
- привести к одному стилю использование SafeArgs (сейчас часть переходов через Bundle),
- или подчистить “лишние” layout/menu варианты, чтобы проект стал проще поддерживать.

Если решишь доделать переход в детали аренды (пример “action + arg” без SafeArgs):

```kotlin
// app/src/main/java/com/example/carcatalogue/ui/contracts/ContractsFragment.kt
findNavController().navigate(
  R.id.action_contractsFragment_to_contractDetailFragment,
  bundleOf("contractId" to contractId)
)
```
