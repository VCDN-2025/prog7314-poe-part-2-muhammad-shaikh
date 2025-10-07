package za.co.studysync

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import za.co.studysync.i18n.LocaleManager
import za.co.studysync.ui.ThemeManager
import za.co.studysync.ui.setOnItemSelectedListenerCompat

class SettingsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        val ctx = LocaleManager.applyLocale(newBase, lang)
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme BEFORE inflating views
        ThemeManager.apply(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // -------- Theme toggle --------
        val swDark = findViewById<SwitchMaterial>(R.id.switchDarkTheme)
        swDark.isChecked = ThemeManager.isDark(this)
        swDark.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDark(this, isChecked)
            // Recreate so current screen rebinds colors
            recreate()
        }

        // -------- Language spinner --------
        val sp = findViewById<Spinner>(R.id.spLanguage)
        val items = listOf(
            getString(R.string.lang_english) to "en",
            getString(R.string.lang_afrikaans) to "af"
        )
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items.map { it.first })

        val currentLang = LocaleManager.getSavedLanguage(this)
        sp.setSelection(if (currentLang == "af") 1 else 0)

        sp.setOnItemSelectedListenerCompat { index ->
            val lang = items[index].second
            if (lang != currentLang) {
                LocaleManager.saveLanguage(this, lang)
                recreate() // apply new locale immediately on this screen
            }
        }

        // -------- Weekly digest (local-only for prototype) --------
        val cbDigest = findViewById<CheckBox>(R.id.cbDigest)
        val spref = getSharedPreferences("settings_prefs", MODE_PRIVATE)
        cbDigest.isChecked = spref.getBoolean("weekly_digest", false)
        cbDigest.setOnCheckedChangeListener { _, isChecked ->
            spref.edit().putBoolean("weekly_digest", isChecked).apply()
        }
    }
}
