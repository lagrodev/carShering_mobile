w# Как сделать «по феншую»: UI + состояние при повороте (Android/Kotlin)

Этот файл — практичная шпаргалка, как правильно **настроить UI и сохранение состояния** в проекте CarSharing, чтобы:

- фильтры **не сбрасывались** при повороте,
- bottom sheet с фильтрами **не терял введённые значения**,
- список/поиск/скролл в каталоге вели себя предсказуемо,
- всё было на современных паттернах (ViewModel + StateFlow + SavedStateHandle).

## 1) База: что именно «сбрасывается» при повороте

При повороте экрана происходит **recreate Activity/Fragment** (если не переопределять конфигурации). Поэтому:

- поля ввода в обычных `EditText` могут сохраняться системой **только если** у view есть `android:id` и включено стандартное сохранение состояния;
- состояние в памяти (поля класса `var currentFilters`) **теряется**;
- `ViewModel` переживает поворот, а `SavedStateHandle` дополнительно переживает **process death**.

Вывод: всё важное состояние держим в `ViewModel`, а то, что должно жить даже после убийства процесса — в `SavedStateHandle`.

## 2) У тебя уже почти правильно: `CatalogueViewModel` и `SavedStateHandle`

В `CatalogueViewModel` фильтры уже сохраняются через `SavedStateHandle`:

- в `applyFilters()` ты делаешь `savedStateHandle[KEY_FILTERS] = newFilters`;
- при создании ViewModel берёшь `savedStateHandle.get<CatalogueFilters>(KEY_FILTERS)`.

Это означает: **применённые фильтры** не должны сбрасываться при повороте.

Если у тебя «сбрасываются» фильтры, чаще всего причина одна из этих:

1) в UI при пересоздании фрагмента ты **не восстанавливаешь** значения контролов (chips/slider/edittext) из `viewModel.filters`;
2) ты сохраняешь только `CatalogueFilters`, но в bottom sheet пользователь меняет значения, **не нажимая “Применить”** — эти «черновики» нигде не живут;
3) у `BottomSheetDialogFragment` пересоздание происходит, а текущие введённые значения не восстановлены.

## 3) «Правильный» вариант: два состояния — Applied и Draft

Чтобы UX был топовым:

- `AppliedFilters` (применённые) живут в `CatalogueViewModel` + `SavedStateHandle`.
- `DraftFilters` (то, что пользователь крутит в bottom sheet прямо сейчас) живут в отдельном `ViewModel` самого bottom sheet, тоже через `SavedStateHandle`.

Идея: пользователь крутит ползунки → черновик сохраняется автоматически; нажал «Применить» → черновик копируется в `CatalogueViewModel`.

### 3.1 Draft ViewModel для bottom sheet (пример)

Создай `FiltersDraftViewModel`:

```kotlin
class FiltersDraftViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _draft = MutableStateFlow(
        savedStateHandle.get<CarFilters>("draft_filters") ?: CarFilters()
    )
    val draft: StateFlow<CarFilters> = _draft.asStateFlow()

    fun setDraft(newValue: CarFilters) {
        _draft.value = newValue
        savedStateHandle["draft_filters"] = newValue
    }

    fun reset(minYear: Int = 2000, maxYear: Int = 2025, minPrice: Int = 500, maxPrice: Int = 20000) {
        setDraft(
            CarFilters(
                minYear = minYear,
                maxYear = maxYear,
                minCell = minPrice,
                maxCell = maxPrice
            )
        )
    }
}
```

В `FiltersBottomSheet`:

- получать draft через `by viewModels()` (не `activityViewModels()`), чтобы draft был независим:

```kotlin
private val draftViewModel: FiltersDraftViewModel by viewModels()
```

- при каждом изменении (slider/edittext/chip) обновлять `draftViewModel.setDraft(...)`.
- при `onViewCreated` сначала подцепить `draftViewModel.draft.collect { renderDraft(it) }`.

Плюс: при повороте bottom sheet восстановит всё, даже если пользователь ещё не нажал «Применить».

### 3.2 Applied фильтры в каталоге (как у тебя)

Когда пользователь жмёт «Показать результаты»:

```kotlin
binding.btnApply.setOnClickListener {
    val draft = collectFilters() // или draftViewModel.draft.value
    viewModel.applyCarFilters(draft) // сохранит в SavedStateHandle
    dismiss()
}
```

## 4) Минимальный вариант без нового ViewModel (быстро, но хуже UX)

Если не хочешь добавлять `FiltersDraftViewModel`, тогда хотя бы сохрани ввод в `onSaveInstanceState` bottom sheet:

```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelable("filters", collectFilters())
}

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val restored = savedInstanceState?.getParcelable<CarFilters>("filters")
        ?: arguments?.getParcelable(ARG_FILTERS)

    currentFilters = restored
    applyCurrentFilters()
}
```

Это сохранит текущее состояние bottom sheet при повороте, даже если пользователь не нажал Apply.

## 5) Важный момент в твоём `FiltersBottomSheet`: chips создаются асинхронно

У тебя `chipGroupClass` и `chipGroupBodyType` заполняются из `Flow` (`carClasses.collect`, `bodyTypes.collect`).

Поэтому восстановление выбранного chip лучше делать **после** того как ты добавил chips.
Это уже сделано у тебя (ты проверяешь `currentFilters?.carClass` и `currentFilters?.bodyType`).

Если всё равно «прыгает», правило простое:

- источник истины — `draft/applied state`;
- UI — это только рендер.

## 6) Каталог: поиск и фильтры, чтобы не ломались

Лучший паттерн:

- любые изменения `searchQuery` тоже сохранять в `SavedStateHandle` (у тебя так и есть);
- список машин (`uiState`) при повороте может перезагрузиться — это ок, но UX лучше, если:
  - при повороте не сбрасывается `filters/searchQuery`;
  - скролл списка сохраняется.

### 6.1 Сохранение позиции RecyclerView (пример)

В `CatalogueFragment`:

```kotlin
private var listState: Parcelable? = null

override fun onPause() {
    super.onPause()
    listState = binding.recyclerView.layoutManager?.onSaveInstanceState()
}

private fun restoreListState() {
    listState?.let { state ->
        binding.recyclerView.layoutManager?.onRestoreInstanceState(state)
    }
}
```

Вызывать `restoreListState()` после того как адаптер получил данные.

## 7) Что в ресурсах уже сделано для удобного UX

- Login/Register переделаны под scroll-friendly layout — на маленьких экранах и при повороте ничего не «уплывает».
- Фоны/цвета унифицированы, а dark theme исправлен.

## 8) Чек-лист «по феншую»

- Держи **одно** состояние фильтров в `CatalogueViewModel` (applied) + `SavedStateHandle`.
- Для bottom sheet либо:
  - делай отдельный draft ViewModel с `SavedStateHandle` (лучше), либо
  - сохраняй `collectFilters()` в `onSaveInstanceState`.
- Не хранить важное в `var` внутри Fragment (оно умирает при recreate).
- Рендерить UI только из state (Flow/LiveData), а не наоборот.

Если хочешь — я сделаю второй шаг «по феншую» прямо в коде Kotlin (ViewModel draft + restore UI), но ты сказал что сейчас правим в основном `res`, поэтому тут только инструкция.
