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
      un producto por apretón; toque corto = 2,5 s. **Solo gatillo** (16 nov 2026, Revisar y Sacar en
      Personal, Revisar en Venta): se quitó la lectura continua y el botón para cambiarla.
- [ ] **Bip de escáner y láser apagado al leer** (16 nov 2026, Revisar y Sacar en Personal, Revisar y Salida/LEER en
      Venta, Revisión de Mercadería): como una pistola lectora real, cuando el gatillo lee el código correcto suena al
      instante un bip corto y fuerte (`jatBipEscaner`, 2700 Hz, 0,1 s) y el láser se apaga; se suelta y se vuelve a
      apretar para el siguiente. El audio se despierta al apretar el gatillo (`jatBipDespertar`).
- [ ] **Gatillo arriba, junto a la cámara** (12 nov 2026, Revisar en Personal y Venta, solo celular):
      va a la derecha de la imagen de la cámara, el recuadro se corre a la izquierda para no quedar
      debajo. En tablet/PC (vista dividida) queda como antes. Bloque `jat-revisar-gatillo` al final.
- [ ] **Conteo como en la PC** (12 nov 2026, Revisar en Personal y Venta): cada escaneo dice y muestra
      "1 de 3", "2 de 3", "3 de 3. Producto completo". Si se pasa: **"Producto excedido"** (escanear un
      producto ya completo, +5/+10 o una cantidad escrita mayor a lo que falta; se guarda solo lo que
      faltaba). La ventana para escribir la cantidad sale sola **solo si el producto trae más de 10**
      (`HIGH_QTY_THRESHOLD = 11`); con 10 o menos se escanea 1 por 1. Botón **"🔢 Escribir cantidad"**
      debajo para escribirla a mano en cualquier producto.
- [ ] **El campo para escribir no se traba** (12 nov 2026): si el cursor ya estaba ahí sin teclado, al
      tocarlo se suelta y el toque lo toma con teclado; el programa no le roba el cursor mientras lo tocas;
      la cámara se esconde recién al levantar el dedo; un parpadeo del teclado no cierra el campo.
- [ ] **Cámara fija arriba y la lista se desliza debajo.** La cámara nunca se va con el scroll
      ni ocupa más de la mitad de la pantalla.
- [ ] **Filtros Pendientes / Revisados (o Sacados) / Todos**, en su propia fila y con contador.
- [ ] **Lee todos los códigos alrededor y escoge el correcto** (16 nov 2026, Revisar y Sacar en Personal,
      Revisar y Salida por ubicación en Venta): con el gatillo (o LEER) apretado la cámara lee la imagen completa, ordena los códigos del
      centro hacia afuera y marca el que coincide completo con el egreso (si hay varios, primero uno
      pendiente). Si lo que ve no está en el egreso sigue buscando; "Producto no encontrado" sale recién al
      soltar el gatillo (o a los 2,5 s del toque corto). Al **registrar ubicaciones** (Sacar y Venta) se
      sigue tomando solo el código del recuadro, porque ahí no hay egreso para comparar.
      **19 nov 2026:** también en Revisión de Mercadería (piloto): ahí, si al soltar lo visto es un producto del
      kardex que no va en el egreso, se anota como sobrante igual que antes; si no, solo avisa.
- [ ] **"Producto no encontrado. Verifique."** (19 nov 2026, Personal, Venta, Sacar y Revisión de Mercadería):
      si al soltar el gatillo no vio ningún código del egreso, lo dice en voz, lo muestra y vibra 3 veces.
      Cuando encuentra el correcto: bip, vibración, dice "1 de 2" y el láser se apaga (se suelta y se vuelve a apretar).
- [ ] **LEER se mantiene apretado** (19 nov 2026, Salida por ubicación y registrar ubicaciones en Venta): igual que el
      gatillo; toque corto sigue leyendo 2 s como antes.
