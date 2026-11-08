# API موبایل ویزیتور — مرجع کامل برای طراحی و پیاده‌سازی اپ اندروید

> **نسخه:** ۳ (افزودن عرضه‌های دسته‌بندی و بسته‌بندی چندواحدی به کاتالوگ موبایل)
> **تاریخ تولید:** ۲۰۲۶-۰۹-۱۹ — **مبنای کد:** کامیت `main` پس از migration `000220`
> **منابع (Source of Truth):**
> - مسیرها: `cmd/api/main.go` (گروه `mobile` + `/mobile/visitor/health` عمومی + `auth/login`)
> - هندلر: `internal/infrastructure/web/handlers/visitor_mobile_handler.go`، `visit_zone_permission_handler.go`، `auth_handler.go`
> - سرویس/مدل‌ها: `internal/core/service/visitor_mobile_service.go`، `visitor_mobile_extras.go`، `internal/core/domain/*`، `internal/infrastructure/web/dto/*`
>
> این سند برای تیمی است که اپ اندروید را **خارج از این ریپازیتوری** طراحی و پیاده‌سازی می‌کند.
> هرجا شک بود، فایل‌های بالا مرجع نهایی‌اند.
>
> **تغییرات نسخه ۲:** Endpointهای سابقه/جزئیات سفارش، فهرست/جزئیات/ویرایش مشتری، ثبت توکن Push، جست‌وجوی بارکد، تأیید چاپ فاکتور، health/نسخه؛ صفحه‌بندی همه‌ی لیست‌ها؛ غنی‌سازی `/sync` (مشتری/تور/تخفیف/موجودی + tombstone)؛ `printed_at`، `is_usable`، `expires_in`؛ قرارداد ساختاریافته‌ی مشتری تکراری.
>
> **تغییرات نسخه ۳:** افزودن `categories` (عرضه‌های دسته‌بندی با قیمت مستقل) و `uoms` (بسته‌بندی چندواحدی) به پاسخ محصول در `products`/`products-promo`/`products/lookup`/`sync`؛ دو Endpoint جدید `GET /mobile/visitor/products/:id/categories` و `GET /mobile/visitor/products/:id/uoms` (بخش‌های ۴.۳۳ و ۴.۳۴).

---

## ۱) مشخصات پایه

| مورد | مقدار |
|---|---|
| **Base URL** | `https://<domain>/api/v1` — تولید: `https://dalbara.ir/api/v1` — توسعه: `http://localhost:8080/api/v1` |
| **پروتکل** | HTTPS (اجباری در تولید) |
| **فرمت بدنه** | JSON، کلیدها **snake_case** |
| **Content-Type** | `application/json; charset=utf-8` |
| **احراز هویت** | `Authorization: Bearer <JWT>` در همه‌ی مسیرها به‌جز `/auth/login` و `/auth/refresh` |
| **فرمت تاریخ** | `YYYY-MM-DD` (میلادی) در ورودی/خروجی؛ زمان‌ها RFC3339 (ISO-8601) |
| **فرمت مبلغ/عدد** | عدد اعشاری JSON (Decimal) |
| **کدگذاری متن** | UTF-8 (فارسی) |
| **نقش‌های مجاز گروه موبایل** | `visitor`، `admin`، `accountant`، `warehouse` (بقیه ۴۰۳ می‌گیرند) |

### ۱.۱ قالب خطا
اکثر خطاها با ساختار یکسان برمی‌گردند:

```json
{ "error": "پیام فارسی قابل نمایش" }
```

برخی خطاها فیلد اضافه دارند:

```json
{ "error": "...", "requires_company": true, "companies": [ { "id": "...", "name": "..." } ] }
{ "error": "...", "retry_after": 900 }
{ "requires_2fa": true, "message": "..." }
```

**مشتری تکراری (ساختاریافته — قرارداد جدید):** اگر `national_id` مشتری موجود باشد، پاسخ `409` است و شناسه‌ی مشتری موجود را هم برمی‌گرداند تا اپ مشتری آفلاین را به رکورد سروری map کند:

```json
{
  "error": "مشتری‌ای با این شناسه ملی از قبل ثبت شده است",
  "duplicate": true,
  "existing_customer_id": "uuid"
}
```

> قالب خطا چندشکلی است: همیشه ابتدا `error` را بخوانید، سپس فیلدهای اختیاری (`requires_company`، `companies`، `retry_after`، `requires_2fa`، `duplicate`، `existing_customer_id`) را با کلیدهای محافظت‌شده parse کنید.

### ۱.۳ صفحه‌بندی و هدرها

همه‌ی لیست‌های موبایل پارامترهای `?page=` و `?page_size=` را می‌پذیرند (پیش‌فرض `page=1`, `page_size=50`, حداکثر `200`). لیست‌هایی که بدنه‌ی آرایه‌ای دارند (کالا، بازدید، پیام، فاکتور) شکل بدنه را حفظ می‌کنند و متادیتای صفحه‌بندی را در هدرها می‌دهند:

| هدر | معنا |
|---|---|
| `X-Page` | شماره‌ی صفحه‌ی جاری |
| `X-Page-Size` | اندازه‌ی صفحه |
| `X-Total-Count` | تعداد کل آیتم‌های قبل از صفحه‌بندی |

Endpointهای جدید (سفارش‌ها، مشتری‌ها) متادیتای صفحه‌بندی را در **بدنه** برمی‌گردانند: `{ ..., "page": 1, "page_size": 50, "has_more": false }`.

### ۱.۲ کدهای وضعیت متداول

| کد | معنا |
|---|---|
| `200 OK` | موفق (شامل replay ایدمپوتنت) |
| `201 Created` | رکورد جدید ساخته شد |
| `400 Bad Request` | اعتبارسنجی بدنه/کوئری ناموفق (یا انتخاب شرکت لازم) |
| `401 Unauthorized` | توکن نامعتبر/منقضی، یا نیاز به کد 2FA |
| `403 Forbidden` | حساب ویزیتور نیست / شرکت یا کاربر غیرفعال |
| `404 Not Found` | منبع در محدوده‌ی شرکت کاربر نیست |
| `409 Conflict` | تضاد وضعیت (گذار نامعتبر) |
| `429 Too Many Requests` | محدودیت نرخ یا قفل حساب (`retry_after`) |
| `500 Internal Server Error` | خطای سرور |

> **محدودیت نرخ:** گروه موبایل `RateLimitUser` دارد (به‌ازای کاربر). `auth/login` و `auth/refresh` هم `RateLimitIP` دارند.

---

## ۲) جریان احراز هویت (برای اپ اندروید)

### ۲.۱ ورود — `POST /auth/login`
- مسیر: `POST /api/v1/auth/login` (بدون توکن)
- بدنه:

```json
{
  "username": "visitor1",
  "password": "secret123",
  "otp": "123456",
  "device_id": "android-imei-or-uuid",
  "company_id": "uuid-optional"
}
```

| فیلد | الزامی | توضیح |
|---|---|---|
| `username` | ✅ | نام کاربری یا ایمیل (۳ تا ۶۰ کاراکتر) |
| `password` | ✅ | حداقل ۸ کاراکتر |
| `otp` | ❌ | کد TOTP؛ وقتی حساب 2FA دارد الزامی می‌شود |
| `device_id` | ❌ | شناسه‌ی دستگاه برای مدیریت نشست (حداکثر ۱۲۸) |
| `company_id` | ❌ | اگر یک نام کاربری در چند شرکت باشد، انتخاب را قطعی می‌کند |

