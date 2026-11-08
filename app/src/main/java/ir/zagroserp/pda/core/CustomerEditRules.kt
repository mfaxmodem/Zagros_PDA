package ir.zagroserp.pda.core

/**
 * Validation and formatting rules for editing a customer's contact details and position.
 *
 * Kept free of Android and Compose types so the rules can be unit tested — a wrong validation
 * here either blocks a legitimate edit or lets a bad value reach the server.
 */
object CustomerEditRules {

    /** Matches the server contract for `name` (۲ تا ۱۲۰ کاراکتر). */
    const val MIN_NAME_LENGTH = 2
    const val MAX_NAME_LENGTH = 120

    const val MAX_PHONE_LENGTH = 20
    const val MAX_ADDRESS_LENGTH = 500
    const val MAX_CITY_LENGTH = 80
    const val MAX_PROVINCE_LENGTH = 80

    /** Returns a Persian error message, or null when the name is acceptable. */
    fun validateName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "نام مشتری را وارد کنید"
            trimmed.length < MIN_NAME_LENGTH -> "نام باید حداقل $MIN_NAME_LENGTH کاراکتر باشد"
            trimmed.length > MAX_NAME_LENGTH -> "نام نمی‌تواند بیش از $MAX_NAME_LENGTH کاراکتر باشد"
            else -> null
        }
    }

    /** Phone is optional; only length and digit shape are checked. */
    fun validatePhone(phone: String): String? {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.length > MAX_PHONE_LENGTH) {
            return "شماره تماس نمی‌تواند بیش از $MAX_PHONE_LENGTH رقم باشد"
        }
        if (trimmed.any { !it.isDigit() && it != '+' && it != '-' }) {
            return "شماره تماس فقط می‌تواند رقم داشته باشد"
        }
        return null
    }

    fun validateAddress(address: String): String? =
        if (address.trim().length > MAX_ADDRESS_LENGTH) {
            "آدرس بیش از حد طولانی است"
        } else {
            null
        }

    /**
     * `0,0` is the server's way of saying "no coordinates recorded", so it must not be treated
     * as a real position — otherwise every customer would look geolocated.
     */
    fun hasCoordinates(latitude: Double?, longitude: Double?): Boolean {
        if (latitude == null || longitude == null) return false
        if (latitude.isNaN() || longitude.isNaN()) return false
        return latitude != 0.0 || longitude != 0.0
    }

    fun withinRadius(
        distanceMeters: Double,
        radiusMeters: Double = Geofence.CHECK_IN_RADIUS_METERS,
    ): Boolean = distanceMeters <= radiusMeters

    /** Readable `lat , lng` for the UI; empty when unknown. */
    fun coordinateLabel(latitude: Double?, longitude: Double?): String =
        if (!hasCoordinates(latitude, longitude)) "" else "$latitude , $longitude"

    /** Short confirmation shown after a successful capture. */
    fun captureConfirmation(latitude: Double, longitude: Double, accuracyMeters: Double): String =
        buildString {
            append("موقعیت ثبت شد: ")
            append(JalaliDate.toPersianDigits(String.format("%.5f", latitude)))
            append(" , ")
            append(JalaliDate.toPersianDigits(String.format("%.5f", longitude)))
            if (accuracyMeters > 0) {
                append(" (دقت ")
                append(JalaliDate.toPersianDigits(accuracyMeters.toInt().toString()))
                append(" متر)")
            }
        }

    /** Warning shown when the fix is too coarse to be useful for the check-in radius. */
    fun accuracyWarning(accuracyMeters: Double): String? =
        if (accuracyMeters > Geofence.MAX_TRUSTED_ACCURACY_METERS) {
            "دقت موقعیت پایین است (${JalaliDate.toPersianDigits(accuracyMeters.toInt().toString())} متر). " +
                "بهتر است در فضای باز دوباره تلاش کنید."
        } else {
            null
        }
}
