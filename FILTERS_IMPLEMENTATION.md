# Реализация фильтров в приложении Car Sharing

## Что было сделано

### 1. Создан FiltersBottomSheet.kt
**Путь:** `app/src/main/java/com/example/carcatalogue/ui/catalogue/FiltersBottomSheet.kt`

Функциональность:
- ✅ Полноценный Bottom Sheet Dialog с фильтрами
- ✅ 7 типов фильтров:
  - **Бренд** (AutoCompleteTextView с предзагруженными вариантами)
  - **Модель** (AutoCompleteTextView)
  - **Тип кузова** (SEDAN, SUV, HATCHBACK, COUPE, WAGON, PICKUP, VAN)
  - **Класс автомобиля** (ECONOMY, COMFORT, BUSINESS, PREMIUM, LUXURY)
  - **Год выпуска** (RangeSlider: 2000-2025)
  - **Цена** (RangeSlider: 500-20000 ₽)
  - **Даты аренды** (DatePicker для начальной и конечной даты)
- ✅ Кнопка "Применить фильтры" - применяет выбранные фильтры и закрывает диалог
- ✅ Кнопка "Сбросить" - сбрасывает все фильтры к значениям по умолчанию
- ✅ Сохранение текущих фильтров при открытии диалога
- ✅ Callback для передачи фильтров обратно во фрагмент

### 2. Обновлен CatalogueViewModel.kt

Добавлены новые классы и методы:

```kotlin
@Parcelize
data class CarFilters(
    val brand: String? = null,
    val model: String? = null,
    val bodyType: String? = null,
    val carClass: String? = null,
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val minCell: Int? = null,
    val maxCell: Int? = null,
    val dateStart: Date? = null,
    val dateEnd: Date? = null
) : Parcelable
```

Методы:
- `applyCarFilters(carFilters: CarFilters)` - конвертирует CarFilters в CatalogueFilters и применяет
- `getCurrentCarFilters(): CarFilters` - получает текущие фильтры в формате CarFilters
- `formatDate(date: Date): String` - форматирует Date в строку для API
- `parseDate(dateString: String): Date?` - парсит строку даты в Date

### 3. Обновлен CatalogueFragment.kt

Изменения в методе `showFiltersBottomSheet()`:

```kotlin
private fun showFiltersBottomSheet() {
    val currentFilters = viewModel.getCurrentCarFilters()
    val bottomSheet = FiltersBottomSheet.newInstance(currentFilters) { filters ->
        viewModel.applyCarFilters(filters)
    }
    bottomSheet.show(parentFragmentManager, "filters")
}
```

Теперь вместо Toast показывается полноценный диалог с фильтрами.

### 4. Добавлен плагин kotlin-parcelize

Обновлен `app/build.gradle`:

```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'androidx.navigation.safeargs.kotlin'
    id 'kotlin-parcelize'  // <-- ДОБАВЛЕНО
}
```

Это позволяет использовать аннотацию `@Parcelize` для автоматической реализации Parcelable.

### 5. Исправлены ошибки компиляции в CarAdapter.kt

Проблемы:
- ❌ `binding.tvCarInfo` не существовал в `item_car_modern.xml`
- ❌ `car.imageUrl` не существовал в `CarListItemResponse`

Исправления:
- ✅ Используем `binding.tvYear` для года
- ✅ Используем `binding.tvCarClass` для класса
- ✅ Используем placeholder изображение (т.к. CarListItemResponse не содержит imageUrl)

### 6. Исправлены ошибки в FiltersBottomSheet.kt

Проблемы:
- ❌ `binding.tvYearRange` не существовал
- ❌ `binding.tvPriceRange` не существовал

Исправления:
- ✅ Используем `binding.tvYearMin` и `binding.tvYearMax`
- ✅ Используем `binding.tvPriceMin` и `binding.tvPriceMax`

## Как работают фильтры

1. Пользователь нажимает кнопку "Фильтры" в каталоге
2. Открывается `FiltersBottomSheet` с текущими фильтрами
3. Пользователь выбирает нужные параметры
4. Нажимает "Применить фильтры"
5. Фильтры передаются в `CatalogueViewModel` через callback
6. ViewModel конвертирует `CarFilters` → `CatalogueFilters`
7. Вызывается `loadCars()` с новыми параметрами
8. API запрос отправляется с фильтрами
9. Результаты отображаются в RecyclerView

## Современный дизайн

Приложение использует:
- ✅ `fragment_catalogue_modern.xml` - современный каталог с градиентами
- ✅ `item_car_modern.xml` - современные карточки автомобилей
- ✅ `bottom_sheet_filters.xml` - красивый диалог фильтров
- ✅ Material Design 3 компоненты
- ✅ Градиентные фоны (#667eea→#764ba2, #f093fb→#f5576c)
- ✅ Скругленные углы (24dp для карточек)
- ✅ FAB для избранного
- ✅ Chips для активных фильтров

## Состояние приложения

✅ Приложение успешно собирается
✅ APK установлен на устройство
✅ Фильтры полностью функциональны
✅ Современный дизайн применен
✅ SavedStateHandle сохраняет фильтры при повороте экрана

## Тестирование

Для тестирования фильтров:
1. Запустите приложение
2. Перейдите в каталог автомобилей
3. Нажмите кнопку "Фильтры"
4. Выберите нужные параметры
5. Нажмите "Применить фильтры"
6. Проверьте, что результаты фильтруются
7. Поверните экран - фильтры должны сохраниться

## API эндпоинт

```kotlin
@GET("car")
suspend fun getCatalogue(
    @Query("brand") brand: String?,
    @Query("model") model: String?,
    @Query("minYear") minYear: Int?,
    @Query("maxYear") maxYear: Int?,
    @Query("bodyType") bodyType: String?,
    @Query("carClass") carClass: String?,
    @Query("dateStart") dateStart: String?,
    @Query("dateEnd") dateEnd: String?,
    @Query("minCell") minCell: Int?,
    @Query("maxCell") maxCell: Int?,
    @Query("page") page: Int = 0,
    @Query("size") size: Int = 20,
    @Query("sort") sort: String = "id,asc"
): Response<PagedModel<CarListItemResponse>>
```

Все параметры фильтров корректно передаются в этот эндпоинт.