- پاسخ موفق `200`:

```json
{
  "token": "<JWT>",
  "type": "Bearer",
  "refresh_token": "<refresh>",
  "full_name": "نام نمایشی",
  "avatar_url": "https://...",
  "expires_in": 86400
}
```

> `expires_in` عمر **توکن دسترسی** بر حسب ثانیه است (قرارداد جدید). اپ باید پیش از انقضا (مثلاً در ۸۰٪ عمر) `auth/refresh` را صدا بزند. مقدار دقیق به پیکربندی سرور بستگی دارد و ممکن است تغییر کند؛ همیشه از همین فیلد بخوانید، نه یک عدد ثابت.

- پاسخ‌های خاص:

| حالت | کد | بدنه |
|---|---|---|
| 2FA لازم/اشتباه | `401` | `{ "requires_2fa": true, "message": "..." }` |
| نام کاربری در چند شرکت | `400` | `{ "error": "...", "requires_company": true, "companies": [...] }` |
| اعتبارنامه اشتباه | `401` | `{ "error": "نام کاربری یا رمز عبور اشتباه است" }` |
| کاربر/شرکت غیرفعال | `403` | `{ "error": "..." }` |
| حساب قفل‌شده | `429` | `{ "error": "...", "retry_after": 900 }` |

> اپ اندروید باید حالت `requires_company` را با نمایش لیست شرکت‌ها و ارسال دوباره با `company_id` مدیریت کند.

### ۲.۲ تمدید توکن — `POST /auth/refresh`
- بدنه: `{ "refresh_token": "<refresh>" }`
- پاسخ `200`: `{ "refresh_token": "<new>", "token": "<new JWT>", "expires_in": 2592000, "message": "..." }`
  - نکته: در `refresh` مقدار `expires_in` عمر **refresh token** است (~۳۰ روز)؛ عمر توکن دسترسی جدید در پاسخ `login` (و در `exp` خود JWT) است.
  - توکن دسترسی تازه هم در بدنه (`token`) و هم در هدر `X-New-Token` برمی‌گردد.
- `401`: رفرش‌توکن نامعتبر/منقضی.

### ۲.۳ خروج — `POST /auth/logout`
- با توکن؛ نشست (jti) را باطل و توکن دسترسی را تا انقضا بلک‌لیست می‌کند. پاسخ `{ "message": "logged out" }`.
- **رفتار refresh token:** این Endpoint توکن دسترسی را باطل می‌کند؛ اپ مسئول دور انداختن `refresh_token` محلی است. برای امنیت بیشتر، پس از logout توکن Push دستگاه را با `DELETE /mobile/visitor/devices` حذف کنید.

### ۲.۴ نکته‌ی SSE
تنها برای مسیرهای streaming، اگر هدر `Authorization` ممکن نیست (EventSource در اندروید/وب‌ویو)، می‌توان توکن را به‌صورت کوئری داد: `?token=<JWT>`. این حالت **فقط** روی مسیرهای stream پذیرفته می‌شود.

---

## ۳) فهرست کامل Endpointهای موبایل

| # | Method | Path | کاربرد | بدنه/کوئری |
|---|---|---|---|---|
| ۱ | GET | `/mobile/visitor/health` | سلامت/نسخه (بدون توکن) | — |
| ۲ | GET | `/mobile/visitor/today` | داشبورد امروز (تورها + مشتریان + بازدیدشده‌ها) | `?date=YYYY-MM-DD` |
| ۳ | GET | `/mobile/visitor/profile` | پروفایل ویزیتور (نام/نام کاربری) | — |
| ۴ | GET | `/mobile/visitor/discounts` | تخفیف‌های فعال | `?page=&page_size=` |
| ۵ | GET | `/mobile/visitor/products` | لیست کالاها + موجودی زنده | `?page=&page_size=` |
| ۶ | GET | `/mobile/visitor/products/lookup` | جست‌وجوی دقیق بارکد | `?barcode=` |
| ۷ | GET | `/mobile/products/stock` | اسنپ‌شات موجودی کالاها | `?page=&page_size=` |
| ۸ | GET | `/mobile/visitor/products-promo` | کالاها با اطلاعات تخفیف | `?page=&page_size=` |
| ۹ | GET | `/mobile/visitor/customers` | فهرست مشتریان تخصیصی (جست‌وجو + صفحه‌بندی) | `?query=&page=&page_size=` |
| ۱۰ | GET | `/mobile/visitor/customers/:id` | جزئیات یک مشتری | — |
| ۱۱ | POST | `/mobile/visitor/customers` | ایجاد مشتری جدید | JSON |
| ۱۲ | PUT | `/mobile/visitor/customers/:id` | ویرایش تماس/آدرس/موقعیت مشتری | JSON |
| ۱۳ | POST | `/mobile/visitor/devices` | ثبت/به‌روزرسانی توکن Push | JSON |
| ۱۴ | DELETE | `/mobile/visitor/devices` | حذف توکن Push | `?device_token=` یا JSON |
| ۱۵ | POST | `/mobile/visitor/visits` | ثبت بازدید | JSON |
| ۱۶ | GET | `/mobile/visitor/visits` | لیست بازدیدها | `?from=&to=&page=&page_size=` |
| ۱۷ | POST | `/mobile/visitor/visits/batch` | ثبت دسته‌ای بازدید (آفلاین) | JSON (۱ تا ۱۰۰) |
| ۱۸ | GET | `/mobile/visitor/orders` | سابقه‌ی سفارش‌ها | `?from=&to=&status=&page=&page_size=` |
| ۱۹ | GET | `/mobile/visitor/orders/:id` | جزئیات سفارش | — |
| ۲۰ | POST | `/mobile/visitor/orders` | ثبت سفارش | JSON |
| ۲۱ | POST | `/mobile/visitor/orders/batch` | ثبت دسته‌ای سفارش (آفلاین) | JSON (۱ تا ۵۰) |
| ۲۲ | GET | `/mobile/visitor/goals` | اهداف و پورسانت | — |
| ۲۳ | GET | `/mobile/visitor/reports` | گزارش عملکرد | `?from=&to=` |
| ۲۴ | GET | `/mobile/visitor/messages` | پیام‌ها/اعلان‌ها | `?page=&page_size=` |
| ۲۵ | GET | `/mobile/visitor/messages/stream` | جریان زنده‌ی پیام (SSE) | `?token=` |
| ۲۶ | GET | `/mobile/visitor/sync` | دلتا سینک | `?since=<RFC3339>` |
| ۲۷ | GET | `/mobile/visitor/approved-invoices` | فاکتورهای تأییدشده (چاپ) | `?page=&page_size=` |
| ۲۸ | POST | `/mobile/visitor/invoices/:id/printed` | تأیید چاپ فاکتور | — |
| ۲۹ | POST | `/mobile/visitor/zone-permissions` | ثبت درخواست مجوز خارج از محدوده | JSON |
| ۳۰ | GET | `/mobile/visitor/zone-permissions` | درخواست‌های خودِ ویزیتور (با `is_usable`) | — |
| ۳۱ | PUT | `/mobile/visitor/zone-permissions/:id/cancel` | لغو درخواست | — |
| ۳۲ | GET | `/mobile/visitor/products/:id/categories` | عرضه‌های دسته‌بندی کالا (هر عرضه قیمت مستقل) | — |
| ۳۳ | GET | `/mobile/visitor/products/:id/uoms` | بسته‌بندی‌های چندواحدی کالا (ضریب + قیمت) | — |
| ۳۴ | POST | `/mobile/visitor/location` | ارسال موقعیت زنده (تکی) | JSON |
| ۳۵ | POST | `/mobile/visitor/locations/batch` | آپلود دسته‌ای موقعیت (صف آفلاین) | JSON (۱ تا ۵۰۰) |

