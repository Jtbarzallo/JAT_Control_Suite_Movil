package ec.jat.controlsuite

import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Puente entre la pantalla web y el teléfono. La pantalla lo ve como "JATAndroid".
 * Los archivos llegan por partes (subida) para no pasar de golpe archivos grandes.
 */
class Puente(private val act: MainActivity) {

    private val subidas = ConcurrentHashMap<String, File>()

    private fun idLimpio(id: String): String = id.filter { it.isLetterOrDigit() }.take(60)

    @JavascriptInterface
    fun version(): String {
        return try {
            act.packageManager.getPackageInfo(act.packageName, 0).versionName ?: "1"
        } catch (e: Exception) {
            "1"
        }
    }

    /** Archivos compartidos a la app (WhatsApp › Compartir › JAT Control Suite). */
    @JavascriptInterface
    fun compartidosPendientes(): String = Compartidos.tomarPendientes()

    /** Egreso traído desde OneView (ventana OneView de la app o marcador de Chrome). */
    @JavascriptInterface
    fun oneViewPendiente(): String = OneView.tomar()

    @JavascriptInterface
    fun abrirOneView() {
        act.abrirOneView()
    }

    // ---------- Archivos que salen de la app (descargas, compartir, imprimir) ----------

    @JavascriptInterface
    fun iniciarSubida(id: String): String {
        return try {
            val f = File(Archivos.carpetaTemporal(act), "subida_" + idLimpio(id) + ".tmp")
            if (f.exists()) f.delete()
            f.createNewFile()
            subidas[id] = f
            "OK"
        } catch (e: Exception) {
            "ERR:" + e.message
        }
    }

    @JavascriptInterface
    fun parteSubida(id: String, base64: String): String {
        val f = subidas[id] ?: return "ERR:subida no iniciada"
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            FileOutputStream(f, true).use { it.write(bytes) }
            "OK"
        } catch (e: Exception) {
            "ERR:" + e.message
        }
    }

    /** Guarda en Descargas/JAT Control Suite (sobrevive a desinstalar). Responde "OK:lugar" o "ERR:motivo". */
    @JavascriptInterface
    fun guardarSubida(id: String, nombre: String, avisar: Boolean): String {
        val f = subidas.remove(id) ?: return "ERR:subida no iniciada"
        val lugar = Archivos.guardarEnDescargas(act, nombre, f)
        f.delete()
        if (lugar == null) {
            act.avisar("No se pudo guardar $nombre. Revisa que haya espacio en el teléfono.")
            return "ERR:no se pudo guardar"
        }
        if (avisar) act.avisar("Guardado en $lugar:\n$nombre")
        return "OK:$lugar"
    }

    @JavascriptInterface
    fun compartirSubida(id: String, nombre: String, tipo: String, titulo: String): String {
        val f = subidas.remove(id) ?: return "ERR:subida no iniciada"
        return try {
            val destino = File(Archivos.carpetaTemporal(act), Archivos.nombreSeguro(nombre))
            if (destino.exists()) destino.delete()
            if (!f.renameTo(destino)) {
                f.copyTo(destino, true)
                f.delete()
            }
            act.runOnUiThread {
                try {
                    Archivos.compartirArchivo(act, destino, tipo, titulo.ifEmpty { nombre })
                } catch (e: Exception) {
                    act.avisar("No se pudo abrir Compartir")
                }
            }
            "OK"
        } catch (e: Exception) {
            "ERR:" + e.message
        }
    }

    @JavascriptInterface
    fun imprimirPdfSubida(id: String, nombre: String): String {
        val f = subidas.remove(id) ?: return "ERR:subida no iniciada"
        return try {
            val destino = File(Archivos.carpetaTemporal(act), Archivos.nombreSeguro(nombre))
            if (destino.exists()) destino.delete()
            if (!f.renameTo(destino)) {
                f.copyTo(destino, true)
                f.delete()
            }
            act.runOnUiThread { act.imprimirPdf(destino, nombre) }
            "OK"
        } catch (e: Exception) {
            "ERR:" + e.message
        }
    }

    @JavascriptInterface
    fun compartirTexto(titulo: String, texto: String) {
        act.runOnUiThread {
            try {
                Archivos.compartirTexto(act, titulo, texto)
            } catch (e: Exception) {
                act.avisar("No se pudo abrir Compartir")
            }
        }
    }

    @JavascriptInterface
    fun imprimirHtml(html: String, titulo: String) {
        act.imprimirHtml(html, titulo)
    }

    @JavascriptInterface
    fun abrirNavegador(url: String) {
        act.runOnUiThread { act.abrirExterno(Uri.parse(url)) }
    }

    /**
     * Guardado automático de egresos (27 nov 2026): escribe texto en Descargas › JAT Control Suite › <ruta>,
     * reemplazando el archivo anterior del mismo egreso. Ej.: "Egresos/EGR123.hat".
     */
    @JavascriptInterface
    fun guardarArchivo(ruta: String, contenido: String, tipo: String): String {
        return try {
            Archivos.escribirEnCarpeta(act, ruta, contenido.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            "ERR:" + (e.message ?: "error al guardar")
        }
    }

    @JavascriptInterface
    fun avisar(mensaje: String) {
        act.avisar(mensaje)
    }

    // ---------- Pantalla ----------

    @JavascriptInterface
    fun pantallaCompleta(activar: Boolean) {
        act.pantallaCompleta(activar)
    }

    // ---------- Voz ----------

    @JavascriptInterface
    fun hablar(id: String, texto: String, idioma: String, velocidad: Double, tono: Double, volumen: Double) {
        act.voz.hablar(id, texto, idioma, velocidad, tono, volumen)
    }

    @JavascriptInterface
    fun pararVoz() {
        act.voz.parar()
    }

    @JavascriptInterface
    fun dictar(id: String, idioma: String) {
        act.dictar(id, idioma)
    }

    // ---------- Lector de códigos de barras ----------

    @JavascriptInterface
    fun detectar(id: String, formatos: String, jpegBase64: String) {
        act.detector.detectar(id, formatos, jpegBase64)
    }

    // ---------- Actualizaciones ----------

    @JavascriptInterface
    fun buscarActualizacion() {
        act.runOnUiThread { act.actualizador.buscar(true) }
    }
}
