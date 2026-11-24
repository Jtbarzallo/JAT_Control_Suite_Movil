# JAT Control Suite — app de Android (APK)

La APK es la versión personal (`JAT_Control_Suite_Movil.html` + `JAT_Salida_Mercaderia.html`) metida en una
ventana web (WebView) con lector de códigos de Google (ML Kit), voz, impresión y guardado en el teléfono.

- Se compila sola en GitHub Actions (`.github/workflows/android.yml`) al cambiar esas páginas o la carpeta `android/`.
- Descarga: https://github.com/Jtbarzallo/JAT_Control_Suite_Movil/releases/latest/download/JATControlSuite.apk
- `preparar.py` copia las páginas a `app/src/main/assets/www`, cambia las librerías de internet por copias locales
  y agrega `web/jat-android.js` (el adaptador entre la página y el teléfono).
- Si falla la compilación, el motivo queda en la rama `registro-compilacion` (archivo `errores.txt`).