> همه‌ی مسیرها نیازمند `Authorization: Bearer` هستند و ویزیتور از توکن استخراج می‌شود؛ بنابراین هر ویزیتور فقط داده‌ی خودش را می‌بیند. تنها استثنا `health` است که عمومی است.

---

## ۴) جزئیات هر Endpoint

### ۴.۱ `GET /mobile/visitor/today`
داشبورد روز. اگر `date` ندهید، امروز سرور.

- کوئری: `date` (اختیاری، `YYYY-MM-DD`)
- پاسخ `200` — `MobileToday`:

```json
{
  "date": "2026-09-19",
  "tours": [
    {
      "id": "uuid",
      "name": "تور هفتگی منطقه ۲",
      "customers": [ { "id": "uuid", "name": "...", "phone": "...", "address": "...", "national_id": "...", "balance": 0, "credit_limit": 0, "lat": 0, "lng": 0 } ]
    }
  ],
  "customers": [ { "...": "لیست تخت و بدون تکرار همه‌ی مشتریان امروز (تخصیص مستقیم + تورها)" } ],
  "visited": [ { "id": "uuid", "visitor_id": "uuid", "customer_id": "uuid", "visit_date": "...", "visit_type": "ROUTE", "status": "VISITED", "notes": "", "latitude": 0, "longitude": 0, "order_id": "uuid", "client_visit_id": "", "created_at": "...", "updated_at": "..." } ]
}
```

> **برای UI:** اگر می‌خواهید «مسیرهای من» را جدا نشان دهید از `tours` استفاده کنید؛ برای لیست تخت از `customers`. `visited` بازدیدهای همان روز است.

### ۴.۲ `GET /mobile/visitor/profile`
- پاسخ `200`:

```json
{ "visitor_id": "uuid", "user_id": "uuid", "name": "نام", "username": "visitor1" }
```

### ۴.۳ `GET /mobile/visitor/discounts`
- پاسخ `200`: `{ "discounts": [ MobileDiscount, ... ] }`

`MobileDiscount`:
```json
{
  "id": "uuid", "title": "تخفیف پاییزه", "type": "percent",
  "description": "...", "value": 10, "value_type": "percent",
  "valid_from": "2026-09-01", "valid_to": "2026-09-30",
  "products": ["uuid"], "is_active": true
}
```
مقادیر `type`: `percent | amount | tiered | x_plus_y | bonus` — `value_type`: `percent | amount`.

### ۴.۴ `GET /mobile/visitor/products`
- پاسخ `200`: آرایه‌ی `ProductResponse` (فیلدهای کلیدی):

```json
{
  "id": "uuid", "company_id": "uuid", "code": "P001", "name": "کالا",
  "latin_name": "", "print_name": "", "barcode": "", "category": "...",
  "unit": "عدد", "buy_price": 0, "sell_price": 0, "warehouse_id": "uuid",
  "image_url": "", "weight_kg": 0, "volume_m3": 0, "min_stock": 0, "max_stock": 0,
  "sstid": "", "tax_mu": 0, "is_active": true,
  "created_at": "...", "updated_at": "...",
  "total_qty": 100, "reserved_qty": 5, "available_qty": 95, "stock_updated_at": "...",
  "categories": [ { "id": "uuid", "category": "باکس ۸ تایی", "sale_price": 800000, "is_default_sale": true } ],
  "uoms": [ { "id": "uuid", "uom_id": "uuid", "unit_name": "شل", "conversion_factor": 8, "unit_price": 800000, "barcode": "", "is_default_for_sale": true } ]
}
```

> تصویر محصول فقط در همین مسیر برمی‌گردد — جزئیات ساخت آدرس کامل در بخش ۴.۶.۱.
> `categories` و `uoms` (قرارداد جدید) فقط در مسیر موبایل پر می‌شوند؛ در Endpointهای وب کالا این دو فیلد `omitempty` و خالی‌اند. `category`/`unit` تکی همان مقدار پیش‌فرض/پایه‌ی کالا هستند.

### ۴.۵ `GET /mobile/products/stock`
- پاسخ `200`: آرایه‌ی اسنپ‌شات:

```json
[ { "product_id": "uuid", "total_qty": 100, "reserved_qty": 5, "available_qty": 95, "stock_updated_at": "..." } ]
```

### ۴.۶ `GET /mobile/visitor/products-promo`
- پاسخ `200`: آرایه‌ی `MobileProduct`:

```json
{
  "id": "uuid", "code": "P001", "name": "کالا", "category": "...", "unit": "عدد",
  "sell_price": 0, "has_promotion": true, "promo_percent": 10,
  "categories": [ { "id": "uuid", "category": "باکس ۸ تایی", "sale_price": 800000, "is_default_sale": true } ],
  "uoms": [ { "id": "uuid", "uom_id": "uuid", "unit_name": "شل", "conversion_factor": 8, "unit_price": 800000, "barcode": "", "is_default_for_sale": true } ]
}
```

> `MobileProduct` فیلد `image_url` ندارد — برای تصویر این کالاها از پاسخ `GET /mobile/visitor/products` (یا کش محلی آن) استفاده کنید.
> `categories` و `uoms` (قرارداد جدید) در `products-promo`، `products/lookup` و بخش `products` در `/sync` هم برمی‌گردند؛ برای کالای تک‌واحدی/بدون عرضه، آرایه‌ی خالی `[]`.

### ۴.۶.۱ تصویر محصول (`image_url`)
- **منبع:** فقط `GET /mobile/visitor/products` فیلد `image_url` را برمی‌گرداند (نوع `ProductResponse`). مقدار آن **مسیر نسبی** است، نه URL کامل:
  ```json
  { "image_url": "/uploads/products/P001.jpg" }
  ```
- **ساخت آدرس کامل:** باید با **origin** (اسکیم + هاست + پورت) ترکیب شود، **نه** با Base URL مستندات (`/api/v1`):
  - ✅ `https://dalbara.ir/uploads/products/P001.jpg`
  - ❌ `https://dalbara.ir/api/v1/uploads/products/P001.jpg`
  - نمونه‌ی Kotlin: `val base = BuildConfig.BASE_URL.substringBefore("/api/")` سپس `base.trimEnd('/') + imageUrl`
  - اگر `image_url` خالی بود، یعنی تصویری ثبت نشده — placeholder نمایش دهید.
