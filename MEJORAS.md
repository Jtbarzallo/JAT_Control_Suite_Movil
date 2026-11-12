# Lista de mejoras de JAT Control Suite

Lista interna. **Leerla antes de tocar cualquier programa de este repositorio** y comprobar,
antes de subir, que la versión nueva sigue cumpliendo cada punto. Cada mejora nueva que pida
Jorge se agrega aquí el mismo día.

## REGLA FIJA DE JORGE (no se rompe nunca)

1. **Toda mejora que se haga en JAT Control Suite se hace también en la versión de venta**
   (`JAT_Control_Suite_Venta.html`), el mismo día, adaptada a ella y sin datos de Tecnodigi.
2. **Ninguna mejora puede dañar lo que ya funciona.** Antes de subir se prueba que lo anterior
   sigue igual en la versión personal y en la de venta. *Lo que funciona no se cambia*, y las
   versiones nuevas conservan los arreglos ya hechos.
3. **Todo tiene que tener respaldo.** Lo que guarda un programa debe quedar también en un archivo
   que sobreviva si se desinstala la app o se borran los datos del navegador (egresos, avance,
   ubicaciones, kardex, categorías). Vale para JAT y para todos los proyectos futuros.

Esta regla también está en la "Bitácora JAT Control Suite".

## En todas las pantallas con escáner (Revisar, Sacar, Revisión de Mercadería, Venta; en PC lo que aplique, ver "Versión de PC")

- [ ] **Gatillo disparador**: botón rojo ESCANEAR; la cámara lee solo mientras se aprieta,
      un producto por apretón; toque corto = 2,5 s. Opción de cambiar a lectura continua.
- [ ] **Cámara fija arriba y la lista se desliza debajo.** La cámara nunca se va con el scroll
      ni ocupa más de la mitad de la pantalla.
- [ ] **Filtros Pendientes / Revisados (o Sacados) / Todos**, en su propia fila y con contador.
- [ ] **Solo se lee el código dentro del recuadro**; si la etiqueta trae varios códigos, se
      acepta el que coincide completo con el egreso.
- [ ] **Teclado solo al tocar un campo**: ningún botón, tarjeta ni lectura abre el teclado.
- [ ] **Botón ＋ con las acciones de cada ventana**, en vez de barras de botones.
- [ ] **El ＋ y el gatillo no tapan ningún botón**: se esconden cuando hay otra ventana encima.
- [ ] **Ningún menú se corta**: los desplegables salen completos aunque el panel sea chico.
- [ ] **Buscar escribiendo**: al escribir con el teclado sale la lista de coincidencias para
      elegir con el dedo; mientras tanto se esconde la cámara para que el teclado no tape los
      resultados, y no se selecciona ni se guarda nada hasta elegir.
- [ ] **Que no se trabe**: la cámara no lee códigos ni texto mientras se escribe; nada de
      animaciones que repinten la cámara (el marco verde anima solo la opacidad).
- [ ] **Dictado por voz (🎤) en los buscadores**: Revisar (botón al lado del campo; un código dictado se
      busca como siempre, palabras muestran la lista para elegir con el dedo), Inicio, Kardex y Sacar
      (micrófono dentro del campo). En Venta: Revisión, Kardex, Bodega y demás buscadores; el verificador
      de Venta ya tenía 🎤, 🔢 y ⌨️. Dictar no abre el teclado. Necesita internet (Chrome).
- [ ] **El teclado no tapa la pantalla**: al tocar un campo, con el teclado abierto se esconden las
      pestañas, el ＋ y el gatillo, y el campo sube al tercio de arriba de lo visible (también dentro de
      Sacar). Al cerrar el teclado todo vuelve como estaba.
