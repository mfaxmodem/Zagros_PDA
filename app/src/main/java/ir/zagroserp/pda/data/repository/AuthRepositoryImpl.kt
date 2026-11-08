package ir.zagroserp.pda.data.repository

import android.content.Context
import ir.zagroserp.pda.BuildConfig
import ir.zagroserp.pda.core.AppError
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.DeviceId
import ir.zagroserp.pda.data.prefs.Session
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.data.remote.api.VisitorApi
import ir.zagroserp.pda.data.remote.apiCall
import ir.zagroserp.pda.data.remote.dto.DeviceDeleteRequest
import ir.zagroserp.pda.data.remote.dto.DeviceRequest
import ir.zagroserp.pda.data.remote.dto.LoginRequest
import ir.zagroserp.pda.data.remote.dto.RefreshRequest
import ir.zagroserp.pda.domain.model.HealthInfo
import ir.zagroserp.pda.domain.repository.AuthRepository
import ir.zagroserp.pda.domain.repository.LoginOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: VisitorApi,
    private val sessionStore: SessionStore,
    private val json: Json,
    @param:ApplicationContext private val context: Context,
) : AuthRepository {

    override val session: Flow<Session?> = sessionStore.session
    override val isLoggedIn: Flow<Boolean> = sessionStore.isLoggedIn

    override suspend fun login(
        username: String,
        password: String,
        otp: String?,
        companyId: String?,
    ): LoginOutcome {
        val request = LoginRequest(
            username = username.trim(),
            password = password,
            otp = otp?.takeIf { it.isNotBlank() },
            deviceId = DeviceId.get(context),
            companyId = companyId?.takeIf { it.isNotBlank() },
        )

        return when (val result = apiCall(json) { api.login(request) }) {
            is AppResult.Failure -> {
                when (val error = result.error) {
                    is AppError.Unauthorized -> {
                        // The API reports "2FA required / wrong OTP" through a 401 envelope.
                        if (error.envelope?.requires2fa == true) {
                            LoginOutcome.Requires2Fa
                        } else {
                            LoginOutcome.Failed(error.message)
                        }
                    }

                    is AppError.Validation -> {
                        if (error.envelope?.requiresCompany == true) {
                            LoginOutcome.RequiresCompany(error.envelope.companies)
                        } else {
                            LoginOutcome.Failed(error.message)
                        }
                    }

                    is AppError.RateLimited -> LoginOutcome.Failed(
                        error.retryAfterSeconds?.let {
                            "حساب موقتاً قفل شده است. ${it / 60} دقیقه دیگر تلاش کنید"
                        } ?: error.message,
                    )

                    else -> LoginOutcome.Failed(error.message)
                }
            }

            is AppResult.Success -> {
                val body = result.data
                if (body.token.isBlank()) {
                    return LoginOutcome.Failed("پاسخ سرور ناقص بود")
                }
                sessionStore.saveLogin(
                    accessToken = body.token,
                    refreshToken = body.refreshToken,
                    expiresInSeconds = body.expiresIn,
                    fullName = body.fullName,
                    avatarUrl = body.avatarUrl,
                )
                loadProfile()
                LoginOutcome.Success(
                    Session(
                        accessToken = body.token,
                        refreshToken = body.refreshToken,
                        expiresAtEpochSeconds = System.currentTimeMillis() / 1000 + body.expiresIn,
                        fullName = body.fullName,
                        avatarUrl = body.avatarUrl,
                        visitorId = "",
                        username = username,
                    ),
                )
            }
        }
    }

    override suspend fun logout() {
        runCatching { api.logout() }
        sessionStore.clear()
    }

    override suspend fun health(): AppResult<HealthInfo> =
        apiCall(json) { api.health() }.let { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(
                    HealthInfo(
                        status = result.data.status,
                        serverVersion = result.data.serverVersion,
                        minAppVersion = result.data.minAppVersion,
                        serverTime = result.data.serverTime,
                    ),
                )

                is AppResult.Failure -> result
            }
        }

    override suspend fun loadProfile(): AppResult<Unit> =
        when (val result = apiCall(json) { api.profile() }) {
            is AppResult.Success -> {
                sessionStore.saveVisitor(
                    visitorId = result.data.visitorId,
                    username = result.data.username,
                    fullName = result.data.name,
                )
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }

    override suspend fun registerPushToken(token: String): AppResult<Unit> {
        if (token.isBlank()) return AppResult.Success(Unit)
        val body = DeviceRequest(
            deviceToken = token,
            platform = "ANDROID",
            deviceId = DeviceId.get(context),
            appVersion = BuildConfig.VERSION_NAME,
        )
        return when (val result = apiCall(json) { api.registerDevice(body) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    override suspend fun unregisterPushToken(token: String): AppResult<Unit> {
        if (token.isBlank()) return AppResult.Success(Unit)
        return when (val result = apiCall(json) { api.deleteDeviceBody(DeviceDeleteRequest(token)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    override suspend fun refreshIfNeeded() {
        val current = sessionStore.cachedSession() ?: return
        if (!current.needsProactiveRefresh()) return
        if (current.refreshToken.isBlank()) return
        runCatching {
            val response = api.refresh(RefreshRequest(current.refreshToken))
            val body = response.body()
            if (response.isSuccessful && body != null && body.token.isNotBlank()) {
                sessionStore.updateTokens(body.token, body.refreshToken, body.expiresIn)
            } else if (response.code() == 401 || response.code() == 403) {
                sessionStore.clear()
            }
            // Otherwise keep the session; the authenticator retries on the next 401.
        }
    }

}
