#!/usr/bin/env python3
"""
Prepara los archivos web que van DENTRO de la APK (carpeta app/src/main/assets/www).

Toma las páginas del repositorio (JAT_Control_Suite_Movil.html y JAT_Salida_Mercaderia.html),
les cambia las direcciones de internet (CDN) por copias locales y les agrega el adaptador de Android.
Uso:  python3 preparar.py [carpeta_node_modules]
"""
import re
import shutil
import sys
from pathlib import Path

AQUI = Path(__file__).resolve().parent
RAIZ = AQUI.parent
NM = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else AQUI / "npmlibs" / "node_modules"
WWW = AQUI / "app" / "src" / "main" / "assets" / "www"
LIB = WWW / "lib"

errores = []
avisos = []


def falta(msg):
    errores.append(msg)
    print("ERROR:", msg)


def copiar(origen, destino):
    origen = Path(origen)
    if not origen.exists():
        falta(f"no existe {origen}")
        return
    destino.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(origen, destino)


def reemplazar(texto, viejo, nuevo, nombre, obligatorio=True, veces=None):
    n = texto.count(viejo)
    if n == 0:
        (falta if obligatorio else avisos.append)(f"{nombre}: no se encontró {viejo[:70]!r}")
        return texto
    if veces is not None and n != veces:
        avisos.append(f"{nombre}: se esperaban {veces} y hay {n} de {viejo[:60]!r}")
    return texto.replace(viejo, nuevo)


shutil.rmtree(WWW, ignore_errors=True)
WWW.mkdir(parents=True)

# ------------------------------------------------------------------ librerías locales
copiar(NM / "pdfjs-dist/build/pdf.min.js", LIB / "pdf.min.js")
copiar(NM / "pdfjs-dist/build/pdf.worker.min.js", LIB / "pdf.worker.min.js")
copiar(NM / "jspdf/dist/jspdf.umd.min.js", LIB / "jspdf.umd.min.js")
copiar(NM / "xlsx/dist/xlsx.full.min.js", LIB / "xlsx.full.min.js")
copiar(NM / "@zxing/library/umd/index.min.js", LIB / "zxing.min.js")
copiar(NM / "tesseract.js/dist/tesseract.min.js", LIB / "tesseract/tesseract.min.js")
copiar(NM / "tesseract.js/dist/worker.min.js", LIB / "tesseract/worker.min.js")
for nombre in ("tesseract-core-lstm.wasm.js", "tesseract-core-simd-lstm.wasm.js"):
    copiar(NM / "tesseract.js-core" / nombre, LIB / "tesseract/core" / nombre)
for idioma in ("eng", "spa"):
    copiar(NM / "@tesseract.js-data" / idioma / "4.0.0_best_int" / f"{idioma}.traineddata.gz",
           LIB / "tesseract/lang" / f"{idioma}.traineddata.gz")

# Tipos de letra (antes venían de Google Fonts)
css = []
for familia, carpeta, pesos in (("Inter", "inter", (400, 500, 600, 700, 800)),
                                ("JetBrains Mono", "jetbrains-mono", (500, 700))):
    for peso in pesos:
        archivo = NM / "@fontsource" / carpeta / "files" / f"{carpeta}-latin-{peso}-normal.woff2"
        if not archivo.exists():
            avisos.append(f"letra sin copia: {archivo.name}")
            continue
        copiar(archivo, LIB / "fonts" / archivo.name)
        css.append("@font-face{font-family:'%s';font-style:normal;font-display:swap;font-weight:%d;"
                   "src:url(fonts/%s) format('woff2');}" % (familia, peso, archivo.name))
(LIB / "fonts.css").write_text("\n".join(css) + "\n", encoding="utf-8")

# ------------------------------------------------------------------ adaptador, datos e íconos
copiar(AQUI / "web" / "jat-android.js", WWW / "jat-android.js")
for nombre in ("kardex_nombres.json", "kardex_categorias.json", "logo-jat-personal.svg",
               "icon-personal-192.png", "icon-personal-512.png"):
    copiar(RAIZ / nombre, WWW / nombre)
# Lector de la pantalla del egreso de OneView (ventana OneView de la app)
copiar(RAIZ / "oneview" / "jat-oneview.js", WWW / "jat-oneview.js")