- **دسترسی:** فایل‌ها استاتیک و **عمومی** (بدون توکن) سرو می‌شوند: `GET /uploads/products/<file>`.
- **نبود تصویر در بقیه‌ی مسیرها:** `products-promo`، `products/lookup?barcode=` و بخش `products` در `GET /mobile/visitor/sync` نوع `MobileProduct` را برمی‌گردانند که `image_url` ندارد. منبع واحد تصویر، همان `GET /mobile/visitor/products` است.
- **آپلود سمت ویزیتور وجود ندارد:** آپلود تصویر محصول فقط از پنل وب و برای نقش‌های `admin`/`accountant` است: `POST /api/v1/products/:id/image` با بدنه‌ی `multipart/form-data` و فیلد `image`؛ پاسخ `{ "image_url": "/uploads/products/<file>" }`. نقش `visitor` مجاز نیست و Endpoint موبایلی برای آپلود تصویر وجود ندارد.

### ۴.۷ `POST /mobile/visitor/customers`
- بدنه:

```json
{
  "name": "فروشگاه آزمون",
  "national_id": "1234567890",
  "phone": "09120000000",
  "address": "...",
  "credit_limit": 0,
  "settlement_days": 0,
  "latitude": 30.28,
  "longitude": 51.28
}
```

| فیلد | الزامی |
|---|---|
| `name` | ✅ (۲ تا ۱۲۰) |
| `national_id` | ✅ |
| `phone`, `address`, `credit_limit`, `settlement_days` | ❌ |
| `latitude` (-90..90)، `longitude` (-180..180) | ❌ |

- پاسخ `201`: `CustomerResponse` (ساختار کامل مشتری؛ فیلدهای کلیدی: `id`, `code`, `name`, `national_id`, `phone`, `address`, `credit_limit`, `settlement_days`, `active`, ...).
- **قاعده‌ی جغرافیایی:** اگر ویزیتور تور فعالِ دارای محدوده داشته باشد، ایجاد مشتری **خارج از محدوده** رد می‌شود (پاسخ خطا). مختصات را بفرستید.

### ۴.۸ `POST /mobile/visitor/visits`
- بدنه:

```json
{
  "customer_id": "uuid",
  "visit_type": "ROUTE",
  "status": "VISITED",
  "notes": "توضیح",
  "latitude": 30.28,
  "longitude": 51.28,
  "order_id": "uuid",
  "client_visit_id": "offline-key-001"
}
```

- `visit_type`: `ROUTE | EXTRA` (اختیاری) — `status`: `PLANNED | VISITED | NO_ANSWER | CLOSED` (الزامی)
- `client_visit_id`: کلید ایدمپوتنسی آفلاین (حداکثر ۶۴) — برای جلوگیری از ثبت تکراری
- پاسخ `201`: `domain.Visit` (ساختار در ۴.۱). **replay ایدمپوتنت** با همان کلید → `200` و رکورد موجود.
- **قواعد پذیرش:** مشتری باید یا به ویزیتور تخصیص یافته باشد، یا داخل محدوده‌ی تور فعالِ امروز باشد، یا مجوز معتبر «خارج از محدوده» داشته باشد؛ در غیر این صورت خطا (`ErrCustomerNotAssigned`).

### ۴.۹ `GET /mobile/visitor/visits`
- کوئری: `from`, `to` (`YYYY-MM-DD`؛ پیش‌فرض امروز). اگر `to < from` آنگاه `to = from`.
- پاسخ `200`: آرایه‌ی `domain.Visit`.

### ۴.۱۰ `POST /mobile/visitor/visits/batch`
- بدنه: `{ "visits": [ { ...همان فیلدهای ثبت بازدید... } ] }` — بین ۱ تا ۱۰۰ آیتم.
- پاسخ `200`:

```json
{
  "results": [ { "client_visit_id": "...", "status": "created|duplicate|error", "visit_id": "uuid", "message": "" } ],
  "summary": { "created": 0, "duplicates": 0, "errors": 0 }
}
```

### ۴.۱۱ `POST /mobile/visitor/orders`
- بدنه:

```json
{
  "fiscal_year_id": "uuid-or-label",
  "customer_id": "uuid",
  "date": "2026-09-19",
  "discount": 0,
  "note": "یادداشت",
  "client_order_id": "offline-order-001",
  "items": [
    { "product_id": "uuid", "quantity": 2, "unit_price": 100000, "discount": 0, "uom_id": "uuid-or-omit" }
  ]
}
```

- `date` الزامی (`YYYY-MM-DD`)؛ `items` حداقل ۱ آیتم؛ `quantity > 0`؛ `unit_price >= 0`.
- `uom_id` اختیاری: واحد بسته‌بندی همان خط (شل/کارتن) از `GET /mobile/visitor/products/:id/uoms`؛ خالی = واحد پایه. اگر `uom_id` بسته‌بندیِ تعریف‌نشده‌ی آن کالا باشد، سفارش با `400` رد می‌شود.
- `fiscal_year_id` می‌تواند UUID یا برچسب غیر-UUID باشد (مقدار غیر-UUID نادیده گرفته و سال مالی جاری شرکت استفاده می‌شود).
- پاسخ `201`: `OrderResponse`:

```json
{
  "id": "uuid", "company_id": "uuid", "fiscal_year_id": "uuid", "visitor_id": "uuid",
  "customer_id": "uuid", "number": "ORD-1001", "date": "2026-09-19",
  "sub_total": 0, "discount": 0, "tax_amount": 0, "final_amount": 0,
  "status": "PENDING", "rejection_reason": "", "note": "",
  "customer_name": "...", "visitor_name": "...", "soft_warning": "",
  "awaiting_supply": false, "shortage_qty": 0,
  "availability_warnings": [ { "product_id": "uuid", "product_name": "...", "requested": 0, "available": 0, "shortage": 0 } ],
  "items": [ { "id": "uuid", "order_id": "uuid", "product_id": "uuid", "batch_id": "uuid", "quantity": 2, "unit_price": 100000, "discount": 0, "total_line": 200000 } ],
  "created_at": "...", "updated_at": "..."
}
```

- **replay ایدمپوتنت** (`client_order_id` تکراری) → `200` با همان سفارش.

### ۴.۱۲ `POST /mobile/visitor/orders/batch`
- بدنه: `{ "orders": [ { ...همان سفارش... } ] }` — بین ۱ تا ۵۰.
- پاسخ `200`:

```json
{
  "results": [ { "client_order_id": "...", "status": "created|duplicate|blocked|error", "order_id": "uuid", "message": "" } ],
  "summary": { "created": 0, "duplicates": 0, "errors": 0, "blocked": 0 }
}
```

> `blocked` یعنی سفارش ساخته شد ولی نیاز به بازبینی پنل دارد (مثلاً سقف اعتبار/پورسانت) — این خطای صف آفلاین نیست.

### ۴.۱۳ `GET /mobile/visitor/goals`
- پاسخ `200`:

```json
{ "target_value": 0, "achieved_value": 0, "progress_percent": 0, "commission_rate": 0, "estimated_commission": 0, "sales_today": 0, "order_count": 0 }
```

### ۴.۱۴ `GET /mobile/visitor/reports`
- کوئری: `from`, `to` (`YYYY-MM-DD`؛ پیش‌فرض ۳۰ روز اخیر).
- پاسخ `200`:

```json
{ "from": "2026-08-20", "to": "2026-09-19", "order_count": 0, "total_sales": 0, "visit_count": 0, "assigned_customers": 0, "debt_total": 0 }
```

### ۴.۱۵ `GET /mobile/visitor/messages`
- پاسخ `200`: آرایه‌ی اعلان‌ها:

```json
[ { "id": "uuid", "type": "ROUTE_ASSIGNED", "title": "...", "body": "...", "is_read": false, "created_at": "..." } ]
```

