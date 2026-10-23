package br.com.cotista.data

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object Http {
    private const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    class Response(val code: Int, val body: ByteArray) {
        val ok get() = code in 200..299
        fun text(charset: String = "UTF-8") = String(body, charset(charset))
    }

    fun get(url: String, headers: Map<String, String> = emptyMap(), readTimeoutMs: Int = 45_000): Response {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = readTimeoutMs
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", UA)
        c.setRequestProperty("Accept", "*/*")
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        return try {
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            Response(code, stream?.use { it.readBytes() } ?: ByteArray(0))
        } finally {
            c.disconnect()
        }
    }

    /** Baixa com cache em disco. Retorna o conteúdo em cache se ainda estiver dentro de [maxAgeMs]. */
    fun cached(
        url: String,
        file: File,
        maxAgeMs: Long,
        force: Boolean = false,
        validate: (ByteArray) -> Boolean = { it.isNotEmpty() },
    ): ByteArray? {
        val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < maxAgeMs
        if (fresh && !force) return file.readBytes()
        return try {
            val r = get(url)
            if (r.ok && validate(r.body)) {
                file.parentFile?.mkdirs()
                val tmp = File(file.path + ".tmp")
                tmp.writeBytes(r.body)
                tmp.renameTo(file)
                r.body
            } else if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            if (file.exists()) file.readBytes() else throw e
        }
    }

    fun isZip(b: ByteArray) = b.size > 4 && b[0] == 'P'.code.toByte() && b[1] == 'K'.code.toByte()
}
