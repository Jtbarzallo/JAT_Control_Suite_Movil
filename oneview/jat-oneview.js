/* =====================================================================
   JAT — Importar egreso desde OneView (24 nov 2026)
   Lee la pantalla del egreso de OneView (vista "Información general" + "Detalle")
   y la manda a JAT Control Suite.

   Se usa de dos formas:
   1) Chrome del celular o PC: marcador "JAT" (ver oneview/index.html). Muestra una
      ventana con botones: abrir en la app, abrir en la página web, descargar respaldo.
   2) App de Android: la ventana "OneView" de la app inyecta este archivo con
      window.__JAT_SOLO_EXTRAER = true y solo pide los datos (JATOneViewExtraer()).

   No guarda ni envía usuarios ni contraseñas: solo lee la tabla que ya está en pantalla.
   ===================================================================== */
(function () {
  'use strict';

  var BASE = 'https://jtbarzallo.github.io/JAT_Control_Suite_Movil/';
  var PAGINAS = { personal: 'JAT_Control_Suite_Movil.html', venta: 'JAT_Control_Suite_Venta.html' };

  function limpio(t) { return String(t == null ? '' : t).replace(/ /g, ' ').replace(/\s+/g, ' ').trim(); }
  function textoDe(el) { return limpio(el ? (el.innerText || el.textContent || '') : ''); }
  function numero(t) {
    var s = limpio(t).replace(/\s/g, '');
    if (!/^\d{1,3}(?:[.,]\d{3})*(?:[.,]\d+)?$|^\d+(?:[.,]\d+)?$/.test(s)) return null;
    // "1,234.00" / "1.234,00" / "12.00" / "12,5"
    var ultPunto = s.lastIndexOf('.'), ultComa = s.lastIndexOf(',');
    if (ultPunto > -1 && ultComa > -1) s = ultPunto > ultComa ? s.replace(/,/g, '') : s.replace(/\./g, '').replace(',', '.');
    else if (ultComa > -1) s = s.replace(',', '.');
    var n = parseFloat(s);
    return isNaN(n) ? null : n;
  }
  // Código de barras: solo números (6 a 18) o con letras al final como 2025122409004GD / 2025031333397N
  function esCodigo(t) {
    var s = limpio(t);
    return /^\d{6,18}$/.test(s) || (/^[A-Za-z0-9][A-Za-z0-9'’´.\-]{4,24}$/.test(s) && /[A-Za-z]/.test(s) && (s.match(/\d/g) || []).length >= 3);
  }

  // Documentos donde buscar: la página y sus marcos (si son del mismo sitio)
  function documentos() {
    var lista = [document];
    function recorrer(win, prof) {
      if (prof > 3) return;
      for (var i = 0; i < win.frames.length; i++) {
        try { var d = win.frames[i].document; if (d && d.body) { lista.push(d); recorrer(win.frames[i], prof + 1); } } catch (e) { }
      }
    }
    try { recorrer(window, 0); } catch (e) { }
    return lista;
  }

  // ---------------------------------------------------------------- detalle (tabla de productos)
  function productosDesdeTablas(doc) {
    var filas = doc.querySelectorAll('tr');
    var cols = null, productos = [];
    for (var f = 0; f < filas.length; f++) {
      var celdasEl = [];
      for (var k = 0; k < filas[f].children.length; k++) {
        var c = filas[f].children[k];
        if (c.tagName === 'TD' || c.tagName === 'TH') celdasEl.push(c);
      }
      if (!celdasEl.length) continue;
      var celdas = celdasEl.map(textoDe);
      var bajo = celdas.map(function (x) { return x.toLowerCase(); });
      var iCod = -1, iProd = -1, iUni = -1, iBod = -1, iCant = -1;
      bajo.forEach(function (x, i) {
        if (iCod < 0 && /^c[oó]digo/.test(x)) iCod = i;
        else if (iProd < 0 && /^(producto|descripci[oó]n)/.test(x)) iProd = i;
        else if (iUni < 0 && /^unidad$/.test(x)) iUni = i;
        else if (iBod < 0 && /^bodega$/.test(x)) iBod = i;
        else if (iCant < 0 && /^cantidad/.test(x)) iCant = i;
      });
      if (iCod >= 0 && iProd >= 0 && iCant >= 0) { cols = { cod: iCod, prod: iProd, uni: iUni, bod: iBod, cant: iCant }; continue; }
      if (!cols) continue;
      var cod = celdas[cols.cod];
      if (!esCodigo(cod)) continue;
      var cant = numero(celdas[cols.cant]);
      if (cant === null) { // columnas corridas: tomar el último número de la fila
        for (var j = celdas.length - 1; j > cols.cod; j--) { cant = numero(celdas[j]); if (cant !== null) break; }
      }
      var nombre = celdas[cols.prod] || '';
      if (!nombre || cant === null || cant <= 0) continue;
      productos.push({
        codigo: limpio(cod), producto: nombre,
        unidad: cols.uni >= 0 ? celdas[cols.uni] || '' : '',
        bodega: cols.bod >= 0 ? celdas[cols.bod] || '' : '',
        cantidad: cant
      });
    }
    return productos;
  }

  // Respaldo: si la tabla no es <table>, se lee el texto de la pantalla línea por línea
  function productosDesdeTexto(doc) {
    var texto = (doc.body && (doc.body.innerText || doc.body.textContent)) || '';
    var productos = [];
    texto.split(/\r?\n/).forEach(function (linea) {
      var p = linea.split('\t').map(limpio).filter(Boolean);
      if (p.length < 3 || !esCodigo(p[0])) return;
      var cant = numero(p[p.length - 1]);
      if (cant === null || cant <= 0) return;
      var unidad = '', bodega = '', resto = p.slice(1, -1);
      var iU = -1;
      resto.forEach(function (x, i) { if (iU < 0 && i > 0 && /^UNIDAD(ES)?$/i.test(x)) iU = i; });
      if (iU > 0) { unidad = resto[iU]; bodega = resto.slice(iU + 1).join(' '); resto = resto.slice(0, iU); }
      productos.push({ codigo: p[0], producto: resto.join(' '), unidad: unidad, bodega: bodega, cantidad: cant });
    });
    return productos;
  }

  // ---------------------------------------------------------------- cabecera
  var ETIQUETAS = {
    fechaComprobante: /^fecha de comprobante:?$/i,
    tipoComprobante: /^tipo de comprobante:?$/i,
    puntoEmision: /^punto de emisi[oó]n:?$/i,
    establecimiento: /^establecimiento:?$/i,
    concepto: /^concepto:?$/i,
    bodegaOrigen: /^bodega (de )?origen:?$/i,
    bodegaDestino: /^bodega (de )?destino:?$/i,
    noComprobante: /^no\.? ?comprobante:?$/i,
    totalDocumento: /^no\.? ?art[ií]culos:?$/i
  };

  function valorJunto(el) {
    // 1) la celda o elemento siguiente
    var sig = el.nextElementSibling;
    while (sig && !textoDe(sig)) sig = sig.nextElementSibling;
    if (sig) return textoDe(sig);
    // 2) el siguiente del padre (cuando la etiqueta va dentro de <b> o <span>)
    var p = el.parentElement;
    if (p && p.children.length === 1) {
      sig = p.nextElementSibling;
      while (sig && !textoDe(sig)) sig = sig.nextElementSibling;
      if (sig) return textoDe(sig);
    }
    return '';
  }

  function cabecera(doc) {
    var meta = {};
    var todos = doc.body ? doc.body.getElementsByTagName('*') : [];
    for (var i = 0; i < todos.length; i++) {
      var el = todos[i];
      if (el.children.length > 2) continue;
      var t = textoDe(el);
      if (!t || t.length > 30) continue;
      for (var k in ETIQUETAS) {
        if (!meta[k] && ETIQUETAS[k].test(t)) { var v = valorJunto(el); if (v && !ETIQUETAS[k].test(v)) meta[k] = v; }
      }
    }
    if (meta.totalDocumento !== undefined && numero(meta.totalDocumento) === null) delete meta.totalDocumento;
    if (meta.noComprobante && !/\d/.test(meta.noComprobante)) delete meta.noComprobante;
    var texto = (doc.body && (doc.body.innerText || doc.body.textContent)) || '';
    var plano = limpio(texto);
    var m = plano.match(/EGR-\d{3}-\d{3}-\d{3}-\d{6,12}/i);
    if (m) meta.secuencial = m[0].toUpperCase();
    if (!meta.noComprobante) { m = plano.match(/No\.?\s*comprobante\s*:?\s*(\d+)/i); if (m) meta.noComprobante = m[1]; }
    if (!meta.totalDocumento) { m = texto.match(/No\.?\s*art[ií]culos\s*:?\s*([\d.,]+)/i); if (m) meta.totalDocumento = m[1]; }
    // Respaldo con el texto de la pantalla (etiqueta y valor separados por tabulación o salto)
    [['fechaComprobante', /Fecha de comprobante[ \t:]*\n?[ \t]*(\d{2}\/\d{2}\/\d{4})/i],
     ['bodegaOrigen', /Bodega (?:de )?origen[ \t:]*([^\t\n]+)/i],
     ['bodegaDestino', /Bodega (?:de )?destino[ \t:]*([^\t\n]+)/i],
     ['puntoEmision', /Punto de emisi[oó]n[ \t:]*([^\t\n]+)/i]].forEach(function (r) {
      if (!meta[r[0]]) { var x = texto.match(r[1]); if (x && limpio(x[1])) meta[r[0]] = limpio(x[1]); }
    });
    if (meta.fechaComprobante) { m = meta.fechaComprobante.match(/\d{2}\/\d{2}\/\d{4}/); meta.fechaComprobante = m ? m[0] : meta.fechaComprobante; }
    if (meta.noComprobante) { m = String(meta.noComprobante).match(/\d+/); meta.noComprobante = m ? m[0] : ''; }
    if (meta.totalDocumento !== undefined) { var n = numero(meta.totalDocumento); if (n === null) delete meta.totalDocumento; else meta.totalDocumento = n; }
    return meta;
  }

  // ---------------------------------------------------------------- todo junto
  function extraer() {
    var docs = documentos();
    var mejor = null;
    for (var i = 0; i < docs.length; i++) {
      var d = docs[i];
      var productos = productosDesdeTablas(d);
      if (!productos.length) productos = productosDesdeTexto(d);
      if (productos.length && (!mejor || productos.length > mejor.productos.length)) mejor = { doc: d, productos: productos };
    }
    if (!mejor) {
      return { ok: false, error: 'No encontré la tabla de productos. Abre el egreso (la pantalla con "Detalle" y la lista de productos) y vuelve a intentar.' };
    }
    var meta = cabecera(mejor.doc);
    var suma = mejor.productos.reduce(function (s, p) { return s + p.cantidad; }, 0);
    return {
      ok: true,
      datos: {
        jatOneView: 1,
        leido: new Date().toISOString(),
        meta: meta,
        productos: mejor.productos,
        totalLeido: Math.round(suma * 1000) / 1000
      }
    };
  }
  window.JATOneViewExtraer = extraer;
  if (window.__JAT_SOLO_EXTRAER) return; // la app de Android solo pide los datos

  // ================================================================ ventana del marcador (Chrome)
  function b64url(texto) {
    var bytes = new TextEncoder().encode(texto), bin = '';
    for (var i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
    return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }
  function destino() {
    var s = (document.currentScript && document.currentScript.src) || window.__JAT_ONEVIEW_SRC || '';
    return /[?&]d=venta/.test(s) ? 'venta' : 'personal';
  }
  function esc(t) { return String(t == null ? '' : t).replace(/[&<>"]/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]; }); }
  function nombreArchivo(meta) {
    var base = (meta.secuencial || 'EGRESO_ONEVIEW') + (meta.bodegaOrigen ? '_' + meta.bodegaOrigen : '') + (meta.bodegaDestino ? '_A_' + meta.bodegaDestino : '');
    return base.replace(/[^A-Za-z0-9ÁÉÍÓÚÑáéíóúñ_.\-]+/g, '_') + '.oneview.json';
  }

  var dest = destino();
  var viejo = document.getElementById('jatOneViewVentana');
  if (viejo) viejo.remove();

  var r = extraer();
  var caja = document.createElement('div');
  caja.id = 'jatOneViewVentana';
  caja.setAttribute('style', 'position:fixed;inset:0;z-index:2147483647;background:rgba(5,12,24,.88);display:flex;align-items:flex-start;justify-content:center;overflow:auto;padding:16px;font-family:Arial,Helvetica,sans-serif;');
  var boton = 'display:block;width:100%;box-sizing:border-box;margin:12px 0 0;padding:18px 14px;border-radius:14px;border:0;font-size:20px;font-weight:bold;text-align:center;text-decoration:none;cursor:pointer;';
  var html = '<div style="background:#fff;color:#0A1322;max-width:560px;width:100%;border-radius:18px;padding:20px;border:4px solid #C9A227;">' +
    '<div style="font-size:24px;font-weight:bold;color:#0A1F44;margin-bottom:10px;">JAT · Importar egreso</div>';

  if (!r.ok) {
    html += '<div style="font-size:20px;font-weight:bold;color:#B00020;line-height:1.35;">' + esc(r.error) + '</div>' +
      '<button data-x style="' + boton + 'background:#0A1F44;color:#fff;">Cerrar</button></div>';
    caja.innerHTML = html;
    document.body.appendChild(caja);
    caja.querySelector('[data-x]').onclick = function () { caja.remove(); };
    return;
  }

  var d = r.datos, meta = d.meta;
  var json = JSON.stringify(d);
  var carga = b64url(json);
  var webUrl = BASE + PAGINAS[dest] + '#oneview=' + carga;
  var appUrl = 'intent://oneview?d=' + carga + '#Intent;scheme=jatcs;package=ec.jat.controlsuite;S.browser_fallback_url=' + encodeURIComponent(webUrl) + ';end';
  var cuadra = meta.totalDocumento === undefined || Math.abs(meta.totalDocumento - d.totalLeido) < 0.001;

  html += '<div style="font-size:21px;font-weight:bold;line-height:1.35;word-break:break-word;">' + esc(meta.secuencial || 'Egreso sin número') + '</div>' +
    '<div style="font-size:19px;font-weight:bold;margin-top:6px;">' + esc(meta.bodegaOrigen || '¿?') + ' → ' + esc(meta.bodegaDestino || '¿?') + '</div>' +
    '<div style="font-size:19px;font-weight:bold;margin-top:6px;">' + d.productos.length + ' productos · ' + d.totalLeido + ' artículos</div>' +
    (cuadra
      ? '<div style="font-size:18px;font-weight:bold;color:#0B7A32;margin-top:8px;">✔ Cuadra con "No. artículos" del egreso</div>'
      : '<div style="font-size:18px;font-weight:bold;color:#B00020;margin-top:8px;">⚠ El egreso dice ' + meta.totalDocumento + ' artículos y se leyeron ' + d.totalLeido + '. Revisa antes de verificar.</div>') +
    (dest === 'personal' ? '<a data-app href="' + esc(appUrl) + '" style="' + boton + 'background:#C9A227;color:#0A1322;">📱 Abrir en la app JAT</a>' : '') +
    '<a data-web href="' + esc(webUrl) + '" target="_blank" style="' + boton + 'background:#0A1F44;color:#fff;">🌐 Abrir en JAT (página web)</a>' +
    '<button data-bajar style="' + boton + 'background:#E8EDF5;color:#0A1F44;border:2px solid #0A1F44;">💾 Descargar archivo (respaldo)</button>' +
    '<button data-x style="' + boton + 'background:#fff;color:#B00020;border:2px solid #B00020;">Cerrar</button>' +
    '<div style="font-size:15px;color:#333;margin-top:12px;line-height:1.4;">El archivo descargado se puede abrir después en JAT con "Abrir archivo de egreso" o compartirlo a la app.</div>' +
    '</div>';
  caja.innerHTML = html;
  document.body.appendChild(caja);
  caja.querySelector('[data-x]').onclick = function () { caja.remove(); };
  caja.querySelector('[data-bajar]').onclick = function () {
    var a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([json], { type: 'application/json' }));
    a.download = nombreArchivo(meta);
    document.body.appendChild(a); a.click();
    setTimeout(function () { URL.revokeObjectURL(a.href); a.remove(); }, 4000);
  };
})();