### ۴.۱۶ `GET /mobile/visitor/messages/stream` (SSE)
- `Content-Type: text/event-stream`
- رویدادها:
  - `event: notification` — `data:` همان `NotificationResponse`
  - `event: ping` — `data: { "ts": <unix> }` هر ۲۵ ثانیه (heartbeat)
- احراز: هدر Bearer یا `?token=<JWT>`.

### ۴.۱۷ `GET /mobile/visitor/sync`
- کوئری: `since` (اختیاری، RFC3339). بدون آن، اسنپ‌شات کامل.
- پاسخ `200` — `MobileSyncDelta` (نسخه ۲ — غنی‌شده):

```json
{
  "server_time": "2026-09-19T10:00:00Z",
  "products": [ MobileProduct ],
  "product_stock": [ { "product_id": "uuid", "total_qty": 0, "reserved_qty": 0, "available_qty": 0, "stock_updated_at": "..." } ],
  "customers": [ MobileCustomer ],
  "tours": [ MobileTour ],
  "discounts": [ MobileDiscount ],
  "approved_invoices": [ MobileInvoice ],
  "visits": [ domain.Visit ],
  "notifications": [ Notification ],
  "deleted_ids": { "products": ["uuid"], "customers": [] }
}
```

**Semantics `since` (قرارداد):**
- `since` روی **`updated_at`** فیلتر می‌کند (نه `created_at`)، و بازه **انحصاری** است (`updated_at > since`).
- `server_time` را از پاسخ قبلی بردارید و همان را بفرستید؛ همیشه `UTC` و `RFC3339` است و یکنواخت (monotonic) به جلو می‌رود.
- بدون `since` (یا صفر) → اسنپ‌شات کامل.

**Tombstone (`deleted_ids`):** رکوردهای غیرفعال‌شده از زمان cursor. اپ باید این شناسه‌ها را از DB محلی حذف کند.
- `products`: کالاهایی که از زمان cursor غیرفعال شده‌اند (پشتیبانی کامل).
- `customers`: فعلاً همیشه خالی است (اسکیمای `deleted_at` برای مشتری هنوز اضافه نشده) — حذف سروری مشتری فعلاً منتقل نمی‌شود؛ در صورت نیاز، دوره‌ای `GET /mobile/visitor/customers` را برای تطبیق کامل بگیرید.

> `customers`، `tours` و `discounts` داده‌ی Master هستند و مستقل از cursor برگردانده می‌شوند (ممکن است در هر sync تکرار شوند). برای صرفه‌جویی، فقط وقتی آن‌ها را اعمال کنید که تغییر کرده‌اند.

### ۴.۱۸ `GET /mobile/visitor/approved-invoices`
- پاسخ `200`: `{ "invoices": [ MobileInvoice ] }`

- پاسخ `200`: `{ "invoices": [ MobileInvoice ], "page": 1, "page_size": 50, "has_more": false }`

`MobileInvoice` (برای چاپ بلوتوث روی تبلت):
```json
{
  "id": "uuid", "number": "INV-1001", "date": "2026-09-19",
  "customer_id": "uuid", "customer_name": "...", "customer_phone": "", "customer_address": "",
  "sub_total": 0, "discount": 0, "tax_amount": 0, "final_amount": 0, "note": "",
  "printed_at": "2026-09-19T10:05:00Z",
  "items": [ { "product_name": "...", "quantity": 2, "unit": "عدد", "unit_price": 0, "discount": 0, "total_line": 0 } ]
}
```

> `printed_at` (قرارداد جدید) زمان آخرین چاپ تأییدشده است؛ خالی = هنوز چاپ نشده. پس از چاپ موفق، `POST /mobile/visitor/invoices/:id/printed` را صدا بزنید تا وضعیت روی سرور ثبت شود (دریافت مجدد لیست، `printed_at` را نشان می‌دهد).

### ۴.۱۹ `POST /mobile/visitor/zone-permissions`
درخواست ویزیت مشتری خارج از محدوده‌ی تور.
- بدنه:

```json
{
  "customer_id": "uuid",
  "scope": "SAME_DAY",
  "target_date": "2026-09-19T00:00:00Z",
  "until": "2026-09-20T00:00:00Z",
  "reason": "مشتری خارج محدوده ولی نیاز فوری دارد",
  "notes": ""
}
```

| فیلد | الزامی | توضیح |
|---|---|---|
| `customer_id` | ✅ | UUID |
| `scope` | ✅ | `SINGLE_VISIT` (یک‌بار مصرف) · `SAME_DAY` (همان روز) · `DATE_RANGE` (بازه) |
| `target_date` | برای `SINGLE_VISIT` و `SAME_DAY` | زمان هدف |
| `until` | برای `DATE_RANGE` | پایان بازه (باید بعد از شروع) |
| `reason` | ✅ | ۳ تا ۵۰۰ کاراکتر |
| `notes` | ❌ | حداکثر ۵۰۰ |

- پاسخ `201`: `VisitZonePermission`:

```json
{
  "id": "uuid", "company_id": "uuid", "visitor_id": "uuid", "customer_id": "uuid",
  "tour_id": "uuid", "requested_by_user_id": "uuid", "scope": "SAME_DAY",
  "valid_from": "...", "valid_until": "...", "reason": "...", "notes": "",
  "status": "PENDING", "decided_by_user_id": "", "decided_at": null,
  "decision_note": "", "consumed_visit_id": null, "consumed_at": null,
  "created_at": "...", "updated_at": "..."
}
```

- `status`: `PENDING | APPROVED | REJECTED | EXPIRED | CANCELLED`.
- **مصرف:** مجوز `SINGLE_VISIT` پس از اولین ثبت بازدید موفق، اتمیک مصرف می‌شود.

### ۴.۲۰ `GET /mobile/visitor/zone-permissions`
- پاسخ `200`: آرایه‌ی `VisitZonePermission` + فیلد محاسباتی `is_usable` (قرارداد جدید) برای هر آیتم:
  - `is_usable = true` فقط اگر `status == "APPROVED"` باشد و (برای `SINGLE_VISIT` هنوز مصرف نشده باشد) و (`valid_until` نگذشته باشد).
  - UI می‌تواند مجوزهای مصرف‌شده/منقضی را غیرفعال نشان دهد.

### ۴.۲۱ `PUT /mobile/visitor/zone-permissions/:id/cancel`
- پاسخ `200`: `{ "ok": true }` (در صورت قابل لغو بودن).

### ۴.۲۲ `GET /mobile/visitor/health` (بدون توکن)
پیش از ورود یا در زمان راه‌اندازی اپ برای handshake نسخه:
```json
{ "status": "ok", "server_version": "1.0.0", "min_app_version": "1.0.0", "server_time": "2026-09-19T10:00:00Z" }
```
> اگر نسخه‌ی اپ کمتر از `min_app_version` بود، پیام «به‌روزرسانی لازم است» نشان دهید.

### ۴.۲۳ `GET /mobile/visitor/products/lookup?barcode=`
- پاسخ `200`: یک `MobileProduct` (همان ساختار ۴.۶) برای بارکد دقیقاً منطبق.
- `400`: اگر `barcode` خالی باشد. `404`: اگر کالایی با این بارکد نباشد.

