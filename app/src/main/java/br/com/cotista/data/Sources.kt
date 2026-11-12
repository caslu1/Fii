package br.com.cotista.data

import android.util.Xml
import br.com.cotista.util.parseDecimal
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

/**
 * B3 — arquivo oficial de cotações históricas (COTAHIST), layout de largura fixa.
 * https://bvmf.bmfbovespa.com.br/InstDados/SerHist/COTAHIST_DddMMyyyy.ZIP
 */
object B3Cotahist {
    private val fmt = DateTimeFormatter.ofPattern("ddMMyyyy")
    fun url(d: LocalDate) = "https://bvmf.bmfbovespa.com.br/InstDados/SerHist/COTAHIST_D${d.format(fmt)}.ZIP"

    /** Retorna as cotações de FIIs (mercado à vista) por ticker. */
    fun parse(zip: ByteArray): Map<String, Quote> {
        val out = HashMap<String, Quote>()
        ZipInputStream(ByteArrayInputStream(zip)).use { zis ->
            val entry = zis.nextEntry ?: return out
            if (entry.isDirectory) return out
            val reader = BufferedReader(InputStreamReader(zis, Charsets.ISO_8859_1))
            while (true) {
                val l = reader.readLine() ?: break
                if (l.length < 245 || !l.startsWith("01")) continue
                if (l.substring(10, 12) != "12") continue          // CODBDI 12 = fundos imobiliários
                if (l.substring(24, 27) != "010") continue         // mercado à vista
                val ticker = l.substring(12, 24).trim()
                if (!ticker.endsWith("11")) continue
                fun p(a: Int, b: Int) = (l.substring(a, b).trim().toLongOrNull() ?: 0L) / 100.0
                out[ticker] = Quote(
                    ticker = ticker,
                    isin = l.substring(230, 242).trim(),
                    shortName = l.substring(27, 39).trim().removePrefix("FII ").trim(),
                    price = p(108, 121),
                    open = p(56, 69),
                    high = p(69, 82),
                    low = p(82, 95),
                    prevClose = null,
                    volume = p(170, 188),
                    trades = l.substring(147, 152).trim().toIntOrNull() ?: 0,
                )
            }
        }
        return out
    }
}

/**
 * CVM — Dados Abertos: informe mensal estruturado de FII.
 * https://dados.cvm.gov.br/dados/FII/DOC/INF_MENSAL/DADOS/inf_mensal_fii_AAAA.zip
 */
object CvmInforme {
    fun url(year: Int) = "https://dados.cvm.gov.br/dados/FII/DOC/INF_MENSAL/DADOS/inf_mensal_fii_$year.zip"

    class Data(
        val latest: Map<String, CvmFund>,          // por CNPJ (só dígitos)
        val history: Map<String, List<CvmPoint>>,  // por CNPJ
        val isinToCnpj: Map<String, String>,
        val balances: Map<String, List<Balance>> = emptyMap(),
    ) {
        val refDate: LocalDate? get() = latest.values.maxOfOrNull { it.refDate }
    }

    private class Geral(val ref: LocalDate, val ver: Int, val name: String, val isin: String, val segment: String,
                        val mandate: String, val mgmt: String, val admin: String, val shares: Double?)
    private class Compl(val ref: LocalDate, val ver: Int, val vp: Double?, val dy: Double?, val holders: Int?,
                        val pl: Double?, val shares: Double?)

