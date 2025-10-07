package za.co.studysync

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.co.studysync.data.Api
import za.co.studysync.data.TaskDto
import za.co.studysync.i18n.LocaleManager
import za.co.studysync.data.LocalDueStore   // <-- NEW: local fallback
import java.time.*

class CalendarActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        val ctx = LocaleManager.applyLocale(newBase, lang)
        super.attachBaseContext(ctx)
    }

    private lateinit var list: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val rows = mutableListOf<String>()
    private var allTasks: List<TaskDto> = emptyList()
    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val toolbar = findViewById<MaterialToolbar>(R.id.topBar)
        toolbar.title = getString(R.string.calendar)

        list = findViewById(R.id.listCalendar)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
        list.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabPickDate).setOnClickListener {
            showDatePicker()
        }

        // Initial load
        loadTasksAndApply()
    }

    override fun onResume() {
        super.onResume()
        // Reload when returning, in case tasks changed
        loadTasksAndApply()
    }

    private fun loadTasksAndApply() {
        lifecycleScope.launch {
            allTasks = withContext(Dispatchers.IO) { Api.svc.getTasks() }
            applyFilterFor(selectedDate)
        }
    }

    private fun showDatePicker() {
        val dlg = android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                applyFilterFor(selectedDate)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
        dlg.show()
    }

    private fun applyFilterFor(date: LocalDate) {
        // Local start/end of selected day → UTC Instant range
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()

        val dayTasks = allTasks.filter { t ->
            // Prefer server dueDateTime; fall back to local stored due date
            val dueIso = t.dueDateTime ?: LocalDueStore.getDue(this, t.id)
            dueIso?.let {
                try {
                    val inst = Instant.parse(it)
                    (inst >= start && inst < end)
                } catch (_: Exception) { false }
            } ?: false
        }

        val pretty = "${date.dayOfMonth} ${date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} ${date.year}"
        title = getString(R.string.tasks_for_date, pretty)

        rows.clear()
        if (dayTasks.isEmpty()) {
            rows.add(getString(R.string.no_tasks))
        } else {
            rows.addAll(dayTasks.map { "• ${it.title}" })
        }
        adapter.notifyDataSetChanged()
    }
}
