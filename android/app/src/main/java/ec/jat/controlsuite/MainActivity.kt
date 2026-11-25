package ec.jat.controlsuite

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JsResult
import android.webkit.JsPromptResult
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import org.json.JSONObject
import java.io.File

class MainActivity : Activity() {

    lateinit var web: WebView
    lateinit var voz: Voz
    lateinit var detector: Detector
    lateinit var actualizador: Actualizador

    private lateinit var raiz: FrameLayout
    private var pedidoCamara: PermissionRequest? = null
    private var elegirArchivos: ValueCallback<Array<Uri>>? = null
    private var fotoUri: Uri? = null
    private var dictadoId: String? = null
    private var ultimoAtras = 0L
    private var inmersivo = false
    private var impresion: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        raiz = FrameLayout(this)
        raiz.setBackgroundColor(Color.parseColor("#0A1322"))
        setContentView(raiz)
        voz = Voz(this) { tipo, id -> nativo("tts", id, tipo) }
        detector = Detector { id, json -> nativo("det", id, json) }
        actualizador = Actualizador(this)
        crearWeb()
        limpiarTemporales()
        Compartidos.recibir(this, intent)
        pedirPermisos()
        web.postDelayed({ actualizador.buscar(false) }, 6000)
    }

    // ---------------------------------------------------------------- pantalla web

    @SuppressLint("SetJavaScriptEnabled")
    private fun crearWeb() {
        val w = WebView(this)
        w.setBackgroundColor(Color.parseColor("#0A1322"))
        w.overScrollMode = View.OVER_SCROLL_NEVER
        val s = w.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.mediaPlaybackRequiresUserGesture = false
        s.setSupportZoom(false)
        s.builtInZoomControls = false
        s.displayZoomControls = false
        s.textZoom = 100
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.setSupportMultipleWindows(false)
        s.userAgentString = s.userAgentString + " JATControlSuiteAPK"
        w.addJavascriptInterface(Puente(this), "JATAndroid")
        w.webViewClient = ClienteWeb()
        w.webChromeClient = ChromeWeb()
        w.setDownloadListener { url, _, _, _, _ ->
            if (url != null && url.startsWith("http")) abrirExterno(Uri.parse(url))
        }
        web = w
        raiz.addView(w, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        w.loadUrl(Recursos.ORIGEN + "/app.html")
    }

    private fun reiniciarWeb() {
        try {
            raiz.removeView(web)
            web.destroy()
        } catch (e: Exception) {
        }
        crearWeb()
    }

    private inner class ClienteWeb : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val u = request?.url ?: return null
            if (u.host == Recursos.HOST && (u.path ?: "").startsWith("/compartido/")) return Compartidos.responder(this@MainActivity, u.path ?: "")
            if (u.host == Recursos.HOST) return Recursos.responder(this@MainActivity, u.path ?: "")
            val local = Recursos.copiaLocalDeCdn(u.toString())
            if (local != null) return Recursos.responder(this@MainActivity, local)
            return null
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val u = request?.url ?: return false
            if (u.host == Recursos.HOST) return false
            abrirExterno(u)
            return true
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            runOnUiThread { reiniciarWeb() }
            return true
        }
    }

    private inner class ChromeWeb : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest?) {
            val r = request ?: return
            runOnUiThread {
                val pide = r.resources
                if (pide.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        r.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    } else {
                        pedidoCamara = r
                        requestPermissions(arrayOf(Manifest.permission.CAMERA), COD_CAMARA)
                    }
                } else {
                    r.deny()
                }
            }
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            elegirArchivos?.onReceiveValue(null)
            elegirArchivos = filePathCallback
            val tipos = tiposDe(fileChooserParams?.acceptTypes)
            val soloImagen = tipos.isNotEmpty() && tipos.all { it.startsWith("image/") }
            if (fileChooserParams != null && fileChooserParams.isCaptureEnabled && soloImagen) {
                if (abrirCamaraFoto()) return true
            }
            try {
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "*/*"
                if (tipos.isNotEmpty() && !tipos.contains("*/*")) i.putExtra(Intent.EXTRA_MIME_TYPES, tipos)
                val varios = fileChooserParams != null && fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, varios)
                startActivityForResult(Intent.createChooser(i, "Elegir archivo"), COD_ARCHIVO)
            } catch (e: Exception) {
                elegirArchivos?.onReceiveValue(null)
                elegirArchivos = null
                avisar("No se pudo abrir el selector de archivos")
            }
            return true
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            val r = result ?: return false
            AlertDialog.Builder(this@MainActivity)
                .setMessage(message ?: "")
                .setPositiveButton("Aceptar") { _, _ -> r.confirm() }
                .setOnCancelListener { r.confirm() }
                .show()
            return true
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            val r = result ?: return false
            AlertDialog.Builder(this@MainActivity)
                .setMessage(message ?: "")
                .setPositiveButton("Aceptar") { _, _ -> r.confirm() }
                .setNegativeButton("Cancelar") { _, _ -> r.cancel() }
                .setOnCancelListener { r.cancel() }
                .show()
            return true
        }

        override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
            val r = result ?: return false
            val campo = EditText(this@MainActivity)
            campo.setText(defaultValue ?: "")
            AlertDialog.Builder(this@MainActivity)
                .setMessage(message ?: "")
                .setView(campo)
                .setPositiveButton("Aceptar") { _, _ -> r.confirm(campo.text.toString()) }
                .setNegativeButton("Cancelar") { _, _ -> r.cancel() }
                .setOnCancelListener { r.cancel() }
                .show()
            return true
        }
    }

    // ---------------------------------------------------------------- archivos y cámara

    /** Convierte lo que pide la página (como ".pdf" o imágenes) en tipos que entiende el selector de Android. */
    private fun tiposDe(aceptar: Array<String>?): Array<String> {
        val lista = ArrayList<String>()
        if (aceptar != null) {
            for (a in aceptar) {
                for (t in a.split(",")) {
                    val x = t.trim().lowercase()
                    if (x.isEmpty()) continue
                    if (x.contains("/")) {
                        lista.add(x)
                    } else if (x.startsWith(".")) {
                        when (x) {
                            ".pdf" -> lista.add("application/pdf")
                            ".xls" -> lista.add("application/vnd.ms-excel")
                            ".xlsx" -> lista.add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            ".csv" -> {
                                lista.add("text/csv")
                                lista.add("text/comma-separated-values")
                                lista.add("application/csv")
                                lista.add("text/plain")
                            }
                            ".json" -> lista.add("application/json")
                            ".jpg", ".jpeg" -> lista.add("image/jpeg")
                            ".png" -> lista.add("image/png")
                            else -> lista.add("*/*")
                        }
                    } else {
                        lista.add("*/*")
                    }
                }
            }
        }
        return lista.distinct().toTypedArray()
    }

    private fun abrirCamaraFoto(): Boolean {
        return try {
            val archivo = File(Archivos.carpetaTemporal(this), "foto_" + System.currentTimeMillis() + ".jpg")
            val uri = Archivos.uriCompartible(this, archivo)
            fotoUri = uri
            val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            i.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            i.clipData = ClipData.newRawUri("", uri)
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(i, COD_FOTO)
            true
        } catch (e: Exception) {
            fotoUri = null
            false
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            COD_ARCHIVO -> {
                val cb = elegirArchivos
                elegirArchivos = null
                cb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            }
            COD_FOTO -> {
                val cb = elegirArchivos
                elegirArchivos = null
                val u = fotoUri
                fotoUri = null
                if (cb != null) {
                    if (resultCode == RESULT_OK && u != null) cb.onReceiveValue(arrayOf(u)) else cb.onReceiveValue(null)
                }
            }
            COD_VOZ -> {
                val id = dictadoId
                dictadoId = null
                if (id != null) {
                    if (resultCode == RESULT_OK && data != null) {
                        val l = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        val t = if (l != null && l.isNotEmpty()) l[0] else ""
                        nativo("sr", id, "ok:$t")
                    } else {
                        nativo("sr", id, "fin")
                    }
                }
            }
        }
    }

    private fun pedirPermisos() {
        val faltan = ArrayList<String>()
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            faltan.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            faltan.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (faltan.isNotEmpty()) requestPermissions(faltan.toTypedArray(), COD_PERMISOS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == COD_CAMARA) {
            val ok = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            val p = pedidoCamara
            pedidoCamara = null
            if (p != null) {
                if (ok) p.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) else p.deny()
            }
            if (!ok) avisar("Para escanear activa el permiso de Cámara: Ajustes › Apps › JAT Control Suite › Permisos")
        }
    }

    /** Borra los archivos temporales viejos (más de un día). */
    private fun limpiarTemporales() {
        Thread {
            try {
                val limite = System.currentTimeMillis() - 24L * 3600_000L
                val archivos = Archivos.carpetaTemporal(this).listFiles()
                if (archivos != null) {
                    for (f in archivos) {
                        if (f.isFile && f.lastModified() < limite) f.delete()
                    }
                }
            } catch (e: Exception) {
            }
        }.start()
    }

    // ---------------------------------------------------------------- servicios para la pantalla

    fun avisar(mensaje: String) {
        runOnUiThread { Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show() }
    }

    fun abrirExterno(uri: Uri) {
        try {
            val i = Intent(Intent.ACTION_VIEW, uri)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) {
            avisar("No se pudo abrir el enlace")
        }
    }

    /** Ejecuta código en la página principal. */
    private fun js(codigo: String) {
        runOnUiThread {
            try {
                web.evaluateJavascript(codigo, null)
            } catch (e: Exception) {
            }
        }
    }

    /** Avisa a la página (voz, dictado, lector de códigos). */
    fun nativo(tipo: String, id: String, carga: String) {
        js(
            "window.__jatNativo&&window.__jatNativo(" + JSONObject.quote(tipo) + "," +
                JSONObject.quote(id) + "," + JSONObject.quote(carga) + ")"
        )
    }

    fun pantallaCompleta(activar: Boolean) {
        runOnUiThread {
            inmersivo = activar
            aplicarInmersivo()
        }
    }

    @Suppress("DEPRECATION")
    private fun aplicarInmersivo() {
        window.decorView.systemUiVisibility = if (inmersivo) {
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        } else {
            View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    fun dictar(id: String, idioma: String) {
        runOnUiThread {
            dictadoId = id
            try {
                val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (idioma.isEmpty()) "es-EC" else idioma)
                i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora")
                i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                startActivityForResult(i, COD_VOZ)
            } catch (e: Exception) {
                dictadoId = null
                nativo("sr", id, "err:service-not-allowed")
            }
        }
    }

    // ---------------------------------------------------------------- impresión

    private fun lanzarImpresion(adaptador: PrintDocumentAdapter, titulo: String) {
        try {
            val gestor = getSystemService(Context.PRINT_SERVICE) as PrintManager
            val atributos = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A3)
                .setMinMargins(PrintAttributes.Margins(300, 300, 300, 300))
                .build()
            gestor.print(if (titulo.isEmpty()) "JAT Control Suite" else titulo, adaptador, atributos)
        } catch (e: Exception) {
            avisar("No se pudo abrir la impresión")
        }
    }

    fun imprimirPdf(archivo: File, nombre: String) {
        lanzarImpresion(Archivos.AdaptadorPdf(archivo, nombre), nombre)
    }

    /** Imprime una página HTML (el reporte) en hoja A3 vertical. */
    fun imprimirHtml(html: String, titulo: String) {
        runOnUiThread {
            val w = WebView(this)
            impresion = w
            w.settings.javaScriptEnabled = true
            var lanzado = false
            w.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val v = view ?: return
                    if (lanzado) return
                    lanzado = true
                    v.postDelayed({ lanzarImpresion(v.createPrintDocumentAdapter(titulo.ifEmpty { "Reporte" }), titulo) }, 700)
                }
            }
            w.loadDataWithBaseURL(Recursos.ORIGEN + "/", html, "text/html", "UTF-8", null)
        }
    }

    // ---------------------------------------------------------------- ciclo de vida y botón Atrás

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        web.evaluateJavascript("(function(){try{return window.__jatAtras?window.__jatAtras():false}catch(e){return false}})()") { r ->
            if (r != "true") {
                val ahora = System.currentTimeMillis()
                if (ahora - ultimoAtras < 2000) {
                    finish()
                } else {
                    ultimoAtras = ahora
                    Toast.makeText(this, "Toca Atrás otra vez para salir", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** Llega un archivo compartido con la app ya abierta. */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (Compartidos.recibir(this, intent)) js("window.JATHayCompartido&&window.JATHayCompartido()")
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
        aplicarInmersivo()
        actualizador.alVolver()
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && inmersivo) aplicarInmersivo()
    }

    override fun onDestroy() {
        voz.cerrar()
        detector.cerrar()
        try {
            impresion?.destroy()
            web.destroy()
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    companion object {
        const val COD_CAMARA = 11
        const val COD_PERMISOS = 12
        const val COD_ARCHIVO = 21
        const val COD_FOTO = 22
        const val COD_VOZ = 23
    }
}
