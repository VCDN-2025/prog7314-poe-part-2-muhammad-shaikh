package za.co.studysync.data

import android.content.Context
import android.content.Context.MODE_PRIVATE

object TokenStore {
    private const val PREFS = "auth_prefs"
    private const val KEY_JWT = "jwt_token"

    fun save(context: Context, token: String) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putString(KEY_JWT, token)
            .apply()
    }

    fun get(context: Context): String? {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString(KEY_JWT, null)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .remove(KEY_JWT)
            .apply()
    }
}
