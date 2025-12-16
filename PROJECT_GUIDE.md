# Руководство по реализации приложения CarSharing

## ✅ Уже создано:

### Модели данных (`data/model/`):
- ✅ User.kt - модели пользователя и авторизации
- ✅ Car.kt - модели автомобилей
- ✅ Contract.kt - модели контрактов
- ✅ Document.kt - модели документов
- ✅ CarModel.kt - модели моделей автомобилей
- ✅ PagedModel.kt - пагинация

### API (`data/api/`):
- ✅ ApiService.kt - полный интерфейс API со всеми endpoints

### Ресурсы:
- ✅ drawable/ic_*.xml - иконки для UI
- ✅ menu/menu_bottom_navigation.xml - нижнее меню
- ✅ menu/menu_toolbar.xml - меню тулбара

## 📋 Требуется реализовать:

### 1. Экраны авторизации и регистрации

**LoginFragment.kt:**
```kotlin
- Реализовать логику входа через ApiService.login()
- Обработка ошибок (неверный логин/пароль)
- Сохранение токенов через TokenManager
- Переход к каталогу при успешном входе
- Кнопка "Забыли пароль" -> диалог сброса пароля
- Переключение на экран регистрации
```

**RegisterFragment.kt:**
```kotlin
- Валидация полей (минимум 6 символов для логина/пароля)
- Проверка email формата
- Регистрация через ApiService.register()
- Автоматический вход после регистрации
- Переключение на экран входа
```

**Layout уже существует:** `fragment_login.xml`, `fragment_register.xml`

### 2. Главный экран с Navigation Bar

**MainActivity.kt:**
```kotlin
- Настроить NavController с nav_graph
- Показывать/скрывать BottomNavigationView в зависимости от авторизации
- Скрывать "Контракты" и "Избранное" для неавторизованных
- Обработка нажатия на иконку профиля в Toolbar
- Показ PopupMenu при клике на профиль:
  - Перейти в профиль
  - Изменить пароль
  - Админ панель (если admin)
  - Выйти
```

**Layout:** `activity_main.xml` (уже существует)

### 3. Каталог с фильтрами и пагинацией

**fragment_catalogue.xml:**
```xml
<androidx.constraintlayout.widget.ConstraintLayout>
    <!-- Toolbar с кнопкой фильтров -->
    <MaterialButton btnFilters />
    
    <!-- RecyclerView для списка авто -->
    <RecyclerView rvCars />
    
    <!-- SwipeRefreshLayout -->
    <!-- ProgressBar -->
    
    <!-- Боковая панель фильтров (DrawerLayout или BottomSheet) -->
    <BottomSheetDialog>
        - Dropdown: Бренд
        - Dropdown: Модель
        - Dropdown: Тип кузова
        - Dropdown: Класс авто
        - RangeSlider: Год выпуска
        - RangeSlider: Цена за день
        - DateRangePicker: Даты аренды
        - Кнопки: Применить / Сбросить
    </BottomSheetDialog>
</androidx.constraintlayout.widget.ConstraintLayout>
```

**CatalogueViewModel.kt:**
```kotlin
class CatalogueViewModel : ViewModel() {
    // StateFlow для списка авто
    // Фильтры (brand, model, year, class, price, dates)
    // Пагинация (loadCars, loadMore)
    // addFavorite/removeFavorite
    
    fun loadCars(page: Int, filters: CarFilters) {
        viewModelScope.launch {
            val response = ApiService.getCatalogue(
                brand = filters.brand,
                model = filters.model,
                // ... все фильтры
                page = page
            )
        }
    }
}
```

**CarAdapter.kt:**
```kotlin
class CarAdapter(
    private val onCarClick: (Car) -> Unit,
    private val onFavoriteClick: (Car) -> Unit
) : ListAdapter<CarListItemResponse, CarViewHolder>() {
    // Bind данные к item_car.xml
    // Обработка клика на карточку
    // Обработка клика на сердечко избранного
    // Загрузка изображения через Coil
}
```

### 4. Детальный просмотр автомобиля

