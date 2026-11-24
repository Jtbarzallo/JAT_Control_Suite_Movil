#!/usr/bin/env python3
"""
Prepara la página que va DENTRO de la app de PC (carpeta pc/app).

Toma JAT_Control_Suite_PC.html del repositorio (la misma versión de la web), cambia las
librerías de internet por copias locales (funciona sin internet) y le agrega el adaptador
que guarda directo en la carpeta de trabajo. No cambia el archivo original.
Uso:  python3 pc/preparar.py
"""
import shutil
import sys
from pathlib import Path

AQUI = Path(__file__).resolve().parent
RAIZ = AQUI.parent
NM = AQUI / "node_modules"
APP = AQUI / "app"
errores = []


def reemplazar(texto, viejo, nuevo, veces=None):
    n = texto.count(viejo)
    if n == 0 or (veces is not None and n != veces):
        errores.append(f"se esperaba {veces or 'al menos 1'} vez/veces y hay {n}: {viejo[:70]!r}")
        return texto
    return texto.replace(viejo, nuevo)


def copiar(origen, destino):
    if not Path(origen).exists():
        errores.append(f"no existe {origen}")
        return
    destino.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(origen, destino)


shutil.rmtree(APP, ignore_errors=True)
(APP / "lib").mkdir(parents=True)

copiar(NM / "pdfjs-dist/build/pdf.min.js", APP / "lib/pdf.min.js")
copiar(NM / "pdfjs-dist/build/pdf.worker.min.js", APP / "lib/pdf.worker.min.js")
copiar(NM / "jspdf/dist/jspdf.umd.min.js", APP / "lib/jspdf.umd.min.js")
for f in ("icon-pc-192.png", "icon-pc-512.png", "logo-jat-pc.svg", "logo-jat-personal.svg"):
    copiar(RAIZ / f, APP / f)
copiar(AQUI / "adaptador.js", APP / "adaptador.js")

html = (RAIZ / "JAT_Control_Suite_PC.html").read_text(encoding="utf-8")
CDN = "https://cdnjs.cloudflare.com/ajax/libs/"
html = reemplazar(html, CDN + "pdf.js/3.11.174/pdf.worker.min.js", "lib/pdf.worker.min.js")
html = reemplazar(html, CDN + "pdf.js/3.11.174/pdf.min.js", "lib/pdf.min.js")
html = reemplazar(html, CDN + "jspdf/2.5.1/jspdf.umd.min.js", "lib/jspdf.umd.min.js")
if CDN in html:
    errores.append("quedó una librería de internet sin copia local")
html = reemplazar(html, '<link rel="manifest" href="manifest-pc.webmanifest">', "", veces=1)
# El adaptador va antes que todo; el inicio de la carpeta lo hace el adaptador al final
if html.find("</head>") > html.find("<body") or "</head>" not in html:
    errores.append("no se encontró el </head> de la página")
html = html.replace("</head>", '<script src="adaptador.js"></script>\n</head>', 1)
html = reemplazar(html, "    iniciarOneDriveGuardado();\n    initMenu();",
                  "    if (!window.__JAT_PC_APP) iniciarOneDriveGuardado();\n    initMenu();", veces=1)
i = html.rfind("</body>")
html = html[:i] + "<script>window.JATPCIniciar && window.JATPCIniciar();</script>\n" + html[i:]
(APP / "index.html").write_text(html, encoding="utf-8")

# Icono del instalador
copiar(RAIZ / "icon-pc-512.png", AQUI / "build/icon.png")

if errores:
    print("\n".join("ERROR: " + e for e in errores))
    sys.exit(1)
print("Listo: pc/app preparado")