### ۴.۲۴ `GET /mobile/visitor/customers`
- کوئری: `query` (جست‌وجو در نام/تلفن/شناسه ملی/آدرس)، `page`, `page_size`.
- دامنه: **همه‌ی مشتریان در دسترس ویزیتور** (تخصیص مستقیم + مشتریان همه‌ی تورهای ویزیتور، مستقل از «امروز»).
- پاسخ `200`:
```json
{
  "customers": [ MobileCustomer ],
  "page": 1, "page_size": 50, "has_more": false
}
```

### ۴.۲۵ `GET /mobile/visitor/customers/:id`
- پاسخ `200`: یک `MobileCustomer`. اگر مشتری در دسترس ویزیتور نباشد `400` (`ErrCustomerNotAssigned`).

### ۴.۲۶ `PUT /mobile/visitor/customers/:id`
- بدنه (فقط فیلدهای تماس/آدرس/موقعیت — فیلدهای اعتباری/هویتی قابل تغییر نیستند):
```json
{ "name": "...", "phone": "09120000000", "address": "...", "city": "...", "province": "...", "latitude": 30.28, "longitude": 51.28 }
```
- پاسخ `200`: `MobileCustomer` به‌روزشده. `400`: مشتری در دسترس ویزیتور نیست.

### ۴.۲۷ `POST /mobile/visitor/devices`
ثبت/به‌روزرسانی توکن Push (FCM) برای اعلان در پس‌زمینه.
- بدنه:
```json
{ "device_token": "fcm-token-...", "platform": "ANDROID", "device_id": "uuid-device", "app_version": "1.0.0" }
```
| فیلد | الزامی | توضیح |
|---|---|---|
| `device_token` | ✅ | ۸ تا ۵۱۲ کاراکتر |
| `platform` | ✅ | `ANDROID \| IOS \| OTHER` |
| `device_id` | ❌ | حداکثر ۱۲۸ |
| `app_version` | ❌ | حداکثر ۳۲ |

- پاسخ `201`: `{ "id": "uuid", "platform": "ANDROID", "device_token": "...", "device_id": "..." }`
- idempotent: ارسال دوباره‌ی همان `device_token` رکورد را به‌روز می‌کند (بدون تکرار).
- **محدوده:** این Endpoint فقط توکن را **ثبت** می‌کند؛ ارسال واقعی Push سمت سرور (FCM) خارج از این نسخه است و باید با تیم Backend هماهنگ شود.

### ۴.۲۸ `DELETE /mobile/visitor/devices`
- `?device_token=<token>` یا بدنه‌ی `{ "device_token": "..." }`.
- پاسخ `200`: `{ "ok": true }`. در logout/حذف اپ صدا بزنید.

### ۴.۲۹ `GET /mobile/visitor/orders`
سابقه‌ی سفارش‌های ویزیتور.
- کوئری: `from`, `to` (`YYYY-MM-DD`)، `status` (`PENDING|APPROVED|REJECTED|CANCELLED|BLOCKED|...`)، `page`, `page_size`.
- پاسخ `200`:
```json
{ "orders": [ OrderResponse ], "page": 1, "page_size": 50, "has_more": false }
```

### ۴.۳۰ `GET /mobile/visitor/orders/:id`
- پاسخ `200`: `OrderResponse` کامل (با `items`). سفارش ویزیتور دیگر → `404`.

### ۴.۳۱ `POST /mobile/visitor/invoices/:id/printed`
- بدون بدنه. پاسخ `200`: `{ "printed_at": "2026-09-19T10:05:00Z" }`.
- idempotent: چاپ دوباره فقط `printed_at` را به‌روز می‌کند.

### ۴.۳۲ معنای وضعیت‌های سفارش و نتیجه‌ی ساخت
| وضعیت/فیلد | معنا | واکنش پیشنهادی UI |
|---|---|---|
| `PENDING` | ثبت شد، در انتظار تأیید | «در انتظار تأیید» |
| `APPROVED` | تأیید شد (و احتمالاً به فاکتور تبدیل شده) | «تأییدشده» |
| `REJECTED` | رد شد؛ `rejection_reason` را نشان دهید | «ردشده» + دلیل |
| `CANCELLED` | لغو شد | «لغوشده» |
| `BLOCKED` | ساخته شد ولی نیاز به بازبینی پنل (سقف اعتبار/پورسانت) | «نیازمند بازبینی» (خطای صف آفلاین نیست) |
| `awaiting_supply` + `shortage_qty` | پیش‌فروش: بخشی از سفارش موجود نبود | «در انتظار تأمین — کسری: X» |
| `soft_warning` | تخفیف بیش از سقف مجاز (غیرمسدودکننده) | هشدار زرد |
| `availability_warnings[]` | کالاهایی که موجودی‌شان در لحظه‌ی ثبت کافی نبود | لیست هشدار |

### ۴.۳۳ `GET /mobile/visitor/products/:id/categories`
عرضه‌های دسته‌بندی یک کالا؛ یک کالا می‌تواند همزمان در چند دسته‌بندی با قیمت فروش مستقل عرضه شود (تعاریف: چند‌دسته‌بندی‌شدن).

- پاسخ `200`:
```json
{ "categories": [ { "id": "uuid", "category": "باکس ۸ تایی", "sale_price": 800000, "is_default_sale": true } ] }
```
- کالای بدون عرضه‌ی اضافه → `{ "categories": [] }`. کالای خارج از شرکت کاربر → `404`.

### ۴.۳۴ `GET /mobile/visitor/products/:id/uoms`
بسته‌بندی‌های چندواحدی یک کالا (مثلاً شل/کارتن ۸ تایی). `conversion_factor` تعداد واحد پایه در هر بسته است و `unit_price` قیمت فروش همان بسته.

- پاسخ `200`:
```json
{ "uoms": [ { "id": "uuid", "uom_id": "uuid", "unit_name": "شل", "conversion_factor": 8, "unit_price": 800000, "barcode": "", "is_default_for_sale": true } ] }
```
- کالای تک‌واحدی → `{ "uoms": [] }`. کالای خارج از شرکت کاربر → `404`.

> **نکته‌ی سفارش‌گیری:** هنگام ثبت سفارش، بسته‌بندی انتخاب‌شده با `uom_id` در هر آیتم ذخیره می‌شود؛ اپ باید هم `quantity`/`unit_price` گزینه‌ی انتخابی و هم `uom_id` را بفرستد تا هنگام صدور فاکتور، مقدار به واحد پایه تبدیل شود (بدون `uom_id` = واحد پایه).

---

### ۴.۳۵ `POST /mobile/visitor/location`
ثبت یک نمونه‌ی موقعیت زنده برای ویزیتورِ احراز‌شده (توکن). برای نقشه‌ی زنده‌ی پنل استفاده می‌شود.

- بدنه:
```json
{ "latitude": 30.2839, "longitude": 57.0834, "accuracy": 15, "recorded_at": "2026-09-20T08:15:00Z" }
```
- `recorded_at` **اختیاری** (RFC3339)؛ خالی = «الان». برای نقاطی که آفلاین ثبت و بعداً ارسال می‌شوند، این را بفرستید تا زمان واقعی حرکت ثبت شود (نه زمان دریافت).
- اعتبارسنجی: `latitude` بین ۲۵ و ۴۰٫۵، `longitude` بین ۴۴ و ۶۳٫۵ (کادر ایران)، `accuracy ≥ 0`. مقادیر `NaN/Infinity` با `400` رد می‌شوند.
- پاسخ `200`: `{ "message": "موقعیت ثبت شد" }`
- فقط برای کاربری که رکورد ویزیتور دارد؛ در غیر این صورت `401`.