**fragment_car_detail.xml:**
```xml
<ScrollView>
    <!-- ViewPager2 для фото (если несколько) -->
    <!-- Кнопка "В избранное" (FAB) -->
    
    <!-- Карточка с информацией -->
    <MaterialCardView>
        - Бренд и модель (большой заголовок)
        - Год, класс, тип кузова
        - VIN, гос. номер
        - Статус (chip)
    </MaterialCardView>
    
    <!-- Карточка "Стоимость аренды" -->
    <MaterialCardView>
        - Цена за день
        - DateRangePicker для выбора дат
        - Расчет общей стоимости
    </MaterialCardView>
    
    <!-- Кнопка "Арендовать" -->
    <MaterialButton btnRent />
</ScrollView>
```

**CarDetailFragment.kt:**
```kotlin
- Загрузка данных авто по carId
- Показ дефолтного изображения если imageUrl == null
- Расчет стоимости при выборе дат
- Создание контракта при нажатии "Арендовать"
- Проверка авторизации перед арендой
```

### 5. Контракты

**fragment_contracts.xml:**
```xml
<LinearLayout vertical>
    <!-- TabLayout: Активные / История -->
    <TabLayout tabs: ["Активные", "Завершенные"] />
    
    <!-- ViewPager2 с двумя RecyclerView -->
    <ViewPager2>
        - ActiveContractsFragment
        - CompletedContractsFragment
    </ViewPager2>
</LinearLayout>
```

**item_contract.xml:**
```xml
<MaterialCardView>
    - Бренд, модель авто
    - Даты (начало - конец)
    - Общая стоимость
    - Статус (chip: PENDING/ACTIVE/COMPLETED/CANCELLED)
    - Кнопка "Подробнее"
</MaterialCardView>
```

**ContractsFragment.kt:**
```kotlin
- Загрузка контрактов пользователя
- Фильтрация по статусу (активные/завершенные)
- Пагинация
- Переход к деталям контракта
```

**fragment_contract_detail.xml:**
```xml
<ScrollView>
    <!-- Информация об авто -->
    <MaterialCardView carInfo />
    
    <!-- Информация о контракте -->
    <MaterialCardView contractInfo>
        - Даты аренды
        - Стоимость
        - Статус
    </MaterialCardView>
    
    <!-- Действия -->
    <LinearLayout buttons>
        - btnEdit (если PENDING)
        - btnCancel (если PENDING/ACTIVE)
    </LinearLayout>
</ScrollView>
```

### 6. Документы

**fragment_documents.xml:**
```xml
<androidx.constraintlayout.widget.ConstraintLayout>
    <!-- Если документа нет -->
    <LinearLayout emptyState>
        <TextView "Добавьте документ для аренды" />
        <Button btnAddDocument />
    </LinearLayout>
    
    <!-- Если документ есть -->
    <MaterialCardView documentCard>
        - Тип документа
        - Серия и номер
        - Дата выдачи
        - Кем выдан
        - Статус верификации (chip)
        
        <LinearLayout buttons>
            - btnEdit
            - btnDelete
        </LinearLayout>
    </MaterialCardView>
</androidx.constraintlayout.widget.ConstraintLayout>
```

**DocumentsFragment.kt:**
```kotlin
- Загрузка документа через ApiService.getDocument()
- Создание документа (dialog)
- Редактирование документа
- Удаление документа
- Показ статуса верификации
```

### 7. Профиль пользователя

**fragment_profile.xml:**
```xml
<ScrollView>
    <!-- Аватар/иконка пользователя -->
    <ImageView avatar />
    
    <!-- Карточка личных данных -->
    <MaterialCardView personalInfo>
        - Имя, Фамилия
        - Email (+ badge верификации)
        - Телефон
        - Кнопка "Редактировать"
    </MaterialCardView>
    
    <!-- Действия -->
    <MaterialCardView actions>
        - ListItem "Мои документы" →
        - ListItem "Изменить пароль" →
        - ListItem "Подтвердить email" (если !verified)
    </MaterialCardView>
    
    <!-- Опасная зона -->
    <MaterialCardView dangerZone>
        - Button "Выйти" (outlined)
        - Button "Удалить аккаунт" (text, red)
    </MaterialCardView>
</ScrollView>
```

