package com.basecamp.turbolinks

data class TurbolinksVisit(
        val location: String,
        val destinationIdentifier: Int,
        val restoreWithCachedSnapshot: Boolean,
        val reload: Boolean,
        val callback: TurbolinksSessionCallback
) {
    val action: String
        get() = when (restoreWithCachedSnapshot && !reload) {
            true -> TurbolinksSession.ACTION_RESTORE
            else -> TurbolinksSession.ACTION_ADVANCE
        }
}
