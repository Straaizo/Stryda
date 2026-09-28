"""Traza el galope de Muybridge como vectores de Android: nítidos a cualquier densidad de pantalla.

Fuente: "The Horse in Motion" (Eadweard Muybridge, 1878), escaneo de alta resolución, dominio público.
https://commons.wikimedia.org/wiki/File:The_Horse_in_Motion_high_res.jpg

Genera:
- app/src/main/res/drawable/caballo_galope_00..10.xml  (11 cuadros del galope, registrados entre sí)
- app/src/main/res/drawable/ic_launcher_foreground.xml  (silueta engrosada para que se lea en el ícono)

Requiere: pip install pillow numpy potracer
Uso: python tools/vectorizar_caballo.py [ruta_al_escaneo.jpg]
"""
import sys
import urllib.request
from collections import deque
from pathlib import Path

import numpy as np
import potrace
from PIL import Image, ImageFilter

RAIZ = Path(__file__).resolve().parent.parent
DRAWABLE = RAIZ / "app/src/main/res/drawable"
URL = "https://upload.wikimedia.org/wikipedia/commons/6/61/The_Horse_in_Motion_high_res.jpg"
CACHE = Path(__file__).resolve().parent / "programas" / "muybridge_high_res.jpg"  # carpeta no versionada

ANCHO, ALTO = 900, 488  # tamaño fijo de cada cuadro: todos quedan registrados y la animación no salta
# Celdas en coordenadas de una miniatura de 1400 px de ancho
COLUMNAS = [(30, 355), (368, 690), (703, 1025), (1040, 1362)]
FILAS = [(35, 240), (255, 460), (475, 680)]
# Recorte común dentro de cada celda: sin los números de arriba ni la pista de abajo
RECORTE_Y = (0.10, 0.87)
RECORTE_X = (0.06, 0.96)


def cargar_escaneo(ruta: str | None) -> Image.Image:
    if ruta:
        return Image.open(ruta).convert("L")
    if not CACHE.exists():
        CACHE.parent.mkdir(parents=True, exist_ok=True)
        pedido = urllib.request.Request(URL, headers={"User-Agent": "StrydaDemo/0.1"})
        CACHE.write_bytes(urllib.request.urlopen(pedido).read())
    return Image.open(CACHE).convert("L")


def componentes(mascara: np.ndarray) -> list[list[tuple[int, int]]]:
    alto, ancho = mascara.shape
    visto = np.zeros_like(mascara, dtype=bool)
    salida = []
    for y0, x0 in zip(*np.nonzero(mascara)):
        if visto[y0, x0]:
            continue
        cola, comp = deque([(y0, x0)]), []
        visto[y0, x0] = True
        while cola:
            y, x = cola.popleft()
            comp.append((y, x))
            for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                ny, nx = y + dy, x + dx
                if 0 <= ny < alto and 0 <= nx < ancho and mascara[ny, nx] and not visto[ny, nx]:
                    visto[ny, nx] = True
                    cola.append((ny, nx))
        salida.append(comp)
    return salida


def silueta(celda: Image.Image, engrosar: int = 0) -> np.ndarray:
    """Máscara booleana del caballo con su jinete, sin cuadrícula, números ni manchas."""
    img = celda.filter(ImageFilter.MedianFilter(5))
    mascara = img.point(lambda p: 255 if p < 100 else 0)
    # Apertura: borra las líneas finas de la cuadrícula y las riendas, conserva las patas
    mascara = mascara.filter(ImageFilter.MinFilter(5)).filter(ImageFilter.MaxFilter(5))
    if engrosar:
        mascara = mascara.filter(ImageFilter.MaxFilter(engrosar))
    m = np.array(mascara) > 127

    # Solo el caballo y lo que esté pegado a él (o el jinete si quedó suelto)
    comps = sorted(componentes(m), key=len, reverse=True)
    limpia = np.zeros_like(m)
    for comp in comps:
        if len(comp) >= 0.04 * len(comps[0]):
            ys, xs = zip(*comp)
            limpia[list(ys), list(xs)] = True

    # Rellenar agujeros chicos (pantalón claro del jinete, reflejos)
    for comp in componentes(~limpia):
        ys, xs = zip(*comp)
        toca_borde = min(ys) == 0 or min(xs) == 0 or max(ys) == m.shape[0] - 1 or max(xs) == m.shape[1] - 1
        if not toca_borde and len(comp) < 4000:
            limpia[list(ys), list(xs)] = True

    # Bordes suaves antes de trazar
    suave = Image.fromarray((limpia * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.5))
    return np.array(suave) > 127