**ProfileFragment.kt:**
```kotlin
- Загрузка профиля пользователя
- Редактирование данных (dialog)
- Изменение пароля (dialog)
- Верификация email
- Выход (clearTokens + navigate to login)
- Удаление аккаунта (подтверждение dialog)
```

### 8. Админ-панель

**fragment_admin.xml:**
```xml
<LinearLayout vertical>
    <!-- TabLayout для разделов -->
    <TabLayout>
        - Автомобили
        - Модели
        - Контракты
        - Клиенты
        - Документы
        - Аналитика (пустая)
    </TabLayout>
    
    <!-- ViewPager2 для фрагментов -->
    <ViewPager2 />
</LinearLayout>
```

**AdminCarsFragment:**
```kotlin
- Список всех авто (как каталог, но с админ-фильтрами)
- Кнопка FAB "Добавить авто"
- При клике на авто -> AdminCarDetailFragment
  - Форма редактирования авто
  - Изменение статуса
  - Загрузка/изменение фото
  - Удаление авто
```

**AdminModelsFragment:**
```kotlin
- Список моделей с фильтрами
- FAB "Создать модель"
- Форма создания:
  - Выбор существующего бренда или создание нового
  - Выбор существующего имени модели или создание нового
  - Выбор класса или создание нового
  - Выбор типа кузова
- Редактирование модели
- Удаление модели
```

**AdminContractsFragment:**
```kotlin
- Список всех контрактов
- Фильтры: статус, пользователь, авто, бренд
- Действия:
  - Подтвердить контракт (если PENDING)
  - Отменить контракт
  - Подтвердить отмену (если запрошена)
```

**AdminClientsFragment:**
```kotlin
- Список пользователей
- Фильтры: роль, заблокирован
- Действия:
  - Просмотр профиля пользователя
  - Изменение роли
  - Забанить/Разбанить
```

**AdminDocumentsFragment:**
```kotlin
- Список документов
- Фильтр: только непроверенные
- Просмотр документа
- Кнопка "Подтвердить" для непроверенных
```

### 9. Избранное

**fragment_favorites.xml:**
```xml
<androidx.constraintlayout.widget.ConstraintLayout>
    <!-- RecyclerView с избранными авто -->
    <RecyclerView rvFavorites />
    
    <!-- Empty state -->
    <LinearLayout emptyState>
        <ImageView ic_favorite_empty />
        <TextView "Нет избранных автомобилей" />
        <Button "Перейти в каталог" />
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

**FavoritesFragment.kt:**
```kotlin
- Загрузка избранных через ApiService.getFavorites()
- Пагинация
- Использование того же CarAdapter
- Переход к деталям авто
- Удаление из избранного (сердечко)
```

### 10. Создание авто (админ)

**dialog_create_car.xml / fragment_create_car.xml:**
```xml
<ScrollView>
    <!-- Выбор/создание модели -->
    <MaterialCardView modelSelection>
        <AutoCompleteTextView brandSpinner />
        <AutoCompleteTextView modelSpinner />
        <AutoCompleteTextView bodyTypeSpinner />
        <AutoCompleteTextView classSpinner />
        
        <Checkbox "Создать новую модель" />
    </MaterialCardView>
    
    <!-- Параметры авто -->
    <MaterialCardView carParams>
        <TextInputEditText yearOfIssue />
        <TextInputEditText gosNumber />
        <TextInputEditText vin />
        <TextInputEditText rent />
        <Spinner carState />
    </MaterialCardView>
    
    <!-- Фото -->
    <MaterialCardView photo>
        <ImageView selectedImage />
        <Button "Выбрать фото" />
    </MaterialCardView>
    
    <Button "Создать автомобиль" />
