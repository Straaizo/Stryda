"""Genera app/src/main/assets/seed.json: el corral de Cristóbal González M.

Qué es real y qué no:
- REAL (programas oficiales del Club Hípico, vía tools/importar_programas.py): ejemplares, padrillo, edad,
  carreras de septiembre (distancia, pista, hora, número, mandil, jinete, peso), posiciones, peso físico
  y rivales. El sexo sale de la condición de la carrera cuando dice HEMBRAS o MACHOS.
- APROXIMADO: el tiempo de cada caballo = tiempo de referencia del programa + 0,2 s por cuerpo de diferencia.
- FICTICIO (acordado con Enzo): trabajos diarios, propietarios y tratamientos de rutina.
  Sin lesiones ni retiros inventados sobre caballos reales: esos estados se cargan desde la app.

Determinista (semilla fija). Las fechas se escriben respecto de FECHA_REFERENCIA (la del último programa);
la app las desplaza al día en que se carga el seed.

Uso: python tools/importar_programas.py && python tools/generar_seed.py
"""
import json
import random
import re
import statistics
import unicodedata
from datetime import date, timedelta
from pathlib import Path

random.seed(1878)  # año de "The Horse in Motion"

# Subir cuando cambien los datos: la app reemplaza el seed anterior (conserva lo registrado en el teléfono)
VERSION_SEED = 4
RAIZ = Path(__file__).resolve().parent
ENTRADA = RAIZ / "datos" / "programas_chs.json"
SALIDA = RAIZ.parent / "app/src/main/assets/seed.json"

PREPARADOR_PROGRAMA = "C. Gonzalez M."
CLUB_HIPICO = "Club Hípico de Santiago"
HIPODROMO_SIN_DATO = "Hipódromo sin dato"
SEGUNDOS_POR_CUERPO = 0.2
DIAS_HISTORIA = 182

# Datos confirmados en otras fuentes públicas (ver docs/fuentes-club-hipico.md)
CONOCIDOS = {
    "MR CASPER": {"sexo": "macho", "pelaje": "Alazán", "madre": "Creme Sucree"},
}

VAREADORES = ["Cristián Soto", "Luis Tapia", "Mauro Díaz", "Patricio Leiva"]
NOTAS = ["Terminó entero", "Salió con ganas", "Algo pesado en la curva", "Tomó bien la mano",
         "Se soltó a mitad de recta", "Cansado al final", "Muy tranquilo"]
PROPIETARIOS = [  # ficticios
    ("p01", "Rodrigo Echeverría"), ("p02", "Stud Las Vertientes"), ("p03", "María José Larraín"),
    ("p04", "Inversiones El Roble Ltda."), ("p05", "Sociedad Hípica Don Aurelio"), ("p06", "Francisca Undurraga"),
    ("p07", "Andrés Bustamante"), ("p08", "Stud Doña Inés"),
]
MINUSCULAS = {"de", "del", "la", "las", "el", "los", "por", "y", "en"}

# Palabras completas que aparecen en la descripción de colores. El PDF corta la columna:
# lo que no está acá al final de la frase es una palabra truncada y se descarta.
VOCABULARIO_COLORES = {
    "amarillo", "azul", "blanco", "burdeo", "cafe", "café", "calipso", "celeste", "dorado", "dorada", "granate",
    "gris", "lila", "morado", "naranjo", "negro", "plateado", "rojo", "rosado", "turquesa", "verde", "fucsia",
    "franja", "franjas", "puños", "mangas", "gorra", "cruz", "estrella", "estrellas", "lunares", "lunar",
    "hombreras", "banda", "rombos", "rombo", "faja", "cuadros", "brazaletes", "tirantes", "herradura", "anillos",
    "anillo", "y", "con", "en", "a", "rayas", "lista", "flor", "lis", "de", "dos",
}
# Una frase no puede terminar en una parte del cuerpo sin su color ("... Gorra" = gorra de color desconocido)
SIN_COLOR_AL_FINAL = {"y", "con", "en", "a", "de", "gorra", "mangas", "puños", "franja", "hombreras", "dos"}


