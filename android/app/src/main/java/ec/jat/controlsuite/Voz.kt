package ec.jat.controlsuite

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Voz del teléfono (la misma que usa Google). Reemplaza a speechSynthesis de la pantalla web,
 * que en Android a veces se queda muda. Avisa a la pantalla cuando empieza y termina cada frase.
 */
class Voz(contexto: Context, private val avisar: (String, String) -> Unit) {

    private class Pedido(val id: String, val texto: String, val idioma: String, val velocidad: Float, val tono: Float, val volumen: Float)

    private var tts: TextToSpeech? = null
    private var listo = false
    private val pendientes = ArrayList<Pedido>()
    private var idiomaActual = ""

    init {
        tts = TextToSpeech(contexto.applicationContext) { estado ->
            if (estado == TextToSpeech.SUCCESS) {
                listo = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (utteranceId != null) avisar("start", utteranceId)
                    }

                    override fun onDone(utteranceId: String?) {
                        if (utteranceId != null) avisar("end", utteranceId)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId != null) avisar("error", utteranceId)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (utteranceId != null) avisar("error", utteranceId)
                    }
                })
                val cola = ArrayList(pendientes)
                pendientes.clear()
                for (p in cola) hablarYa(p)
            } else {
                val cola = ArrayList(pendientes)
                pendientes.clear()
                for (p in cola) avisar("error", p.id)
            }
        }
    }

    private fun elegirIdioma(codigo: String) {
        if (codigo == idiomaActual) return
        val motor = tts ?: return
        val candidatos = ArrayList<Locale>()
        if (codigo.isNotEmpty()) candidatos.add(Locale.forLanguageTag(codigo))
        candidatos.add(Locale.forLanguageTag("es-EC"))
        candidatos.add(Locale.forLanguageTag("es-MX"))
        candidatos.add(Locale.forLanguageTag("es-US"))
        candidatos.add(Locale.forLanguageTag("es-ES"))
        candidatos.add(Locale("es"))
        for (l in candidatos) {
            val r = motor.setLanguage(l)
            if (r == TextToSpeech.LANG_AVAILABLE || r == TextToSpeech.LANG_COUNTRY_AVAILABLE || r == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                idiomaActual = codigo
                return
            }
        }
    }

    private fun hablarYa(p: Pedido) {
        val motor = tts ?: return
        try {
            elegirIdioma(p.idioma)
            motor.setSpeechRate(p.velocidad.coerceIn(0.3f, 3.0f))
            motor.setPitch(p.tono.coerceIn(0.3f, 2.0f))
            val extras = Bundle()
            extras.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, p.volumen.coerceIn(0f, 1f))
            val r = motor.speak(p.texto, TextToSpeech.QUEUE_ADD, extras, p.id)
            if (r != TextToSpeech.SUCCESS) avisar("error", p.id)
        } catch (e: Exception) {
            avisar("error", p.id)
        }
    }

    fun hablar(id: String, texto: String, idioma: String, velocidad: Double, tono: Double, volumen: Double) {
        val p = Pedido(id, texto, idioma, velocidad.toFloat(), tono.toFloat(), volumen.toFloat())
        if (listo) hablarYa(p) else pendientes.add(p)
    }

    fun parar() {
        pendientes.clear()
        try {
            tts?.stop()
        } catch (e: Exception) {
        }
    }

    fun cerrar() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
        }
        tts = null
        listo = false
    }
}