</ScrollView>
```

## 🎨 Стилистика приложения:

### Цветовая схема:
```kotlin
- Primary: Градиент #667eea → #764ba2
- Secondary: #f093fb → #f5576c
- Background: Светлые оттенки (#FAFAFA, #FFFFFF)
- Cards: Закругленные углы 16dp, elevation 4dp
- Buttons: Закругленные 12dp
- Icons: Material Icons, 24dp
```

### Анимации:
```kotlin
- Smooth transitions между экранами
- Ripple effect на кликабельных элементах
- Fade in/out для загрузки
- Slide up для Bottom Sheets
```

### UX улучшения:
```kotlin
- Skeleton loaders во время загрузки
- Swipe to refresh везде где есть списки
- Pull to load more для пагинации
- Error states с retry кнопкой
- Empty states с helpful text
- Снекбары для уведомлений
- Диалоги подтверждения для опасных действий
```

## 🔧 Утилиты и хелперы:

### DateUtils.kt:
```kotlin
object DateUtils {
    fun formatDate(dateString: String): String
    fun formatDateRange(start: String, end: String): String
    fun calculateDays(start: String, end: String): Int
}
```

### PriceUtils.kt:
```kotlin
object PriceUtils {
    fun formatPrice(price: Double): String  // "2500 ₽"
    fun calculateTotal(daily: Double, days: Int): Double
}
```

### ValidationUtils.kt:
```kotlin
object ValidationUtils {
    fun isValidEmail(email: String): Boolean
    fun isValidPassword(password: String): Boolean
    fun isValidLogin(login: String): Boolean
}
```

### ImageLoader.kt:
```kotlin
object ImageLoader {
    fun loadCarImage(
        imageView: ImageView,
        url: String?,
        placeholder: Int = R.drawable.ic_car
    ) {
        imageView.load(url) {
            placeholder(placeholder)
            error(placeholder)
            crossfade(true)
        }
    }
}
```

## 📱 Реализация пагинации:

```kotlin
// В ViewModel
private var currentPage = 0
private var isLastPage = false
private var isLoading = false

fun loadMore() {
    if (isLoading || isLastPage) return
    
    isLoading = true
    currentPage++
    
    viewModelScope.launch {
        val response = apiService.getCatalogue(page = currentPage)
        if (response.isSuccessful) {
            val data = response.body()
            isLastPage = data?.content?.size ?: 0 < 20
            // Добавить к существующему списку
        }
        isLoading = false
    }
}

// В Fragment/RecyclerView
recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (!recyclerView.canScrollVertically(1)) {
            viewModel.loadMore()
        }
    }
})
```

## 🔐 Обработка авторизации:

```kotlin
// В MainActivity
private fun setupNavigation() {
    navController.addOnDestinationChangedListener { _, destination, _ ->
        when (destination.id) {
            R.id.loginFragment, R.id.registerFragment -> {
                // Скрыть Toolbar и BottomNav
                toolbar.visibility = View.GONE
                bottomNav.visibility = View.GONE
            }
            else -> {
                // Показать
                toolbar.visibility = View.VISIBLE
                bottomNav.visibility = View.VISIBLE
                
                // Скрыть Contracts и Favorites если не авторизован
                if (!TokenManager.getInstance(this).isLoggedIn()) {
                    bottomNav.menu.findItem(R.id.contractsFragment).isVisible = false
                    bottomNav.menu.findItem(R.id.favoritesFragment).isVisible = false
                }
            }
        }
    }
}
```

## 📊 Состояния UI:

```kotlin
sealed class UIState<out T> {
    object Idle : UIState<Nothing>()
    object Loading : UIState<Nothing>()
    data class Success<T>(val data: T) : UIState<T>()
    data class Error(val message: String) : UIState<Nothing>()
}
```

## 🚀 Следующие шаги:

1. ✅ Создать структуру пакетов
2. ✅ Создать модели данных
3. ✅ Создать API интерфейс
4. ✅ Создать базовые drawable и menu
5. ⏳ Реализовать TokenManager и RetrofitClient
6. ⏳ Создать все Fragment классы
7. ⏳ Создать все ViewModel классы
8. ⏳ Создать все Adapter классы
9. ⏳ Создать все Layout файлы
10. ⏳ Добавить обработку ошибок
11. ⏳ Добавить анимации и transitions
12. ⏳ Тестирование и отладка
