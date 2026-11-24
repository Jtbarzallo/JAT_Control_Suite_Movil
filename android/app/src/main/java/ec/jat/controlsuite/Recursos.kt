package ec.jat.controlsuite

import android.content.Context
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

/**
 * Entrega a la pantalla (WebView) los archivos que van dentro de la app (carpeta assets/www),
 * como si vinieran de internet. Así todo funciona sin conexión y con "origen seguro" (cámara, almacenamiento).
 */
object Recursos {
    const val HOST = "appassets.androidplatform.net"
    const val ORIGEN = "https://appassets.androidplatform.net"

    private val tipos = mapOf(
        "html" to "text/html", "htm" to "text/html",
        "js" to "text/javascript", "mjs" to "text/javascript",
        "css" to "text/css", "json" to "application/json",
        "webmanifest" to "application/manifest+json",
        "svg" to "image/svg+xml", "png" to "image/png", "jpg" to "image/jpeg", "jpeg" to "image/jpeg",
        "webp" to "image/webp", "gif" to "image/gif", "ico" to "image/x-icon",
        "woff2" to "font/woff2", "woff" to "font/woff", "ttf" to "font/ttf",
        "wasm" to "application/wasm", "gz" to "application/octet-stream",
        "traineddata" to "application/octet-stream", "txt" to "text/plain"
    )

    private fun tipoDe(ruta: String): String {
        val ext = ruta.substringAfterLast('.', "").lowercase()
        return tipos[ext] ?: "application/octet-stream"
    }

    private fun esTexto(tipo: String): Boolean =
        tipo.startsWith("text/") || tipo.contains("json") || tipo.contains("svg") || tipo.contains("javascript")

    /** Responde con un archivo de assets/www. Si no existe, responde 404. */
    fun responder(ctx: Context, rutaOriginal: String): WebResourceResponse {
        var ruta = rutaOriginal.substringBefore('?').substringBefore('#').trimStart('/')
        if (ruta.isEmpty()) ruta = "app.html"
        val cabeceras = HashMap<String, String>()
        cabeceras["Access-Control-Allow-Origin"] = "*"
        cabeceras["Cache-Control"] = "no-cache"
        if (ruta.contains("..")) {
            return WebResourceResponse("text/plain", "utf-8", 403, "Prohibido", cabeceras, ByteArrayInputStream(ByteArray(0)))
        }
        return try {
            val flujo = ctx.assets.open("www/$ruta")
            val tipo = tipoDe(ruta)
            WebResourceResponse(tipo, if (esTexto(tipo)) "utf-8" else null, 200, "OK", cabeceras, flujo)
        } catch (e: FileNotFoundException) {
            WebResourceResponse("text/plain", "utf-8", 404, "No encontrado", cabeceras, ByteArrayInputStream(ByteArray(0)))
        } catch (e: Exception) {
            WebResourceResponse("text/plain", "utf-8", 500, "Error", cabeceras, ByteArrayInputStream(ByteArray(0)))
        }
    }

    /**
     * Red de seguridad: si alguna parte de la página todavía pide una librería a internet (CDN),
     * se le entrega la copia que viene dentro de la app. Devuelve la ruta dentro de assets/www o null.
     */
    fun copiaLocalDeCdn(url: String): String? {
        val u = url.substringBefore('?').substringBefore('#')
        val nombre = u.substringAfterLast('/')
        return when {
            u.contains("cdnjs.cloudflare.com") || u.contains("cdn.jsdelivr.net") -> when {
                nombre == "pdf.min.js" -> "lib/pdf.min.js"
                nombre == "pdf.worker.min.js" -> "lib/pdf.worker.min.js"
                nombre == "jspdf.umd.min.js" -> "lib/jspdf.umd.min.js"
                nombre == "xlsx.full.min.js" -> "lib/xlsx.full.min.js"
                u.contains("tesseract.js-core") && nombre.endsWith(".js") -> "lib/tesseract/core/$nombre"
                u.contains("tesseract.js") && nombre == "tesseract.min.js" -> "lib/tesseract/tesseract.min.js"
                u.contains("tesseract.js") && nombre == "worker.min.js" -> "lib/tesseract/worker.min.js"
                u.contains("@zxing/library") && nombre == "index.min.js" -> "lib/zxing.min.js"
                nombre.endsWith(".traineddata.gz") -> "lib/tesseract/lang/$nombre"
                else -> null
            }
            u.contains("fonts.googleapis.com") -> "lib/fonts.css"
            else -> null
        }
    }
}
