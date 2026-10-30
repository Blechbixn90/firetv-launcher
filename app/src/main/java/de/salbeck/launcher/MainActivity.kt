package de.salbeck.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private data class App(val name: String, val pkg: String)

    private val apps = listOf(
        App("Plex", "com.plexapp.android"),
        App("Prime Video", "com.amazon.firebat"),
        App("Netflix", "com.netflix.ninja"),
        App("Disney+", "com.disney.disneyplus"),
        App("YouTube", "org.smarttube.stable"),
        App("Browser", "com.amazon.cloud9")
    )

    private val ui = Handler(Looper.getMainLooper())
    private lateinit var ha: HaClient
    private lateinit var chipRow: LinearLayout
    @Volatile private var running = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ha = HaClient(this)
        setContentView(build())
    }

    override fun onResume() {
        super.onResume()
        running = true
        Thread {
            while (running) {
                refresh()
                try { Thread.sleep(60_000) } catch (_: InterruptedException) { return@Thread }
            }
        }.start()
    }

    override fun onPause() {
        running = false
        super.onPause()
    }

    // Home-Taste im Launcher selbst: nichts tun (wir sind schon zu Hause).
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return true // Launcher nicht verlassen
        return super.onKeyDown(keyCode, event)
    }

    private fun text(s: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun build(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(32), dp(48), dp(28))
        }

        // Kopfzeile: Uhr links, Info-Chips rechts
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val clockCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        clockCol.addView(TextClock(this).apply {
            format24Hour = "HH:mm"; format12Hour = "HH:mm"
            textSize = 56f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD)
        })
        clockCol.addView(TextClock(this).apply {
            format24Hour = "EEEE, d. MMMM"; format12Hour = "EEEE, d. MMMM"
            textSize = 18f; setTextColor(0xFFBCC6D3.toInt())
        })
        header.addView(clockCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        chipRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        header.addView(chipRow)
        root.addView(header)

        // Spacer
        root.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))

        // App-Kacheln
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        apps.forEach { a -> row.addView(appTile(a), LinearLayout.LayoutParams(0, dp(150), 1f).apply { setMargins(dp(7), 0, dp(7), 0) }) }
        root.addView(row)

        // Untere Reihe: Dashboard + Einstellungen
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(
            tile("Zuhause-Dashboard", "Heizung, Kamera, Energie") { startActivity(Intent(this, DashboardActivity::class.java)) },
            LinearLayout.LayoutParams(0, dp(84), 3f).apply { setMargins(dp(7), dp(14), dp(7), 0) }
        )
        bottom.addView(
            tile("Einstellungen", "Home Assistant verbinden") { settings() },
            LinearLayout.LayoutParams(0, dp(84), 1.2f).apply { setMargins(dp(7), dp(14), dp(7), 0) }
        )
        root.addView(bottom)

        root.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        return FrameLayout(this).apply { addView(root) }
    }

    private fun focusable(v: View, onClick: () -> Unit): View {
        v.isFocusable = true
        v.isClickable = true
        v.background = resources.getDrawable(R.drawable.tile_bg, theme)
        v.setOnClickListener { onClick() }
        v.setOnFocusChangeListener { view, has ->
            view.animate().scaleX(if (has) 1.07f else 1f).scaleY(if (has) 1.07f else 1f).setDuration(140).start()
            view.elevation = if (has) dp(10).toFloat() else 0f
        }
        return v
    }

    private fun appTile(a: App): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(8), dp(10), dp(8), dp(10)) }
        val icon = ImageView(this).apply {
            try { setImageDrawable(packageManager.getApplicationIcon(a.pkg)) } catch (_: Exception) { alpha = 0.25f; setImageResource(R.drawable.ic_launcher) }
        }
        col.addView(icon, LinearLayout.LayoutParams(dp(64), dp(64)))
        col.addView(text(a.name, 15f, Color.WHITE, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) })
        return focusable(col) { launch(a) }
    }

    private fun tile(title: String, sub: String, onClick: () -> Unit): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(8), dp(18), dp(8)) }
        col.addView(text(title, 17f, Color.WHITE, true))
        col.addView(text(sub, 13f, 0xFFBCC6D3.toInt()))
        return focusable(col, onClick)
    }

    private fun launch(a: App) {
        val i = packageManager.getLeanbackLaunchIntentForPackage(a.pkg) ?: packageManager.getLaunchIntentForPackage(a.pkg)
        if (i == null) Toast.makeText(this, "${a.name} ist nicht installiert", Toast.LENGTH_LONG).show()
        else startActivity(i)
    }

    private fun settings() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(12), dp(24), 0) }
        val url = EditText(this).apply { setText(ha.baseUrl); hint = "http://192.168.1.3:8123"; inputType = InputType.TYPE_TEXT_VARIATION_URI }
        val tok = EditText(this).apply { hint = "Langlebiger Zugriffstoken"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val dash = EditText(this).apply { setText(ha.dashboardPath); hint = "/zuhause-test/zuhause"; inputType = InputType.TYPE_TEXT_VARIATION_URI }
        box.addView(url); box.addView(tok); box.addView(dash)
        AlertDialog.Builder(this)
            .setTitle("Home Assistant")
            .setMessage("Token in HA: Profil → Sicherheit → Langlebige Zugriffstoken. Leer lassen = Token behalten.")
            .setView(box)
            .setPositiveButton("Speichern") { _, _ ->
                ha.baseUrl = url.text.toString()
                ha.dashboardPath = dash.text.toString()
                if (tok.text.isNotBlank()) ha.token = tok.text.toString()
                Thread { refresh() }.start()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    // ---- Home-Assistant-Infos ----

    private fun chip(label: String, value: String): View {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(7), dp(14), dp(7)); setBackgroundResource(R.drawable.chip_bg) }
        c.addView(text(label, 11f, 0xFFBCC6D3.toInt()))
        c.addView(text(value, 17f, Color.WHITE, true))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.leftMargin = dp(10)
        c.layoutParams = lp
        return c
    }

    private fun refresh() {
        if (!ha.configured) {
            ui.post { chipRow.removeAllViews(); chipRow.addView(chip("Home Assistant", "Einstellungen öffnen")) }
            return
        }
        val out = ha.state("sensor.aussentemperatur")?.toDoubleOrNull()?.let { "%.1f °C".format(it) } ?: "–"
        val ist = ha.state("sensor.temperatur_esszimmer_temperature")?.toDoubleOrNull()?.let { "%.1f °C".format(it) } ?: "–"
        val fenster = ha.state("sensor.offene_fenster") ?: "–"
        val lichter = ha.state("sensor.angeschaltete_lichter") ?: "–"
        val muell = ha.state("input_text.nachster_mull_ab_mittag")?.replace(" · ", " am ") ?: "–"
        ui.post {
            chipRow.removeAllViews()
            chipRow.addView(chip("Außen", out))
            chipRow.addView(chip("Esszimmer", ist))
            chipRow.addView(chip("Fenster offen", fenster))
            chipRow.addView(chip("Lichter an", lichter))
            chipRow.addView(chip("Nächster Müll", muell))
        }
    }
}