### ۴.۳۶ `POST /mobile/visitor/locations/batch`
آپلود دسته‌ای نمونه‌های موقعیت از **صف آفلاین**. هر نمونه زمان ثبت خودش را نگه می‌دارد تا مسیر تاریخی دقیق بماند.

- بدنه (۱ تا ۵۰۰ نمونه):
```json
{
  "locations": [
    { "latitude": 30.2839, "longitude": 57.0834, "accuracy": 15, "recorded_at": "2026-09-20T08:15:00Z" },
    { "latitude": 30.2841, "longitude": 57.0837, "accuracy": 20, "recorded_at": "2026-09-20T08:20:00Z" }
  ]
}
```
- `recorded_at` اختیاری (RFC3339)؛ خالی = «الان». نمونه‌های `NaN/Infinity` بی‌صدا حذف می‌شوند.
- پاسخ `200`: `{ "message": "موقعیت‌ها ثبت شد", "stored": 2 }`
- اگر هیچ نمونه‌ی معتبری نماند → `400`.
- کل دسته در **یک تراکنش** ذخیره می‌شود؛ خطای وسط کار همه را برمی‌گرداند.

> نمونه‌های تاریخچه‌ی هر ویزیتور برای پنل از `GET /api/v1/visitors/:id/locations?start=&end=` و آخرین موقعیت همه از `GET /api/v1/visitors/locations/latest` خوانده می‌شود (گروه `auth`، نه موبایل).

---

## ۵) مدل‌های مشترک (خلاصه)

### `MobileCustomer`
```json
{ "id": "uuid", "name": "...", "phone": "...", "address": "...", "national_id": "...", "balance": 0, "credit_limit": 0, "lat": 0, "lng": 0 }
```

### `MobileTour`
```json
{ "id": "uuid", "name": "...", "customers": [ MobileCustomer ] }
```

### `domain.Visit`
```json
{ "id": "uuid", "company_id": "uuid", "visitor_id": "uuid", "customer_id": "uuid", "visit_date": "...", "visit_type": "ROUTE|EXTRA", "status": "PLANNED|VISITED|NO_ANSWER|CLOSED", "notes": "...", "latitude": 0, "longitude": 0, "order_id": "uuid", "client_visit_id": "...", "created_at": "...", "updated_at": "..." }
```

### `NotificationResponse`
```json
{ "id": "uuid", "type": "ROUTE_ASSIGNED", "title": "...", "body": "...", "is_read": false, "created_at": "..." }
```

### `ProductStockSnapshot`
```json
{ "product_id": "uuid", "total_qty": 0, "reserved_qty": 0, "available_qty": 0, "stock_updated_at": "..." }
```

---

## ۶) قرارداد آفلاین و ایدمپوتنسی (مهم برای اندروید)

1. **کلید ایدمپوتنسی اجباری:** برای هر سفارش `client_order_id` و برای هر بازدید `client_visit_id` تولید کنید (UUID یا رشته‌ی یکتا، حداکثر ۶۴). ارسال دوباره با همان کلید → `200` و همان رکورد (بدون تکرار).
2. **ثبت دسته‌ای:** سفارش‌ها حداکثر ۵۰ و بازدیدها حداکثر ۱۰۰ در هر درخواست.
3. **صف آفلاین:** نتایج هر آیتم در `results` جداگانه برمی‌گردد؛ یک خطا کل دسته را خراب نمی‌کند. آیتم‌های `error` را در صف نگه دارید و `created`/`duplicate`/`blocked` را از صف حذف کنید.
4. **بوت‌استرپ و دلتا:** یک‌بار `GET /mobile/visitor/sync` بدون `since` بگیرید، سپس فقط با `since=<server_time>`. دلتا شامل `products`، `product_stock`، `customers`، `tours`، `discounts`، `approved_invoices`، `visits`، `notifications` و `deleted_ids` است.
5. **Tombstone:** `deleted_ids.products` را از DB محلی حذف کنید. برای مشتری‌ها فعلاً tombstone وجود ندارد؛ برای تطبیق کامل، `GET /mobile/visitor/customers` را دوره‌ای همگام کنید.
6. **اعلان بلادرنگ:** SSE روی `/mobile/visitor/messages/stream`؛ در صورت قطع، با `GET /mobile/visitor/messages?page=1` شکاف را پر کنید (بازپخش خودکار SSE پشتیبانی نمی‌شود — `Last-Event-ID` نادیده گرفته می‌شود).
7. **اعلان پس‌زمینه:** توکن FCM را با `POST /mobile/visitor/devices` ثبت کنید و در logout با `DELETE` حذف کنید. (ارسال سمت سرور در این نسخه فعال نیست.)
8. **صفحه‌بندی:** برای لیست‌های بزرگ (کالا/سفارش/مشتری) از `page`/`page_size` و هدر `X-Total-Count` (یا `has_more` در بدنه) استفاده کنید و Infinite Scroll بگذارید.

---

## ۷) نقشه‌ی صفحه‌های پیشنهادی اپ (بر پایه‌ی API)

| صفحه | Endpointهای مرتبط |
|---|---|
| راه‌اندازی/بررسی نسخه | `visitor/health` |
| ورود | `auth/login` (+ انتخاب شرکت، 2FA)، `auth/refresh` |
| داشبورد امروز | `visitor/today`, `visitor/profile`, `visitor/goals` |
| مشتریان (جست‌وجو) | `visitor/customers` (فهرست/جست‌وجو)، `visitor/customers/:id` (جزئیات/ویرایش)، `POST` (ایجاد) |
| مشتریان امروز | `visitor/today` |
| ثبت بازدید | `visitor/visits` (+batch)، `visitor/zone-permissions` |
| سفارش‌گیری | `visitor/products` / `products-promo` / `products/lookup` (بارکد), `products/:id/categories`, `products/:id/uoms`, `products/stock`, `visitor/orders` (+batch) |
| سابقه/جزئیات سفارش | `visitor/orders`, `visitor/orders/:id` |
| تخفیف‌ها | `visitor/discounts`, `products-promo` |
| فاکتور/چاپ | `visitor/approved-invoices`, `visitor/invoices/:id/printed` |
| گزارش من | `visitor/reports`, `visitor/goals` |
| پیام‌ها | `visitor/messages`, `visitor/messages/stream` |
| همگام‌سازی | `visitor/sync` |
| اعلان پس‌زمینه | `visitor/devices` (ثبت/حذف توکن) |

---

## ۸) نکات پیاده‌سازی اندروید

