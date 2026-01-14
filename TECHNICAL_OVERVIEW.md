# ZipGo — Техническое описание проекта

## Общая информация

**ZipGo** — Android-приложение для каршеринга, реализованное на Kotlin с использованием современных практик Android-разработки. Проект демонстрирует полный цикл разработки клиент-серверного мобильного приложения от архитектуры до UI/UX.

---

## Архитектура приложения

### Архитектурный паттерн: MVVM (Model-View-ViewModel)

Приложение строго следует MVVM-архитектуре с чёткым разделением ответственности:

**View (UI Layer)**
- `Fragment` классы отвечают только за отображение UI и реакцию на пользовательские действия
- Используют `ViewBinding` для type-safe доступа к View элементам
- Подписываются на `LiveData` из ViewModel для реактивного обновления UI
- Никакой бизнес-логики — только делегирование действий в ViewModel

**ViewModel**
- Управляет состоянием UI
- Содержит бизнес-логику представления
- Lifecycle-aware: переживает конфигурационные изменения (поворот экрана)
- Взаимодействует с Repository через Kotlin Coroutines
- Использует `LiveData` / `MutableLiveData` для передачи данных во View
- Пример: `CatalogueViewModel` управляет списком авто, фильтрами, состоянием загрузки

**Model (Data Layer)**
- Repository Pattern: абстракция источника данных (сеть, кеш, БД)
- DTO классы (`Car`, `Contract`, `User`, `UserStats`) — pure data classes
- `ApiService` (Retrofit) — единственный источник сетевых данных
- `DataStore` — персистентное хранилище токенов и preferences

### Repository Pattern

Каждый домен имеет свой репозиторий:

**CarRepository**
```kotlin
class CarRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
)
```
- `getCars(brands, classes, ...)` — получение каталога с фильтрами
- `getCarDetails(id)` — детали автомобиля
- Абстрагирует ViewModel от источника данных
- В будущем легко добавить кеширование без изменения ViewModel

**ContractRepository**
- Создание/просмотр/отмена контрактов
- Управление статусами (PENDING, ACTIVE, COMPLETED, CANCELLED)

**UserRepository**
- Авторизация, регистрация, logout
- Управление профилем
- Загрузка статистики

### Single-Activity Architecture

Приложение использует один `MainActivity` + Navigation Component:
- **Один Activity** — вся навигация через фрагменты
- **Navigation Graph** (`nav_graph.xml`) — декларативное описание всех маршрутов
- **Safe Args** — type-safe передача параметров между фрагментами
- **Bottom Navigation** — глобальная навигация между главными разделами

Преимущества:
- Упрощённый lifecycle management
- Плавные переходы между экранами
- Единая точка входа для deep links
- Меньше boilerplate кода

---

## Стилистика и качество кода

### Kotlin Code Style

**Иммутабельность по умолчанию**
```kotlin
// Prefer val over var
val cars: List<Car> = emptyList()
val selectedBrands: MutableList<String> = mutableListOf()  // только если необходима мутация
```

**Null-safety**
```kotlin
// Nullable types явно указываются
var token: String? = null

// Safe calls и Elvis operator
val userName = user?.name ?: "Guest"

// let для null-safe блоков
user?.let { displayUserInfo(it) }
```

**Data classes для моделей**
```kotlin
data class Car(
    val id: Int,
    val brand: String,
    val model: String,
    val imageUrl: String?,  // nullable — может отсутствовать
    val pricePerDay: Double
)
```
- Автоматическая генерация `equals()`, `hashCode()`, `toString()`, `copy()`
- Immutable по умолчанию (все поля `val`)

**Extension functions для читаемости**
```kotlin
// Вместо утилит-классов
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}
```

**Scope functions для конфигурации**
```kotlin
binding.apply {
    carTitle.text = car.model
    carPrice.text = "${car.pricePerDay} ₽/день"
    carImage.load(car.imageUrl) {
        placeholder(R.drawable.default_car_photo_1)
    }
}
```

### Соглашения по именованию

