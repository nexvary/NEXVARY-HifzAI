package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class LocalDataResetTest {
    private class Journal : ResetJournal {
        override var pending = false
        var beginSucceeds = true
        var completeSucceeds = true
        override var readable = true
        override fun begin(): Boolean {
            if (beginSucceeds) pending = true
            return beginSucceeds
        }
        override fun complete(): Boolean {
            if (completeSucceeds) pending = false
            return completeSucceeds
        }
    }

    @Test fun failedIntentDoesNotDeleteAnything() {
        val journal = Journal().apply { beginSucceeds = false }
        val reset = LocalDataReset(journal, { error("must not delete") }, { error("must not delete") })
        assertEquals(LocalDataReset.Result.NOT_STARTED, reset.request())
        assertFalse(journal.pending)
    }

    @Test fun bothStoresAreAttemptedAndPartialFailureRecoversOnNextLaunch() {
        val journal = Journal()
        var progressWorks = false
        var notebookCalls = 0
        val reset = LocalDataReset(journal, { assertTrue(journal.pending); progressWorks }, {
            assertTrue(journal.pending); notebookCalls++; true
        })
        assertEquals(LocalDataReset.Result.PENDING, reset.request())
        assertTrue(journal.pending)
        assertEquals(1, notebookCalls)
        progressWorks = true
        val nextLaunch = LocalDataReset(journal, { progressWorks }, { notebookCalls++; true })
        assertEquals(LocalDataReset.Result.READY, nextLaunch.recover())
        assertFalse(journal.pending)
        assertEquals(2, notebookCalls)
    }

    @Test fun unreadableJournalDoesNotAuthorizeDeletingExistingData() {
        val journal = Journal().apply { readable = false; pending = true }
        val reset = LocalDataReset(journal, { error("must not delete") }, { error("must not delete") })
        assertEquals(LocalDataReset.Result.PENDING, reset.recover())
    }

    @Test fun notebookFailureKeepsTheIntentEvenAfterProgressDeletionSucceeds() {
        val journal = Journal()
        var notebookWorks = false
        val reset = LocalDataReset(journal, { true }, { notebookWorks })
        assertEquals(LocalDataReset.Result.PENDING, reset.request())
        assertTrue(journal.pending)
        notebookWorks = true
        assertEquals(LocalDataReset.Result.READY, reset.recover())
        assertFalse(journal.pending)
    }

    @Test fun processDeathAfterIntentOrFirstDeletionKeepsRecoveryRequired() {
        val journal = Journal()
        assertTrue(journal.begin())
        var deleted = 0
        val reset = LocalDataReset(journal, { deleted++; true }, { error("simulated process death") })
        assertThrows(IllegalStateException::class.java) { reset.recover() }
        assertTrue(journal.pending)
        assertEquals(1, deleted)
        assertEquals(LocalDataReset.Result.READY, LocalDataReset(journal, { true }, { true }).recover())
    }

    @Test fun failedCompletionRetainsIntentAndOrdinaryLaunchDoesNotDelete() {
        val journal = Journal().apply { completeSucceeds = false }
        val reset = LocalDataReset(journal, { true }, { true })
        assertEquals(LocalDataReset.Result.PENDING, reset.request())
        journal.completeSucceeds = true
        assertEquals(LocalDataReset.Result.READY, reset.recover())
        val ordinary = LocalDataReset(journal, { error("unexpected delete") }, { error("unexpected delete") })
        assertEquals(LocalDataReset.Result.READY, ordinary.recover())
    }
}
