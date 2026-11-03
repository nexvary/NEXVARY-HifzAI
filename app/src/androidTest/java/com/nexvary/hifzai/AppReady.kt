package com.nexvary.hifzai

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText

/** Corpus verification runs on IO; wait for its real completion, never a fixed sleep. */
internal fun ComposeTestRule.awaitHifzReady() {
    waitUntil(15_000) {
        // Native loading has no Compose hierarchy yet. The predicate stays false;
        // a bad source or startup failure still times out and fails the test.
        runCatching { onAllNodesWithText("حِفْظ | NEXVARY").fetchSemanticsNodes().isNotEmpty() }
            .getOrDefault(false)
    }
}