- **Классы**: PascalCase — `CarDetailFragment`, `CatalogueViewModel`
- **Функции/переменные**: camelCase — `loadCatalogue()`, `selectedBrands`
- **Константы**: UPPER_SNAKE_CASE — `BASE_URL`, `DEFAULT_PAGE_SIZE`
- **Resources**: snake_case — `fragment_profile.xml`, `ic_launcher_foreground.xml`
- **Layout IDs**: camelCase с префиксом — `btnLogin`, `tvCarTitle`, `rvCarsList`

### Организация кода

**Package by feature**
```
ui/
├── auth/           # Всё, что относится к авторизации
├── catalogue/      # Каталог: фрагмент, адаптер, ViewModel, bottom sheet
├── profile/        # Профиль: просмотр, редактирование, смена пароля
└── contracts/      # Управление контрактами
```

**Separation of Concerns**
- Один класс = одна ответственность
- Fragment — только UI
- ViewModel — бизнес-логика + state management
- Repository — работа с данными
- Adapter — только отображение списка

---

## Применение паттернов проектирования

### 1. Repository Pattern
- Абстракция источника данных
- ViewModel не знает, откуда приходят данные (сеть, БД, кеш)
- Единая точка изменения логики получения данных

### 2. Observer Pattern (LiveData/Flow)
```kotlin
// ViewModel
val cars: LiveData<List<Car>> = _cars

// Fragment
viewModel.cars.observe(viewLifecycleOwner) { carList ->
    adapter.submitList(carList)
}
```
- UI автоматически обновляется при изменении данных
- Lifecycle-aware: подписка активна только когда Fragment visible

### 3. Dependency Injection (Manual DI, готово к Hilt/Koin)
```kotlin
class CatalogueViewModel(
    private val repository: CarRepository  // зависимость инжектится
) : ViewModel()
```
- Зависимости передаются через конструктор
- Легко подменить repository для тестирования
- Готово к миграции на Hilt/Koin

### 4. Adapter Pattern (RecyclerView.Adapter)
```kotlin
class CarAdapter : RecyclerView.Adapter<CarAdapter.ViewHolder>()
```
- Адаптирует `List<Car>` к RecyclerView
- ViewHolder pattern для переиспользования View

### 5. Builder Pattern (Retrofit, OkHttp)
```kotlin
Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

### 6. Strategy Pattern (Coil ImageLoader)
```kotlin
imageView.load(url) {
    crossfade(true)
    placeholder(R.drawable.default_car_photo_1)
    error(R.drawable.default_car_photo_2)
}
```
- Различные стратегии загрузки изображений

### 7. Singleton Pattern (RetrofitClient)
```kotlin
object RetrofitClient {
    private val retrofit: Retrofit by lazy { /* ... */ }
    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}
```
- Единственный экземпляр HTTP-клиента

---

## Использование графов и структур данных

### Navigation Graph (Directed Graph)

**nav_graph.xml** — направленный граф навигации:
```xml
<navigation>
    <fragment id="@+id/loginFragment" />
    <fragment id="@+id/catalogueFragment" />
    <fragment id="@+id/carDetailFragment" />
    
    <action id="@+id/action_login_to_catalogue"
        app:destination="@id/catalogueFragment"
        app:popUpTo="@id/loginFragment"
        app:popUpToInclusive="true" />
</navigation>
```

**Структура графа:**
- **Узлы (nodes)**: фрагменты (destinations)
- **Рёбра (edges)**: действия перехода (actions)
- **Back Stack**: управляется автоматически
- **Циклы**: предотвращаются через `popUpTo`

**Применение:**
- Декларативное описание всех возможных путей навигации
- Визуализация в Android Studio (Graph Editor)
- Type-safe передача данных через Safe Args

### Dependency Graph (Directed Acyclic Graph - DAG)

Зависимости между компонентами образуют DAG:
```
Fragment -> ViewModel -> Repository -> ApiService
                                    -> DataStore