- **Retrofit/OkHttp:** یک `Interceptor` برای افزودن `Authorization: Bearer` و یک `Authenticator` برای `auth/refresh` خودکار هنگام `401` قرار دهید.
- **Room/DataStore:** `server_time` سینک، صف سفارش/بازدید و `client_*_id`ها را ذخیره کنید.
- **SSE:** کتابخانه‌ی SSE (مثل `okhttp-sse`)؛ توکن را از `?token=` بدهید چون EventSource هدر نمی‌فرستد. بازپخش پس از قطع وجود ندارد؛ با `messages` شکاف را پر کنید.
- **WorkManager:** ارسال دسته‌ای صف آفلاین در پس‌زمینه با محدودیت‌های ۵۰/۱۰۰.
- **FCM:** توکن را پس از ورود با `POST /mobile/visitor/devices` ثبت کنید؛ در `onNewToken` دوباره ثبت کنید؛ در logout حذف کنید.
- **صفحه‌بندی:** از `page`/`page_size` و `has_more` (یا `X-Total-Count`) برای Infinite Scroll استفاده کنید؛ در Room با Paging 3 ادغام کنید.
- **Geo:** برای ایجاد مشتری و ثبت بازدید، `latitude`/`longitude` را از GPS پر کنید (قواعد محدوده‌ی تور روی سرور اعمال می‌شود).
- **زمان/تاریخ:** تاریخ‌ها میلادی `YYYY-MM-DD`؛ در UI به شمسی تبدیل کنید.
- **تخمین مالیات:** `tax_amount`/`final_amount` فقط سمت سرور محاسبه می‌شوند؛ در حالت آفلاین مبلغ نمایش‌داده‌شده «تخمینی» است و نهایی پس از تأیید سرور.

---

## ۹) فایل‌های مرجع در ریپو
- `cmd/api/main.go` — تعریف مسیرها و میدل‌ورها
- `internal/infrastructure/web/handlers/visitor_mobile_handler.go`
- `internal/infrastructure/web/handlers/visit_zone_permission_handler.go`
- `internal/infrastructure/web/handlers/auth_handler.go`
- `internal/core/service/visitor_mobile_service.go` + `visitor_mobile_extras.go` (سفارش/مشتری/دستگاه/چاپ/بارکد/health)
- `internal/core/domain/treasury_complete.go` (Visit)، `product.go` (ProductStockSnapshot)، `visit_zone_permission.go`، `mobile_device.go`
- `internal/infrastructure/web/dto/` (ProductResponse، CustomerResponse، OrderResponse، NotificationResponse، auth_*)
- `internal/infrastructure/persistence/migrations/000220_mobile_devices_and_invoice_prints.up.sql`
- مستندات قدیمی‌تر مرتبط: `ai/133-mobile-api.md`، `ai/140-mobile-api-contract.md`

---

## ۱۰) نگاشت شکاف‌های `ai/141` به وضعیت رفع (نسخه ۲)

| # | شکاف | وضعیت | راه‌حل در این نسخه |
|---|---|---|---|
| 1.1 | لیست/جزئیات سفارش | ✅ رفع شد | `GET /mobile/visitor/orders` + `/orders/:id` |
| 1.2 | ثبت توکن Push | ✅ Endpoint رفع شد | `POST/DELETE /mobile/visitor/devices` (ارسال واقعی FCM خارج از دامنه) |
| 1.3 | مشتری مستقل از «امروز» | ✅ رفع شد | `GET /mobile/visitor/customers` (+`:id`) |
| 1.4 | ویرایش مشتری | ✅ رفع شد | `PUT /mobile/visitor/customers/:id` |
| 1.5 | جست‌وجوی بارکد | ✅ رفع شد | `GET /mobile/visitor/products/lookup?barcode=` |
| 2.1 | `/sync` بدون مشتری/تور/تخفیف | ✅ رفع شد | `customers`، `tours`، `discounts`، `product_stock` اضافه شد |
| 2.2 | Tombstone | ⚠️ جزئی | `deleted_ids.products` فعال؛ `deleted_ids.customers` نیازمند ستون `deleted_at` در آینده |
| 2.3 | فلگ چاپ فاکتور | ✅ رفع شد | `printed_at` + `POST /mobile/visitor/invoices/:id/printed` |
| 2.4 | وضعیت مصرف مجوز محدوده | ✅ رفع شد | فیلد محاسباتی `is_usable` در لیست مجوزها |
| 2.5 | فرمول مالیات | 📄 مستند شد | UI: «مبلغ تخمینی — نهایی پس از تأیید سرور» |
| 2.6 | عمر توکن دسترسی | ✅ رفع شد | `expires_in` در پاسخ `auth/login` |
| 3.1 | معنای `since` | 📄 مستند شد | روی `updated_at`، انحصاری، UTC/RFC3339، monotonic |
| 3.2 | `fiscal_year_id` | 📄 مستند شد | **اختیاری**؛ مقدار غیر-UUID نادیده و سال مالی جاری اعمال می‌شود |
| 3.3 | وضعیت‌های سفارش | 📄 مستند شد | جدول ۴.۳۲ |
| 3.4 | خطای مشتری تکراری | ✅ رفع شد | `409` + `duplicate: true` + `existing_customer_id` |
| 3.5 | `Last-Event-ID` در SSE | 📄 تصمیم | پشتیبانی نمی‌شود؛ شکاف با `GET /mobile/visitor/messages` پر می‌شود |
| 3.6 | ابطال refresh در logout | 📄 مستند شد | logout توکن دسترسی را باطل می‌کند؛ اپ refresh_token را دور می‌اندازد |
| 3.7 | Rate Limit دقیق | 📄 مستند شد | گروه موبایل `RateLimitUser`؛ `login/refresh` `RateLimitIP` (مقدار در `cfg`) |
| 3.8 | سقف `client_order_id` | ✅ تأیید | ۶۴ کاراکتر (هم‌ارز `client_visit_id`) |
| 4.1 | قالب خطا | 📄 مستند شد | چندشکلی + راهنمای parse (بخش ۱.۱) |
| 4.2 | شکل لیست‌ها | 📄 مستند شد | لیست‌های آرایه‌ای + هدر صفحه‌بندی؛ Endpointهای جدید با بدنه‌ی صفحه‌بندی |
| 4.3 | Versioning | 📄 تصمیم | مسیرها زیر `/api/v1`؛ قرارداد سازگاری عقب‌رو حفظ می‌شود |
| 4.4 | توکن در Query (SSE) | 📄 تصمیم | فقط روی مسیرهای stream؛ برای بقیه هدر Bearer الزامی |
| 5.1 | نبود Pagination | ✅ رفع شد | `page`/`page_size` + `X-Total-Count` روی همه‌ی لیست‌ها |
| 5.2 | تکرار در `/today` | ✅ از قبل | `customers` تخت و dedup شده است (`mergeTourCustomers`) |
| 5.3 | سه منبع موازی کالا | 📄 تصمیم | `/sync` اکنون `products` + `product_stock` را یکجا می‌دهد |
| 5.4 | ETag/Cache | 📄 تصمیم | اتکا به `/sync` (delta) به‌جای ETag |
| 5.5 | مسیر تصویر محصول | 📄 مستند شد | `image_url` نسبی است؛ با origin (نه `/api/v1`) ترکیب شود؛ فقط در `visitor/products` و آپلود ادمین‌محور (بخش ۴.۶.۱) |
| 6.1 | محدودیت iOS | ⚪ خارج از API | تصمیم مدیریتی (Android-first) |
| 6.2 | چاپ حرارتی iOS | ⚪ خارج از API | `InvoicePrinter` مستقل از پلتفرم |
| 6.3 | Endpoint سلامت/نسخه | ✅ رفع شد | `GET /mobile/visitor/health` |

> ✅ = پیاده‌سازی شد · 📄 = مستندسازی/تصمیم قرارداد · ⚠️ = جزئی · ⚪ = خارج از دامنه‌ی API
