package ec.jat.controlsuite

import android.content.Intent
import android.util.Base64

/**
 * Egreso que llega desde OneView (24 nov 2026), por dos caminos:
 *  - la ventana "OneView" de la app (OneViewActivity), botón IMPORTAR ESTE EGRESO A JAT;
 *  - el marcador "JAT" en Chrome, botón "Abrir en la app JAT" (enlace jatcs://oneview?d=<datos>).
 * Se guarda aquí hasta que la página lo pide con JATAndroid.oneViewPendiente().
 */
object OneView {
    const val INICIO = "http://oneview.lavictoria.ec:8080/OneView/"

    @Volatile
    private var pendiente: String? = null

    fun guardar(json: String) {
        pendiente = json
    }

    /** La página lo pide una sola vez. */
    fun tomar(): String {
        val t = pendiente ?: ""
        pendiente = null
        return t
    }

    /** Devuelve true si el intent traía un egreso de OneView (enlace del marcador de Chrome). */
    fun desdeIntent(intent: Intent?): Boolean {
        val u = intent?.data ?: return false
        if (intent.action != Intent.ACTION_VIEW || u.scheme != "jatcs" || u.host != "oneview") return false
        val d = u.getQueryParameter("d") ?: return false
        return try {
            val json = String(Base64.decode(d, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), Charsets.UTF_8)
            if (!json.contains("\"jatOneView\"")) return false
            pendiente = json
            // Que no se vuelva a importar al girar la pantalla
            intent.action = Intent.ACTION_MAIN
            intent.data = null
            true
        } catch (e: Exception) {
            false
        }
    }
}
