package ir.zagroserp.pda.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
    @SerialName("otp") val otp: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("company_id") val companyId: String? = null,
)

@Serializable
data class LoginResponse(
    @SerialName("token") val token: String = "",
    @SerialName("type") val type: String = "Bearer",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("expires_in") val expiresIn: Long = 3600,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class RefreshResponse(
    @SerialName("token") val token: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: Long = 2_592_000,
    @SerialName("message") val message: String = "",
)

@Serializable
data class ProfileResponse(
    @SerialName("visitor_id") val visitorId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("username") val username: String = "",
)

@Serializable
data class HealthResponse(
    @SerialName("status") val status: String = "",
    @SerialName("server_version") val serverVersion: String = "",
    @SerialName("min_app_version") val minAppVersion: String = "",
    @SerialName("server_time") val serverTime: String = "",
)

@Serializable
data class DeviceRequest(
    @SerialName("device_token") val deviceToken: String,
    @SerialName("platform") val platform: String = "ANDROID",
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
)

@Serializable
data class DeviceResponseDto(
    @SerialName("id") val id: String = "",
    @SerialName("platform") val platform: String = "ANDROID",
    @SerialName("device_token") val deviceToken: String = "",
    @SerialName("device_id") val deviceId: String = "",
)

@Serializable
data class DeviceDeleteRequest(
    @SerialName("device_token") val deviceToken: String,
)
