# گزارش راستی‌آزمایی و رفع شکاف‌های بک‌اند اپ ویزیتور

> مرجع شکاف‌ها: `ai/mobile/backend-requirements.md`
> دامنه: فقط بک‌اند سورس مادر پخش‌یار (`/opt/pakhshYar`).
> این سند برای تحویل به مدل توسعه‌ی اپ اندروید (`zagros-pda`) است تا قرارداد نهایی API را از آن مصرف کند.

---

## ۱. خلاصه‌ی اجرایی

هر ۹ مورد گزارش با شاهد کد بررسی شد. نتیجه:

| شناسه | ادعا | حکم راستی‌آزمایی | اقدام |
|---|---|---|---|
| P0-1 | `lat`/`lng` در پاسخ مشتری نیست | ✅ **واقعی** | **رفع شد** |
| P0-2 | `PUT /customers/:id` جایگزینی کامل است | ✅ **واقعی** | **رفع شد (PATCH واقعی)** |
| P0-3 | `POST /visits` فاصله را اعتبارسنجی نمی‌کند | ✅ **واقعی** | **رفع شد (شعاع سمت سرور)** |
| P1-1 | `GET /orders` فیلد `customer_name` ندارد | ✅ **واقعی** | **رفع شد** |
| P1-2 | `GET /products` فیلد `image_url` ندارد | ❌ **نادرست** | نیازی نیست |
| P1-3 | `printed_at` برنمی‌گردد | ❌ **نادرست** | نیازی نیست |
| P1-4 | `categories`/`uoms` برنمی‌گردد | ❌ **نادرست** | نیازی نیست |
| P2-1 | Endpoint «خوانده‌شده» برای پیام‌ها نیست | ✅ **واقعی** | **رفع شد** |
| P2-2 | FCM ارسال نمی‌شود | ⚠️ **نادرست (config)** | بدون تغییر کد |
| P2-3 | هشدار `soft_warning` در بازدید نیست | ⚠️ **طراحی‌محور** | بدون تغییر |

**۴ شکاف واقعی رفع شد؛ ۳ ادعا نادرست بود؛ ۲ مورد طراحی/پیکربندی.**

نتیجه‌ی آزمون‌ها پس از تغییرات: `go test ./...` ✅ · `npx tsc -b` ✅ · `npm test` (۱۹۰/۱۹۰) ✅.

---

## ۲. P0-1 — مختصات مشتری در پاسخ‌ها

**ریشه‌ی مشکل (شاهد کد):** نگاشت رکورد دیتابیس به دامنه در
`internal/infrastructure/persistence/postgres/visitor_repository.go` → `toDomainCustomer`
فقط `ID/Name/Address/Phone` را کپی می‌کرد؛ `latitude/longitude/national_id/credit_limit/city/province` دور ریخته می‌شدند.

**تغییر:** نگاشت کامل شد و DTO اپ (`MobileCustomer`) قرارداد یکنواخت گرفت:

- `lat` و `lng` همیشه عدد برمی‌گردانند؛ **`0` یعنی «مختصات ثبت‌نشده»** (نه `null`).
- `national_id`، `credit_limit`، `city`، `province` نیز اکنون پر می‌شوند.

```json
{
  "id": "51a89b36-...",
  "name": "آرش ارجمن",
  "phone": "09177433881",
  "address": "...",
  "national_id": "",
  "balance": 0,
  "credit_limit": 0,
  "lat": 30.2839,
  "lng": 57.0834
}
```

> **توجه برای اپ:** `balance` هنوز همیشه `0` است (فیلد مانده در دامنه‌ی `Customer` نگه‌داری نمی‌شود). اگر به مانده‌ی لحظه‌ای نیاز دارید، یک endpoint جدا لازم است — در این دور پیاده نشد.

**اعمال روی هر چهار مسیر** چون همه از `customerToMobile`/`mapMobileCustomers` رد می‌شوند:
`GET /customers` · `GET /customers/:id` · `GET /today` (هم `customers` و هم `tours[].customers`) · `GET /sync`.

---

## ۳. P0-2 — `PUT /mobile/visitor/customers/:id` حالا PATCH واقعی است

**ریشه‌ی مشکل:** `internal/core/service/visitor_mobile_extras.go` → `UpdateCustomer`
فیلدهای رشته‌ای را بی‌قید از بدنه جایگزین می‌کرد (`upd.Phone = strings.TrimSpace(req.Phone)`)، پس ارسال فقط `latitude/longitude` تلفن و آدرس را پاک می‌کرد.

**تغییر:**

