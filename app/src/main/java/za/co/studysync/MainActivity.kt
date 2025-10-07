package za.co.studysync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.co.studysync.data.Api
import za.co.studysync.data.CreateTask
import za.co.studysync.data.TaskDto
import za.co.studysync.data.UpdateTask
import za.co.studysync.i18n.LocaleManager

// NEW: Tips
import android.content.SharedPreferences
import za.co.studysync.feature.tips.TipsRepo

// NEW: Local due-date store (STEP 2)
import za.co.studysync.data.LocalDueStore

// NEW: Theme
import za.co.studysync.ui.ThemeManager

// Existing: java.time for ISO handling
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        val ctx = LocaleManager.applyLocale(newBase, lang)
        super.attachBaseContext(ctx)
    }

    private lateinit var list: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val rows = mutableListOf<String>()
    private val tasks = mutableListOf<TaskDto>()

    // --- Tip state (remember current tip across launches) ---
    private lateinit var prefs: SharedPreferences
    private var tipIndex: Int
        get() = prefs.getInt("tip_index", 0)
        set(v) { prefs.edit().putInt("tip_index", v).apply() }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Apply saved theme BEFORE inflating any views
        ThemeManager.apply(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("studysync_prefs", Context.MODE_PRIVATE)

        // Simple title-only toolbar (no menu)
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.title = getString(R.string.app_name)

        list = findViewById(R.id.listView)

        // ---------- TIP HEADER ----------
        val header = LayoutInflater.from(this).inflate(R.layout.include_tip_card, list, false)
        val tvTip = header.findViewById<TextView>(R.id.tipText)
        val btnNext = header.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNextTip)
        val btnShare = header.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShareTip)

        tvTip.text = currentTip()

        btnNext.setOnClickListener {
            tipIndex = (tipIndex + 1) % TipsRepo.tips.size
            tvTip.text = currentTip()
        }
        btnShare.setOnClickListener { shareText(currentTip()) }

        // Attach header BEFORE setting adapter
        list.addHeaderView(header, null, false)

        // ---------- Tasks list ----------
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
        list.adapter = adapter

        // Add Task FAB (bottom-right)
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener { showAddDialog() }

        // Settings FAB (bottom-left)
        findViewById<FloatingActionButton>(R.id.fabSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Calendar FAB (bottom-center)
        findViewById<FloatingActionButton>(R.id.fabCalendar).setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        // Toggle done/open on tap (adjust index because of header)
        list.setOnItemClickListener { _, _, position, _ ->
            val dataIndex = position - list.headerViewsCount
            if (dataIndex < 0 || dataIndex >= tasks.size) return@setOnItemClickListener
            val t = tasks[dataIndex]
            val newStatus = if (t.status == "open") "done" else "open"
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) { Api.svc.updateTask(t.id, UpdateTask(status = newStatus)) }
                    refreshTasks()
                } catch (e: Exception) {
                    showError(e)
                }
            }
        }

        // Delete on long-press (adjust index because of header)
        list.setOnItemLongClickListener { _, _, position, _ ->
            val dataIndex = position - list.headerViewsCount
            if (dataIndex < 0 || dataIndex >= tasks.size) return@setOnItemLongClickListener true

            val t = tasks[dataIndex]
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_task))
                .setMessage(getString(R.string.delete_confirm, t.title))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) { Api.svc.deleteTask(t.id) }
                            // also remove local due date on delete
                            LocalDueStore.removeDue(this@MainActivity, t.id)
                            refreshTasks()
                        } catch (e: Exception) { showError(e) }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }

        refreshTasks()
    }

    // -------- Tips helpers --------
    private fun currentTip(): String = TipsRepo.tips[tipIndex % TipsRepo.tips.size]

    private fun shareText(text: String) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(i, getString(R.string.btn_share_tip)))
    }

    // --- Add Task with optional Due Date (local fallback) ---
    private fun showAddDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.task_title_hint)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.new_task))
            .setView(input)
            .setPositiveButton(getString(R.string.next)) { _, _ ->
                val title = input.text.toString().trim()
                if (title.isEmpty()) return@setPositiveButton

                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_due_date))
                    .setMessage(getString(R.string.add_due_date_prompt))
                    .setPositiveButton(getString(R.string.yes)) { _, _ ->
                        pickDateTime { isoUtc ->
                            createTask(title, isoUtc)
                        }
                    }
                    .setNegativeButton(getString(R.string.no)) { _, _ ->
                        createTask(title, null)
                    }
                    .show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun createTask(title: String, dueIso: String?) {
        lifecycleScope.launch {
            try {
                val created = withContext(Dispatchers.IO) {
                    Api.svc.createTask(CreateTask(title = title, dueDateTime = dueIso))
                }
                if (!dueIso.isNullOrBlank()) {
                    LocalDueStore.setDue(this@MainActivity, created.id, dueIso)
                }
                refreshTasks()
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    private fun pickDateTime(onPicked: (String) -> Unit) {
        val now = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                android.app.TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val iso = toIsoUtc(year, month + 1, day, hour, minute)
                        onPicked(iso)
                    },
                    now.get(java.util.Calendar.HOUR_OF_DAY),
                    now.get(java.util.Calendar.MINUTE),
                    true
                ).show()
            },
            now.get(java.util.Calendar.YEAR),
            now.get(java.util.Calendar.MONTH),
            now.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun toIsoUtc(year: Int, month: Int, day: Int, hour: Int, minute: Int): String {
        val dt = LocalDateTime.of(year, month, day, hour, minute)
        val z = dt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC)
        return z.toInstant().toString()
    }

    private fun refreshTasks() {
        lifecycleScope.launch {
            try {
                val data = withContext(Dispatchers.IO) { Api.svc.getTasks() }
                tasks.clear(); tasks.addAll(data)
                rows.clear()
                rows.addAll(data.map { t ->
                    val dueIso = t.dueDateTime ?: LocalDueStore.getDue(this@MainActivity, t.id)
                    val due = dueIso?.let { formatDue(it) } ?: getString(R.string.no_due)
                    "• ${t.title}  —  $due  [${t.status}]"
                })
                adapter.notifyDataSetChanged()
            } catch (e: Exception) { showError(e) }
        }
    }

    private fun formatDue(iso: String): String = try {
        val inst = Instant.parse(iso)
        val zdt = inst.atZone(ZoneId.systemDefault())
        val d = zdt.toLocalDate()
        val t = zdt.toLocalTime().withSecond(0).withNano(0)
        "${d.dayOfMonth} ${d.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)}, $t"
    } catch (_: Exception) { iso }

    private fun showError(e: Exception) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.error))
            .setMessage(e.localizedMessage ?: "Unknown error")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