    private val COMPL_COLS = listOf(
        "Valor_Ativo", "Patrimonio_Liquido", "Cotas_Emitidas", "Valor_Patrimonial_Cotas",
        "Percentual_Despesas_Taxa_Administracao", "Percentual_Rentabilidade_Efetiva_Mes",
        "Percentual_Rentabilidade_Patrimonial_Mes", "Percentual_Dividend_Yield_Mes",
        "Percentual_Amortizacao_Cotas_Mes", "Total_Numero_Cotistas", "Numero_Cotistas_Pessoa_Fisica",
    )
    private val AP_COLS = listOf(
        "Total_Necessidades_Liquidez", "Total_Investido", "Direitos_Bens_Imoveis", "Terrenos",
        "Imoveis_Renda_Acabados", "Imoveis_Renda_Construcao", "Imoveis_Venda_Acabados", "Imoveis_Venda_Construcao",
        "Outros_Direitos_Reais", "Acoes", "Acoes_Sociedades_Atividades_FII", "Cotas_Sociedades_Atividades_FII",
        "FII", "CRI", "CRI_CRA", "LCI", "LCI_LCA", "LIG", "Letras_Hipotecarias", "Debentures", "Fundo_Acoes", "FIP",
        "FDIC", "Outras_Cotas_FI", "Notas_Promissorias", "CEPAC", "Outros_Valores_Mobliarios",
        "Valores_Receber", "Contas_Receber_Aluguel", "Rendimentos_Distribuir", "Taxa_Administracao_Pagar",
        "Obrigacoes_Aquisicao_Imoveis", "Obrigacoes_Securitizacao_Recebiveis", "Provisoes_Contigencias", "Total_Passivo",
    )

    private class Row(val ver: Int, val v: DoubleArray)

