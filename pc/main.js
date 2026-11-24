// JAT Control Suite PC — aplicación de escritorio (Windows).
// Abre el verificador como programa propio y guarda directo en la carpeta de trabajo
// (OneDrive › BODEGA RICAURTE › REVISION DIGITAL), sin pedir permisos cada vez.
const { app, BrowserWindow, ipcMain, dialog, shell, Menu } = require('electron');
const path = require('path');
const fs = require('fs');
const os = require('os');

const CARPETA_DEFECTO = path.join('BODEGA RICAURTE', 'REVISION DIGITAL');
const SUBCARPETAS = ['Egresos', 'Backups', 'Reportes', 'Imprimir', 'Config'];

let ventana = null;

// ---------------------------------------------------------------- configuración
const archivoConfig = () => path.join(app.getPath('userData'), 'config.json');
function leerConfig() {
  try { return JSON.parse(fs.readFileSync(archivoConfig(), 'utf8')); } catch (_) { return {}; }
}
function guardarConfig(c) {
  try {
    fs.mkdirSync(path.dirname(archivoConfig()), { recursive: true });
    fs.writeFileSync(archivoConfig(), JSON.stringify(c, null, 2));
  } catch (e) { console.warn('config', e); }
}

// Busca REVISION DIGITAL dentro de OneDrive de este usuario (sirve en cualquier PC,
// sin depender del nombre de usuario de Windows).
function buscarCarpetaDefecto() {
  const raices = [process.env.OneDrive, process.env.OneDriveConsumer, process.env.OneDriveCommercial,
    path.join(os.homedir(), 'OneDrive'), path.join(os.homedir(), 'OneDrive - Personal')].filter(Boolean);
  for (const r of raices) {
    const p = path.join(r, CARPETA_DEFECTO);
    if (fs.existsSync(p)) return p;
  }
  return null;
}

function carpetaBase() {
  const c = leerConfig();
  if (c.carpeta && fs.existsSync(c.carpeta)) return c.carpeta;
  const d = buscarCarpetaDefecto();
  if (d) { guardarConfig({ ...c, carpeta: d }); return d; }
  return null;
}

function prepararSubcarpetas(base) {
  for (const s of SUBCARPETAS) {
    try { fs.mkdirSync(path.join(base, s), { recursive: true }); } catch (_) {}
  }
}

// Ruta segura: nunca sale de la carpeta de trabajo.
function ruta(rel) {
  const base = carpetaBase();
  if (!base) throw new Error('SIN_CARPETA');
  const partes = String(rel || '').replace(/\\/g, '/').split('/').filter(p => p && p !== '.');
  if (partes.some(p => p === '..')) throw new Error('Ruta no permitida');
  const r = path.join(base, ...partes);
  if (r !== base && !r.startsWith(base + path.sep)) throw new Error('Ruta no permitida');
  return r;
}

// ---------------------------------------------------------------- archivos
ipcMain.handle('carpeta:info', () => {
  const base = carpetaBase();
  if (base) prepararSubcarpetas(base);
  return base ? { ruta: base, nombre: path.basename(base) } : null;
});

ipcMain.handle('carpeta:elegir', async () => {
  const actual = carpetaBase();
  const r = await dialog.showOpenDialog(ventana, {
    title: 'Elige la carpeta de trabajo (REVISION DIGITAL)',
    defaultPath: actual || process.env.OneDrive || os.homedir(),
    properties: ['openDirectory', 'createDirectory']
  });
  if (r.canceled || !r.filePaths[0]) return null;
  const base = r.filePaths[0];
  guardarConfig({ ...leerConfig(), carpeta: base });
  prepararSubcarpetas(base);
  return { ruta: base, nombre: path.basename(base) };
});

ipcMain.handle('carpeta:abrir', async () => {
  const base = carpetaBase();
  if (base) await shell.openPath(base);
});

