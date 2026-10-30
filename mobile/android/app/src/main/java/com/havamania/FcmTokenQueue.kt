package com.havamania

import android.app.Application
import com.google.firebase.auth.FirebaseAuth

/** Default-process single writer. Records desired state only; no network/SDK token request. */
internal object FcmTokenQueue {
    private var journal: FcmTokenJournal? = null
    private fun journal(app: Application) = journal ?: FcmTokenJournal(AtomicFcmJournalStorage(app)).also { journal = it }

    @Synchronized fun reconcile(app: Application) {
        val queue = journal(app)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || AccountDeletionManager.getInstance(app).state.value.pending) queue.clearAccount()
        else queue.update(uid, queue.snapshot().token)
    }

    @Synchronized fun recordToken(app: Application, token: String) {
        val queue = journal(app)
        if (AccountDeletionManager.getInstance(app).state.value.pending) queue.clearAccount()
        else queue.update(FirebaseAuth.getInstance().currentUser?.uid, token)
    }

    @Synchronized fun clearAccount(app: Application) { journal(app).clearAccount() }
}
