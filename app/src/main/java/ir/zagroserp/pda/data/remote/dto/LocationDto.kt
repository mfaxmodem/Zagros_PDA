package ir.zagroserp.pda.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationRequest(
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("accuracy") val accuracy: Double = 0.0,
    @SerialName("recorded_at") val recordedAt: String? = null,
)

@Serializable
data class LocationBatchRequest(
    @SerialName("locations") val locations: List<LocationRequest>,
)

@Serializable
data class LocationBatchResponse(
    @SerialName("message") val message: String = "",
    @SerialName("stored") val stored: Int = 0,
)
