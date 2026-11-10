package com.havamania

/** One active action per resource, with at most one remembered follow-up signal. */
internal class CitySyncRequestGate {
    private class Entry(var dirty: Boolean = false)
    private val active = mutableMapOf<String, Entry>()

    suspend fun run(key: String, action: suspend () -> Unit) {
        val entry = synchronized(active) {
            active[key]?.let { it.dirty = true; return }
            Entry().also { active[key] = it }
        }
        try {
            while (true) {
                action()
                val repeat = synchronized(active) {
                    if (entry.dirty) { entry.dirty = false; true }
                    else { active.remove(key); false }
                }
                if (!repeat) return
            }
        } finally {
            synchronized(active) { if (active[key] === entry) active.remove(key) }
        }
    }
}