```

**Правила:**
- Односторонние зависимости (no cycles)
- Верхний уровень зависит от нижнего, но не наоборот
- Data flow: снизу вверх (через callbacks/LiveData)
- Control flow: сверху вниз (через method calls)

### Tree Structure (View Hierarchy)

**XML Layout** — дерево View элементов:
```xml
<androidx.constraintlayout.widget.ConstraintLayout>
    <com.google.android.material.appbar.MaterialToolbar />
    <androidx.recyclerview.widget.RecyclerView />
    <com.google.android.material.floatingactionbutton.FloatingActionButton />
</androidx.constraintlayout.widget.ConstraintLayout>
```

**Обход дерева:** findViewById → ViewBinding (compile-time safety)

### Lists (Collections)

**Коллекции для фильтров:**
```kotlin
val selectedBrands: MutableList<String> = mutableListOf()
val selectedClasses: MutableList<String> = mutableListOf()

// Multi-select реализован через add/remove
chip.setOnCheckedChangeListener { _, isChecked ->
    if (isChecked) selectedBrands.add(brand)
    else selectedBrands.remove(brand)
}
```

**Immutable snapshots для API:**
```kotlin
fun applyFilters(brands: List<String>, classes: List<String>) {
    // Создаём копии для thread-safety
    repository.getCars(
        brands = brands.toList(),
        classes = classes.toList()
    )
}
```

---

## Асинхронность и Coroutines

### Structured Concurrency

**ViewModel Scope:**
```kotlin
fun loadCatalogue() {
    viewModelScope.launch {
        _loading.value = true
        try {
            val cars = repository.getCars()
            _cars.value = cars
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _loading.value = false
        }
    }
}
```

**Преимущества:**
- Автоматическая отмена при уничтожении ViewModel
- No memory leaks
- Чёткая иерархия корутин

### Dispatchers

```kotlin
// IO dispatcher для сетевых запросов
suspend fun getCars(): List<Car> = withContext(Dispatchers.IO) {
    apiService.getCatalogue()
}

// Main dispatcher для обновления UI (default в viewModelScope)
_cars.postValue(carsList)  // thread-safe posting
```

### Suspend Functions

```kotlin
// Repository
suspend fun authenticate(email: String, password: String): User

// ApiService (Retrofit автоматически делает suspend)
@POST("/api/auth")
suspend fun login(@Body credentials: LoginRequest): LoginResponse
```

---

## Работа с сетью (Retrofit + OkHttp)

### REST API интеграция

**ApiService interface:**
```kotlin
interface ApiService {
    @GET("/api/car/catalogue")
    suspend fun getCatalogue(
        @Query("brand") brands: List<String>?,
        @Query("car_class") classes: List<String>?
    ): List<Car>
    
    @POST("/api/contracts")
    suspend fun createContract(@Body contract: CreateContractRequest): Contract
}
```

**Ключевые особенности:**
- `suspend` функции для корутин
- `@Query` с `List<String>` — автоматическое повторение параметра (`?brand=BMW&brand=Audi`)
- Type-safe request/response через data classes

### Interceptors (Chain of Responsibility)

**AuthInterceptor:**
```kotlin
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val token = tokenManager.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

**Применение паттернов:**
- Chain of Responsibility: interceptor chain
- Decorator: wrapping requests

**Logging Interceptor (Debug only):**
```kotlin
if (BuildConfig.DEBUG) {
    client.addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
}
```

### Error Handling

```kotlin
try {
    val response = apiService.getCatalogue()
    Success(response)
} catch (e: HttpException) {
    when (e.code()) {
        401 -> Error("Unauthorized")
        404 -> Error("Not found")
        409 -> Error("Resource conflict")
        else -> Error("Network error: ${e.message}")
    }
} catch (e: IOException) {
    Error("Connection failed")
}
```

---

## UI Framework и Material Design 3

### ViewBinding vs findViewById

**Старый подход (findViewById):**
```kotlin
val button = findViewById<Button>(R.id.btnLogin)  // runtime, nullable, unsafe
```

