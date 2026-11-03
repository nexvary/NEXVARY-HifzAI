package com.nexvary.hifzai.learning

import android.content.Context
import java.util.UUID

/** Nonpersonal invalidation marker kept outside the dataset it invalidates. No network/identity. */
class RuntimeResetStore(context: Context) {
    private val preferences = context.getSharedPreferences("hifz_runtime_reset_v1", Context.MODE_PRIVATE)
    val token: String get() = runCatching { preferences.getString("generation", "initial") ?: "initial" }
        .getOrDefault("invalid")
    fun acceptsSaved(saved: String?): Boolean = (saved ?: "initial") == token
    /** Persist before recreation so even process restoration rejects the previous saved state. */
    fun advance(): Boolean = preferences.edit().putString("generation", UUID.randomUUID().toString()).commit()
}