- [ ] **Categorías en Revisar y el producto en negrita grande** (20 nov 2026, Jorge ve poco): en Personal, la lista
      de debajo de la cámara (Pendientes / Revisados / Todos) ahora tiene una fila de botones por categoría con
      pendientes/total y la lista agrupada con su título ("Cables — 1 pendiente de 2"); tocar otra vez la categoría
      vuelve a Todas. "Ver tabla" (Personal, Venta y PC) también va agrupada y con botones por categoría (PC no tenía).
      El **nombre del producto va en negrita y más grande** en todos los programas (Personal, Venta, PC, Sacar,
      Revisión de Mercadería). En "Ver tabla" del celular cada fila es una tarjeta con el producto arriba, a lo ancho,
      y el texto ya no sale casi blanco sobre fondo blanco en pantalla oscura. Bloque `jat-categorias-negrita`.
      Historia: las categorías no se borraron; desde el 9 nov la lista de debajo de la cámara pasó a ser la pantalla
      principal y quedó sin categorías (solo estaban en "Ver tabla").
- [ ] **Nombre del egreso completo y en negrita** (20 nov 2026, Jorge ve poco): en la lista de Inicio (Personal), en el
      botón Continuar, en la barra de Revisar (Personal y Venta), en Mis egresos (Venta) y en la lista de Revisión de
      Mercadería el nombre ya no se corta con "…": baja a otra línea, en negrita y más grande. Si un nombre largo
      se corta en algún lado, es un error: corregirlo igual.
- [ ] **Ventanas blancas nítidas** (20 nov 2026, Jorge ve poco): en Buscar egresos (y las demás ventanas blancas:
      Guardar como, etc.) el texto salía casi blanco sobre fondo blanco, gris y cortado. Ahora texto azul oscuro casi
      negro, nombre del egreso completo en negrita, borde grueso en el buscador y los porcentajes en colores fuertes
      (verde, naranja oscuro, azul). Regla: en este programa nada en gris claro; todo con contraste alto y en colores.
      Aplica a Personal, Venta y PC.
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
- [ ] **PDF revisado al terminar** (12 nov 2026): cuando el egreso llega a 100 % verificado se genera solo
      "REVISADO_<egreso>_<fecha>_<hora>.pdf" (título "EGRESO REVISADO - 100% VERIFICADO" y hora de término).
      Se guarda en la carpeta de trabajo › Imprimir si está conectada (PC); si no, baja a Descargas. Además queda
      una copia en la lista **🖨️ Para imprimir** (menú y aviso de completado) con Imprimir, Compartir y "Ya impreso".
      Solo se genera cuando el egreso PASA a completo mientras se revisa, no al abrir uno ya completo.
      En Personal, Venta y PC. Revisión de Mercadería (piloto) no se toca.
- [ ] **Impresión en UNA sola hoja A3 vertical** (20 nov 2026): el PDF (el revisado y el de "Exportar PDF") sale en A3
      vertical (297 x 420 mm) y la letra se ajusta sola: usa la más grande (hasta 12) con la que caben todos los
      productos en una hoja, mínimo 6; solo si son muchísimos (unos 95 o más) pasa a la cantidad mínima de hojas con
      la letra más grande posible. Descripción en negrita, columnas medidas al texto, línea fina entre filas y cabecera
      repetida si hay más de una hoja. El botón "Imprimir" de la hoja en pantalla también pide A3 vertical y se achica
      solo para entrar en una hoja. En Personal, Venta y PC.
- [ ] En la versión personal, el egreso abierto en Inicio aparece en Sacar, y el elegido en
      Sacar pasa a Revisar (es el mismo documento).
- [ ] **Escáner USB / Bluetooth tipo pistola (NETUM y similares)** (20 nov 2026): escribe como un teclado, así que
      ya contaba con el cuadro de código tocado; ahora, si el cuadro pierde el foco (tocaste otra cosa), el primer
      dígito que llega lo vuelve a enfocar y ningún código se pierde. No actúa si escribes en otro campo ni si hay
      otra ventana abierta; no abre el teclado en pantalla (vuelve a salir normal al tocar el cuadro con el dedo).
      En Personal, Venta, Revisión de Mercadería y Sacar (ventana de registrar ubicación). PC ya lo traía.