- ساختار `service.MobileCustomerUpdate` به فیلدهای pointer تبدیل شد (`*string`/`*float64`).
- فقط فیلدهای **حاضر در بدنه** تغییر می‌کنند؛ فیلد غایب دست‌نخورده می‌ماند.
- `name` اگر pointer باشد ولی رشته‌ی خالی بفرستد، نادیده گرفته می‌شود (جلوگیری از پاک شدن نام).
- `national_id`، `credit_limit` و فیلدهای هویتی **هرگز** از بدنه خوانده نمی‌شوند (از رکورد موجود کپی می‌شوند).

قرارداد:

```http
PUT /api/v1/mobile/visitor/customers/:id
Content-Type: application/json

{ "latitude": 32.6546, "longitude": 51.6680 }
```

→ فقط مختصات تغییر می‌کند؛ `phone`/`address`/`name` حفظ می‌شوند. برای ویرایش هر فیلد، همان فیلد را در بدنه بفرستید.

> **نکته:** رفتار «ارسال مجموعه‌ی کامل» که اپ الان انجام می‌دهد هم سازگار است و بی‌خطر می‌ماند.

---

## ۴. P0-3 — اعتبارسنجی فاصله در ثبت بازدید (سمت سرور)

**ریشه‌ی مشکل:** `RegisterVisit` در `visitor_mobile_service.go` فقط تخصیص مشتری/محدوده را بررسی می‌کرد و فاصله را نه.

**تغییر:**

- تنظیم جدید per-company: **`visit_radius_meters`** (پیش‌فرض **۵۰**، `0` = خاموش) — مایگریشن `000238_visit_radius_meters`.
- در `POST /mobile/visitor/visits` و `POST /mobile/visitor/visits/batch`:

  1. **اولویت مجوز خارج از محدوده:** اگر مجوز معتبر `visit_zone_permission` وجود دارد، بررسی فاصله **اجرا نمی‌شود**.
  2. مشتری **بدون مختصات** (یا `0,0`) بدون محدودیت فاصله بازدید می‌شود.
  3. اگر مشتری مختصات دارد و شعاع > ۰:
     - نبودن مختصات در بدنه → `400` «ثبت بازدید برای این مشتری نیازمند موقعیت مکانی است.»
     - `is_mock: true` → `400` «موقعیت جعلی (mock) پذیرفته نمی‌شود…»
     - `accuracy > 100` متر → `400` «دقت موقعیت مکانی کافی نیست…»
     - فاصله بیش از شعاع → `400` با بدنه‌ی غنی:

```json
{
  "error": "فاصله‌ی شما تا مشتری 340 متر است. ثبت بازدید فقط تا شعاع 50 متر امکان‌پذیر است.",
  "distance_meters": 340.2,
  "allowed_meters": 50
}
```

**فیلدهای جدید بدنه‌ی ثبت بازدید (اختیاری):**

```json
{
  "customer_id": "...",
  "status": "VISITED",
  "latitude": 32.7001,
  "longitude": 51.7001,
  "accuracy": 12.5,
  "is_mock": false
}
```

**تنظیم شرکت** (مسیر `PUT /api/v1/company` با مجوز `settings.company`؛ تغییر در `audit_log` ثبت می‌شود):

```json
{ "name": "نام شرکت", "visit_radius_meters": 50 }
```

---

## ۵. P1-1 — `customer_name`/`visitor_name` در لیست سفارش‌های موبایل

**ریشه‌ی مشکل:** کوئری `ListOrdersByVisitor` در `internal/infrastructure/persistence/queries/order.sql` به‌صورت `SELECT * FROM orders` بود (بدون join)، در حالی که `GetOrderByID` join داشت.

**تغییر:** کوئری دقیقاً مثل `GetOrderByID`/`ListOrders` join شد:

```sql
SELECT o.*, c.name AS customer_name, COALESCE(u.full_name, u.username) AS visitor_name
FROM orders o
JOIN customers c ON o.customer_id = c.id
JOIN visitors v ON o.visitor_id = v.id
JOIN users u ON v.user_id = u.id
WHERE o.visitor_id = $1 AND o.company_id = $2
ORDER BY o.date DESC, o.created_at DESC
LIMIT $3 OFFSET $4;
```

نگاشت در `order_repository.go` → `ListByVisitor` هم `CustomerName`/`VisitorName` را پر می‌کند. خروجی `GET /mobile/visitor/orders` اکنون برای هر سفارش `customer_name` و `visitor_name` دارد.

---

## ۶. P2-1 — علامت‌گذاری پیام‌ها به‌عنوان خوانده‌شده

دو مسیر جدید (گروه ویزیتور، نیازمند توکن ویزیتور):

| متد | مسیر | کار |
|---|---|---|
| `PUT` | `/api/v1/mobile/visitor/messages/read` | همه‌ی پیام‌های ویزیتور را خوانده‌شده می‌کند |
| `PUT` | `/api/v1/mobile/visitor/messages/:id/read` | یک پیام را خوانده‌شده می‌کند |

پاسخ‌ها:

```json
// همه
{ "ok": true }
// یکی
{ "ok": true, "updated": 1 }
```

اگر شناسه‌ی پیام متعلق به ویزیتور نباشد → `404` با پیام «اعلان یافت نشد».
پیام‌ها با `user_id` کاربر ویزیتور ذخیره می‌شوند؛ اپ باید بعد از باز کردن صفحه‌ی پیام‌ها، مسیر «همه» را صدا بزند تا نشان خوانده‌نشده بین دستگاه‌ها همگام شود.

---

## ۷. موارد نادرست (نیازی به تغییر نیست)

- **P1-2 (`image_url`):** در `ProductResponse` وجود دارد (`json:"image_url,omitempty"`). اگر خالی دیده شد، یعنی آن کالا تصویر ندارد (در داده‌ی فعلی فقط بخشی از کالاها تصویر دارند)، نه اینکه فیلد نباشد.
- **P1-3 (`printed_at`):** `attachPrintTimestamps` در `visitor_mobile_service.go` مقدار را از جدول چاپ‌ها پر می‌کند؛ فیلد حاضر است.
- **P1-4 (`categories`/`uoms`):** هندلر `GET /products` پس از نگاشت، `Categories` و `UOMs` را هم پر می‌کند.
- **P2-2 (FCM):** ارسال FCM **پیاده‌سازی شده** (`internal/infrastructure/push/fcm.go` و سیم‌کشی در `cmd/api/main.go`) ولی **config-gated** است؛ روی این سرور متغیرهای `FCM_*` تنظیم نشده‌اند، پس خاموش است. برای فعال‌سازی، اعتبارنامه‌ی سرویس FCM در `.env` سرور لازم است (تغییر محیطی، نه سورس).
- **P2-3 (`soft_warning` در بازدید):** هشدارها فقط در لحظه‌ی ثبت نمایش داده می‌شوند و در رکورد بازدید ذخیره نمی‌شوند — یک انتخاب طراحی است، نه شکاف.

---

## ۸. مایگریشن و استقرار

- مایگریشن جدید: `internal/infrastructure/persistence/migrations/000238_visit_radius_meters.{up,down}.sql`
  - `ALTER TABLE companies ADD COLUMN visit_radius_meters INTEGER NOT NULL DEFAULT 50 CHECK (>= 0)`
- ستون `visit_radius_meters` در مدل sqlc و DTO شرکت و فرم «تنظیمات شرکت» فرانت اضافه شد.
- اعمال روی سرور: مایگریشن اجرا شد (`238/u visit_radius_meters`) و سرویس API بازسازی و سالم شد (`/health` = 200).

---

## ۹. شواهد آزمون

| آزمون | نتیجه |
|---|---|
| `go test ./...` | ✅ |
| `go vet ./internal/...` | ✅ |
| `npx tsc -b` (فرانت) | ✅ |
| `npm test` (فرانت) | ✅ ۱۹۰/۱۹۰ |
| اجرای SQL join سفارش‌ها روی DB | ✅ نام مشتری/ویزیتور برمی‌گردد |
| ثبت/خواندن `visit_radius_meters` از `PUT/GET /company` | ✅ |
| ثبت مسیرهای جدید پیام (بدون توکن → 401 نه 404) | ✅ |

تست‌های واحد افزوده‌شده در `internal/core/service/visitor_mobile_service_test.go`:
`TestRegisterVisitRejectsTooFar` · `TestRegisterVisitAcceptsWithinRadius` · `TestRegisterVisitRequiresLocationWhenCustomerHasCoords` · `TestRegisterVisitRejectsMockLocation` · `TestRegisterVisitRejectsCoarseAccuracy` · `TestRegisterVisitDistanceOff` · `TestUpdateCustomerPartialDoesNotWipe` · `TestCustomerToMobileCoordinates` · `TestMarkMessageRead`.

---

## ۱۰. چک‌لیست نهایی برای مدل اپ

- [x] `lat`/`lng` در هر چهار پاسخ مشتری (۰ = ثبت‌نشده).
- [x] `PUT /customers/:id` فقط فیلدهای ارسالی را تغییر می‌دهد.
- [x] `POST /visits` فاصله را سمت سرور اعتبارسنجی می‌کند (با پیام و `distance_meters`/`allowed_meters`).
- [x] `POST /visits/batch` هم فاصله را اعتبارسنجی می‌کند.
- [x] مجوز خارج از محدوده بر بررسی فاصله اولویت دارد.
- [x] `accuracy` بالا رد می‌شود؛ `is_mock: true` رد می‌شود.
- [x] `customer_name`/`visitor_name` در لیست سفارش‌ها.
- [x] `PUT /messages/read` و `PUT /messages/:id/read`.
- [ ] `balance` مشتری (هنوز پیاده نشده — خارج از دامنه‌ی این دور).