def limpiar_colores(variantes: list[str]) -> str | None:
    """Toma la variante más completa y quita lo que el PDF dejó cortado."""
    if not variantes:
        return None
    palabras = max(variantes, key=len).replace(",", " ").replace(".", " ").split()
    while palabras and palabras[-1].lower() not in VOCABULARIO_COLORES:
        palabras.pop()
    while palabras and palabras[-1].lower() in SIN_COLOR_AL_FINAL:
        palabras.pop()
    if not palabras:
        return None
    return " ".join([palabras[0].capitalize()] + [w.lower() for w in palabras[1:]])


def iso(d: date) -> str:
    return d.isoformat()


def slug(nombre: str) -> str:
    base = unicodedata.normalize("NFKD", nombre).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "-", base).strip("-")


def nombre_propio(nombre: str) -> str:
    """'SOS DE BOCA' → 'Sos de Boca' · 'TIGRO (ARG)' → 'Tigro (ARG)'"""
    palabras = []
    for i, p in enumerate(nombre.split()):
        if p.startswith("("):
            palabras.append(p.upper())
        elif i > 0 and p.lower() in MINUSCULAS:
            palabras.append(p.lower())
        else:
            palabras.append(p.capitalize())
    return " ".join(palabras)


def sexo_por_condicion(condicion: str | None) -> str | None:
    c = (condicion or "").upper()
    if "HEMBRAS" in c:
        return "hembra"
    if "MACHOS" in c:
        return "macho"
    return None


def tiempo_propio(ultima: dict, gano: bool) -> float | None:
    if gano:
        return round(ultima["tiempo_referencia"], 2)
    if ultima["cuerpos"] is None:
        return None
    return round(ultima["tiempo_referencia"] + ultima["cuerpos"] * SEGUNDOS_POR_CUERPO, 2)