- [ ] **Nunca "almacenamiento lleno"** (16 nov 2026, Personal y Venta): los egresos se guardan siempre en el
      almacén grande del teléfono (IndexedDB `jat_egresos` / `jatv_egresos`, clave `egresos`) y, si cabe, también en
      localStorage como copia. Si no cabe, se quita la copia vieja de localStorage para no abrir una versión atrasada.
      Al abrir se usa la versión más nueva de los dos. Las copias por egreso `hat_backup_*` viven en el almacén grande
      (se mueven solas al abrir). Sacar (`JAT_Salida_Mercaderia.html`) también lee los egresos del almacén grande.
      Ningún `localStorage.setItem` suelto puede romper el guardado (todos con try).

- [ ] **Bienvenida "Dónde se guardan tus datos"** (16 nov 2026, Personal, Venta y Revisión de Mercadería): la
      primera vez (en Venta después de iniciar sesión; en Revisión después de escribir el nombre) explica que todo se
      guarda en el teléfono y que el respaldo baja solo a **Descargas** con el nombre `<archivo>_auto_FECHA_HORA`
      (en la PC, si hay carpeta de trabajo conectada, va a su subcarpeta **Backups**), cuándo se guarda y cómo
      recuperarlo. Botones: "Guardar una copia ahora en OneDrive, Drive o WhatsApp", en PC "Elegir otra carpeta",
      "Entendido". Se vuelve a abrir con "¿Dónde?" junto a "Respaldar ahora", tocando "Último respaldo…" o, en
      Personal, Menú › "Dónde se guardan mis datos". Clave `<clave>_bienvenida`.
- [ ] **"Último respaldo: hoy 21:10 · en Descargas"**: dice dónde quedó y se pone en **rojo** si no hay respaldo o
      tiene más de 24 h.
- [ ] **Revisión de Mercadería ya tiene respaldo** (regla 3): `JAT_Revision_respaldo_auto_….json` al empezar una
      revisión, al cerrarla y una vez al día con cambios; ＋ › "Respaldar ahora" y "Recuperar respaldo" (no pisa lo
      que ya está, solo agrega las revisiones que falten).
- [ ] Sin egresos no se baja respaldo por "cambios del día".

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
- [ ] Icono distinto por programa: Personal azul y dorado, Venta rojo, Revisión verde, PC violeta (20 nov 2026:
      icono y manifiesto propios para guardar el acceso directo en el escritorio de la PC con su logo).
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

## App de Android (APK) de la versión personal (`android/`)

Se instala como cualquier app (`JATControlSuite.apk`, en Releases del repositorio) y trae adentro
`JAT_Control_Suite_Movil.html` + `JAT_Salida_Mercaderia.html`. Se compila sola en GitHub Actions
(`.github/workflows/android.yml`) cada vez que cambia una de esas páginas, los kardex, los íconos o `android/`.
Firmada con la misma llave `jat.keystore` de las otras apps JAT, así se actualiza encima sin perder datos.

- [ ] **Funciona sin internet**: pdf.js, jsPDF, SheetJS, ZXing, tesseract (OCR en español e inglés) y las
      letras Inter/JetBrains Mono van dentro de la app (`android/preparar.py` las copia al compilar).
- [ ] **Lector de códigos de barras de Google (ML Kit)** en lugar del lector de JavaScript: la página sigue
      usando `BarcodeDetector`; el adaptador (`android/web/jat-android.js`) manda la imagen a ML Kit.
- [ ] **Respaldo que sobrevive** (regla fija 3): los archivos `.hat` automáticos y todas las descargas (PDF, CSV,
      Excel) se guardan en **Descargas › JAT Control Suite** con MediaStore, que no se borra al desinstalar. Además
      la app permite la copia automática de Android (`allowBackup`). Para recuperar: Inicio › ＋ › Importar / abrir archivo.
- [ ] **Imprimir en A3 vertical**: "Imprimir PDF revisado" abre la impresión de Android con el PDF (hoja A3);
      el reporte HTML de respaldo también sale en A3 vertical.
