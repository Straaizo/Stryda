"""Convierte los programas oficiales del Club Hípico (PDF) en tools/datos/programas_chs.json.

Los PDF se descargan a mano desde https://www.clubhipico.cl/carreras/programa-pdf/ y se guardan
en tools/programas/ con el nombre DD-MM-AAAA.pdf. Esa carpeta no se versiona: es material del Club Hípico.

Requiere `pdftotext` (xpdf o poppler) en el PATH.

Uso: python tools/importar_programas.py
"""
import json
import re
import subprocess
import tempfile
from datetime import datetime
from pathlib import Path

RAIZ = Path(__file__).resolve().parent
CARPETA_PDF = RAIZ / "programas"
SALIDA = RAIZ / "datos" / "programas_chs.json"

RE_CARRERA = re.compile(r"^\s*(\d{1,2})[°ºª]\s+(\d{1,2}:\d{2})\b")
RE_PISTA = re.compile(r"\b(ARENA|PASTO)\s*-\s*([\d.]+)\s*mts")
RE_CONDICION = re.compile(r"^\s{2,}(\S.*?)\s{2,}(?:PREMIO:|GANADOR)")
RE_CABEZA = re.compile(
    r"^(?P<mandil>\d{1,2})\s+(?P<nombre>.+?)(?P<rep>\*\*)?\s+-\s+(?P<padrillo>.+?)\s*"
    r"(?P<pj>\d{2})(?P<pedido>\*+)?\s+(?P<edad>\d{1,2})\s+(?P<resto>.*)$"
)
RE_COLA = re.compile(
    r"(?P<fecha>\d\d-\d\d-\d\d)\s+(?P<dist>\d{3,4})\s+(?P<serie>\S+)\s+(?P<cpos>.*?)\s*"
    r"(?P<tpo>\d\d[:.]\d\d\.\d\d)\s+\$(?P<div>[\d.,]+)\s+(?P<pf>\d{3})\b"
)
RE_DEBUT = re.compile(r"D\s+E\s+B\s+U\s+T\s+A\s*-\s*(?P<padre>.+?)\s*/\s*(?P<madre>.+?)\s{2,}")
RE_STUD = re.compile(r"^\s*(?P<stud>.+?)\s+-\s+(?P<colores>.+?)(?:\s{2,}G\d)?\s*$")
RE_LLEGADA = re.compile(r"(\*?)(\d{1,2})[°ºª]([a-z]{2}\d?)")

FRACCIONES = {"¼": 0.25, "½": 0.5, "¾": 0.75}
MARGENES_CORTOS = {"pczo": 0.1, "pzo": 0.1, "cbza": 0.1, "cza": 0.1, "hocico": 0.05, "nariz": 0.05, "vcza": 0.1}


def cuerpos(texto: str) -> float | None:
    """'4¾' → 4.75 · '13 ½' → 13.5 · 'pczo.' → 0.1. Desconocido → None."""
    t = texto.strip().lower().rstrip(".")
    if not t:
        return None
    if t in MARGENES_CORTOS:
        return MARGENES_CORTOS[t]
    # "½ pczo." = medio cuerpo y un pescuezo
    partes = t.split()
    if len(partes) == 2 and partes[1].rstrip(".") in MARGENES_CORTOS and (base := cuerpos(partes[0])) is not None:
        return base + MARGENES_CORTOS[partes[1].rstrip(".")]
    m = re.fullmatch(r"(\d+)?\s*([¼½¾])?", t)
    if not m or (m.group(1) is None and m.group(2) is None):
        return None
    return (int(m.group(1)) if m.group(1) else 0) + FRACCIONES.get(m.group(2) or "", 0.0)


def segundos(tpo: str) -> float:
    """'01:08.45' o '01.15.45' → 68.45"""
    minutos, resto = re.split(r"[:.]", tpo, maxsplit=1)
    return int(minutos) * 60 + float(resto)


def fecha_iso(ddmmaa: str) -> str:
    return datetime.strptime(ddmmaa, "%d-%m-%y").date().isoformat()


def texto_pdf(pdf: Path) -> str:
    # A stdout, pdftotext ignora -enc en Windows: se pasa por un archivo temporal
    with tempfile.TemporaryDirectory() as tmp:
        txt = Path(tmp) / "programa.txt"
        subprocess.run(["pdftotext", "-table", "-enc", "UTF-8", str(pdf), str(txt)], capture_output=True, check=True)
        return txt.read_text(encoding="utf-8", errors="replace")