ipcMain.handle('fs:tipo', (_e, rel) => {
  try { const st = fs.statSync(ruta(rel)); return st.isDirectory() ? 'directory' : 'file'; }
  catch (_) { return null; }
});

ipcMain.handle('fs:mkdir', (_e, rel) => { fs.mkdirSync(ruta(rel), { recursive: true }); return true; });

ipcMain.handle('fs:listar', (_e, rel) => {
  const dir = ruta(rel);
  return fs.readdirSync(dir, { withFileTypes: true })
    .filter(d => d.isFile() || d.isDirectory())
    .map(d => ({ nombre: d.name, tipo: d.isDirectory() ? 'directory' : 'file' }));
});

ipcMain.handle('fs:leer', (_e, rel) => {
  const r = ruta(rel);
  const st = fs.statSync(r);
  return { datos: fs.readFileSync(r), modificado: st.mtimeMs };
});

ipcMain.handle('fs:escribir', (_e, rel, datos) => {
  const r = ruta(rel);
  fs.mkdirSync(path.dirname(r), { recursive: true });
  const tmp = r + '.jatpc-tmp';
  fs.writeFileSync(tmp, Buffer.from(datos));
  fs.renameSync(tmp, r); // escritura segura: el archivo nunca queda a medias
  return true;
});

ipcMain.handle('fs:borrar', (_e, rel, recursivo) => {
  fs.rmSync(ruta(rel), { recursive: !!recursivo, force: false });
  return true;
});

// ---------------------------------------------------------------- ventana
function crearVentana() {
  ventana = new BrowserWindow({
    width: 1440, height: 900, minWidth: 1000, minHeight: 650,
    title: 'JAT Control Suite PC',
    icon: path.join(__dirname, 'app', 'icon-pc-512.png'),
    backgroundColor: '#061b34',
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      spellcheck: false,
      plugins: true // visor de PDF para imprimir los PDF revisados
    }
  });
  ventana.maximize();
  ventana.once('ready-to-show', () => ventana.show());
  ventana.loadFile(path.join(__dirname, 'app', 'index.html'));

  // Ventanas de reportes e impresión que abre el programa
  ventana.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/i.test(url)) { shell.openExternal(url); return { action: 'deny' }; }
    return { action: 'allow', overrideBrowserWindowOptions: { width: 1100, height: 850, autoHideMenuBar: true } };
  });

  // Lo que el programa "descarga" va directo a Descargas, sin preguntar
  ventana.webContents.session.on('will-download', (_e, item) => {
    item.setSavePath(path.join(app.getPath('downloads'), item.getFilename()));
  });

  // Zoom con Ctrl + / Ctrl - / Ctrl 0 (letra más grande) y se recuerda
  const z = leerConfig().zoom;
  ventana.webContents.on('did-finish-load', () => { if (z) ventana.webContents.setZoomFactor(z); });
  ventana.webContents.on('before-input-event', (ev, input) => {
    if (input.type !== 'keyDown' || !input.control || input.alt) return;
    const wc = ventana.webContents;
    let f = null;
    if (input.key === '+' || input.key === '=' ) f = Math.min(3, wc.getZoomFactor() + 0.1);
    else if (input.key === '-') f = Math.max(0.5, wc.getZoomFactor() - 0.1);
    else if (input.key === '0') f = 1;
    if (f !== null) { ev.preventDefault(); wc.setZoomFactor(f); guardarConfig({ ...leerConfig(), zoom: f }); }
  });

  // Micrófono para el dictado
  ventana.webContents.session.setPermissionRequestHandler((_wc, permiso, cb) => {
    cb(['media', 'clipboard-read', 'clipboard-sanitized-write', 'notifications', 'fullscreen'].includes(permiso));
  });
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (ventana) { if (ventana.isMinimized()) ventana.restore(); ventana.focus(); }
  });
  app.whenReady().then(() => {
    Menu.setApplicationMenu(null);
    crearVentana();
  });
  app.on('window-all-closed', () => app.quit());
}