def a_path(mascara: np.ndarray, escala: float = 1.0, dx: float = 0.0, dy: float = 0.0) -> str:
    # potracer rellena lo oscuro (0): se le pasa el fondo en 1 y el caballo en 0
    trazo = potrace.Bitmap(~mascara).trace(turdsize=40, alphamax=1.0, opticurve=True, opttolerance=0.3)

    def p(pt) -> str:
        return f"{pt.x * escala + dx:.1f},{pt.y * escala + dy:.1f}"

    partes = []
    for curva in trazo:
        partes.append(f"M{p(curva.start_point)}")
        for seg in curva.segments:
            if seg.is_corner:
                partes.append(f"L{p(seg.c)}L{p(seg.end_point)}")
            else:
                partes.append(f"C{p(seg.c1)} {p(seg.c2)} {p(seg.end_point)}")
        partes.append("Z")
    return "".join(partes)


def vector(ancho_dp: float, alto_dp: float, vw: float, vh: float, path: str, color: str = "#FFFFFFFF") -> str:
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generado por tools/vectorizar_caballo.py desde Muybridge (1878), dominio público. No editar a mano.\n"
        "     Trazo largo a propósito: la silueta necesita ese detalle. Compose dibuja cada cuadro una vez y lo reutiliza. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:tools="http://schemas.android.com/tools"\n'
        '    tools:ignore="VectorRaster,VectorPath"\n'
        f'    android:width="{ancho_dp:.0f}dp"\n    android:height="{alto_dp:.0f}dp"\n'
        f'    android:viewportWidth="{vw:.0f}"\n    android:viewportHeight="{vh:.0f}">\n'
        f'    <path\n        android:fillColor="{color}"\n        android:fillType="evenOdd"\n'
        f'        android:pathData="{path}" />\n'
        "</vector>\n"
    )


def celdas(escaneo: Image.Image):
    f = escaneo.width / 1400
    for fila in FILAS:
        for col in COLUMNAS:
            x0, x1 = col[0] * f, col[1] * f
            y0, y1 = fila[0] * f, fila[1] * f
            w, h = x1 - x0, y1 - y0
            recorte = escaneo.crop((
                int(x0 + w * RECORTE_X[0]), int(y0 + h * RECORTE_Y[0]),
                int(x0 + w * RECORTE_X[1]), int(y0 + h * RECORTE_Y[1]),
            ))
            yield recorte.resize((ANCHO, ALTO), Image.LANCZOS)


def main():
    escaneo = cargar_escaneo(sys.argv[1] if len(sys.argv) > 1 else None)
    todas = list(celdas(escaneo))[:11]  # la celda 12 es el caballo detenido
    DRAWABLE.mkdir(parents=True, exist_ok=True)
    for i, celda in enumerate(todas):
        m = silueta(celda)
        alto, ancho = m.shape
        (DRAWABLE / f"caballo_galope_{i:02d}.xml").write_text(
            vector(300, 300 * alto / ancho, ancho, alto, a_path(m)), encoding="utf-8",
        )
        print(f"cuadro {i:02d}: {m.mean() * 100:.1f}% relleno")

    # Ícono: cuadro 6 (patas extendidas, se reconoce al instante), silueta engrosada para tamaños chicos
    m = silueta(todas[6], engrosar=9)
    ys, xs = np.nonzero(m)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    ancho_objetivo = 64.0  # dp dentro del lienzo de 108: la zona segura del ícono adaptativo
    escala = ancho_objetivo / (x1 - x0)
    alto_px = (y1 - y0) * escala
    path = a_path(m, escala, (108 - ancho_objetivo) / 2 - x0 * escala, (108 - alto_px) / 2 - y0 * escala)
    (DRAWABLE / "ic_launcher_foreground.xml").write_text(vector(108, 108, 108, 108, path), encoding="utf-8")
    print("ícono listo")


if __name__ == "__main__":
    main()