**ViewBinding:**
```kotlin
private var _binding: FragmentProfileBinding? = null
private val binding get() = _binding!!

override fun onCreateView(...): View {
    _binding = FragmentProfileBinding.inflate(inflater, container, false)
    return binding.root
}

// Type-safe, compile-time checked, non-null
binding.btnLogin.setOnClickListener { }
```

**Преимущества:**
- Compile-time safety (нет ошибок с ID)
- Null-safety через lateinit/nullable
- Автоматическая генерация из XML

### Material Design 3 Components

**Material Card:**
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="16dp"
    app:cardElevation="2dp"
    app:strokeWidth="1dp"
    app:strokeColor="@color/outline">
```

**Material Button:**
```xml
<com.google.android.material.button.MaterialButton
    style="@style/Widget.Material3.Button.Elevated"
    app:icon="@drawable/ic_filter"
    app:cornerRadius="12dp">
```

**Chips (Multi-select):**
```xml
<com.google.android.material.chip.ChipGroup
    app:singleSelection="false"
    app:selectionRequired="false">
    
    <com.google.android.material.chip.Chip
        style="@style/Widget.Material3.Chip.Filter"
        android:checkable="true" />
</com.google.android.material.chip.ChipGroup>
```

### ConstraintLayout для адаптивности

```xml
<androidx.constraintlayout.widget.ConstraintLayout>
    <ImageView
        android:id="@+id/carImage"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintDimensionRatio="16:9" />
    
    <TextView
        android:id="@+id/carTitle"
        app:layout_constraintTop_toBottomOf="@id/carImage"
        app:layout_constraintStart_toStartOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

**Преимущества:**
- Плоская иерархия (лучше производительность, чем nested layouts)
- Constraint chains для равномерного распределения
- Barriers для динамического контента
- Guidelines для выравнивания

---

## Реализация ключевых фич

### 1. Multi-Select фильтры

**Проблема:** Позволить пользователю выбрать несколько брендов/классов одновременно

**Решение:**
```kotlin
// FiltersBottomSheet
val selectedBrands = mutableListOf<String>()

chipBMW.setOnCheckedChangeListener { _, isChecked ->
    if (isChecked) selectedBrands.add("BMW")
    else selectedBrands.remove("BMW")
}

// При применении фильтров
viewModel.applyFilters(
    brands = selectedBrands.toList(),  // immutable copy
    classes = selectedClasses.toList()
)

// ViewModel -> Repository -> ApiService
@GET("/api/car/catalogue")
suspend fun getCatalogue(
    @Query("brand") brands: List<String>?,  // Retrofit добавит ?brand=BMW&brand=Audi
    @Query("car_class") classes: List<String>?
): List<Car>
```

**Особенности реализации:**
- Material3 Chips с `checkable="true"`
- `ChipGroup` с `singleSelection="false"`
- State сохраняется в ViewModel (переживает поворот экрана)
- Retrofit автоматически сериализует `List<String>` в повторяющиеся query параметры

### 2. Fallback изображения для автомобилей

**Проблема:** Backend может не вернуть `imageUrl` для некоторых авто

**Решение:**
```kotlin
// CarAdapter
Coil для загрузки:
binding.carImage.load(car.imageUrl) {
    crossfade(true)
    placeholder(R.drawable.default_car_photo_1)  // показывается во время загрузки
    error(getRandomDefaultPhoto())  // показывается при ошибке/null URL
}

// Randomize fallback для визуального разнообразия
private fun getRandomDefaultPhoto(): Int {
    return listOf(
        R.drawable.default_car_photo_1,
        R.drawable.default_car_photo_2,
        R.drawable.default_car_photo_3,
        R.drawable.default_car_photo_4
    ).random()
}
```

**Vector Drawables для fallback:**
- Масштабируются без потери качества
- Малый размер APK
- Легко перекрашиваются через tint

### 3. Статистика пользователя в профиле