def parsear_corredor(linea: str) -> dict | None:
    cabeza = RE_CABEZA.match(linea)
    if not cabeza or (not RE_COLA.search(linea) and not RE_DEBUT.search(linea)):
        return None
    resto = cabeza.group("resto")
    cola = RE_COLA.search(resto)
    debut = RE_DEBUT.search(resto)
    antes_cola = resto[: cola.start()] if cola else resto[: debut.start()] if debut else resto
    columnas = re.split(r"\s{2,}", antes_cola.strip())
    corredor = {
        "mandil": int(cabeza.group("mandil")),
        "nombre": cabeza.group("nombre").strip(),
        "reaparece": bool(cabeza.group("rep")),
        "padrillo": cabeza.group("padrillo").strip(),
        "peso_jinete": int(cabeza.group("pj")),
        "edad": int(cabeza.group("edad")),
        "jinete": columnas[0] if columnas else None,
        "preparador": re.sub(r"\s*\d+R?\d*$", "", columnas[1]).strip() if len(columnas) > 1 else None,
        "llegadas": [
            {"gano": g == "*", "posicion": int(p), "codigo": c} for g, p, c in RE_LLEGADA.findall(antes_cola)
        ],
        "ultima": None,
        "debut": None,
        "colores": None,
    }
    if cola:
        corredor["ultima"] = {
            "fecha": fecha_iso(cola.group("fecha")),
            "distancia": int(cola.group("dist")),
            "serie": cola.group("serie"),
            "cuerpos": cuerpos(cola.group("cpos")),
            "cuerpos_texto": cola.group("cpos").strip(),
            "tiempo_referencia": segundos(cola.group("tpo")),
            "dividendo": float(cola.group("div").replace(",", ".")),
            "peso_fisico": int(cola.group("pf")),
        }
    # Colores de la chaquetilla (la columna viene truncada en el PDF)
    fin = resto[cola.end():] if cola else resto[debut.end():] if debut else ""
    if m := RE_STUD.match(fin):
        corredor["colores"] = re.sub(r"\s+", " ", m.group("colores")).strip()
        # El stud solo se usa para completar colores truncados (los colores son del stud)
        corredor["stud"] = m.group("stud").strip()
    if debut:
        corredor["debut"] = {"padre": debut.group("padre").strip().title(), "madre": debut.group("madre").strip().title()}
    return corredor


def parsear_programa(pdf: Path) -> dict:
    fecha = datetime.strptime(pdf.stem, "%d-%m-%Y").date().isoformat()
    carreras, actual, partes_condicion = [], None, []
    for linea in texto_pdf(pdf).splitlines():
        if m := RE_CARRERA.match(linea):
            # La condición puede venir en varias líneas antes del número ("CONDICIONAL HEMBRAS..." + "NO GANADORAS")
            actual = {"numero": int(m.group(1)), "hora": m.group(2), "condicion": " ".join(partes_condicion) or None,
                      "distancia": None, "superficie": None, "corredores": []}
            carreras.append(actual)
            partes_condicion = []
            continue
        if (m := RE_CONDICION.match(linea)) and not m.group(1).startswith(("CABALLO", "Director", "Reuni")):
            partes_condicion.append(m.group(1).strip())
        if actual is None:
            continue
        if actual["distancia"] is None and (m := RE_PISTA.search(linea)):
            actual["superficie"] = m.group(1).lower()
            actual["distancia"] = int(m.group(2).replace(".", ""))
        if c := parsear_corredor(linea):
            actual["corredores"].append(c)
    return {"fecha": fecha, "carreras": [c for c in carreras if c["corredores"]]}


def main():
    pdfs = sorted(CARPETA_PDF.glob("*.pdf"), key=lambda p: datetime.strptime(p.stem, "%d-%m-%Y"))
    if not pdfs:
        raise SystemExit(f"No hay programas en {CARPETA_PDF}")
    programas = [parsear_programa(p) for p in pdfs]
    SALIDA.parent.mkdir(parents=True, exist_ok=True)
    SALIDA.write_text(json.dumps({"fuente": "Programa oficial Club Hípico de Santiago", "programas": programas},
                                 ensure_ascii=False, indent=1), encoding="utf-8")
    for p in programas:
        corredores = sum(len(c["corredores"]) for c in p["carreras"])
        propios = sum(1 for c in p["carreras"] for k in c["corredores"] if (k["preparador"] or "").startswith("C. Gonzalez M"))
        print(f'{p["fecha"]}: {len(p["carreras"])} carreras, {corredores} corredores, {propios} de C. González M.')


if __name__ == "__main__":
    main()
