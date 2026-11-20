package com.nexvary.hifzai

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Offline, paginated legal text; no browser/server or modification of the license asset. */
@Composable
internal fun BundledLicenseText(title: String, asset: String) {
    val context = LocalContext.current
    var expanded by remember(asset) { mutableStateOf(false) }
    var content by remember(asset) { mutableStateOf<String?>(null) }
    var page by remember(asset) { mutableIntStateOf(0) }
    OutlinedButton(onClick={ expanded = !expanded }) { Text(if (expanded) "إخفاء: $title" else title) }
    LaunchedEffect(asset,expanded) {
        if (expanded && content==null) content = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(asset).bufferedReader(Charsets.UTF_8).use { it.readText() } }
                .getOrDefault("تعذر قراءة ملف الترخيص المحلي")
        }
    }
    if (expanded) {
        val text = content
        if (text==null) Text("جارٍ فتح الترخيص…") else {
            val count = maxOf(1,(text.length+3999)/4000)
            Text("جزء ${page+1} من $count • النص الأصلي دون اختصار")
            Text(text.substring((page*4000).coerceAtMost(text.length),((page+1)*4000).coerceAtMost(text.length)),fontFamily=FontFamily.Monospace)
            if (page>0) OutlinedButton(onClick={ page-- }) { Text("الجزء السابق من الترخيص") }
            if (page+1<count) OutlinedButton(onClick={ page++ }) { Text("الجزء التالي من الترخيص") }
        }
    }
}