**Интеграция с OpenAPI:**
```kotlin
// API endpoint из api-docs.json
@GET("/api/stats/overview/client")
suspend fun getOverviewStats(): UserStats

// Model
data class UserStats(
    val totalRentals: Int,
    val totalSpent: Double,
    val averageDriveDuration: Int  // в минутах
)

// ProfileFragment
viewModel.loadUserStats()

viewModel.userStats.observe(viewLifecycleOwner) { stats ->
    binding.apply {
        tvTotalRentals.text = stats.totalRentals.toString()
        tvTotalSpent.text = "${stats.totalSpent} ₽"
        tvAvgDuration.text = "${stats.averageDriveDuration} мин"
    }
}
```

**Apple HIG-inspired UI:**
- Крупные числа (статистика) — главный акцент
- Карточки с soft shadows
- Мягкая цветовая палитра (#F3E5F5, #E3F2FD)
- Generous whitespace

### 4. Редактирование профиля и смена пароля

**PATCH endpoints:**
```kotlin
// Update profile
@PATCH("/api/profile")
suspend fun updateProfile(@Body request: UpdateProfileRequest): User

data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String,
    val phone: String
)

// Change password
@PATCH("/api/profile/password")
suspend fun changePassword(@Body request: ChangePasswordRequest): MessageResponse

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)
```

**Валидация на клиенте:**
```kotlin
fun validatePasswordChange(): Boolean {
    return when {
        oldPassword.isEmpty() -> {
            showError("Введите старый пароль")
            false
        }
        newPassword.length < 6 -> {
            showError("Пароль должен быть не менее 6 символов")
            false
        }
        newPassword != confirmPassword -> {
            showError("Пароли не совпадают")
            false
        }
        else -> true
    }
}
```

### 5. Адаптивная иконка приложения

**Структура:**
```
res/
├── mipmap-mdpi/
│   ├── ic_launcher.webp
│   └── ic_launcher_round.webp
├── mipmap-hdpi/...
├── mipmap-xhdpi/...
├── mipmap-xxhdpi/...
├── mipmap-xxxhdpi/...
└── drawable/
    ├── ic_launcher_foreground.xml  # Vector (Z logo)
    └── ic_launcher_background.xml  # Solid color
```

**AndroidManifest.xml:**
```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:adaptiveIcon="@drawable/ic_launcher">
```

**Генерация WebP mipmap'ов:**
- Использовали Pillow (Python) для автоматической генерации всех плотностей
- WebP — лучшее сжатие, чем PNG (меньше APK)

---

## Безопасность

### 1. Хранение токенов (DataStore)

```kotlin
class TokenManager(context: Context) {
    private val dataStore = context.createDataStore("auth_prefs")
    
    suspend fun saveToken(token: String) {
        dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }
    
    suspend fun getToken(): String? {
        return dataStore.data.map { it[TOKEN_KEY] }.first()
    }
}
```

**Преимущества над SharedPreferences:**
- Асинхронные операции (Coroutines/Flow)
- Type-safety через Preferences Keys
- Транзакционность (atomic updates)
- Автоматическое шифрование на новых версиях Android

### 2. Автоматическая подстановка токена

```kotlin
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val token = runBlocking { tokenManager.getToken() }
        val request = token?.let {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $it")
                .build()
        } ?: chain.request()
        return chain.proceed(request)
    }
}
```

**Security best practices:**
- Токен никогда не передаётся явно в каждом запросе (DRY principle)
- Централизованная логика авторизации
- Легко добавить refresh token logic

### 3. HTTPS

```kotlin
const val BASE_URL = "https://your-api.example.com/"  // HTTPS обязателен в production
```

### 4. ProGuard/R8 для обфускации

```gradle
buildTypes {
    release {
        minifyEnabled true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

---

## Производительность

### 1. RecyclerView оптимизации

```kotlin
class CarAdapter : RecyclerView.Adapter<CarAdapter.ViewHolder>() {
    
    // ViewHolder pattern: переиспользование View
    class ViewHolder(val binding: ItemCarBinding) : RecyclerView.ViewHolder(binding.root)
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val car = cars[position]
        holder.binding.apply {
            // Минимум операций в onBind (вызывается часто при скролле)
            carTitle.text = car.model
            carPrice.text = "${car.pricePerDay} ₽"
            
            // Coil автоматически кеширует изображения
            carImage.load(car.imageUrl)
        }
    }
    
    // DiffUtil для эффективного обновления списка
    fun submitList(newCars: List<Car>) {
        val diffResult = DiffUtil.calculateDiff(CarDiffCallback(cars, newCars))
        cars = newCars
        diffResult.dispatchUpdatesTo(this)
    }
}
```

### 2. Coil image caching

- **Memory cache**: LRU cache для быстрого доступа
- **Disk cache**: OkHttp cache для персистентности
- **Automatic bitmap pooling**: переиспользование Bitmap объектов

### 3. Lazy initialization

```kotlin
object RetrofitClient {
    val apiService: ApiService by lazy {  // создаётся только при первом обращении
        retrofit.create(ApiService::class.java)
    }
}
```

### 4. Coroutines вместо Threads

- Легковесные (тысячи корутин = один thread)
- Structured concurrency (автоотмена)
- No callback hell

---

## Тестируемость

Хотя тесты не написаны, архитектура готова к тестированию:

**Unit тесты (ViewModel):**
```kotlin
@Test
fun `applyFilters updates cars LiveData`() = runTest {
    // Given
    val mockRepository = mockk<CarRepository>()
    coEvery { mockRepository.getCars(any(), any()) } returns listOf(mockCar)
    val viewModel = CatalogueViewModel(mockRepository)
    
    // When
    viewModel.applyFilters(brands = listOf("BMW"), classes = emptyList())
    
    // Then
    assertEquals(1, viewModel.cars.value?.size)
}
```

**Integration тесты (Repository):**
```kotlin
@Test
fun `repository returns cars from API`() = runTest {
    // Given
    val mockApi = mockk<ApiService>()
    coEvery { mockApi.getCatalogue() } returns listOf(mockCar)
    val repository = CarRepository(mockApi, mockTokenManager)
    
    // When
    val result = repository.getCars()
    
    // Then
    assertEquals(1, result.size)
}
```

**UI тесты (Espresso):**
```kotlin
@Test
fun clickFilterButton_opensBottomSheet() {
    onView(withId(R.id.btnFilters)).perform(click())
    onView(withId(R.id.filtersBottomSheet)).check(matches(isDisplayed()))
}
```

---

## Масштабируемость

### Готовность к расширению

**1. Dependency Injection готовность**
- Зависимости через конструктор
- Легко мигрировать на Hilt:
```kotlin
@HiltViewModel
class CatalogueViewModel @Inject constructor(
    private val repository: CarRepository
) : ViewModel()
```

**2. Модульность**
- Package by feature
- Каждая фича независима
- Легко выделить в отдельный модуль (multi-module project)

**3. Room БД для кеширования**
```kotlin
// Готово к добавлению
@Entity
data class CarEntity(...)

@Dao
interface CarDao {
    @Query("SELECT * FROM cars")
    fun getAllCars(): Flow<List<CarEntity>>
}

// Repository будет решать: сеть или БД
```

**4. Feature Flags**
```kotlin
object FeatureFlags {
    const val ENABLE_DARK_THEME = false
    const val ENABLE_PUSH_NOTIFICATIONS = false
}
```

---

## Выводы

**ZipGo демонстрирует:**
1. **Clean Architecture** — чёткое разделение слоёв
2. **MVVM Pattern** — отделение UI от бизнес-логики
3. **Repository Pattern** — абстракция данных
4. **Kotlin Best Practices** — null-safety, coroutines, data classes
5. **Material Design 3** — современный UI
6. **RESTful API Integration** — Retrofit + OkHttp
7. **Reactive Programming** — LiveData/Flow
8. **Navigation Component** — Single-Activity
9. **Performance** — RecyclerView, Coil, Coroutines
10. **Testability** — готовность к Unit/Integration/UI тестам

Проект готов к production-use с минимальными доработками (тесты, CI/CD, crash reporting).
