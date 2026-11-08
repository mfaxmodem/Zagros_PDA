# مشخصات پیاده‌سازی — ویرایش اطلاعات تماس و موقعیت مشتری

> **نسخه:** ۱.۰ — **تاریخ:** ۲۰۲۶-۰۹-۲۲
> **اپ:** `zagros-pda` (Kotlin / Jetpack Compose / Clean Architecture + MVVM)
> **هدف سند:** مشخصات کامل و بدون ابهام برای پیاده‌سازی توسط یک مدل/توسعه‌دهنده
> **وضعیت:** آماده‌ی پیاده‌سازی — قرارداد API روی سرور زنده راستی‌آزمایی شده است

---

## ۱. هدف

در تب **مشتریان**، وقتی ویزیتور روی یک مشتری کلیک می‌کند، باید بتواند اطلاعات **تماس و موقعیت مکانی** او را ویرایش کند و تغییرات را روی سرور ذخیره کند.

این قابلیت‌ها باید پوشش داده شوند:

1. ویرایش نام، تلفن، آدرس، شهر، استان
2. ثبت/به‌روزرسانی مختصات جغرافیایی مشتری از موقعیت فعلی ویزیتور
3. ذخیره روی `PUT /mobile/visitor/customers/:id`
4. کار کردن **هم** به‌عنوان ویرایش کامل و **هم** به‌عنوان «فقط ثبت مختصات»

---

## ۲. وضعیت فعلی کد (نقطه‌ی شروع)

| فایل | وضعیت |
|---|---|
| `ui/screens/customers/CustomerDetailScreen.kt` | صفحه‌ی جزئیات مشتری. کارت «تماس و موقعیت» دارد که فقط **نمایش** می‌دهد |
| `ui/screens/customers/CustomerDetailViewModel.kt` | متد `saveCoordinates()` دارد که فقط مختصات را می‌فرستد |
| `ui/screens/customers/CustomerFormScreen.kt` | فرم **ایجاد** مشتری جدید — الگوی UI را از این بردار |
| `domain/repository/Repositories.kt` → `CustomerRepository.updateCustomer(...)` | موجود و آماده |
| `data/repository/CustomerRepositoryImpl.kt` → `updateCustomer(...)` | پیاده‌سازی شده و امن |
| `data/remote/dto/CustomerDto.kt` → `UpdateCustomerRequest` | موجود |

**نتیجه:** لایه‌ی داده کامل است. کار اصلی **UI + ViewModel** است.

---

## ۳. قرارداد API (راستی‌آزمایی‌شده روی سرور زنده)

### `PUT /api/v1/mobile/visitor/customers/:id`

**نکته‌ی حیاتی:** این Endpoint حالا یک **PATCH واقعی** است. فقط فیلدهایی که در بدنه می‌فرستید تغییر می‌کنند؛ فیلدهای غایب دست‌نخورده می‌مانند.

> **تاریخچه:** قبلاً جایگزینی کامل بود و ارسال فقط `latitude`/`longitude` تلفن و آدرس مشتری را پاک می‌کرد. این باگ در بک‌اند رفع شده و راستی‌آزمایی شده است.

**بدنه (همه اختیاری):**

```json
{
  "name": "فروشگاه آزمون",
  "phone": "09120000000",
  "address": "اصفهان، خیابان...",
  "city": "اصفهان",
  "province": "اصفهان",
  "latitude": 32.6546,
  "longitude": 51.6680
}
```

**فیلدهای غیرقابل ویرایش** (سرور آن‌ها را از رکورد موجود کپی می‌کند و از بدنه نمی‌خواند):
`national_id` · `credit_limit` · `settlement_days` · `balance`

**پاسخ `200`:** `MobileCustomer` کامل، شامل `lat` و `lng`:

```json
{
  "id": "2b88438c-2203-4b45-824a-315952cfe616",
  "name": "hgg", "phone": "09364208524", "address": "nvhh",
  "national_id": "", "balance": 0, "credit_limit": 0,
  "lat": 32.6546, "lng": 51.668
}
```

**خطاها:**

| کد | معنا |
|---|---|
| `400` | مشتری در دسترس این ویزیتور نیست |
| `401` | توکن نامعتبر |
| `404` | مشتری یافت نشد |

### ⚠️ نکته‌ی مهم درباره‌ی خواندن مختصات

`GET /customers/:id` **مختصات را برمی‌گرداند** (`lat`/`lng`؛ مقدار `0` یعنی «ثبت‌نشده»). پس اپ می‌تواند تشخیص دهد مشتری مختصات دارد یا نه.

---

## ۴. طراحی UI

### ۴.۱ نقطه‌ی ورود

