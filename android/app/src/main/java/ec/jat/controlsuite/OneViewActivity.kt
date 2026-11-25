package ec.jat.controlsuite

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ventana "OneView" dentro de la app (24 nov 2026).
 * Jorge inicia sesión aquí mismo (la app no guarda usuario ni contraseña; solo la sesión, como Chrome),
 * abre el egreso y toca IMPORTAR ESTE EGRESO A JAT. Se lee la tabla con oneview/jat-oneview.js
 * (el mismo lector del marcador de Chrome) y el egreso entra a JAT por el mismo camino que el PDF.
 */
class OneViewActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var titulo: TextView
    private lateinit var carga: ProgressBar
    private val prefs by lazy { getSharedPreferences("oneview", MODE_PRIVATE) }

    private val azul = Color.parseColor("#0A1F44")
    private val oro = Color.parseColor("#C9A227")
    private val fondo = Color.parseColor("#0A1322")

    private fun dp(v: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun boton(texto: String, colorFondo: Int, colorTexto: Int, tam: Float, borde: Int? = null): Button {
        val b = Button(this)
        b.text = texto
        b.isAllCaps = false
        b.setTextColor(colorTexto)
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, tam)
        b.typeface = Typeface.DEFAULT_BOLD
        val g = GradientDrawable()
        g.setColor(colorFondo)
        g.cornerRadius = dp(12).toFloat()
        if (borde != null) g.setStroke(dp(2), borde)
        b.background = g
        b.setPadding(dp(10), dp(6), dp(10), dp(6))
        b.stateListAnimator = null
        return b
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(fondo)

        // ---- barra de arriba
        val barra = LinearLayout(this)
        barra.orientation = LinearLayout.HORIZONTAL
        barra.gravity = Gravity.CENTER_VERTICAL
        barra.setPadding(dp(8), dp(8), dp(8), dp(8))
        val volver = boton("← JAT", azul, Color.WHITE, 18f, oro)
        volver.setOnClickListener { finish() }
        titulo = TextView(this)
        titulo.text = "OneView"
        titulo.setTextColor(Color.WHITE)
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        titulo.typeface = Typeface.DEFAULT_BOLD
        titulo.maxLines = 1
        titulo.setPadding(dp(10), 0, dp(10), 0)
        val inicio = boton("Inicio", azul, Color.WHITE, 16f, oro)
        inicio.setOnClickListener { web.loadUrl(OneView.INICIO) }
        val recargar = boton("⟳", azul, Color.WHITE, 20f, oro)
        recargar.setOnClickListener { web.reload() }
        barra.addView(volver, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(52)))
        barra.addView(titulo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        barra.addView(inicio, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(52)).apply { marginEnd = dp(6) })
        barra.addView(recargar, LinearLayout.LayoutParams(dp(56), dp(52)))
        raiz.addView(barra, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        carga = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        carga.max = 100
        carga.visibility = View.GONE
        raiz.addView(carga, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)))

        // ---- OneView
        web = WebView(this)
        web.setBackgroundColor(Color.WHITE)
        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.javaScriptCanOpenWindowsAutomatically = true
        s.setSupportMultipleWindows(false) // las ventanas nuevas de OneView se abren aquí mismo
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.cacheMode = WebSettings.LOAD_DEFAULT
        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(web, true)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val u = request?.url ?: return false
                if (u.scheme == "http" || u.scheme == "https") return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, u))
                } catch (e: Exception) {
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                carga.visibility = View.GONE
                if (url != null && url.startsWith("http")) prefs.edit().putString("ultima", url).apply()
                titulo.text = view?.title?.takeIf { it.isNotBlank() } ?: "OneView"
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                carga.progress = newProgress
                carga.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                val r = result ?: return false
                AlertDialog.Builder(this@OneViewActivity).setMessage(message ?: "")
                    .setPositiveButton("Aceptar") { _, _ -> r.confirm() }
                    .setOnCancelListener { r.confirm() }.show()
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                val r = result ?: return false
                AlertDialog.Builder(this@OneViewActivity).setMessage(message ?: "")
                    .setPositiveButton("Aceptar") { _, _ -> r.confirm() }
                    .setNegativeButton("Cancelar") { _, _ -> r.cancel() }
                    .setOnCancelListener { r.cancel() }.show()
                return true
            }
        }
        web.setDownloadListener { url, _, _, _, _ ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
            }
        }
        raiz.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // ---- botón grande de abajo
        val importar = boton("📥  IMPORTAR ESTE EGRESO A JAT", oro, Color.parseColor("#0A1322"), 21f)
        importar.setOnClickListener { importar() }
        val pie = LinearLayout(this)
        pie.setPadding(dp(10), dp(8), dp(10), dp(10))
        pie.addView(importar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(68)))
        raiz.addView(pie, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        setContentView(raiz)
        web.loadUrl(prefs.getString("ultima", null) ?: OneView.INICIO)
    }

    private fun lector(): String = try {
        assets.open("www/jat-oneview.js").bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (e: Exception) {
        ""
    }

    private fun aviso(texto: String) {
        val t = TextView(this)
        t.text = texto
        t.setTextColor(Color.BLACK)
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        t.typeface = Typeface.DEFAULT_BOLD
        t.setPadding(dp(22), dp(18), dp(22), dp(6))
        AlertDialog.Builder(this).setView(t).setPositiveButton("Entendido", null).show()
    }

    private fun importar() {
        val codigo = lector()
        if (codigo.isEmpty()) {
            aviso("Falta el lector de OneView en la app. Actualiza JAT Control Suite.")
            return
        }
        val js = "(function(){try{window.__JAT_SOLO_EXTRAER=true;\n" + codigo +
            "\n;return JSON.stringify(window.JATOneViewExtraer());}catch(e){return JSON.stringify({ok:false,error:'No se pudo leer esta pantalla: '+e.message});}})()"
        web.evaluateJavascript(js) { crudo ->
            try {
                // evaluateJavascript devuelve el texto entre comillas
                val texto = JSONArray("[$crudo]").getString(0)
                val r = JSONObject(texto)
                if (!r.optBoolean("ok")) {
                    aviso(r.optString("error", "No encontré la tabla de productos. Abre el egreso con la lista de productos."))
                    return@evaluateJavascript
                }
                confirmar(r.getJSONObject("datos"))
            } catch (e: Exception) {
                aviso("No encontré la tabla de productos. Abre el egreso (la pantalla con \"Detalle\") y vuelve a tocar IMPORTAR.")
            }
        }
    }

    private fun confirmar(datos: JSONObject) {
        val meta = datos.optJSONObject("meta") ?: JSONObject()
        val productos = datos.optJSONArray("productos")?.length() ?: 0
        val leido = datos.optDouble("totalLeido", 0.0)
        val sec = meta.optString("secuencial").ifEmpty { "Egreso sin número" }
        val ruta = meta.optString("bodegaOrigen", "¿?") + " → " + meta.optString("bodegaDestino", "¿?")
        val fmt = { n: Double -> if (n % 1.0 == 0.0) n.toLong().toString() else n.toString() }
        val cuadra = !meta.has("totalDocumento") || Math.abs(meta.optDouble("totalDocumento") - leido) < 0.001
        val linea = if (cuadra) "✔ Cuadra con \"No. artículos\" del egreso"
        else "⚠ El egreso dice " + fmt(meta.optDouble("totalDocumento")) + " artículos y se leyeron " + fmt(leido) + ". Revisa antes de verificar."

        val t = TextView(this)
        t.text = "$sec\n$ruta\n$productos productos · ${fmt(leido)} artículos\n\n$linea"
        t.setTextColor(if (cuadra) Color.BLACK else Color.parseColor("#B00020"))
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        t.typeface = Typeface.DEFAULT_BOLD
        t.setPadding(dp(22), dp(18), dp(22), dp(6))
        AlertDialog.Builder(this)
            .setTitle("¿Importar este egreso a JAT?")
            .setView(t)
            .setPositiveButton("IMPORTAR") { _, _ ->
                OneView.guardar(datos.toString())
                setResult(RESULT_OK)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else finish()
    }

    override fun onPause() {
        CookieManager.getInstance().flush()
        web.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onDestroy() {
        try {
            web.destroy()
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}
