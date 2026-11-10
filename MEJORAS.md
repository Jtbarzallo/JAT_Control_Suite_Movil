# Lista de mejoras de JAT Control Suite

Lista interna. **Leerla antes de tocar cualquier programa de este repositorio** y comprobar,
antes de subir, que la versión nueva sigue cumpliendo cada punto. Cada mejora nueva que pida
Jorge se agrega aquí el mismo día.

Regla general de Jorge: *lo que funciona no se cambia*, y las versiones nuevas conservan los
arreglos ya hechos.

## En todas las pantallas con escáner (Revisar, Sacar, Revisión de Mercadería, Venta)

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
- [ ] Lectura de texto (OCR) para productos sin código de barras.
- [ ] Voz, sonido y vibración distintos por caso (correcto, repetido, no encontrado, completo).
- [ ] Aviso de producto repetido y de "ya está completo".

## Egresos

- [ ] Se pueden elegir PDF desde OneDrive y Descargas sin que salgan en gris.
- [ ] El egreso se recibe también compartiendo el PDF desde WhatsApp.
- [ ] Todo se guarda solo; no hay botón obligatorio de guardar.
- [ ] En la versión personal, el egreso abierto en Inicio aparece en Sacar, y el elegido en
      Sacar pasa a Revisar (es el mismo documento).

## Kardex y ubicaciones

- [ ] El kardex se carga desde el Excel de inventario tal como viene (encabezados en la fila 4).
- [ ] Lista agrupada por categoría del kardex o por ubicación.
- [ ] Las ubicaciones guardadas no se borran al reemplazar el kardex.
- [ ] Las ubicaciones se guardan en un archivo para subir a OneDrive.

## Diseño

- [ ] Pantalla de Inicio simple: buscar egresos, cargar egreso, ingresar mercadería.
- [ ] Pestañas abajo. Versión personal: Inicio, Sacar, Revisar, Kardex, Menú.
- [ ] Botones grandes para dedo; nada de atajos de teclado de Windows en el celular.
- [ ] Icono distinto por programa: Personal azul y dorado, Venta rojo, Revisión verde.
- [ ] El enlace corto de la página abre el programa (index.html), sin error.

## Versiones

- [ ] La versión de venta no lleva datos de Tecnodigi, UCACUE, OneView ni cartas de constancia.
- [ ] Las cartas de constancia solo van en la versión personal.
- [ ] Revisión de Mercadería: kardex solo con código y producto, sin cantidades.

## Historial de esta lista

- 2026-11-09: creada. Se agregaron los filtros y el gatillo en Sacar, la lista con filtros
  debajo de la cámara en Revisar, y el arreglo del menú "Más" de la cámara que se cortaba.
