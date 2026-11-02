package com.nexvary.hifzai.learning

/** Optional in-app suggestion only. No alarms, notifications, streaks or efficacy claim. */
object GentleReturnPolicy {
    fun shouldSuggest(enabled: Boolean, anchorDay: Long?, today: Long): Boolean =
        enabled && anchorDay != null && anchorDay >= 0 && today >= anchorDay && today - anchorDay >= 7
}