- [ ] **Compartir** (WhatsApp, Drive, OneDrive, correo) con la hoja de compartir de Android.
- [ ] **Voz y dictado nativos**: la voz del teléfono (TextToSpeech) reemplaza a la de Chrome y el dictado usa el
      reconocedor de Google del teléfono.
- [ ] **Escáner USB/Bluetooth (NETUM)** y gatillo siguen igual (son teclado; el WebView los recibe).
- [ ] **Cámara** pedida al abrir; pantalla siempre encendida; pantalla completa según la opción de la app; botón
      Atrás cierra ventanas y para salir hay que tocarlo dos veces.
- [ ] **Actualización dentro de la app**: avisa cuando hay versión nueva en Releases (Menú › Buscar actualización
      para revisar a mano). Los datos se conservan al actualizar.
- [ ] Regla 1 (Venta): la APK es solo de la versión personal; si se pide la de venta se arma igual con
      `JAT_Control_Suite_Venta.html`.

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
- 2026-11-12: Revisar (Personal y Venta): gatillo arriba junto a la cámara; conteo "1 de 3… Producto
  completo / Producto excedido"; cantidad escrita solo para más de 10 y botón "Escribir cantidad"; arreglo
  del campo de escribir que se trababa.
- 2026-11-11: versión de PC subida al repositorio (`JAT_Control_Suite_PC.html`, PC 3.4) con las mejoras
  del celular que sirven en PC: filtros con contador, buscar escribiendo, dictado (F8), pantalla
  oscura, egreso en curso y respaldo automático. Por la regla 1, el verificador de Venta recibió
  filtros con contador, hora de marcado y buscar escribiendo.
- 2026-11-12: PDF revisado automático al terminar de revisar un egreso, guardado en carpeta › Imprimir o Descargas y en la lista "Para imprimir" (Personal, Venta y PC).
- 2026-11-16: escáner solo con gatillo y lectura de todos los códigos alrededor, eligiendo el del egreso
  (Revisar y Sacar en Personal, Revisar y Salida por ubicación en Venta).
- 2026-11-16: arreglo de "No se pudo guardar localmente (almacenamiento lleno)": egresos al almacén grande
  del teléfono (IndexedDB) en Personal y Venta; Sacar los lee de ahí.
- 2026-11-16: bienvenida "Dónde se guardan tus datos" y línea de último respaldo con lugar (rojo si > 24 h) en
  Personal, Venta y Revisión de Mercadería; Revisión de Mercadería recibe respaldo automático y "Recuperar respaldo".
- 2026-11-16: bip de escáner real al leer con el gatillo y láser apagado (Personal, Venta y Revisión de Mercadería).
- 2026-11-19: gatillo "mantener apretado": busca entre todos los códigos (SAP, EAN, UPC, serie) el del egreso, y al
  soltar sin encontrarlo dice "Producto no encontrado. Verifique." (Personal, Venta, Sacar y Revisión de Mercadería,
  que ahora también lee todos los códigos alrededor). LEER de Venta se mantiene apretado.
- 2026-11-20: categorías en la lista de Revisar (Personal) y en la tabla de PC; producto en negrita grande en todos los
  programas; tabla del celular como tarjetas con texto oscuro legible.
- 2026-11-20: nombre del egreso completo (sin "…"), en negrita y más grande en todas las listas del celular.
- 2026-11-20: ventanas blancas (Buscar egresos, etc.) con texto oscuro nítido, nombre completo y porcentajes en colores fuertes.
- 2026-11-20: escáner USB/Bluetooth (NETUM): recupera el foco solo y no pierde códigos (Personal, Venta, Revisión y Sacar).
- 2026-11-20: impresión del egreso en una sola hoja A3 vertical, con letra que se ajusta sola (Personal, Venta y PC).
- 2026-11-20: icono violeta con logo JAT y manifiesto propio para la versión de PC (acceso directo en el escritorio).
- 2026-11-24: app de Android (APK) de la versión personal: sin internet, lector ML Kit, respaldo en Descargas › JAT Control Suite, impresión A3, voz y dictado nativos, actualización dentro de la app.
