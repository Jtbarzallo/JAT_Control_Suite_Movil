// Adaptador de la app de PC (Electron).
// La página usa la "carpeta de trabajo" del navegador (File System Access API).
// Aquí esa carpeta se reemplaza por la carpeta real del disco a través de window.jatPC,
// así guarda directo en REVISION DIGITAL sin pedir permisos cada vez que se abre.
(function () {
  if (!window.jatPC) return;
  window.__JAT_PC_APP = true;

  const unir = (a, b) => (a ? a + '/' + b : b);
  const noExiste = (n) => new DOMException('No existe: ' + n, 'NotFoundError');

  class ArchivoPC {
    constructor(rel, nombre) { this.kind = 'file'; this.name = nombre; this._rel = rel; }
    async getFile() {
      const r = await window.jatPC.leer(this._rel);
      return new File([r.datos], this.name, { lastModified: Math.round(r.modificado) });
    }
    async createWritable() {
      const rel = this._rel; let partes = [];
      return {
        async write(d) {
          if (d && typeof d === 'object' && d.type === 'write') d = d.data;
          if (d && typeof d === 'object' && (d.type === 'truncate' || d.type === 'seek')) return;
          partes.push(d instanceof Blob ? d : new Blob([d]));
        },
        async truncate() { partes = []; },
        async close() {
          const buf = new Uint8Array(await new Blob(partes).arrayBuffer());
          await window.jatPC.escribir(rel, buf);
        },
        async abort() { partes = []; }
      };
    }
    async queryPermission() { return 'granted'; }
    async requestPermission() { return 'granted'; }
    async isSameEntry(o) { return !!o && o._rel === this._rel; }
  }

  class CarpetaPC {
    constructor(rel, nombre) { this.kind = 'directory'; this.name = nombre; this._rel = rel; }
    async getDirectoryHandle(nombre, op) {
      const rel = unir(this._rel, nombre);
      const t = await window.jatPC.tipo(rel);
      if (t === 'directory') return new CarpetaPC(rel, nombre);
      if (t === 'file') throw new DOMException(nombre + ' es un archivo', 'TypeMismatchError');
      if (op && op.create) { await window.jatPC.mkdir(rel); return new CarpetaPC(rel, nombre); }
      throw noExiste(nombre);
    }
    async getFileHandle(nombre, op) {
      const rel = unir(this._rel, nombre);
      const t = await window.jatPC.tipo(rel);
      if (t === 'file') return new ArchivoPC(rel, nombre);
      if (t === 'directory') throw new DOMException(nombre + ' es una carpeta', 'TypeMismatchError');
      if (op && op.create) { await window.jatPC.escribir(rel, new Uint8Array(0)); return new ArchivoPC(rel, nombre); }
      throw noExiste(nombre);
    }
    async removeEntry(nombre, op) {
      const rel = unir(this._rel, nombre);
      if (!(await window.jatPC.tipo(rel))) throw noExiste(nombre);
      await window.jatPC.borrar(rel, !!(op && op.recursive));
    }
    async *entries() {
      const lista = await window.jatPC.listar(this._rel);
      for (const it of lista) {
        const rel = unir(this._rel, it.nombre);
        yield [it.nombre, it.tipo === 'directory' ? new CarpetaPC(rel, it.nombre) : new ArchivoPC(rel, it.nombre)];
      }
    }
    async *values() { for await (const [, h] of this.entries()) yield h; }
    async *keys() { for await (const [n] of this.entries()) yield n; }
    [Symbol.asyncIterator]() { return this.entries(); }
    async queryPermission() { return 'granted'; }
    async requestPermission() { return 'granted'; }
    async isSameEntry(o) { return !!o && o._rel === this._rel; }
  }

  const raiz = (nombre) => new CarpetaPC('', nombre || 'REVISION DIGITAL');
  window.JATCarpetaPC = { raiz };

  // "Elegir carpeta" abre la ventana de Windows para escoger la carpeta
  window.showDirectoryPicker = async function () {
    const r = await window.jatPC.elegirCarpeta();
    if (!r) throw new DOMException('Cancelado', 'AbortError');
    localStorage.setItem('hat_work_folder_name', r.nombre);
    return raiz(r.nombre);
  };

  // Abrir/guardar archivo: la ventana de Windows no acepta la carpeta simulada como inicio
  ['showOpenFilePicker', 'showSaveFilePicker'].forEach(fn => {
    const orig = window[fn];
    if (typeof orig !== 'function') return;
    window[fn] = function (op) {
      const o = Object.assign({}, op || {});
      if (o.startIn && (o.startIn instanceof CarpetaPC || o.startIn instanceof ArchivoPC)) delete o.startIn;
      return orig.call(window, o);
    };
  });

  // Lo que se ejecuta cuando ya cargó todo el programa
  window.JATPCIniciar = async function () {
    try {
      // La carpeta la recuerda la app (no el navegador)
      window.leerHandleOneDrive = async function () {
        const i = await window.jatPC.carpetaInfo();
        return i ? raiz(i.nombre) : null;
      };
      window.guardarHandleOneDrive = async function () {};
      const i = await window.jatPC.carpetaInfo();
      if (i) {
        localStorage.setItem('oneliv_onedrive_enabled', '1');
        localStorage.setItem('hat_work_folder_name', i.nombre);
      }
      if (typeof iniciarOneDriveGuardado === 'function') await iniciarOneDriveGuardado();
      if (i && typeof setOneDriveStatus === 'function') {
        setOneDriveStatus('Guardado automático activo: ' + i.nombre, 'ok');
      }
      // Respaldos en Descargas si no hubiera carpeta: se intentan de nuevo con la carpeta lista
      document.title = 'JAT Control Suite PC';
    } catch (e) { console.warn('JAT PC', e); }
  };
})();
