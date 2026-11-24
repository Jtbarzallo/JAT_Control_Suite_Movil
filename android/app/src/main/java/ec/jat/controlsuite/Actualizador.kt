package ec.jat.controlsuite

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Avisa cuando hay una versión nueva de la app en GitHub y la instala con un toque.
 * Revisa como máximo una vez cada pocas horas y solo si hay internet.
 */
class Actualizador(private val act: Activity) {

    private val prefs = act.getSharedPreferences("actualizador", Context.MODE_PRIVATE)
    private var descargaPendiente: String? = null
    private var enCurso = false

    @Suppress("DEPRECATION")
    private fun versionActual(): Int {
        return try {
            act.packageManager.getPackageInfo(act.packageName, 0).versionCode
        } catch (e: Exception) {
            0
        }
    }

    /** Busca en segundo plano si hay una versión más nueva. forzar = ignorar la espera entre revisiones. */
    fun buscar(forzar: Boolean) {
        val ahora = System.currentTimeMillis()
        if (!forzar && ahora - prefs.getLong("ultimaRevision", 0L) < 3L * 3600_000L) return
        Thread {
            try {
                val con = URL(API).openConnection() as HttpURLConnection
                con.connectTimeout = 8000
                con.readTimeout = 8000
                con.setRequestProperty("Accept", "application/vnd.github+json")
                con.setRequestProperty("User-Agent", "JATControlSuite")
                if (con.responseCode != 200) {
                    if (forzar) avisarUi("No se pudo revisar si hay una versión nueva")
                    return@Thread
                }
                val texto = con.inputStream.bufferedReader().use { it.readText() }
                prefs.edit().putLong("ultimaRevision", ahora).apply()
                val j = JSONObject(texto)
                val etiqueta = j.optString("tag_name")
                val nueva = etiqueta.substringAfterLast('.').toIntOrNull() ?: 0
                var enlace = ""
                val archivos = j.optJSONArray("assets")
                if (archivos != null) {
                    for (i in 0 until archivos.length()) {
                        val a = archivos.getJSONObject(i)
                        if (a.optString("name").endsWith(".apk")) {
                            enlace = a.optString("browser_download_url")
                            break
                        }
                    }
                }
                if (nueva > versionActual() && enlace.isNotEmpty()) {
                    val yaAvisada = prefs.getString("avisada", "") == etiqueta
                    val hace = ahora - prefs.getLong("ultimoAviso", 0L)
                    if (forzar || !yaAvisada || hace > 12L * 3600_000L) {
                        prefs.edit().putString("avisada", etiqueta).putLong("ultimoAviso", ahora).apply()
                        act.runOnUiThread { preguntar(etiqueta, enlace) }
                    }
                } else if (forzar) {
                    avisarUi("Ya tienes la última versión")
                }
            } catch (e: Exception) {
                if (forzar) avisarUi("No se pudo revisar: sin internet")
            }
        }.start()
    }

    private fun avisarUi(mensaje: String) {
        act.runOnUiThread { Toast.makeText(act, mensaje, Toast.LENGTH_LONG).show() }
    }

    private fun preguntar(etiqueta: String, enlace: String) {
        if (act.isFinishing) return
        AlertDialog.Builder(act)
            .setTitle("Hay una versión nueva")
            .setMessage("Salió la versión $etiqueta de JAT Control Suite con mejoras.\n\nTus egresos y datos se conservan al actualizar.\n\n¿Actualizar ahora?")
            .setPositiveButton("Actualizar") { _, _ -> descargarEInstalar(enlace) }
            .setNegativeButton("Más tarde", null)
            .show()
    }

    /** Se llama al volver a la app: si faltaba el permiso para instalar y ya se dio, sigue con la descarga. */
    fun alVolver() {
        val enlace = descargaPendiente
        if (enlace != null && puedeInstalar()) {
            descargaPendiente = null
            descargarEInstalar(enlace)
        }
    }

    private fun puedeInstalar(): Boolean =
        Build.VERSION.SDK_INT < 26 || act.packageManager.canRequestPackageInstalls()

    private fun descargarEInstalar(enlace: String) {
        if (!puedeInstalar()) {
            descargaPendiente = enlace
            Toast.makeText(act, "Activa \"Permitir de esta fuente\" y vuelve a la app", Toast.LENGTH_LONG).show()
            try {
                val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + act.packageName))
                act.startActivity(i)
            } catch (e: Exception) {
            }
            return
        }
        if (enCurso) return
        enCurso = true
        val dialogo = AlertDialog.Builder(act)
            .setTitle("Descargando actualización")
            .setMessage("0 %")
            .setCancelable(false)
            .create()
        dialogo.show()
        Thread {
            try {
                val destino = File(Archivos.carpetaTemporal(act), "JATControlSuite.apk")
                if (destino.exists()) destino.delete()
                val con = URL(enlace).openConnection() as HttpURLConnection
                con.connectTimeout = 15000
                con.readTimeout = 30000
                con.instanceFollowRedirects = true
                val total = con.contentLength.toLong()
                var leido = 0L
                var ultimoPorcentaje = -1
                con.inputStream.use { entrada ->
                    FileOutputStream(destino).use { salida ->
                        val bloque = ByteArray(64 * 1024)
                        while (true) {
                            val n = entrada.read(bloque)
                            if (n < 0) break
                            salida.write(bloque, 0, n)
                            leido += n
                            if (total > 0) {
                                val p = (leido * 100 / total).toInt()
                                if (p != ultimoPorcentaje) {
                                    ultimoPorcentaje = p
                                    act.runOnUiThread { dialogo.setMessage("$p %") }
                                }
                            }
                        }
                    }
                }
                act.runOnUiThread {
                    dialogo.dismiss()
                    enCurso = false
                    instalar(destino)
                }
            } catch (e: Exception) {
                act.runOnUiThread {
                    dialogo.dismiss()
                    enCurso = false
                    Toast.makeText(act, "No se pudo descargar la actualización. Revisa tu internet.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun instalar(apk: File) {
        try {
            val uri = FileProvider.getUriForFile(act, act.packageName + ".archivos", apk)
            val i = Intent(Intent.ACTION_VIEW)
            i.setDataAndType(uri, "application/vnd.android.package-archive")
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            act.startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(act, "No se pudo abrir el instalador", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val API = "https://api.github.com/repos/Jtbarzallo/JAT_Control_Suite_Movil/releases/latest"
    }
}