def generar():
    programas = json.loads(ENTRADA.read_text(encoding="utf-8"))["programas"]
    fechas_programa = {p["fecha"] for p in programas}
    referencia = date.fromisoformat(max(fechas_programa))
    inicio_historia = referencia - timedelta(days=DIAS_HISTORIA)

    # Resultado de cada caballo en cada fecha, leído del programa siguiente en que reaparece
    resultados: dict[tuple[str, str], dict] = {}
    for p in programas:
        for c in p["carreras"]:
            for k in c["corredores"]:
                u = k["ultima"]
                if u and k["llegadas"]:
                    ult = k["llegadas"][-1]
                    resultados[(k["nombre"], u["fecha"])] = {
                        "posicion": ult["posicion"], "tiempo": tiempo_propio(u, ult["gano"]),
                        "dividendo": u["dividendo"], "distancia": u["distancia"], "serie": u["serie"],
                        "peso_fisico": u["peso_fisico"],
                    }

    # Los colores son del stud: se junta la descripción más completa de cada uno entre todos los corredores
    colores_stud: dict[str, list[str]] = {}
    for p in programas:
        for c in p["carreras"]:
            for k in c["corredores"]:
                if k.get("stud") and k.get("colores"):
                    colores_stud.setdefault(k["stud"], []).append(k["colores"])

    propios: dict[str, dict] = {}
    rivales: dict[str, dict] = {}
    carreras, participaciones, mediciones = [], [], []
    fechas_carrera: dict[str, list[date]] = {}

    def registrar_caballo(k: dict, condicion: str | None, es_propio: bool):
        destino = propios if es_propio else rivales
        ficha = destino.setdefault(k["nombre"], {"sexo": None, "madre": None, "pelaje": None, "colores": []})
        if k.get("colores"):
            ficha["colores"].append(k["colores"])
        if k.get("stud"):
            ficha["colores"].extend(colores_stud.get(k["stud"], []))
        ficha.update(padre=k["padrillo"], edad=k["edad"])
        ficha["sexo"] = ficha["sexo"] or sexo_por_condicion(condicion)
        if k["debut"]:
            ficha["madre"] = k["debut"]["madre"]
        ficha.update(CONOCIDOS.get(k["nombre"], {}))
        return ficha

    for p in programas:
        for c in p["carreras"]:
            if not any(k["preparador"] == PREPARADOR_PROGRAMA for k in c["corredores"]):
                continue
            cid = f"chs-{p['fecha']}-{c['numero']:02d}"
            carreras.append({
                "id": cid, "hipodromo": CLUB_HIPICO, "fecha": p["fecha"], "hora": c["hora"], "numero": c["numero"],
                "distancia_m": c["distancia"], "superficie": c["superficie"], "condicion_pista": None,
                "serie": nombre_propio(c["condicion"]) if c["condicion"] else None, "premio": None,
            })
            for k in c["corredores"]:
                es_propio = k["preparador"] == PREPARADOR_PROGRAMA
                registrar_caballo(k, c["condicion"], es_propio)
                prefijo = "cg" if es_propio else "r"
                res = resultados.get((k["nombre"], p["fecha"]))
                participaciones.append({
                    "id": f"{cid}-{k['mandil']:02d}", "carrera_id": cid, "caballo_id": f"{prefijo}-{slug(k['nombre'])}",
                    "numero": k["mandil"], "jinete": k["jinete"], "peso_kg": k["peso_jinete"],
                    "posicion": res["posicion"] if res else None,
                    "tiempo_seg": res["tiempo"] if res else None,
                    "dividendo": res["dividendo"] if res else None,
                })
                if es_propio:
                    fechas_carrera.setdefault(k["nombre"], []).append(date.fromisoformat(p["fecha"]))

    # Carreras previas a los programas que tenemos (la "última carrera" de su primera aparición)
    for nombre in propios:
        primeras = [(p["fecha"], k) for p in programas for c in p["carreras"] for k in c["corredores"] if k["nombre"] == nombre]
        _, k = min(primeras, key=lambda x: x[0])
        u = k["ultima"]
        if u and u["fecha"] not in fechas_programa and k["llegadas"]:
            cid = f"hist-{slug(nombre)}-{u['fecha']}"
            carreras.append({
                "id": cid, "hipodromo": HIPODROMO_SIN_DATO, "fecha": u["fecha"], "hora": None, "numero": None,
                "distancia_m": u["distancia"], "superficie": None, "condicion_pista": None,
                "serie": None if u["serie"] in ("Cl", "Cd") else u["serie"], "premio": None,
            })
            participaciones.append({
                "id": f"{cid}-p", "carrera_id": cid, "caballo_id": f"cg-{slug(nombre)}", "numero": None,
                "jinete": None, "peso_kg": None, "posicion": k["llegadas"][-1]["posicion"],
                "tiempo_seg": tiempo_propio(u, k["llegadas"][-1]["gano"]), "dividendo": u["dividendo"],
            })
            fechas_carrera.setdefault(nombre, []).append(date.fromisoformat(u["fecha"]))

    # Peso físico real: el que declara el programa para su última carrera
    vistos = set()
    for (nombre, fecha), res in sorted(resultados.items(), key=lambda x: x[0][1]):
        if nombre in propios and (nombre, fecha) not in vistos and res["peso_fisico"]:
            vistos.add((nombre, fecha))
            mediciones.append({"id": f"pf-{slug(nombre)}-{fecha}", "caballo_id": f"cg-{slug(nombre)}",
                               "fecha": fecha, "tipo": "peso_kg", "valor": float(res["peso_fisico"])})

    caballos = [
        {"id": f"cg-{slug(n)}", "corral_id": "c01", "nombre": nombre_propio(n), "sexo": f["sexo"], "edad": f["edad"],
         "pelaje": f["pelaje"], "padre": f["padre"], "madre": f["madre"], "estado": "activo", "foto_url": None,
         "colores": limpiar_colores(f["colores"])}
        for n, f in sorted(propios.items())
    ] + [
        {"id": f"r-{slug(n)}", "corral_id": None, "nombre": nombre_propio(n), "sexo": f["sexo"], "edad": f["edad"],
         "pelaje": None, "padre": f["padre"], "madre": f["madre"], "estado": "activo", "foto_url": None,
         "colores": limpiar_colores(f["colores"])}
        for n, f in sorted(rivales.items()) if n not in propios
    ]

    # Velocidad de carrera real de cada caballo, para que sus aprontes ficticios sean coherentes
    velocidad = {}
    for part in participaciones:
        if part["caballo_id"].startswith("cg-") and part["tiempo_seg"]:
            dist = next(c["distancia_m"] for c in carreras if c["id"] == part["carrera_id"])
            velocidad.setdefault(part["caballo_id"], []).append(dist / part["tiempo_seg"])

    sesiones = []
    n_sesion = 0
    for n in sorted(propios):
        kid = f"cg-{slug(n)}"
        corridas = sorted(fechas_carrera.get(n, []))
        debuta = not any(part["caballo_id"] == kid and part["posicion"] for part in participaciones)
        inicio = referencia - timedelta(days=90) if debuta else inicio_historia
        v_base = statistics.median(velocidad[kid]) - 0.4 if kid in velocidad else 15.9
        d = inicio
        while d < referencia:
            if d in corridas:
                d += timedelta(days=1)
                continue
            futuras = [c for c in corridas if c > d]
            pasadas = [c for c in corridas if c < d]
            tipo = tipo_del_dia(d, (futuras[0] - d).days if futuras else None, (d - pasadas[-1]).days if pasadas else None)
            if debuta and tipo == "apronte" and random.random() < 0.5:
                tipo = "huinchada"
            distancia, tiempo = distancia_y_tiempo(tipo, v_base)
            n_sesion += 1
            sesiones.append({
                "id": f"s{n_sesion:05d}", "caballo_id": kid, "fecha": iso(d), "tipo": tipo,
                "distancia_m": distancia, "tiempo_seg": tiempo,
                "vareador": None if tipo == "descanso" else random.choice(VAREADORES),
                "pista": None if tipo in ("descanso", "caminadora", "nado") else random.choice(["arena", "arena", "pasto"]),
                "estado_pista": None if tipo in ("descanso", "caminadora", "nado") else random.choice(["normal", "normal", "pesada", "liviana"]),
                "esfuerzo": esfuerzo(tipo),
                "notas": random.choice(NOTAS) if tipo in ("apronte", "huinchada", "galope_fuerte") and random.random() < 0.3 else None,
            })
            d += timedelta(days=1)

    ids_propios = [f"cg-{slug(n)}" for n in sorted(propios)]
    caballo_propietario = [{"caballo_id": kid, "propietario_id": PROPIETARIOS[i % len(PROPIETARIOS)][0], "porcentaje": 100}
                           for i, kid in enumerate(ids_propios)]
    # Un ejemplar con tres dueños, para mostrar porcentajes
    copropiedad = ids_propios[len(ids_propios) // 2]
    caballo_propietario = [cp for cp in caballo_propietario if cp["caballo_id"] != copropiedad] + [
        {"caballo_id": copropiedad, "propietario_id": "p06", "porcentaje": 50},
        {"caballo_id": copropiedad, "propietario_id": "p07", "porcentaje": 30},
        {"caballo_id": copropiedad, "propietario_id": "p08", "porcentaje": 20},
    ]

    tratamientos = []
    for i, kid in enumerate(ids_propios):
        for dias, producto, via in [(150, "Vacuna influenza equina", "intramuscular"), (60, "Ivermectina", "oral")]:
            tratamientos.append({
                "id": f"t{i:02d}{dias}", "caballo_id": kid, "fecha": iso(referencia - timedelta(days=dias)),
                "producto": producto, "via": via, "veterinario": "Dra. Paula Rojas", "dias_retiro": 0, "visibilidad": "completa",
            })

    return {
        "version": VERSION_SEED,
        "fecha_referencia": iso(referencia),
        "corrales": [{"id": "c01", "nombre": "Corral Cristóbal González", "preparador": "Cristóbal González M.", "hipodromo": CLUB_HIPICO}],
        "caballos": caballos,
        "propietarios": [{"id": pid, "nombre": nombre, "email": f"contacto{pid[1:]}@ejemplo.cl", "telefono": f"+56 9 5555 0{pid[1:]}"}
                         for pid, nombre in PROPIETARIOS],
        "caballo_propietario": caballo_propietario,
        "sesiones": sesiones,
        "mediciones": mediciones,
        "tratamientos": tratamientos,
        "carreras": carreras,
        "participaciones": participaciones,
        "parciales": [],  # GMAX no viene en el programa: la app lo dice en vez de inventarlo
    }


def tipo_del_dia(d: date, dias_a_carrera: int | None, dias_desde_carrera: int | None) -> str:
    if dias_desde_carrera is not None and dias_desde_carrera <= 2:
        return "descanso" if dias_desde_carrera == 1 else "caminadora"
    if dias_a_carrera == 1:
        return "vareo"
    if dias_a_carrera == 5:
        return "apronte"
    dow = d.weekday()
    if dow == 6:
        return "descanso"
    if dow == 0:
        return "vareo"
    if dow == 1:
        return "galope_suave"
    if dow == 2:
        return random.choice(["trote", "trote", "caminadora"])
    if dow == 3:
        return "apronte" if d.isocalendar().week % 2 == 0 else "galope_fuerte"
    if dow == 4:
        return random.choice(["vareo", "vareo", "nado"])
    return "huinchada" if random.random() < 0.2 else "galope_suave"


def distancia_y_tiempo(tipo: str, v_base: float) -> tuple[int | None, float | None]:
    distancia = {
        "apronte": random.choice([600, 800, 800, 1000]), "huinchada": 400,
        "galope_suave": random.choice([2000, 2400, 2800, 3200]), "galope_fuerte": random.choice([2000, 2400, 2800, 3200]),
        "trote": random.choice([1600, 2000, 2400]),
    }.get(tipo)
    if tipo == "vareo" and random.random() < 0.5:
        distancia = random.choice([1200, 1600])
    tiempo = None
    if tipo in ("apronte", "huinchada") and random.random() > 0.12:
        v = v_base + (0.8 if tipo == "huinchada" else 0) - (distancia - 600) / 800 * 0.5
        tiempo = round(distancia / (v + random.gauss(0, 0.12)), 1)
    return distancia, tiempo


def esfuerzo(tipo: str) -> int | None:
    if tipo == "descanso" or random.random() < 0.1:
        return None
    base = {"apronte": 4, "huinchada": 5, "galope_fuerte": 4, "galope_suave": 3, "trote": 2, "vareo": 1, "nado": 2, "caminadora": 1}[tipo]
    return max(1, min(5, base + random.choice([0, 0, 1, -1])))


if __name__ == "__main__":
    datos = generar()
    SALIDA.parent.mkdir(parents=True, exist_ok=True)
    SALIDA.write_text(json.dumps(datos, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    propios = [c for c in datos["caballos"] if c["corral_id"]]
    print({k: len(v) for k, v in datos.items() if isinstance(v, list)}, f"{SALIDA.stat().st_size / 1024:.0f} KB")
    print(f"{len(propios)} ejemplares propios:", ", ".join(c["nombre"] for c in propios))