    fun parse(zips: List<ByteArray>): Data {
        // chave: cnpj|ref -> registro de maior versão
        val geral = HashMap<String, Geral>()
        val compl = HashMap<String, Compl>()
        val complRows = HashMap<String, Row>()
        val apRows = HashMap<String, Row>()
        val cnpjByKey = HashMap<String, String>()
        for (zip in zips) {
            ZipInputStream(ByteArrayInputStream(zip)).use { zis ->
                while (true) {
                    val e = zis.nextEntry ?: break
                    val name = e.name.lowercase()
                    val isGeral = name.contains("geral")
                    val isCompl = name.contains("complemento")
                    val isAp = name.contains("ativo_passivo")
                    if (!isGeral && !isCompl && !isAp) continue
                    val reader = BufferedReader(InputStreamReader(zis, Charsets.ISO_8859_1))
                    val header = reader.readLine()?.split(';')?.map { it.trim().trim('"') } ?: continue
                    fun idx(vararg names: String): Int {
                        for (n in names) { val i = header.indexOfFirst { it.equals(n, true) }; if (i >= 0) return i }
                        for (n in names) { val i = header.indexOfFirst { it.startsWith(n, true) }; if (i >= 0) return i }
                        return -1
                    }
                    val iCnpj = idx("CNPJ_Fundo_Classe", "CNPJ_Fundo")
                    val iRef = idx("Data_Referencia")
                    val iVer = idx("Versao")
                    if (iCnpj < 0 || iRef < 0) continue
                    fun exact(n: String) = header.indexOfFirst { it.equals(n, true) }
                    if (isAp) {
                        val cols = AP_COLS.map { exact(it) }
                        while (true) {
                            val line = reader.readLine() ?: break
                            val c = splitCsv(line)
                            val cnpj = c.getOrNull(iCnpj)?.filter { it.isDigit() } ?: continue
                            val ref = parseDate(c.getOrNull(iRef)) ?: continue
                            val ver = c.getOrNull(iVer)?.trim()?.toIntOrNull() ?: 1
                            val key = "$cnpj|$ref"
                            val old = apRows[key]
                            if (old != null && old.ver > ver) continue
                            apRows[key] = Row(ver, DoubleArray(cols.size) { j -> if (cols[j] < 0) Double.NaN else parseDecimal(c.getOrNull(cols[j])) ?: Double.NaN })
                            cnpjByKey[key] = cnpj
                        }
                        continue
                    }
                    if (isGeral) {
                        val iName = idx("Nome_Fundo_Classe", "Nome_Fundo")
                        val iIsin = idx("Codigo_ISIN")
                        val iSeg = idx("Segmento_Atuacao")
                        val iMand = idx("Mandato")
                        val iMgmt = idx("Tipo_Gestao")
                        val iAdm = idx("Nome_Administrador", "Nome_Administradora")
                        val iSh = idx("Quantidade_Cotas_Emitidas")
                        while (true) {
                            val line = reader.readLine() ?: break
                            val c = splitCsv(line)
                            val cnpj = c.getOrNull(iCnpj)?.filter { it.isDigit() } ?: continue
                            val ref = parseDate(c.getOrNull(iRef)) ?: continue
                            val ver = c.getOrNull(iVer)?.trim()?.toIntOrNull() ?: 1
                            val key = "$cnpj|$ref"
                            val old = geral[key]
                            if (old != null && old.ver > ver) continue
                            geral[key] = Geral(ref, ver, c.getOrNull(iName).orEmpty().trim(),
                                c.getOrNull(iIsin).orEmpty().trim().uppercase(), c.getOrNull(iSeg).orEmpty().trim(),
                                c.getOrNull(iMand).orEmpty().trim(), c.getOrNull(iMgmt).orEmpty().trim(),
                                c.getOrNull(iAdm).orEmpty().trim(), parseDecimal(c.getOrNull(iSh)))
                            cnpjByKey[key] = cnpj
                        }
                    } else {
                        val iVp = idx("Valor_Patrimonial_Cotas")
                        val iDy = idx("Percentual_Dividend_Yield_Mes")
                        val iHold = idx("Total_Numero_Cotistas")
                        val iPl = idx("Patrimonio_Liquido")
                        val iSh = idx("Cotas_Emitidas")
                        val cols = COMPL_COLS.map { exact(it) }
                        while (true) {
                            val line = reader.readLine() ?: break
                            val c = splitCsv(line)
                            val cnpj = c.getOrNull(iCnpj)?.filter { it.isDigit() } ?: continue
                            val ref = parseDate(c.getOrNull(iRef)) ?: continue
                            val ver = c.getOrNull(iVer)?.trim()?.toIntOrNull() ?: 1
                            val key = "$cnpj|$ref"
                            val old = compl[key]
                            if (old != null && old.ver > ver) continue
                            compl[key] = Compl(ref, ver, parseDecimal(c.getOrNull(iVp)), parseDecimal(c.getOrNull(iDy)),
                                parseDecimal(c.getOrNull(iHold))?.toInt(), parseDecimal(c.getOrNull(iPl)),
                                parseDecimal(c.getOrNull(iSh)))
                            complRows[key] = Row(ver, DoubleArray(cols.size) { j -> if (cols[j] < 0) Double.NaN else parseDecimal(c.getOrNull(cols[j])) ?: Double.NaN })
                            cnpjByKey[key] = cnpj
                        }
                    }
                }
            }
        }
        val history = HashMap<String, MutableList<CvmPoint>>()
        for ((key, cp) in compl) {
            val cnpj = cnpjByKey[key] ?: continue
            history.getOrPut(cnpj) { mutableListOf() }.add(CvmPoint(cp.ref, cp.dy, cp.vp))
        }
        history.values.forEach { it.sortBy { p -> p.ref } }

        val latest = HashMap<String, CvmFund>()
        val isinToCnpj = HashMap<String, String>()
        val geralByCnpj = HashMap<String, Geral>()
        for ((key, g) in geral) {
            val cnpj = cnpjByKey[key] ?: continue
            val old = geralByCnpj[cnpj]
            if (old == null || g.ref.isAfter(old.ref)) geralByCnpj[cnpj] = g
        }
        val complByCnpj = HashMap<String, Compl>()
        for ((key, cp) in compl) {
            val cnpj = cnpjByKey[key] ?: continue
            val old = complByCnpj[cnpj]
            if (old == null || cp.ref.isAfter(old.ref)) complByCnpj[cnpj] = cp
        }
        for ((cnpj, g) in geralByCnpj) {
            val cp = complByCnpj[cnpj]
            latest[cnpj] = CvmFund(
                cnpj = cnpj, isin = g.isin, name = g.name, segment = g.segment, mandate = g.mandate,
                management = g.mgmt, admin = g.admin, refDate = cp?.ref ?: g.ref, vpCota = cp?.vp,
                dyMonth = cp?.dy, holders = cp?.holders, netWorth = cp?.pl, shares = cp?.shares ?: g.shares,
            )
            if (g.isin.length == 12) isinToCnpj[g.isin] = cnpj
        }
        val balances = HashMap<String, MutableList<Balance>>()
        for (key in complRows.keys + apRows.keys) {
            val cnpj = cnpjByKey[key] ?: continue
            val ref = LocalDate.parse(key.substringAfter('|'))
            if (balances[cnpj]?.any { it.ref == ref } == true) continue
            val cr = complRows[key]?.v
            val ar = apRows[key]?.v
            fun c(n: String): Double? = cr?.get(COMPL_COLS.indexOf(n))?.takeIf { !it.isNaN() }
            fun a(n: String): Double = ar?.get(AP_COLS.indexOf(n))?.takeIf { !it.isNaN() } ?: 0.0
            val listed = a("Acoes") + a("Acoes_Sociedades_Atividades_FII") + a("Cotas_Sociedades_Atividades_FII")
            val otherSec = a("Debentures") + a("Fundo_Acoes") + a("FIP") + a("FDIC") + a("Outras_Cotas_FI") +
                a("Notas_Promissorias") + a("CEPAC") + a("Outros_Valores_Mobliarios") + a("Outros_Direitos_Reais")
            balances.getOrPut(cnpj) { mutableListOf() }.add(
                Balance(
                    ref = ref,
                    totalAssets = c("Valor_Ativo"), netWorth = c("Patrimonio_Liquido"), shares = c("Cotas_Emitidas"),
                    vpCota = c("Valor_Patrimonial_Cotas"), adminFee = c("Percentual_Despesas_Taxa_Administracao"),
                    returnEffective = c("Percentual_Rentabilidade_Efetiva_Mes"),
                    returnPatrimonial = c("Percentual_Rentabilidade_Patrimonial_Mes"),
                    dy = c("Percentual_Dividend_Yield_Mes"), amortization = c("Percentual_Amortizacao_Cotas_Mes"),
                    holders = c("Total_Numero_Cotistas")?.toInt(), holdersPF = c("Numero_Cotistas_Pessoa_Fisica")?.toInt(),
                    liquidity = a("Total_Necessidades_Liquidez"),
                    realEstate = a("Direitos_Bens_Imoveis"),
                    realEstateIncome = a("Imoveis_Renda_Acabados"),
                    realEstateConstruction = a("Imoveis_Renda_Construcao"),
                    realEstateForSale = a("Imoveis_Venda_Acabados") + a("Imoveis_Venda_Construcao"),
                    land = a("Terrenos"),
                    cri = a("CRI") + a("CRI_CRA"),
                    lci = a("LCI") + a("LCI_LCA") + a("LIG") + a("Letras_Hipotecarias"),
                    fiiShares = a("FII"),
                    companies = listed,
                    otherSecurities = otherSec,
                    receivables = a("Valores_Receber"),
                    rentReceivable = a("Contas_Receber_Aluguel"),
                    liabilities = a("Total_Passivo"),
                    dividendsPayable = a("Rendimentos_Distribuir"),
                    adminFeePayable = a("Taxa_Administracao_Pagar"),
                    propertyObligations = a("Obrigacoes_Aquisicao_Imoveis"),
                    securitization = a("Obrigacoes_Securitizacao_Recebiveis"),
                    provisions = a("Provisoes_Contigencias"),
                ),
            )
        }
        balances.values.forEach { it.sortBy { b -> b.ref } }
        return Data(latest, history, isinToCnpj, balances)
    }

