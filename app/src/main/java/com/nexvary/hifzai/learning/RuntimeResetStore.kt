package com.nexvary.hifzai.learning

import android.content.Context
import java.util.UUID

/** Nonpersonal invalidation marker kept outside the dataset it invalidates. No network/identity. */
class RuntimeResetStore(context: Context) : ResetJournal {
    private val preferences = context.getSharedPreferences("hifz_runtime_reset_v1", Context.MODE_PRIVATE)
    val token: String get() = runCatching { preferences.getString("generation", "initial") ?: "initial" }
        .getOrDefault("invalid")
    override val pending: Boolean get() = runCatching { preferences.getBoolean("pending", false) }
        .getOrDefault(false)
    override val readable: Boolean get() = runCatching {
        preferences.getBoolean("pending", false)
        token != "invalid"
    }.getOrDefault(false)
    fun acceptsSaved(saved: String?): Boolean = readable && !pending && token != "invalid" && (saved ?: "initial") == token
    override fun begin(): Boolean = preferences.edit()
        .putString("generation", UUID.randomUUID().toString()).putBoolean("pending", true).commit()
    override fun complete(): Boolean = preferences.edit().putBoolean("pending", false).commit()
    /** Persist before recreation so even process restoration rejects the previous saved state. */
    fun advance(): Boolean = preferences.edit().putString("generation", UUID.randomUUID().toString()).commit()
}