در `CustomerDetailScreen`، کارت «تماس و موقعیت» یک دکمه‌ی **«ویرایش اطلاعات»** بگیرد که به صفحه‌ی ویرایش می‌رود.

دکمه‌ی فعلی «ثبت مختصات فعلی روی مشتری» **حذف** شود و به داخل صفحه‌ی ویرایش منتقل شود (تا ویرایش در یک جا متمرکز باشد).

### ۴.۲ صفحه‌ی جدید: `CustomerEditScreen`

الگوی UI را دقیقاً از `CustomerFormScreen` بردار (همان `ScreenScaffold`، همان `ZagrosCard`، همان `ZagrosTextField`، همان دکمه‌ی پایین‌چسب).

**ساختار:**

```
ScreenScaffold(
    title = "ویرایش مشتری",
    subtitle = نام مشتری,
    onBack = ...,
    bottomBar = دکمه‌ی «ذخیره تغییرات» (تمام‌عرض، ارتفاع ۵۲dp)
) {
    LazyColumn {
        // کارت ۱ — اطلاعات تماس
        ZagrosCard {
            SectionHeader("اطلاعات تماس")
            ZagrosTextField(نام *, الزامی، ۲ تا ۱۲۰ کاراکتر)
            ZagrosTextField(تلفن، عددی)
            ZagrosTextField(آدرس، چندخطی، maxLines = 3)
            Row {
                ZagrosTextField(شهر, weight 1f)
                ZagrosTextField(استان, weight 1f)
            }
        }

        // کارت ۲ — موقعیت مکانی
        ZagrosCard {
            SectionHeader("موقعیت مکانی")

            // نمایش وضعیت
            StatusPill(
                text = if (دارد) "ثبت‌شده: lat, lng" else "ثبت نشده",
                tone = if (دارد) SUCCESS else WARNING
            )

            // دکمه‌ی ثبت موقعیت فعلی
            OutlinedButton("ثبت موقعیت فعلی من")  → viewModel.captureLocation()

            // اگر مختصات دارد: نمایش فاصله
            if (دارد) {
                OutlinedButton("اندازه‌گیری فاصله تا مشتری")  → viewModel.measureDistance()
                StatusPill(فاصله + داخل/خارج شعاع ۵۰ متر)
            }

            Text(توضیح: «ثبت مختصات باعث می‌شود از این پس ثبت بازدید فقط از شعاع ۵۰ متری ممکن باشد»)
        }

        // پیام‌ها
        if (error != null) StatusPill(error, DANGER)
        if (message != null) StatusPill(message, SUCCESS)
    }
}
```

### ۴.۳ رفتار دکمه‌ها

| دکمه | رفتار |
|---|---|
| **ثبت موقعیت فعلی من** | `LocationProvider.freshFix()` می‌گیرد، `lat`/`lng` را در state می‌ریزد و یک پیش‌نمایش نشان می‌دهد. **ذخیره نمی‌کند** — فقط state را پر می‌کند |
| **اندازه‌گیری فاصله تا مشتری** | فاصله‌ی Haversine بین موقعیت فعلی و مختصات ذخیره‌شده‌ی مشتری را حساب می‌کند و می‌گوید داخل/خارج شعاع ۵۰ متر است |
| **ذخیره تغییرات** | `PUT` می‌زند و در موفقیت `onBack()` می‌کند |

---

## ۵. تغییرات فنی

### ۵.۱ `Routes`

```kotlin
// ui/navigation/Destinations.kt
const val CUSTOMER_EDIT = "customer/{customerId}/edit"
fun customerEdit(id: String) = "customer/$id/edit"
```

در `AppNavHost` ثبت شود:

```kotlin
composable(
    route = Routes.CUSTOMER_EDIT,
    arguments = listOf(navArgument(Routes.ARG_CUSTOMER_ID) { type = NavType.StringType }),
) {
    CustomerEditScreen(onBack = { navController.popBackStack() })
}
```

> ⚠️ `CustomerEditViewModel` باید `savedStateHandle.get<String>(Routes.ARG_CUSTOMER_ID)` را بخواند. همان الگوی `CustomerDetailViewModel`.

### ۵.۲ `CustomerEditViewModel` (جدید)

```kotlin
data class CustomerEditUiState(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val province: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isLocating: Boolean = false,
    val isMeasuring: Boolean = false,
    val distanceMeters: Double? = null,
    val nameError: String? = null,
    val message: String? = null,
    val error: String? = null,
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null &&
        (latitude != 0.0 || longitude != 0.0)

    val canSave: Boolean get() = !isSaving && name.trim().length >= 2
}
```

**وابستگی‌های تزریقی:**
- `SavedStateHandle` (برای `customerId`)
- `CustomerRepository`
- `LocationProvider`

**متدها:**