    private fun parseDate(s: String?): LocalDate? = try {
        val t = s?.trim()?.trim('"') ?: return null
        if (t.contains('/')) LocalDate.parse(t, DateTimeFormatter.ofPattern("dd/MM/yyyy")) else LocalDate.parse(t.take(10))
    } catch (_: Exception) { null }

    private fun splitCsv(line: String): List<String> {
        if (!line.contains('"')) return line.split(';')
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var q = false
        for (ch in line) {
            when {
                ch == '"' -> q = !q
                ch == ';' && !q -> { out.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(ch)
            }
        }
        out.add(sb.toString())
        return out
    }
}

/**
 * B3 FNET — avisos estruturados "Rendimentos e Amortizações" publicados pelos administradores.
 */
object Fnet {
    private const val BASE = "https://fnet.bmfbovespa.com.br/fnet/publico"

    data class DocRef(val id: Long, val fundName: String, val delivered: String, val reference: String = "")

    /** Categoria 14/tipo 41 = avisos de rendimentos; categoria 7/tipo 9 = relatório gerencial. */
    fun listUrl(start: Int, length: Int, cnpj: String? = null, category: Int = 14, type: Int = 41): String {
        val sb = StringBuilder("$BASE/pesquisarGerenciadorDocumentosDados?d=0&s=$start&l=$length")
        sb.append("&o%5B0%5D%5BdataEntrega%5D=desc&tipoFundo=1&idCategoriaDocumento=$category&idTipoDocumento=$type&idEspecieDocumento=0")
        if (cnpj != null) sb.append("&cnpjFundo=").append(cnpj)
        return sb.toString()
    }