- [ ] Lectura de texto (OCR) para productos sin código de barras.
- [ ] Voz, sonido y vibración distintos por caso (correcto, repetido, no encontrado, completo).
- [ ] Aviso de producto repetido y de "ya está completo".
- [ ] **Fecha y hora de escaneo** (12 nov 2026): cada producto contado o sacado muestra
      🕒 dd/mm/aaaa HH:MM, en todos los programas (Revisar, Sacar, Revisión de Mercadería, Venta
      incluida su Salida por ubicación, y PC). También sale en los reportes: columna "Fecha y hora"
      en el PDF (con pie "Generado: … · Página x de y"), en el CSV/Excel y en la hoja de imprimir.
      En Revisión de Mercadería se guarda en cada producto (`t`) y va en el Excel.

## Egresos

- [ ] Se pueden elegir PDF desde OneDrive y Descargas sin que salgan en gris.
- [ ] El egreso se recibe también compartiendo el PDF desde WhatsApp.
- [ ] Todo se guarda solo; no hay botón obligatorio de guardar.
- [ ] En la versión personal, el egreso abierto en Inicio aparece en Sacar, y el elegido en
      Sacar pasa a Revisar (es el mismo documento).

## Kardex y ubicaciones

- [ ] **Kardex por categorías grandes** (las mismas de Sacar): fila de chips "Todas" + una por categoría
      con su cantidad, y la lista agrupada con su título (Cables, Guitarras y bajos…). Se respeta la
      categoría elegida a mano en Sacar (`jat_familias` / `jatv_familias`) y se recuerda el chip elegido
      (`jat_kardex_fam`). La tabla de categorías está copiada en `window.JATFamilias` (Personal y Venta):
      si se cambia `FAMILIAS`/`FAM_FIJA` en `JAT_Salida_Mercaderia.html`, copiarla igual ahí.
- [ ] Venta: Kardex con los mismos chips y grupos (usa la categoría del kardex del cliente).

- [ ] El kardex se carga desde el Excel de inventario tal como viene (encabezados en la fila 4).
- [ ] Lista agrupada por categoría del kardex o por ubicación.
- [ ] **Sacar: categorías grandes de bodega** (Cables, Guitarras y bajos, Cuerdas, Parlantes y
      amplificadores, Micrófonos, Redes y WiFi, Laptops…): cada categoría fina del kardex se junta en
      su familia (tabla `FAMILIAS` + correcciones `FAM_FIJA`); si no hay categoría, se adivina por el
      nombre. "Por categoría" es la vista por defecto y se recuerda (`jat_sacar_agrupar`).
- [ ] **Sacar: fila de chips por categoría** con pendientes/total para ver solo esa categoría.
- [ ] **Sacar: cambiar la categoría de un producto** en su ficha (o crear una nueva); se recuerda
      por código (`jat_familias`) y viaja en el archivo de ubicaciones.
- [ ] Las ubicaciones guardadas no se borran al reemplazar el kardex.
- [ ] Las ubicaciones se guardan en un archivo para subir a OneDrive.

## Diseño

- [ ] Pantalla de Inicio simple: buscar egresos, cargar egreso, ingresar mercadería.
- [ ] Pestañas abajo. Versión personal: Inicio, Sacar, Revisar, Kardex, Menú.
- [ ] Botones grandes para dedo; nada de atajos de teclado de Windows en el celular.
- [ ] Icono distinto por programa: Personal azul y dorado, Venta rojo, Revisión verde.
- [ ] El enlace corto de la página abre el programa (index.html), sin error.
- [ ] **Diseño industrial** (bloque `<style id="jat-industrial">` al final del archivo, todo bajo
      `body.d-ind`): cabecera oscura con marca y barra de estado (carpeta, egreso, guardado),
      panel KPI (progreso, verificadas, pendientes, escáner), chips Todos / Pendientes /
      Verificados, lista como tabla densa con código en monoespaciada y chip de estado,
      vista dividida cámara | lista desde 900 px de ancho.
- [ ] **Pantalla clara / oscura**: se cambia en la barra de estado, en Menú y en el "Más" de la
      cámara; se recuerda (`jat_tema`). Todo color nuevo va por variables para que sirva en ambos.
