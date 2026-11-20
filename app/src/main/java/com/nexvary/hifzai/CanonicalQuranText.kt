package com.nexvary.hifzai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexvary.hifzai.learning.QuranCorpus
import com.nexvary.hifzai.learning.VerifiedVerse

/** Each displayed substring is an exact source slice; the opening is not an ayah number. */
@Composable
internal fun CanonicalQuranText(corpus: QuranCorpus, verse: VerifiedVerse, fontSize: Int = 29,
    color: Color = Color(0xFFEEF1F2), showAyahNumber: Boolean = false) {
    val parts = corpus.presentation(verse)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.opening?.let { opening ->
            Text("بسملة افتتاح السورة • غير مرقمة", fontSize = 12.sp, color = Color(0xFFB9C6CF))
            Text(opening, fontFamily = quranTypeface, fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.85f).sp, color = Color(0xFFD6B465),
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        if (showAyahNumber) Text("﴿ " + verse.verse + " ﴾", color = Color(0xFFD6B465),
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(parts.ayahText, fontFamily = quranTypeface, color = color, fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.85f).sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}
