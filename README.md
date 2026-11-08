# زاگرس PDA

اپلیکیشن اندروید ویزیتور میدانی، ساخته‌شده با Kotlin و Jetpack Compose.

این اپ برای پخش (PDA) و گوشی ویزیتورهای فروش طراحی شده است: مسیر روز، مشتریان، ثبت بازدید با
اعتبارسنجی موقعیت، کاتالوگ و سبد خرید، ثبت سفارش، فاکتورها با چاپ حرارتی بلوتوث، گزارش‌ها و
پیام‌های مدیریتی — و مهم‌تر از همه، **کار کردن کامل بدون اینترنت** و همگام‌سازی خودکار با سرور.

مستندات قرارداد API و الزامات بک‌اند در همین مخزن نگه‌داری می‌شوند (بخش [مستندات](#مستندات)).

---

## قابلیت‌ها

| قابلیت | توضیح |
|---|---|
| **مسیر من** | لیست مشتریان تخصیص‌یافته‌ی روز، وضعیت بازدید، دلیل رد شدن بازدید |
| **مشتریان** | جست‌وجو و صفحه‌بندی، جزئیات، ایجاد مشتری جدید، ویرایش اطلاعات تماس و موقعیت |
| **ثبت بازدید** | اعتبارسنجی شعاع ژئوفنس، رد موقعیت جعلی (mock)، ثبت موقعیت دقیق با `freshFix` |
| **کاتالوگ و سبد** | جست‌وجوی بارکد، تصاویر محصول، عرضه‌های دسته‌بندی، بسته‌بندی چندواحدی، تخفیف، موجودی |
| **سفارش** | ثبت سفارش آفلاین در صف، اعتبارسنجی سبد، تاریخچه و جزئیات سفارش |
| **فاکتور و چاپ** | فاکتورهای تأییدشده، پیش‌نمایش و چاپ ESC/POS روی پرینتر حرارتی بلوتوث (۵۸ و ۸۰ میلی‌متر) |
| **گزارش‌ها** | گزارش عملکرد ویزیتور (فروش، بازدید، اهداف) |
| **پیام‌ها** | دریافت زنده با SSE + اعلان FCM data-only، علامت‌گذاری خوانده‌شده |
| **ردیابی موقعیت** | Foreground Service با پروفایل تطبیقی پیش/پس‌زمینه و ارسال دسته‌ای |
| **مجوزهای خارج از محدوده** | درخواست و پیگیری تأیید بازدید خارج از ژئوفنس |
| **همگام‌سازی آفلاین** | صف محلی در Room + WorkManager (سینک و دلتا-سینک دوره‌ای) |
| **راه‌اندازی پویا** | انتخاب آدرس سرور شرکت در اولین اجرا، تست ارتباط، ورود |

---

## پشته‌ی فنی

| لایه | فناوری |
|---|---|
| زبان | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2026.02.01)، Material 3، Navigation Compose، Paging 3 |
| معماری | MVVM + StateFlow، لایه‌بندی data/domain/ui، Hilt 2.60.1 |
| محلی | Room 2.8.5، DataStore Preferences |
| شبکه | Retrofit 2.11، OkHttp 4.12 (+SSE)، kotlinx.serialization، Kotlinx Coroutines |
| پس‌زمینه | WorkManager 2.11، Foreground Service، Firebase Messaging |
| موقعیت | Google Play Services Location 21.4 |
| تصاویر | Coil 2.7 |
| ساخت | AGP 9.3.3، Gradle 9.5، KSP، JDK toolchain 25 |

`minSdk = 28`، `targetSdk = compileSdk = 37`، `applicationId = ir.zagroserp.pda`.

---

## معماری

```
ui/         صفحه‌ها و ViewModelها (Compose) — داشبورد، مسیر، مشتری، کاتالوگ، سفارش، فاکتور، گزارش، پیام، تنظیمات
   ↓
domain/     مدل‌ها، قواعد کسب‌وکار (Geofence، OrderMath، SystemAlerts)، اینترفیس ریپازیتوری‌ها
   ↓
data/       ریپازیتوری‌ها، Room، Retrofit، Interceptorها، DataStore
   ↑
work/       WorkManager: OfflineSyncWorker، DeltaSyncWorker، SyncScheduler
location/   LocationTrackingService، LocationProvider، LocationUploadPolicy، BootReceiver
push/       ZagrosMessagingService، PushTokenManager، PushNotificationHelper
print/      تولید فرمان ESC/POS و رستر
```

اصول پیاده‌شده:

- **آفلاین-فرست**: هر عملیات نوشتنی ابتدا در Room ذخیره می‌شود و سپس با صف و `WorkManager` به سرور می‌رود.
- **آدرس سرور در زمان اجرا**: `BaseUrlInterceptor` هر درخواست را روی آدرسی که ویزیتور در
  `ServerSetupScreen` تنظیم کرده بازنویسی می‌کند؛ `BuildConfig.BASE_URL` فقط مقدار پیش‌فرض است.
- **تازه‌سازی توکن**: `AuthInterceptor` + `TokenAuthenticator` با مسیر `auth/refresh`.
- **بدون پروکسی باتری**: پروفایل موقعیت با `ProcessLifecycleOwner` بین حالت اپ باز و بسته سوییچ می‌شود.

---

## شروع به کار

### پیش‌نیازها

- Android Studio با پشتیبانی AGP 9
- **JDK 25** — در `gradle/gradle-daemon-jvm.properties` تعریف شده و Gradle آن را از
  [foojay](https://api.foojay.io) خودکار دریافت می‌کند
- Android SDK برای `compileSdk 37`
- فایل `app/google-services.json` (پیکربندی Firebase) در ریشه‌ی ماژول `app`

مسیر SDK را در `local.properties` قرار دهید (این فایل در مخزن نیست):

```properties
sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
```

### دستورات

```bash
./gradlew assembleDebug          # ساخت APK دیباگ
./gradlew installDebug           # نصب روی دستگاه/امولاتور متصل
./gradlew testDebugUnitTest      # تست‌های واحد
./gradlew lint                   # لینت
./gradlew bundleRelease          # بسته‌ی انتشار برای Play (نیازمند امضای release)
```

> `debug` و `release` همان `applicationId` را به اشتراک می‌گذارند، چون `google-services.json` فقط
> برای همین پکیج ثبت شده است.

### اجرای اول روی دستگاه

1. اپ را نصب و باز کنید.
2. در صفحه‌ی دسترسی‌ها همه‌ی مجوزها را بدهید — مخصوصاً **موقعیت در پس‌زمینه** (اندروید ۱۱+ اجازه نمی‌دهد
   هم‌زمان با مجوز پیش‌زمینه درخواست شود؛ اپ این را در دو مرحله می‌پرسد).
3. آدرس سرور شرکت را وارد کنید → تست ارتباط → ورود.
4. در **تنظیمات**، اپ را از بهینه‌سازی باتری معاف کنید (روی برخی ROMهای صنعتی هم «Autostart» را فعال کنید).

---

## مجوزهای اندروید

| مجوز | کاربرد |
|---|---|
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | ثبت بازدید و موقعیت مشتری |
| `ACCESS_BACKGROUND_LOCATION` | ردیابی وقتی اپ بسته است |
| `FOREGROUND_SERVICE_LOCATION` | سرویس ردیابی |
| `POST_NOTIFICATIONS` | اعلان ردیابی و پیام‌ها (اندروید ۱۳+) |
| `RECEIVE_BOOT_COMPLETED` | بازگرداندن ردیابی پس از ریبوت |
| `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` | چاپ روی پرینتر حرارتی |

FCM بدون Google Play Services کار نمی‌کند؛ اگر GMS نباشد، دلیل خطا در کارت «اعلان فوری» در تنظیمات
نمایش داده می‌شود.

---

## تست‌ها

۱۳ فایل تست، **۱۰۹ تست واحد** با JUnit4، MockK و Turbine:

```bash
./gradlew testDebugUnitTest
```

پوشش اصلی: قواعد ژئوفنس و موقعیت جعلی، محاسبات سبد و مبالغ، سیاست دسته‌بندی ارسال موقعیت،
سیاست تلاش مجدد صف، تاریخ جلالی/میلادی، مپرها، آلارم‌های سیستمی و فرمت‌کننده‌ها.

---

## مستندات

| فایل | موضوع |
|---|---|
| [`141-mobile-api-android.md`](141-mobile-api-android.md) | مرجع کامل قرارداد API موبایل (endpointها، DTOها، خطاها، صفحه‌بندی) |
| [`backend-requirements.md`](backend-requirements.md) | الزامات و باگ‌های سمت بک‌اند که روی اپ اثر می‌گذارند |
| [`backend-fixes-report.md`](backend-fixes-report.md) | گزارش رفع و راستی‌آزمایی موارد بک‌اند |
| [`android-implementation.md`](android-implementation.md) | جزئیات پیاده‌سازی ردیابی موقعیت و اعلان FCM |
| [`spec-customer-edit.md`](spec-customer-edit.md) | مشخصات ویرایش اطلاعات تماس و موقعیت مشتری |
