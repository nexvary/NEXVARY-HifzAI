package com.nexvary.hifzai.learning

import android.content.Context
import java.io.File
import java.io.IOException
import java.io.InputStream

internal class LocalRecitationLibrary(
    val catalog: RecitationCatalog,
    private val directory: File,
    private val source: (String) -> InputStream
) {
    /** Run on an I/O worker. Serialize installations even across Activity recreation. */
    fun bindRange(first: RecordingAddress, last: RecordingAddress, reader: String, today: Long): List<BoundRecitation> =
        synchronized(cacheLock) {
            val records = catalog.selectRange(first, last, reader, today)
            require(records.sumOf { it.byteLength } <= QUOTA_BYTES) {
                "اختر نطاقًا أصغر؛ حجمه يتجاوز مساحة التلاوات المحلية"
            }
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create recitation cache")
            val store = OfflineAudioStore(directory, QUOTA_BYTES)
            records.map { record ->
                val rights = record.rights
                val target = File(directory, rights.sha256 + ".audio")
                val cached = try {
                    catalog.bind(rights.recordingId, target, today)
                } catch (_: IllegalArgumentException) {
                    null
                } catch (_: IOException) {
                    null
                }
                cached ?: run {
                    if (!store.remove(rights)) throw IOException("Cannot replace invalid cache entry")
                    val installed = store.install(source(rights.recordingId), rights)
                    try {
                        catalog.bind(rights.recordingId, installed, today)
                    } catch (error: IllegalArgumentException) {
                        store.remove(rights)
                        throw error
                    }
                }
            }
        }

    companion object {
        private const val QUOTA_BYTES = 250_000_000L
        private val cacheLock = Any()

        // Add entries only after review of the recording, its reference and distribution rights.
        private val bundledRecords = emptyList<CatalogRecording>()

        fun bundled(context: Context, corpus: QuranCorpus): LocalRecitationLibrary {
            val app = context.applicationContext
            return LocalRecitationLibrary(RecitationCatalog.create(bundledRecords, corpus),
                File(app.noBackupFilesDir, "reviewed-recitation-cache")) { id ->
                app.assets.open("recitations/$id.audio")
            }
        }
    }
}
