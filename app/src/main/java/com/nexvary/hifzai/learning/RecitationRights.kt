package com.nexvary.hifzai.learning

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
    val timingVerified: Boolean = false
) {
    fun permitsUse(offline: Boolean, synchronizedHighlight: Boolean = false): Boolean =
        recordingId.isNotBlank() && reciterId.isNotBlank() && sourceUrl.startsWith("https://") &&
        sha256.matches(Regex("[0-9a-f]{64}")) && licenseDocument.isNotBlank() &&
        reviewerApproval.isNotBlank() && riwayah == "hafs-an-asim" && permitsPlayback &&
        (!offline || permitsOffline) && (!synchronizedHighlight || timingVerified)
}
