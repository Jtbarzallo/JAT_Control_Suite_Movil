// JAT Control Suite — service worker mínimo: permite instalar la app (pantalla completa).
// No guarda copias: siempre carga la versión más nueva desde internet.
// Además recibe los egresos que se comparten desde WhatsApp hacia "JAT Revisión".
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', e => e.waitUntil(self.clients.claim()));
self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);
  if (e.request.method === 'POST' && url.pathname.endsWith('JAT_Revision_Mercaderia.html') && url.searchParams.has('compartido')) {
    e.respondWith((async () => {
      try {
        const datos = await e.request.formData();
        const f = datos.get('archivo');
        const cache = await caches.open('jatrev-compartido');
        const meta = { texto: [datos.get('title'), datos.get('text')].filter(Boolean).join('\n') };
        if (f && f.size) {
          meta.nombre = f.name; meta.tipo = f.type;
          await cache.put('archivo', new Response(f, { headers: { 'Content-Type': f.type || 'application/octet-stream' } }));
        }
        await cache.put('meta', new Response(JSON.stringify(meta), { headers: { 'Content-Type': 'application/json' } }));
      } catch (err) {}
      return Response.redirect('JAT_Revision_Mercaderia.html?compartido=1', 303);
    })());
  }
});
