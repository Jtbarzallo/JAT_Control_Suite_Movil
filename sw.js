// JAT Control Suite — service worker mínimo: permite instalar la app (pantalla completa).
// No guarda copias: siempre carga la versión más nueva desde internet.
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', e => e.waitUntil(self.clients.claim()));
self.addEventListener('fetch', () => {});
