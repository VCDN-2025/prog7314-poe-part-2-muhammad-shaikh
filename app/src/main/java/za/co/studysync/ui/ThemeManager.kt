package za.co.studysync.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS = "studysync_prefs"
    private const val KEY_DARK = "dark_theme"

    fun isDark(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false)

    fun setDark(ctx: Context, dark: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK, dark).apply()
        apply(ctx)
    }

    /** Call this in Activities before setContentView(...) */
    fun apply(ctx: Context) {
        val dark = isDark(ctx)
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