    fun docUrl(id: Long) = "$BASE/downloadDocumento?id=$id"

    val headers = mapOf("X-Requested-With" to "XMLHttpRequest", "Accept" to "application/json, text/javascript, */*")

    fun parseList(json: String): List<DocRef> {
        val o = JSONObject(json)
        val arr = o.optJSONArray("data") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val d = arr.getJSONObject(i)
            val id = d.optLong("id", -1)
            if (id <= 0) null else DocRef(id, d.optString("descricaoFundo"), d.optString("dataEntrega"), d.optString("dataReferencia"))
        }
    }

    fun decode(body: ByteArray): String? {
        var s = String(body, Charsets.UTF_8).trim()
        if (!s.startsWith("<")) {
            s = try { String(android.util.Base64.decode(s, android.util.Base64.DEFAULT), Charsets.UTF_8).trim() } catch (_: Exception) { return null }
        }
        return if (s.contains("<DadosEconomicoFinanceiros")) s else null
    }

    /** Converte o XML do aviso em proventos (um por <Provento>/<Rendimento|Amortizacao>). */
    fun parseDoc(xml: String, docId: Long): List<Dividend> {
        val out = ArrayList<Dividend>()
        val p = Xml.newPullParser()
        p.setInput(StringReader(xml))
        var fundName = ""
        var ticker = ""
        var isin = ""
        var kind: String? = null
        var comDate: LocalDate? = null
        var payDate: LocalDate? = null
        var value: Double? = null
        val text = StringBuilder()
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> {
                    text.setLength(0)
                    when (p.name) {
                        "Provento" -> { ticker = ""; isin = "" }
                        "Rendimento" -> { kind = "Rendimento"; comDate = null; payDate = null; value = null }
                        "Amortizacao" -> { kind = "Amortização"; comDate = null; payDate = null; value = null }
                    }
                }
                XmlPullParser.TEXT -> text.append(p.text)
                XmlPullParser.END_TAG -> {
                    val t = text.toString().trim()
                    when (p.name) {
                        "NomeFundo" -> fundName = t
                        "CodNegociacao", "CodNegociacaoCota" -> if (t.isNotEmpty()) ticker = t.uppercase()
                        "CodISIN", "CodigoISIN" -> if (t.isNotEmpty()) isin = t.uppercase()
                        "DataBase" -> comDate = date(t)
                        "DataPagamento" -> payDate = date(t)
                        "ValorProvento", "ValorProventoCota" -> value = parseDecimal(t)
                        "Rendimento", "Amortizacao" -> {
                            val v = value
                            val tk = ticker.ifEmpty { isin }
                            if (kind != null && v != null && v > 0 && tk.isNotEmpty()) {
                                out.add(Dividend(tk, v, comDate, payDate, kind!!, false, docId, fundName))
                            }
                            kind = null
                        }
                    }
                    text.setLength(0)
                }
            }
            ev = p.next()
        }
        return out
    }

    private fun date(s: String): LocalDate? = try {
        if (s.contains('/')) LocalDate.parse(s, DateTimeFormatter.ofPattern("dd/MM/yyyy")) else LocalDate.parse(s.take(10))
    } catch (_: Exception) { null }
}
