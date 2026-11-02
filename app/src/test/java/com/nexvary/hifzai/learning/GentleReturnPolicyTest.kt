package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class GentleReturnPolicyTest {
    @Test fun noSuggestionWithoutExplicitConsentOrKnownDate() {
        assertFalse(GentleReturnPolicy.shouldSuggest(false, 10, 30))
        assertFalse(GentleReturnPolicy.shouldSuggest(true, null, 30))
    }
    @Test fun aFullWeekIsRequiredAndFutureOrCorruptDatesDoNotTrigger() {
        assertFalse(GentleReturnPolicy.shouldSuggest(true, 10, 16))
        assertTrue(GentleReturnPolicy.shouldSuggest(true, 10, 17))
        assertFalse(GentleReturnPolicy.shouldSuggest(true, 30, 10))
        assertFalse(GentleReturnPolicy.shouldSuggest(true, -1, 30))
    }
    @Test fun postposingResetsTheWeekWithoutAnyStreakPenalty() {
        assertTrue(GentleReturnPolicy.shouldSuggest(true, 10, 17))
        assertFalse(GentleReturnPolicy.shouldSuggest(true, 17, 17))
        assertTrue(GentleReturnPolicy.shouldSuggest(true, 17, 24))
    }
}
