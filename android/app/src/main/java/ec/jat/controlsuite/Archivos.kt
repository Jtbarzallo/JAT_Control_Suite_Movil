package ec.jat.controlsuite

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Guardar, compartir e imprimir archivos. */
object Archivos {
    const val CARPETA = "JAT Control Suite"

    fun carpetaTemporal(ctx: Context): File {
        val d = File(ctx.cacheDir, "jat")
        if (!d.exists()) d.mkdirs()
        return d
    }

    /** Quita del nombre lo que Android no acepta en un archivo. */
    fun nombreSeguro(nombre: String): String {
        val limpio = nombre.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_").trim()
        return if (limpio.isEmpty()) "archivo" else limpio.take(150)
    }

    /**
     * Android agrega una extensión extra si el tipo no coincide con el nombre (por ejemplo ".hat" con "application/json").
     * Por eso solo se declara el tipo cuando se conoce la extensión; lo demás va como "octet-stream".
     */
    fun tipoParaGuardar(nombre: String): String {
        return when (nombre.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "txt" -> "text/plain"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "application/octet-stream"
        }
    }

    /**
     * Guarda en Descargas/JAT Control Suite. Ese lugar NO se borra al desinstalar la app ni al borrar sus datos.
     * Devuelve el lugar donde quedó, o null si no se pudo.
     */
    fun guardarEnDescargas(ctx: Context, nombreOriginal: String, origen: File): String? {
        val nombre = nombreSeguro(nombreOriginal)
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val valores = ContentValues()
                valores.put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
                valores.put(MediaStore.MediaColumns.MIME_TYPE, tipoParaGuardar(nombre))
                valores.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + CARPETA)
                valores.put(MediaStore.MediaColumns.IS_PENDING, 1)
                val resolver = ctx.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores) ?: return null
                val salida = resolver.openOutputStream(uri)
                if (salida == null) {
                    resolver.delete(uri, null, null)
                    return null
                }
                salida.use { s -> FileInputStream(origen).use { e -> e.copyTo(s) } }
                val fin = ContentValues()
                fin.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, fin, null, null)
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), CARPETA)
                if (!dir.exists()) dir.mkdirs()
                var destino = File(dir, nombre)
                var n = 1
                val base = nombre.substringBeforeLast('.', nombre)
                val ext = if (nombre.contains('.')) "." + nombre.substringAfterLast('.') else ""
                while (destino.exists()) {
                    destino = File(dir, "$base ($n)$ext")
                    n++
                }
                FileInputStream(origen).use { e -> FileOutputStream(destino).use { s -> e.copyTo(s) } }
                MediaScannerConnection.scanFile(ctx, arrayOf(destino.absolutePath), null, null)
            }
            "Descargas › $CARPETA"
        } catch (e: Exception) {
            null
        }
    }

    fun uriCompartible(ctx: Context, archivo: File): Uri =
        FileProvider.getUriForFile(ctx, ctx.packageName + ".archivos", archivo)

    /** Abre la hoja de compartir de Android (WhatsApp, Drive, OneDrive, correo...) con un archivo. */
    fun compartirArchivo(ctx: Context, archivo: File, tipo: String, titulo: String) {
        val uri = uriCompartible(ctx, archivo)
        val envio = Intent(Intent.ACTION_SEND)
        envio.type = if (tipo.isEmpty()) "application/octet-stream" else tipo
        envio.putExtra(Intent.EXTRA_STREAM, uri)
        envio.putExtra(Intent.EXTRA_SUBJECT, titulo)
        envio.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val selector = Intent.createChooser(envio, titulo)
        selector.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(selector)
    }

    fun compartirTexto(ctx: Context, titulo: String, texto: String) {
        val envio = Intent(Intent.ACTION_SEND)
        envio.type = "text/plain"
        envio.putExtra(Intent.EXTRA_SUBJECT, titulo)
        envio.putExtra(Intent.EXTRA_TEXT, texto)
        val selector = Intent.createChooser(envio, titulo.ifEmpty { "Compartir" })
        selector.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(selector)
    }

    /** Entrega un PDF ya hecho (como el egreso revisado) al servicio de impresión de Android. */
    class AdaptadorPdf(private val archivo: File, private val nombre: String) : PrintDocumentAdapter() {
        override fun onLayout(
            anterior: PrintAttributes?,
            nuevo: PrintAttributes?,
            cancelar: CancellationSignal?,
            respuesta: LayoutResultCallback?,
            extras: Bundle?
        ) {
            if (cancelar != null && cancelar.isCanceled) {
                respuesta?.onLayoutCancelled()
                return
            }
            var paginas = PrintDocumentInfo.PAGE_COUNT_UNKNOWN
            try {
                val pfd = ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY)
                val lector = PdfRenderer(pfd)
                paginas = lector.pageCount
                lector.close()
                pfd.close()
            } catch (e: Exception) {
                // se imprime igual, sin saber cuántas hojas son
            }
            val info = PrintDocumentInfo.Builder(nombre)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(paginas)
                .build()
            respuesta?.onLayoutFinished(info, true)
        }

        override fun onWrite(
            paginas: Array<out PageRange>?,
            destino: ParcelFileDescriptor?,
            cancelar: CancellationSignal?,
            respuesta: WriteResultCallback?
        ) {
            try {
                if (destino == null) {
                    respuesta?.onWriteFailed("Sin destino")
                    return
                }
                FileInputStream(archivo).use { entrada ->
                    FileOutputStream(destino.fileDescriptor).use { salida -> entrada.copyTo(salida) }
                }
                respuesta?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                respuesta?.onWriteFailed(e.message)
            }
        }
    }
}
