/* JAT Control Suite para Android — adaptador entre la página y el teléfono.
   Solo hace algo dentro de la APK (cuando existe window.JATAndroid). En el navegador no cambia nada. */
(function () {
  'use strict';
  var N = window.JATAndroid;
  if (!N) return;

  var TOP;
  try { void window.top.document; TOP = window.top; } catch (e) { TOP = window; }
  if (!TOP.__jatEsperas) TOP.__jatEsperas = {};
  if (!TOP.__jatNativo) {
    // El teléfono avisa aquí (siempre a la página principal) y se reparte a quien esperaba la respuesta.
    TOP.__jatNativo = function (tipo, id, carga) {
      var e = TOP.__jatEsperas[id];
      if (e) { try { e(tipo, carga); } catch (_) {} }
    };
  }
  var esperas = TOP.__jatEsperas;
  var contador = 0;
  function nuevoId(p) {
    contador++;
    return p + Date.now().toString(36) + contador + Math.floor(Math.random() * 1e6).toString(36);
  }

  /* ---------- Guardado automático de egresos en Descargas › JAT Control Suite (27 nov 2026) ----------
     La página ya sabe guardar en el teléfono con "HatAndroid" (Egresos/*.hat, Backups, Reportes, Config).
     Aquí se lo damos: cada egreso queda como archivo en Descargas › JAT Control Suite y OneSync lo sube a OneDrive. */
  var LUGAR = 'Descargas › JAT Control Suite';
  if (!window.HatAndroid && typeof N.guardarArchivo === 'function') {
    window.HatAndroid = {
      guardarArchivo: function (ruta, contenido, tipo) {
        try { return String(N.guardarArchivo(String(ruta), String(contenido == null ? '' : contenido), String(tipo || ''))); }
        catch (e) { return 'ERR:' + (e && e.message || e); }
      },
      elegirCarpetaOneDrive: function () {
        try { N.avisar('Los egresos se guardan solos en ' + LUGAR + '. OneSync los sube a OneDrive.'); } catch (_) {}
        setTimeout(function () { try { if (typeof syncOneDriveNow === 'function') syncOneDriveNow(true); } catch (_) {} }, 300);
      }
    };
    try { localStorage.setItem('hat_work_folder_name', LUGAR); } catch (_) {}
    // Al abrir la app: una copia de todo (Config/HAT_EGRESOS_RESPALDO.json) y del egreso en curso
    window.addEventListener('load', function () {
      setTimeout(function () {
        try { if (typeof syncOneDriveNow === 'function' && Array.isArray(egresosGuardados) && egresosGuardados.length) syncOneDriveNow(false); } catch (_) {}
      }, 4000);
    });
  }

  /* ---------- Se comporta como app instalada ---------- */
  try { Object.defineProperty(navigator, 'standalone', { value: true, configurable: true }); } catch (_) {}

  /* ---------- Pantalla completa: la maneja el teléfono ---------- */
  function pc(on) { try { N.pantallaCompleta(!!on); } catch (_) {} return Promise.resolve(); }
  try {
    Element.prototype.requestFullscreen = function () { return pc(true); };
    Element.prototype.webkitRequestFullscreen = Element.prototype.requestFullscreen;
    Document.prototype.exitFullscreen = function () { return pc(false); };
    Document.prototype.webkitExitFullscreen = Document.prototype.exitFullscreen;
  } catch (_) {}
  if (window === TOP) {
    var fsAntes = null;
    var revisarFs = function () {
      var q = true;
      try { q = localStorage.getItem('jat_pantalla_completa') !== 'no'; } catch (_) {}
      if (q !== fsAntes) { fsAntes = q; try { N.pantallaCompleta(q); } catch (_) {} }
    };
    revisarFs();
    setInterval(revisarFs, 1500);
  }

  /* ---------- Archivos hacia el teléfono (por partes) ---------- */
  function leerB64(blob) {
    return new Promise(function (ok, mal) {
      var fr = new FileReader();
      fr.onload = function () { var s = String(fr.result); ok(s.slice(s.indexOf(',') + 1)); };
      fr.onerror = function () { mal(fr.error); };
      fr.readAsDataURL(blob);
    });
  }
  async function subir(blob) {
    var id = nuevoId('u');
    var r = String(N.iniciarSubida(id));
    if (r.indexOf('OK') !== 0) throw new Error(r);
    var T = 196608; /* múltiplo de 3: cada parte se convierte sola */
    for (var i = 0; i < blob.size; i += T) {
      var b64 = await leerB64(blob.slice(i, i + T));
      r = String(N.parteSubida(id, b64));
      if (r.indexOf('OK') !== 0) throw new Error(r);
    }
    return id;
  }

  /* ---------- Descargas: se guardan en Descargas/JAT Control Suite ---------- */
  function interceptar(a) {
    try {
      if (!a || !a.hasAttribute || !a.hasAttribute('download')) return false;
      var href = a.href || '';
      if (!/^(blob:|data:)/i.test(href)) return false;
      var nom = a.getAttribute('download') || 'archivo';
      var silencio = /_auto_/.test(nom); /* los respaldos automáticos no hacen ruido */
      fetch(href).then(function (r) { return r.blob(); })
        .then(function (b) { return subir(b); })
        .then(function (id) { N.guardarSubida(id, nom, !silencio); })
        .catch(function () { try { N.avisar('No se pudo guardar ' + nom); } catch (_) {} });
      return true;
    } catch (_) { return false; }
  }
  try {
    var clickOriginal = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function () {
      if (interceptar(this)) return;
      return clickOriginal.apply(this, arguments);
    };
    var despachoOriginal = EventTarget.prototype.dispatchEvent;
    EventTarget.prototype.dispatchEvent = function (ev) {
      try { if (ev && ev.type === 'click' && this instanceof HTMLAnchorElement && interceptar(this)) return true; } catch (_) {}
      return despachoOriginal.apply(this, arguments);
    };
    document.addEventListener('click', function (e) {
      var a = e.target && e.target.closest ? e.target.closest('a[download]') : null;
      if (a && interceptar(a)) { e.preventDefault(); e.stopPropagation(); }
    }, true);
  } catch (_) {}

  /* ---------- Ventanas nuevas: imprimir reporte, PDF, enlaces ---------- */
  function ventanaVacia() {
    var w = { closed: false, focus: function () {}, blur: function () {}, close: function () { w.closed = true; },
      print: function () {}, addEventListener: function () {}, removeEventListener: function () {},
      location: { href: '' }, document: { write: function () {}, writeln: function () {}, open: function () {}, close: function () {} } };
    return w;
  }
  function ventanaImpresion() {
    var partes = [];
    var w = ventanaVacia();
    w.document.write = function (s) { partes.push(String(s)); };
    w.document.writeln = function (s) { partes.push(String(s) + '\n'); };
    w.print = function () {
      var h = partes.join('');
      var m = /<title>([^<]*)<\/title>/i.exec(h);
      try { N.imprimirHtml(h, m ? m[1] : 'Reporte'); } catch (_) {}
    };
    return w;
  }
  function abrirBlob(url) {
    fetch(url).then(function (r) { return r.blob(); }).then(async function (b) {
      var id = await subir(b);
      if (/pdf/i.test(b.type)) N.imprimirPdfSubida(id, 'Egreso revisado.pdf');
      else N.guardarSubida(id, 'archivo', true);
    }).catch(function () { try { N.avisar('No se pudo abrir el archivo'); } catch (_) {} });
  }
  window.open = function (url) {
    url = url == null ? '' : String(url);
    if (url === '' || url === 'about:blank') return ventanaImpresion();
    if (/^blob:/i.test(url)) { abrirBlob(url); return ventanaVacia(); }
    if (/^(https?|tel|mailto|whatsapp|intent):/i.test(url)) { try { N.abrirNavegador(url); } catch (_) {} }
    return ventanaVacia();
  };
  window.print = function () {
    try { N.imprimirHtml('<!DOCTYPE html>' + document.documentElement.outerHTML, document.title || 'JAT Control Suite'); } catch (_) {}
  };

  /* ---------- Compartir (WhatsApp, Drive, OneDrive, correo) ---------- */
  try {
    navigator.canShare = function (d) {
      if (!d) return false;
      if (d.files && d.files.length) return true;
      return !!(d.text || d.url || d.title);
    };
    navigator.share = async function (d) {
      d = d || {};
      if (d.files && d.files.length) {
        var f = d.files[0];
        var id = await subir(f);
        var r = String(N.compartirSubida(id, f.name || 'archivo', f.type || '', d.title || f.name || ''));
        if (r.indexOf('OK') !== 0) throw new Error(r);
        return;
      }
      var txt = [d.text, d.url].filter(Boolean).join('\n');
      N.compartirTexto(d.title || '', txt);
    };
  } catch (_) {}

  /* ---------- Lector de códigos de barras de Google (ML Kit) ---------- */
  if (N.detectar) {
    var FORMATOS = ['aztec', 'codabar', 'code_128', 'code_39', 'code_93', 'data_matrix', 'ean_13', 'ean_8', 'itf', 'pdf417', 'qr_code', 'upc_a', 'upc_e'];
    var lienzo = null;
    var dibujar = function (src, maximo) {
      var w = src.videoWidth || src.naturalWidth || src.width;
      var h = src.videoHeight || src.naturalHeight || src.height;
      if (!w || !h) return null;
      var esc = Math.min(1, maximo / Math.max(w, h));
      var cw = Math.max(1, Math.round(w * esc)), ch = Math.max(1, Math.round(h * esc));
      if (!lienzo) lienzo = document.createElement('canvas');
      lienzo.width = cw; lienzo.height = ch;
      lienzo.getContext('2d').drawImage(src, 0, 0, cw, ch);
      var url = lienzo.toDataURL('image/jpeg', 0.85);
      return { b64: url.slice(url.indexOf(',') + 1), esc: esc };
    };
    var BD = function (op) {
      this._f = (op && op.formats && op.formats.length) ? op.formats.join(',') : '';
    };
    BD.getSupportedFormats = function () { return Promise.resolve(FORMATOS.slice()); };
    BD.prototype.detect = function (src) {
      var self = this;
      var pedir = function (fuente) {
        return new Promise(function (ok) {
          try {
            var d = dibujar(fuente, (fuente && fuente.tagName === 'VIDEO') ? 1280 : 2000);
            if (!d) { ok([]); return; }
            var id = nuevoId('d');
            var fin = false;
            var t = setTimeout(function () { if (!fin) { fin = true; delete esperas[id]; ok([]); } }, 4000);
            esperas[id] = function (tipo, carga) {
              if (fin) return;
              fin = true; clearTimeout(t); delete esperas[id];
              var lista = [];
              try { lista = JSON.parse(carga); } catch (_) {}
              ok(lista.map(function (c) {
                var bb = c.boundingBox;
                if (bb) c.boundingBox = { x: bb.x / d.esc, y: bb.y / d.esc, width: bb.width / d.esc, height: bb.height / d.esc };
                c.cornerPoints = [];
                return c;
              }));
            };
            N.detectar(id, self._f, d.b64);
          } catch (e) { ok([]); }
        });
      };
      if (typeof Blob !== 'undefined' && src instanceof Blob && window.createImageBitmap) {
        return createImageBitmap(src).then(pedir);
      }
      return pedir(src);
    };
    window.BarcodeDetector = BD;
  }

  /* ---------- Voz del teléfono ---------- */
  if (N.hablar) {
    var Enunciado = function (t) {
      this.text = t == null ? '' : String(t);
      this.lang = ''; this.rate = 1; this.pitch = 1; this.volume = 1; this.voice = null;
      this.onstart = null; this.onend = null; this.onerror = null; this.onboundary = null;
      this.onpause = null; this.onresume = null; this.onmark = null;
    };
    Enunciado.prototype.addEventListener = function (t, f) { this['on' + t] = f; };
    Enunciado.prototype.removeEventListener = function () {};
    window.SpeechSynthesisUtterance = Enunciado;

    var VOZ = { name: 'Español (voz del teléfono)', lang: 'es-EC', localService: true, 'default': true, voiceURI: 'jat-es' };
    var activos = {};
    var cuantos = 0;
    var alCambiarVoces = null;
    var sintesis = {
      paused: false,
      getVoices: function () { return [VOZ]; },
      speak: function (u) {
        var id = nuevoId('t');
        activos[id] = u; cuantos++;
        esperas[id] = function (_t, evento) {
          if (evento === 'start') {
            try { if (u.onstart) u.onstart({ type: 'start', utterance: u }); } catch (_) {}
          } else if (evento === 'end' || evento === 'error') {
            delete esperas[id];
            if (activos[id]) { delete activos[id]; cuantos = Math.max(0, cuantos - 1); }
            var h = evento === 'end' ? u.onend : u.onerror;
            try { if (h) h({ type: evento, utterance: u, error: evento === 'error' ? 'synthesis-failed' : undefined }); } catch (_) {}
          }
        };
        var idioma = (u.voice && u.voice.lang) || u.lang || 'es-EC';
        var vol = (u.volume === undefined || u.volume === null) ? 1 : Number(u.volume);
        try { N.hablar(id, String(u.text), idioma, Number(u.rate) || 1, Number(u.pitch) || 1, vol); }
        catch (_) { esperas[id]('tts', 'error'); }
      },
      cancel: function () {
        try { N.pararVoz(); } catch (_) {}
        var ids = Object.keys(activos);
        for (var i = 0; i < ids.length; i++) {
          var u = activos[ids[i]];
          delete activos[ids[i]]; delete esperas[ids[i]];
          try { if (u && u.onerror) u.onerror({ type: 'error', utterance: u, error: 'interrupted' }); } catch (_) {}
        }
        cuantos = 0;
      },
      pause: function () {}, resume: function () {},
      addEventListener: function () {}, removeEventListener: function () {}
    };
    Object.defineProperty(sintesis, 'speaking', { get: function () { return cuantos > 0; } });
    Object.defineProperty(sintesis, 'pending', { get: function () { return false; } });
    Object.defineProperty(sintesis, 'onvoiceschanged', {
      get: function () { return alCambiarVoces; },
      set: function (f) { alCambiarVoces = f; if (typeof f === 'function') setTimeout(function () { try { f({ type: 'voiceschanged' }); } catch (_) {} }, 0); }
    });
    try { Object.defineProperty(window, 'speechSynthesis', { value: sintesis, configurable: true, writable: true }); } catch (_) {}
  }

  /* ---------- Dictado por voz ---------- */
  if (N.dictar) {
    var Dictado = function () {
      this.lang = 'es-EC'; this.interimResults = false; this.maxAlternatives = 1; this.continuous = false;
      this.onstart = null; this.onresult = null; this.onerror = null; this.onend = null; this._id = null;
    };
    Dictado.prototype.start = function () {
      var self = this;
      if (self._id) throw new Error('InvalidStateError');
      var id = nuevoId('s');
      self._id = id;
      esperas[id] = function (tipo, carga) {
        delete esperas[id]; self._id = null;
        if (carga.indexOf('ok:') === 0) {
          var t = carga.slice(3);
          if (t) {
            var alt = { transcript: t, confidence: 0.9 };
            var res = [alt]; res.isFinal = true;
            var lista = [res];
            try { if (self.onresult) self.onresult({ type: 'result', resultIndex: 0, results: lista }); } catch (_) {}
          } else {
            try { if (self.onerror) self.onerror({ type: 'error', error: 'no-speech' }); } catch (_) {}
          }
        } else if (carga.indexOf('err:') === 0) {
          try { if (self.onerror) self.onerror({ type: 'error', error: carga.slice(4) }); } catch (_) {}
        }
        try { if (self.onend) self.onend({ type: 'end' }); } catch (_) {}
      };
      try { if (self.onstart) self.onstart({ type: 'start' }); } catch (_) {}
      N.dictar(id, self.lang || 'es-EC');
    };
    Dictado.prototype.stop = function () {};
    Dictado.prototype.abort = function () {};
    window.SpeechRecognition = Dictado;
    window.webkitSpeechRecognition = Dictado;
  }

  /* ---------- Botón Atrás del teléfono: cierra ventanas abiertas ---------- */
  window.__jatAtras = function () {
    try {
      var f = document.getElementById('jsBodegaFrame');
      if (f && f.contentWindow && f.contentWindow.__jatAtras && f.contentWindow.__jatAtras()) return true;
    } catch (_) {}
    try {
      var q = document.querySelector('.resp-bv, #jimpModal, .jimp-modal');
      if (q) { q.remove(); return true; }
      if (document.body && document.body.classList.contains('jat-lanz-abierto')) {
        document.body.classList.remove('jat-lanz-abierto');
        return true;
      }
      var t = document.getElementById('tablaPantallaCompleta');
      if (t && t.classList.contains('abierta') && typeof window.cerrarTablaPantallaCompleta === 'function') {
        window.cerrarTablaPantallaCompleta();
        return true;
      }
      var abiertos = document.querySelectorAll('.modal, [id^="sh"]');
      for (var i = abiertos.length - 1; i >= 0; i--) {
        var el = abiertos[i];
        if (el.classList.contains('modal') || /^sh[A-Z]/.test(el.id || '')) {
          var cs = getComputedStyle(el);
          if (cs.display !== 'none' && cs.visibility !== 'hidden') { el.style.display = 'none'; return true; }
        }
      }
    } catch (_) {}
    return false;
  };

  /* ---------- Opciones para el tesseract (lector de texto sin internet) ---------- */
  var base = location.origin;
  window.JAT_TESS_OPTS = {
    workerPath: base + '/lib/tesseract/worker.min.js',
    corePath: base + '/lib/tesseract/core',
    langPath: base + '/lib/tesseract/lang',
    gzip: true,
    workerBlobURL: false
  };

  window.JAT_APK = true;
})();