| متد | کار |
|---|---|
| `init` | مشتری را از `customerRepository.getCustomer(id)` بخواند و فیلدها را پر کند. اگر در کش نبود، `refreshCustomers()` بعد دوباره بخواند |
| `onNameChange` / `onPhoneChange` / … | به‌روزرسانی state + پاک کردن خطا |
| `captureLocation()` | `locationProvider.freshFix()`. اگر `null` → خطا. اگر `isMock` → خطای «موقعیت جعلی». وگرنه `latitude`/`longitude` را ست کند |
| `measureDistance()` | فاصله را حساب کند (فقط اگر مختصات مشتری موجود باشد) |
| `save()` | اعتبارسنجی → `customerRepository.updateCustomer(...)` → موفق: `message` + برگشت |

**نمونه‌ی `save()`:**

```kotlin
fun save(onSaved: () -> Unit) {
    val s = _state.value
    if (s.name.trim().length < 2) {
        _state.update { it.copy(nameError = "نام باید حداقل ۲ کاراکتر باشد") }
        return
    }
    _state.update { it.copy(isSaving = true, error = null, message = null) }

    viewModelScope.launch {
        when (val result = customerRepository.updateCustomer(
            id = customerId,
            name = s.name.trim(),
            phone = s.phone.trim(),
            address = s.address.trim(),
            city = s.city.trim(),
            province = s.province.trim(),
            latitude = s.latitude,
            longitude = s.longitude,
        )) {
            is AppResult.Success -> {
                _state.update { it.copy(isSaving = false, message = "تغییرات ذخیره شد") }
                onSaved()
            }
            is AppResult.Failure -> {
                _state.update { it.copy(isSaving = false, error = result.error.message) }
            }
        }
    }
}
```

### ۵.۳ `CustomerDetailScreen` — تغییرات

1. دکمه‌ی «ویرایش اطلاعات» به کارت «تماس و موقعیت» اضافه شود.
2. دکمه‌ی «ثبت مختصات فعلی روی مشتری» و کارت زرد «مختصات این مشتری ثبت نشده» **حذف** شوند (به صفحه‌ی ویرایش منتقل شدند).
3. `saveCoordinates()` از `CustomerDetailViewModel` **حذف** شود.
4. `onEdit: () -> Unit` به امضای کامپوزبل اضافه شود.

### ۵.۴ چیزهایی که **نباید** تغییر کنند

- ❌ `CustomerRepository` و `CustomerRepositoryImpl` — آماده و امن هستند
- ❌ `UpdateCustomerRequest` — درست است
- ❌ `VisitorApi.updateCustomer` — درست است
- ❌ `CustomerFormScreen` (ایجاد مشتری) — دست نزن، فقط الگو بردار

---

## ۶. موارد لبه (Edge cases)

| حالت | رفتار لازم |
|---|---|
| **آفلاین** | `PUT` به شبکه نیاز دارد. خطای «ارتباط با سرور برقرار نشد» را از `AppError.Network` نشان بده. **صف آفلاین در این نسخه پیاده نشود** (خارج از دامنه) |
| **موقعیت جعلی** | `captureLocation()` باید `isMock` را رد کند: «موقعیت جعلی شناسایی شد و ثبت نشد» |
| **موقعیت در دسترس نیست** | «موقعیت مکانی در دسترس نیست. دسترسی GPS را بررسی کنید» |
| **دقت پایین** | اگر `accuracy > 100` متر بود، هشدار بده ولی جلوی ثبت را نگیر (سرور خودش در ثبت بازدید چک می‌کند) |
| **مشتری بدون مختصات** | حالت عادی. دکمه‌ی «اندازه‌گیری فاصله» نمایش داده نشود |
| **نام خالی** | ذخیره نشود، `nameError` نشان داده شود |
| **مشتری در کش نیست** | `getCustomer()` نال برگرداند → ابتدا `refreshCustomers()` و سپس دوباره بخوان. اگر باز هم نبود: خطای «اطلاعات مشتری در دسترس نیست» |
| **مختصات `0,0`** | یعنی «ثبت‌نشده» — نه یک موقعیت واقعی. `hasCoordinates` باید `0.0` را نادیده بگیرد |
| **چرخش صفحه / بازگشت از پس‌زمینه** | state باید در ViewModel بماند؛ از `remember` در کامپوزبل استفاده نکن |
| **دو بار زدن ذخیره** | `canSave` باید `isSaving` را چک کند |

---

## ۷. تست‌های لازم

### تست واحد (اجباری)

منطق خالص را جدا کن تا تست‌پذیر باشد. اگر اعتبارسنجی یا محاسبه‌ی فاصله در ViewModel ماند، تست واحد سخت می‌شود.

