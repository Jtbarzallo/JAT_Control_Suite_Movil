package ec.jat.controlsuite

import android.graphics.BitmapFactory
import android.util.Base64
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lector de códigos de barras de Google (ML Kit). La pantalla web le manda una foto del video
 * y recibe los códigos que encontró. Es mucho más rápido y certero que el lector en JavaScript.
 */
class Detector(private val responder: (String, String) -> Unit) {

    private val lectores = HashMap<String, BarcodeScanner>()

    private fun formatoDe(nombre: String): Int = when (nombre.trim().lowercase()) {
        "code_128" -> Barcode.FORMAT_CODE_128
        "code_39" -> Barcode.FORMAT_CODE_39
        "code_93" -> Barcode.FORMAT_CODE_93
        "codabar" -> Barcode.FORMAT_CODABAR
        "ean_13" -> Barcode.FORMAT_EAN_13
        "ean_8" -> Barcode.FORMAT_EAN_8
        "itf" -> Barcode.FORMAT_ITF
        "upc_a" -> Barcode.FORMAT_UPC_A
        "upc_e" -> Barcode.FORMAT_UPC_E
        "qr_code" -> Barcode.FORMAT_QR_CODE
        "pdf417" -> Barcode.FORMAT_PDF417
        "aztec" -> Barcode.FORMAT_AZTEC
        "data_matrix" -> Barcode.FORMAT_DATA_MATRIX
        else -> 0
    }

    private fun nombreDe(formato: Int): String = when (formato) {
        Barcode.FORMAT_CODE_128 -> "code_128"
        Barcode.FORMAT_CODE_39 -> "code_39"
        Barcode.FORMAT_CODE_93 -> "code_93"
        Barcode.FORMAT_CODABAR -> "codabar"
        Barcode.FORMAT_EAN_13 -> "ean_13"
        Barcode.FORMAT_EAN_8 -> "ean_8"
        Barcode.FORMAT_ITF -> "itf"
        Barcode.FORMAT_UPC_A -> "upc_a"
        Barcode.FORMAT_UPC_E -> "upc_e"
        Barcode.FORMAT_QR_CODE -> "qr_code"
        Barcode.FORMAT_PDF417 -> "pdf417"
        Barcode.FORMAT_AZTEC -> "aztec"
        Barcode.FORMAT_DATA_MATRIX -> "data_matrix"
        else -> "unknown"
    }

    @Synchronized
    private fun lectorPara(formatos: String): BarcodeScanner {
        val clave = formatos.trim().lowercase()
        val existente = lectores[clave]
        if (existente != null) return existente
        val numeros = ArrayList<Int>()
        for (parte in clave.split(",")) {
            val f = formatoDe(parte)
            if (f != 0 && !numeros.contains(f)) numeros.add(f)
        }
        val opciones = if (numeros.isEmpty()) {
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
        } else {
            val resto = IntArray(numeros.size - 1)
            for (i in 1 until numeros.size) resto[i - 1] = numeros[i]
            BarcodeScannerOptions.Builder().setBarcodeFormats(numeros[0], *resto).build()
        }
        val nuevo = BarcodeScanning.getClient(opciones)
        lectores[clave] = nuevo
        return nuevo
    }

    /** Lee una imagen (JPEG en base64) y responde con la lista de códigos en formato JSON. */
    fun detectar(id: String, formatos: String, jpegBase64: String) {
        try {
            val bytes = Base64.decode(jpegBase64, Base64.DEFAULT)
            val mapa = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (mapa == null) {
                responder(id, "[]")
                return
            }
            val imagen = InputImage.fromBitmap(mapa, 0)
            lectorPara(formatos).process(imagen)
                .addOnSuccessListener { lista ->
                    val salida = JSONArray()
                    for (b in lista) {
                        val valor = b.rawValue
                        if (valor.isNullOrEmpty()) continue
                        val o = JSONObject()
                        o.put("rawValue", valor)
                        o.put("format", nombreDe(b.format))
                        val caja = b.boundingBox
                        if (caja != null) {
                            val c = JSONObject()
                            c.put("x", caja.left)
                            c.put("y", caja.top)
                            c.put("width", caja.width())
                            c.put("height", caja.height())
                            o.put("boundingBox", c)
                        }
                        salida.put(o)
                    }
                    mapa.recycle()
                    responder(id, salida.toString())
                }
                .addOnFailureListener {
                    mapa.recycle()
                    responder(id, "[]")
                }
        } catch (e: Exception) {
            responder(id, "[]")
        }
    }

    fun cerrar() {
        for (l in lectores.values) {
            try {
                l.close()
            } catch (e: Exception) {
            }
        }
        lectores.clear()
    }
}
