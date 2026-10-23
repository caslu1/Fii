package br.com.cotista.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PortfolioStore(context: Context) {
    private val prefs = context.getSharedPreferences("cotista", Context.MODE_PRIVATE)

    fun load(): List<Position> = runCatching {
        val a = JSONArray(prefs.getString("positions", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Position(o.getString("t"), o.getDouble("q"), o.getDouble("p"))
        }
    }.getOrDefault(emptyList())

    fun save(list: List<Position>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("t", it.ticker).put("q", it.quantity).put("p", it.avgPrice)) }
        prefs.edit().putString("positions", a.toString()).apply()
    }

    var hideValues: Boolean
        get() = prefs.getBoolean("hide", false)
        set(v) { prefs.edit().putBoolean("hide", v).apply() }
}