# ------------------------------------------------------------------ páginas
CDN = [
    ("https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js", "lib/pdf.min.js"),
    ("https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js", "lib/pdf.worker.min.js"),
    ("https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js", "lib/jspdf.umd.min.js"),
    ("https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js", "lib/xlsx.full.min.js"),
    ("https://cdn.jsdelivr.net/npm/tesseract.js@5/dist/tesseract.min.js", "lib/tesseract/tesseract.min.js"),
    ("https://cdn.jsdelivr.net/npm/@zxing/library@0.20.0/umd/index.min.js", "lib/zxing.min.js"),
]
FUENTES = ('<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="stylesheet" '
           'href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap">')


def procesar(origen, destino, extra=None):
    nombre = origen.name
    t = origen.read_text(encoding="utf-8")
    # 1) el adaptador va primero de todo
    m = re.search(r"<head[^>]*>", t, re.I)
    if not m:
        falta(f"{nombre}: no tiene <head>")
        return
    t = t[:m.end()] + '\n<script src="jat-android.js"></script>' + t[m.end():]
    # 2) librerías de internet -> copias locales
    for url, local in CDN:
        if url in t:
            t = t.replace(url, local)
    if FUENTES in t:
        t = t.replace(FUENTES, '<link rel="stylesheet" href="lib/fonts.css">')
    else:
        avisos.append(f"{nombre}: no se encontró el enlace de Google Fonts")
    # 3) lector de texto (OCR) con archivos locales
    t, n = re.subn(r"createWorker\(\s*\[\s*'eng'\s*,\s*'spa'\s*\]\s*\)",
                   "createWorker(['eng', 'spa'], 1, window.JAT_TESS_OPTS)", t)
    if n == 0:
        avisos.append(f"{nombre}: no se encontró createWorker")
    if extra:
        t = extra(t, nombre)
    # 4) aviso si queda alguna dirección de internet de librerías
    for rest in sorted(set(re.findall(r"https://(?:cdnjs\.cloudflare\.com|cdn\.jsdelivr\.net|fonts\.googleapis\.com)[^'\"\s)]*", t))):
        avisos.append(f"{nombre}: sigue pidiendo a internet {rest}")
    (WWW / destino).write_text(t, encoding="utf-8")


def extra_principal(t, nombre):
    # El respaldo dice dónde quedó el archivo
    t = reemplazar(t, "bajar(nom, txt); return 'Descargas';",
                   "bajar(nom, txt); return window.JATAndroid ? 'Descargas › JAT Control Suite' : 'Descargas';",
                   nombre, obligatorio=False)
    # En la app no se "instala": en su lugar se ofrece buscar actualización
    viejo1 = "JAT_FS.enApp() ? null : { ico: '📲', txt: 'Instalar como app', sub: 'Se abre siempre en pantalla completa', run: instalar }"
    nuevo1 = ("window.JATAndroid ? { ico: '🔄', txt: 'Buscar actualización', sub: 'Descarga la última versión de la app', "
              "run: () => JATAndroid.buscarActualizacion() } : " + viejo1)
    t = reemplazar(t, viejo1, nuevo1, nombre, obligatorio=False)
    viejo2 = "JAT_FS.enApp() ? null : { ico: '📲', txt: 'Instalar como app', run: instalar }"
    nuevo2 = ("window.JATAndroid ? { ico: '🔄', txt: 'Buscar actualización', "
              "run: () => JATAndroid.buscarActualizacion() } : " + viejo2)
    t = reemplazar(t, viejo2, nuevo2, nombre, obligatorio=False)
    return t


procesar(RAIZ / "JAT_Control_Suite_Movil.html", "app.html", extra_principal)
procesar(RAIZ / "JAT_Salida_Mercaderia.html", "JAT_Salida_Mercaderia.html")

# ------------------------------------------------------------------ resumen
total = sum(f.stat().st_size for f in WWW.rglob("*") if f.is_file())
print(f"Listo: {sum(1 for f in WWW.rglob('*') if f.is_file())} archivos, {total / 1e6:.1f} MB en {WWW}")
for a in avisos:
    print("AVISO:", a)
if errores:
    print(f"{len(errores)} error(es)")
    sys.exit(1)
