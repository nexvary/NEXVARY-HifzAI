package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class ReciterCatalogTest {
    @Test fun nameKeysAreUnique() {
        assertEquals(ReciterCatalog.all.size, ReciterCatalog.all.map { it.slug }.distinct().size)
    }
    @Test fun catalogContainsEgyptAndSaudiArabia() {
        assertEquals(3, ReciterCatalog.byCountry(ReciterCountry.EGYPT).size)
        assertEquals(3, ReciterCatalog.byCountry(ReciterCountry.SAUDI_ARABIA).size)
    }
    @Test fun catalogDoesNotPretendToHaveLicensedAudio() {
        assertTrue(ReciterCatalog.all.none { it.hasLicensedAudio })
    }
}