پیشنهاد: یک تابع خالص اضافه کن، مثلاً در `core/CustomerEditRules.kt`:

```kotlin
object CustomerEditRules {
    const val MIN_NAME_LENGTH = 2
    const val MAX_NAME_LENGTH = 120

    fun validateName(name: String): String?  // null = معتبر
    fun hasCoordinates(lat: Double?, lng: Double?): Boolean
    fun withinRadius(distanceMeters: Double): Boolean
}
```

و این‌ها را تست کن (الگوی موجود: `app/src/test/java/ir/zagroserp/pda/`):

- نام خالی / یک‌حرفی → خطا
- نام ۱۲۱ کاراکتری → خطا
- نام ۲ کاراکتری → معتبر
- `hasCoordinates(null, null)` → false
- `hasCoordinates(0.0, 0.0)` → false
- `hasCoordinates(32.65, 51.66)` → true
- مرز شعاع ۵۰ متر

> از `Geofence.distanceMeters(...)` که موجود است استفاده کن — تست‌هایش در `GeofenceTest` هست.

---

## ۸. معیار پذیرش

- [ ] در تب مشتریان → کلیک روی مشتری → دکمه‌ی «ویرایش اطلاعات» دیده می‌شود
- [ ] صفحه‌ی ویرایش، فیلدها را با **مقادیر فعلی مشتری** پر می‌کند
- [ ] ویرایش تلفن و ذخیره → روی سرور اعمال می‌شود (با `GET /customers/:id` قابل بررسی)
- [ ] ویرایش آدرس و ذخیره → **تلفن پاک نمی‌شود** (رگرسیون باگ قبلی)
- [ ] «ثبت موقعیت فعلی من» → مختصات را پر می‌کند و در پیش‌نمایش نشان می‌دهد
- [ ] ذخیره‌ی مختصات → `lat`/`lng` روی سرور ثبت می‌شود
- [ ] بعد از ذخیره، کارت «موقعیت مکانی» در صفحه‌ی جزئیات مختصات جدید را نشان می‌دهد
- [ ] در حالت آفلاین، خطای واضح نشان داده می‌شود و اپ کرش نمی‌کند
- [ ] موقعیت جعلی رد می‌شود
- [ ] مشتری بدون مختصات، همچنان بدون محدودیت قابل بازدید است
- [ ] تست‌های واحد پاس می‌شوند
- [ ] `assembleDebug` و `assembleRelease` بدون هشدار

---

## ۹. دستورات ساخت و تست

```bash
# از ریشه‌ی پروژه
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"

.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:testDebugUnitTest
```

> **نکته:** `JAVA_HOME` باید روی JBR اندروید استودیو تنظیم شود. بدون آن، Gradle پیدا نمی‌شود.

---

## ۱۰. زمینه‌ی معماری (برای درک سریع)

```
ui/screens/customers/     ← اینجا کار می‌کنی (Compose + ViewModel)
        ↓
domain/repository/        ← CustomerRepository (رابط) — آماده
        ↓
data/repository/          ← CustomerRepositoryImpl — آماده
        ↓
data/remote/api/          ← VisitorApi.updateCustomer — آماده
```

**قواعد پروژه که باید رعایت شوند:**

1. **بدون کامنت اضافه** — کد باید خودش گویا باشد
2. **تمام UI فارسی و RTL** است (`LocalLayoutDirection.Rtl` سراسری)
3. **فونت پیش‌فرض `IranSansFaNum`** است — ارقام خودکار فارسی می‌شوند. برای متن‌هایی که باید لاتین بمانند (URL، پورت، نسخه) `fontFamily = IranSans` بده
4. **اعداد از `formatMoney` / `formatQty` / `formatCount`** در `ui/util/Formatters.kt` استفاده کنند
5. **هرگز `try/catch` خالی نگذار** — خطاها باید به `AppResult.Failure` تبدیل شوند
6. **رنگ‌ها از `LocalZagrosAccents`** و **فاصله‌ها از `ZagrosSpacing`** بیایند
7. **بدون کامنت فارسی در کد** — توضیحات فقط در سند

---

## ۱۱. منابع

| موضوع | فایل |
|---|---|
| الگوی فرم | `ui/screens/customers/CustomerFormScreen.kt` |
| الگوی ViewModel با `SavedStateHandle` | `ui/screens/customers/CustomerDetailViewModel.kt` |
| محاسبه‌ی فاصله | `core/Geofence.kt` |
| گرفتن موقعیت | `location/LocationProvider.kt` |
| فرمت اعداد | `ui/util/Formatters.kt` |
| قرارداد API | `141-mobile-api-android.md` بخش ۴.۲۶ |
| گزارش رفع بک‌اند | `backend-fixes-report.md` بخش ۳ |