- [ ] En Menú se puede volver al diseño clásico (`jat_diseno`). El diseño nuevo no cambia IDs ni
      funciones: solo agrega estilos y elementos de lectura.
- [ ] **Diseño Marino (azul marino y dorado)** es el diseño por defecto (bloque `<style id="jat-marino">`,
      todo bajo `body.d-mar`, encima del industrial). Sin emojis en Inicio, Menú y hojas del ＋ (íconos de
      línea). Inicio: saludo, tarjeta "En curso" con anillo de avance, siguiente producto y su ubicación,
      botón "Continuar revisión" y "Sacar este egreso"; resumen Pendientes / Completos / Egresos; lista con
      anillos. ＋ dorado, gatillo rojo, retícula con esquinas doradas. Sacar (iframe) también en Marino.
      La primera vez abre en pantalla oscura. Menú → Diseño pasa por Marino → Industrial → Clásico.
- [ ] Venta: Diseño Marino con pantalla clara / oscura (`jatv_diseno`, `jatv_tema`), opciones en Más.

## Respaldo (regla fija 3)

- [ ] Módulo `JATRespaldo` en Personal y Venta. Baja solo un archivo a Descargas cuando entra un egreso
      nuevo, cuando un egreso queda completo y cuando hay cambios y el último respaldo tiene más de 4 h.
      Al abrir la app por primera vez con datos también respalda.
- [ ] Personal: archivo `JAT_Personal_respaldo_auto_FECHA.hat` con todos los egresos y además
      ubicaciones, pasillos, categorías y demás datos. Se recupera con Inicio → Abrir archivo de egreso;
      lo que ya está en el celular no se pisa.
- [ ] Venta: archivo `JAT_copia_auto_FECHA.json` con el mismo formato de Más → Restaurar una copia.
- [ ] Botón "Respaldar ahora" (permite mandar a OneDrive, Drive, WhatsApp o correo) y la línea
      "Último respaldo: …" en Inicio y Menú (Personal) y en Revisión y Más (Venta).
- [ ] Se pide a Chrome que no borre los datos de la app (`navigator.storage.persist`).
- [ ] El kardex no va en el respaldo: se recarga desde el Excel de inventario, que es su respaldo.

## Versión de PC (`JAT_Control_Suite_PC.html`, antes VERIFICADOR_DE_EGRESOS_VOZ_3_3)

Programa de escritorio (menú lateral, escáner de pistola USB, atajos de teclado). Las mejoras del
celular que sirven en PC están en dos bloques al final del archivo: `<style id="jat-verif-extras">`
(compartido con Venta) y `<style id="jat-pc-extras">`. No cambian IDs ni funciones: solo envuelven.

- [ ] **Filtros Pendientes / Revisados / Todos con contador** (en "Ver tabla").
- [ ] **Buscar escribiendo**: al escribir un nombre o parte del código sale la lista de coincidencias;
      con clic se elige, ENTER suma. Las flechas ← → siguen navegando y marcan la fila de la lista.
      Un código completo de pistola no abre lista.
- [ ] **Dictado por voz** 🎤 en el buscador del verificador (también con **F8**), en "Filtrar tabla" y
      en "Buscar egresos". Necesita internet (Chrome o Edge).
- [ ] **Pantalla clara / oscura**: botón en la barra de abajo y en Ajustes; se recuerda (`jat_pc_tema`).
      Las cartas y actas siguen en blanco para imprimir.
- [ ] **Egreso en curso**: en la barra lateral, anillo de avance, unidades, siguiente pendiente y botón
      "Continuar revisión". En Nuevo egreso, tarjeta "En curso" con el último egreso sin terminar.
- [ ] **Respaldo (regla 3)**: archivo `JAT_PC_respaldo_auto_FECHA.hat` con todos los egresos, cartas
      de constancia y actas UCACUE. Va a la carpeta de trabajo › Backups si está conectada; si no, a
      Descargas. Cuándo: egreso nuevo, egreso completo, cada 4 h con cambios, primera vez con datos.
      "Respaldar ahora" en Archivo, en Ajustes y en la barra de abajo ("Último respaldo: …", en rojo si
      tiene más de 24 h). Archivo → **Recuperar respaldo** trae todos los egresos que falten sin pisar
      los que ya están. `navigator.storage.persist`.
