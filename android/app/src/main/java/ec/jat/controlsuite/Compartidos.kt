package ec.jat.controlsuite

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.WebResourceResponse
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

/**
 * Archivos que llegan a la app desde "Compartir" o "Abrir con" (WhatsApp, Archivos, Drive...).
 * Se copian a la carpeta interna "compartidos" y la página los pide en
 * https://appassets.androidplatform.net/compartido/<n> para cargarlos como egreso.
 */
object Compartidos {
    private val pendientes = mutableListOf<JSONObject>()

    private fun carpeta(ctx: Context): File = File(ctx.filesDir, "compartidos").apply { mkdirs() }

    /** Devuelve true si el intent traía archivos. */
    fun recibir(ctx: Context, intent: Intent?): Boolean {
        if (intent == null) return false
        val uris = mutableListOf<Uri>()
        when (intent.action) {
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uris.add(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
            }
            Intent.ACTION_VIEW -> intent.data?.let { uris.add(it) }
        }
        if (uris.isEmpty()) return false
        val dir = carpeta(ctx)
        synchronized(pendientes) {
            pendientes.clear()
            dir.listFiles()?.forEach { it.delete() }
            uris.take(10).forEachIndexed { i, uri ->
                try {
                    val nombre = nombreDe(ctx, uri) ?: "egreso_$i.pdf"
                    val tipo = ctx.contentResolver.getType(uri) ?: tipoPorNombre(nombre)
                    val destino = File(dir, "a$i")
                    ctx.contentResolver.openInputStream(uri)?.use { ent -> destino.outputStream().use { ent.copyTo(it) } }
                    if (destino.length() > 0) {
                        pendientes.add(JSONObject().put("url", Recursos.ORIGEN + "/compartido/a$i").put("nombre", nombre).put("tipo", tipo))
                    }
                } catch (e: Exception) {
                }
            }
        }
        // Que Android no vuelva a entregar el mismo archivo al girar la pantalla
        intent.action = Intent.ACTION_MAIN
        return pendientes.isNotEmpty()
    }

    /** La página pide la lista (una sola vez por archivo compartido). */
    fun tomarPendientes(): String = synchronized(pendientes) {
        val a = JSONArray(); pendientes.forEach { a.put(it) }; pendientes.clear(); a.toString()
    }

    fun responder(ctx: Context, ruta: String): WebResourceResponse? {
        val n = ruta.removePrefix("/compartido/").filter { it.isLetterOrDigit() }
        val f = File(carpeta(ctx), n)
        if (!f.isFile) return WebResourceResponse("text/plain", "utf-8", 404, "No encontrado", mapOf(), "".byteInputStream())
        return WebResourceResponse("application/octet-stream", null, FileInputStream(f))
    }

    private fun nombreDe(ctx: Context, uri: Uri): String? {
        try {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) { val v = c.getString(0); if (!v.isNullOrBlank()) return v }
            }
        } catch (e: Exception) {
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun tipoPorNombre(n: String): String = when {
        n.endsWith(".pdf", true) -> "application/pdf"
        n.endsWith(".csv", true) -> "text/csv"
        n.endsWith(".json", true) -> "application/json"
        else -> "application/octet-stream"
    }
}
