package com.nexvary.hifzai

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText

/** Corpus verification runs on IO; wait for its real completion, never a fixed sleep. */
internal fun ComposeTestRule.awaitHifzReady() {
    waitUntil(15_000) { onAllNodesWithText("حِفْظ | NEXVARY").fetchSemanticsNodes().isNotEmpty() }
}
