package br.com.cotista.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

class Repository(context: Context) {
    private val dir = File(context.filesDir, "data").apply { mkdirs() }
    private val fnetDir = File(dir, "fnet").apply { mkdirs() }

    class QuotesResult(val date: LocalDate, val quotes: Map<String, Quote>)

    /** Último pregão disponível, com fechamento anterior para variação diária. */
    suspend fun quotes(force: Boolean): QuotesResult = withContext(Dispatchers.IO) {
        val found = ArrayList<Pair<LocalDate, Map<String, Quote>>>()
        var d = LocalDate.now()
        var tries = 0
        while (found.size < 2 && tries < 14) {
            tries++
            if (d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY) {
                val f = File(dir, "cotahist_$d.zip")
                val bytes: ByteArray? = if (f.exists()) f.readBytes() else try {
                    val r = Http.get(B3Cotahist.url(d), readTimeoutMs = 60_000)
                    if (r.ok && Http.isZip(r.body)) { f.writeBytes(r.body); r.body } else null
                } catch (e: Exception) { if (found.isEmpty() && tries > 10) throw e; null }
                if (bytes != null) {
                    val q = B3Cotahist.parse(bytes)
                    if (q.isNotEmpty()) found.add(d to q)
                }
            }
            d = d.minusDays(1)
        }
        // limpeza de arquivos antigos
        dir.listFiles()?.filter { it.name.startsWith("cotahist_") }?.forEach { file ->
            val date = runCatching { LocalDate.parse(file.name.removePrefix("cotahist_").removeSuffix(".zip")) }.getOrNull()
            if (date != null && date.isBefore(LocalDate.now().minusDays(20))) file.delete()
        }
        if (found.isEmpty()) error("Não foi possível baixar as cotações da B3")
        val (date, latest) = found[0]
        val prev = found.getOrNull(1)?.second
        QuotesResult(date, latest.mapValues { (t, q) -> q.copy(prevClose = prev?.get(t)?.price) })
    }

    suspend fun cvm(force: Boolean): CvmInforme.Data = withContext(Dispatchers.IO) {
        val year = LocalDate.now().year
        val zips = listOf(year - 1, year).mapNotNull { y ->
            val maxAge = if (y < year) 30L * 86_400_000 else 2L * 86_400_000
            runCatching { Http.cached(CvmInforme.url(y), File(dir, "cvm_$y.zip"), maxAge, force, Http::isZip) }.getOrNull()
        }
        if (zips.isEmpty()) error("Não foi possível baixar os informes da CVM")
        CvmInforme.parse(zips)
    }

    // ------------------------------------------------------------------ FNET

    private val sem = Semaphore(6)

    private fun docFile(id: Long) = File(fnetDir, "$id.json")

    private fun loadDoc(id: Long): List<Dividend>? {
        val f = docFile(id)
        if (!f.exists()) return null
        return runCatching { fromJson(JSONArray(f.readText())) }.getOrNull()
    }

    private fun fetchDoc(ref: Fnet.DocRef): List<Dividend> {
        loadDoc(ref.id)?.let { return it }
        val r = Http.get(Fnet.docUrl(ref.id), readTimeoutMs = 20_000)
        if (!r.ok) return emptyList()
        val xml = Fnet.decode(r.body) ?: run {
            docFile(ref.id).writeText("[]")
            return emptyList()
        }
        val list = Fnet.parseDoc(xml, ref.id).map { if (it.fundName.isBlank()) it.copy(fundName = ref.fundName) else it }
        docFile(ref.id).writeText(toJson(list).toString())
        return list
    }

    /** Avisos mais recentes de todos os FIIs. [onProgress] recebe (feitos, total). */
    suspend fun recentAnnouncements(
        count: Int,
        force: Boolean,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): List<Dividend> = withContext(Dispatchers.IO) {
        val refs = listRefs(null, count, if (force) 0 else 20 * 60_000L)
        fetchAll(refs, onProgress)
    }

    /** Histórico de avisos de um fundo específico (por CNPJ). */
    suspend fun fundAnnouncements(cnpj: String, count: Int = 30, force: Boolean = false): List<Dividend> =
        withContext(Dispatchers.IO) {
            val refs = listRefs(cnpj, count, if (force) 0 else 6 * 3_600_000L)
            fetchAll(refs) { _, _ -> }
        }

    private fun listRefs(cnpj: String?, count: Int, maxAge: Long): List<Fnet.DocRef> {
        val out = ArrayList<Fnet.DocRef>()
        val page = 100
        var start = 0
        while (out.size < count) {
            val len = minOf(page, count - out.size)
            val cacheFile = File(dir, "fnetlist_${cnpj ?: "all"}_$start.json")
            val json: String? = try {
                val fresh = cacheFile.exists() && System.currentTimeMillis() - cacheFile.lastModified() < maxAge
                if (fresh) cacheFile.readText() else {
                    val r = Http.get(Fnet.listUrl(start, len, cnpj), Fnet.headers, 30_000)
                    if (!r.ok) error("FNET ${r.code}")
                    r.text().also { cacheFile.writeText(it) }
                }
            } catch (e: Exception) {
                if (cacheFile.exists()) cacheFile.readText() else if (out.isEmpty()) throw e else null
            }
            if (json == null) break
            val refs = Fnet.parseList(json)
            out.addAll(refs)
            if (refs.size < len) break
            start += len
        }
        return out.distinctBy { it.id }
    }

    private suspend fun fetchAll(refs: List<Fnet.DocRef>, onProgress: (Int, Int) -> Unit): List<Dividend> = coroutineScope {
        var done = 0
        val lock = Any()
        refs.map { ref ->
            async {
                val r = sem.withPermit { runCatching { fetchDoc(ref) }.getOrDefault(emptyList()) }
                synchronized(lock) { done++; if (done % 10 == 0 || done == refs.size) onProgress(done, refs.size) }
                r
            }
        }.awaitAll().flatten()
    }

    private fun toJson(list: List<Dividend>) = JSONArray().apply {
        list.forEach { d ->
            put(JSONObject().apply {
                put("t", d.ticker); put("v", d.value); put("k", d.kind); put("n", d.fundName)
                d.comDate?.let { put("c", it.toString()) }
                d.payDate?.let { put("p", it.toString()) }
                d.docId?.let { put("id", it) }
            })
        }
    }

    private fun fromJson(a: JSONArray): List<Dividend> = (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        Dividend(
            ticker = o.getString("t"), value = o.getDouble("v"),
            comDate = o.optString("c").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
            payDate = o.optString("p").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
            kind = o.optString("k", "Rendimento"), estimated = false,
            docId = o.optLong("id").takeIf { it > 0 }, fundName = o.optString("n"),
        )
    }
}
