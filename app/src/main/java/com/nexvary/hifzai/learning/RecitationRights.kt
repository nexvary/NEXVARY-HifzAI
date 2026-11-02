package com.nexvary.hifzai.learning

import java.net.URI
import java.time.LocalDate

/** All rights fields are supplied by a reviewed rights register, never inferred from a URL. */
data class RecitationRights(
    val recordingId: String,
    val reciterId: String,
    val sourceUrl: String,
    val sha256: String,
    val licenseDocument: String,
    val reviewerApproval: String,
    val riwayah: String,
    val permitsPlayback: Boolean,
    val permitsOffline: Boolean,
    val timingVerified: Boolean = false,
    /** Null means no expiry was recorded, not a legal assertion of perpetual rights. */
    val expiresEpochDay: Long? = null,
    val revoked: Boolean = false
) {
    fun permitsUse(offline: Boolean, synchronizedHighlight: Boolean = false,
        todayEpochDay: Long = LocalDate.now().toEpochDay()): Boolean =
        !revoked && (expiresEpochDay == null || todayEpochDay <= expiresEpochDay) &&
        recordingId.isNotBlank() && reciterId.isNotBlank() && runCatching {
            val url = URI(sourceUrl)
            url.scheme == "https" && !url.host.isNullOrBlank() && url.userInfo == null
        }.getOrDefault(false) &&
        sha256.matches(Regex("[0-9a-f]{64}")) && licenseDocument.isNotBlank() &&
        reviewerApproval.isNotBlank() && riwayah == "hafs-an-asim" && permitsPlayback &&
        (!offline || permitsOffline) && (!synchronizedHighlight || timingVerified)
}
