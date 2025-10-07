package za.co.studysync.data

import android.content.Context

object LocalDueStore {
    private const val PREF = "studysync_due"
    private const val KEY_PREFIX = "due_"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun setDue(ctx: Context, taskId: String, isoUtc: String) {
        prefs(ctx).edit().putString(KEY_PREFIX + taskId, isoUtc).apply()
    }

    fun getDue(ctx: Context, taskId: String): String? =
        prefs(ctx).getString(KEY_PREFIX + taskId, null)

    fun removeDue(ctx: Context, taskId: String) {
        prefs(ctx).edit().remove(KEY_PREFIX + taskId).apply()
    }
}
