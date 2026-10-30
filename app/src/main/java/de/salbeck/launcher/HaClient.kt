package de.salbeck.launcher

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Minimaler Home-Assistant-Client (REST, Bearer-Token). Token wird nur lokal in den App-Einstellungen gespeichert. */
class HaClient(private val ctx: Context) {
    private val prefs get() = ctx.getSharedPreferences("ha", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString("url", "http://192.168.1.3:8123")!!.trimEnd('/')
        set(v) { prefs.edit().putString("url", v.trim()).apply() }

    var token: String
        get() = prefs.getString("token", "")!!
        set(v) { prefs.edit().putString("token", v.trim()).apply() }

    var dashboardPath: String
        get() = prefs.getString("dash", "/zuhause-test/zuhause")!!
        set(v) { prefs.edit().putString("dash", v.trim()).apply() }

    val configured get() = token.isNotEmpty()

    /** Liefert den Zustand einer Entität oder null (Fehler/unbekannt). */
    fun state(entity: String): String? {
        if (!configured) return null
        return try {
            val c = URL("$baseUrl/api/states/$entity").openConnection() as HttpURLConnection
            c.connectTimeout = 4000
            c.readTimeout = 4000
            c.setRequestProperty("Authorization", "Bearer $token")
            if (c.responseCode != 200) return null
            val s = JSONObject(c.inputStream.bufferedReader().readText()).getString("state")
            if (s == "unknown" || s == "unavailable") null else s
        } catch (e: Exception) {
            null
        }
    }
}
