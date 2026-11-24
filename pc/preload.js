// Puente seguro entre el programa (página) y la carpeta de trabajo en el disco.
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('jatPC', {
  version: () => process.versions.electron,
  carpetaInfo: () => ipcRenderer.invoke('carpeta:info'),
  elegirCarpeta: () => ipcRenderer.invoke('carpeta:elegir'),
  abrirCarpeta: () => ipcRenderer.invoke('carpeta:abrir'),
  tipo: (rel) => ipcRenderer.invoke('fs:tipo', rel),
  mkdir: (rel) => ipcRenderer.invoke('fs:mkdir', rel),
  listar: (rel) => ipcRenderer.invoke('fs:listar', rel),
  leer: (rel) => ipcRenderer.invoke('fs:leer', rel),
  escribir: (rel, datos) => ipcRenderer.invoke('fs:escribir', rel, datos),
  borrar: (rel, recursivo) => ipcRenderer.invoke('fs:borrar', rel, recursivo)
});