- [ ] Ya tenía y se conserva: hora de marcado, aviso de repetido, voz por caso, conteo rápido
      (F4/F9/Insert, CÓDIGO*cantidad), deshacer, cierre seguro, guardado en carpeta OneDrive, OneView,
      cartas de constancia y actas UCACUE.
- No aplica en PC: gatillo, cámara, OCR, teclado del celular, botón ＋. Kardex, ubicaciones y
  categorías de bodega quedan en el celular (Sacar).

## Versiones

- [ ] La versión de venta no lleva datos de Tecnodigi, UCACUE, OneView ni cartas de constancia.
- [ ] Las cartas de constancia solo van en la versión personal.
- [ ] Revisión de Mercadería: kardex solo con código y producto, sin cantidades.

## Pendiente de pasar a la versión de venta

Mejoras de la versión personal que todavía no están en `JAT_Control_Suite_Venta.html`:

- [ ] Filtros Pendientes / **Sacados** / Todos en Salida (Venta solo tiene Pendientes y Todos).

Ya pasadas: categorías grandes de bodega en Salida (11 nov 2026). En el verificador de Venta
(11 nov 2026, junto con la versión de PC): Pendientes / Revisados / Todos con contador, hora de
marcado en cada producto contado y buscar escribiendo con lista para elegir con el dedo
(bloque `jat-verif-extras`, el mismo de PC).

## Historial de esta lista

- 2026-11-09: creada. Se agregaron los filtros y el gatillo en Sacar, la lista con filtros
  debajo de la cámara en Revisar, y el arreglo del menú "Más" de la cámara que se cortaba.
  Después se agregó la hora de marcado en las tarjetas de Revisar y de Sacar (faltaba).
  Rediseño industrial con tema claro y oscuro, también dentro de Sacar.
  Búsqueda escrita con lista para elegir, sin trabarse y sin que el teclado tape los resultados.

- 2026-11-10: Sacar clasificado por categorías grandes de bodega (cables, guitarras, cuerdas…),
  con chips para filtrar y opción de corregir la categoría de cada producto.
- 2026-11-11: regla fija: toda mejora va también a la versión de venta y nada puede dañar lo que
  ya funciona. Las categorías de bodega de Sacar se pasaron a "Salida por ubicación" de Venta
  (chips por categoría, vista por categoría o por ubicación, categoría editable por producto;
  usa la categoría del kardex del cliente, no la de Tecnodigi).
- 2026-11-11: regla fija 3: todo tiene que tener respaldo (también en proyectos futuros). Respaldo
  automático a Descargas en Personal y Venta, con botón "Respaldar ahora". Diseño Marino (azul marino y
  dorado) en todas las ventanas de Personal y en Venta, con pantalla clara y oscura.
- 2026-11-11: dictado por voz en todos los buscadores y teclado que no tapa la pantalla, en Personal
  (incluido Sacar) y en Venta.
- 2026-11-11: Kardex agrupado por categorías grandes de bodega, con chips, en Personal y Venta.
- 2026-11-12: fecha y hora de escaneo en pantalla y en reportes (PDF, CSV/Excel, imprimir) en los cinco
  programas; Venta ahora guarda la fecha y hora en que se sacó cada producto en Salida por ubicación.
- 2026-11-11: versión de PC subida al repositorio (`JAT_Control_Suite_PC.html`, PC 3.4) con las mejoras
  del celular que sirven en PC: filtros con contador, buscar escribiendo, dictado (F8), pantalla
  oscura, egreso en curso y respaldo automático. Por la regla 1, el verificador de Venta recibió
  filtros con contador, hora de marcado y buscar escribiendo.
