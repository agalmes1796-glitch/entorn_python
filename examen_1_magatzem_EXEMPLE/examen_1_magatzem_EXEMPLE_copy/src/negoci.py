"""Mòdul de negoci per generar i validar informes de reposició."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def llegir_inventari():
    """Llegeix l'inventari des de dades/inventari.json."""
    path = ROOT / "dades" / "inventari.json"
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def generar_informe(inventari=None):
    """Genera un informe de basat en les dades de l'inventari.

    L'informe conté:
    - reposicions: llista de diccionaris amb id i quantitat per productes amb estoc baix
    - unitats_totals: total de unitats a reposar
    - descartats: llista d'ids de productes descartaos (stock negatiu o suficient)
    """
    if inventari is None:
        inventari = llegir_inventari()

    reposicions = []
    descartats = []
    total = 0

    for producte in inventari:
        pid = producte["id"]
        estoc = producte["estoc"]
        minim = producte["minim"]
        objectiu = producte["objectiu"]

        # Si l'estoc és negatiu, el producte es descarta
        if estoc < 0:
            descartats.append(pid)
            continue

        # Si l'estoc és estrictament inferior al mínim, necessitem reposició
        if estoc < minim:
            quantitat = objectiu - estoc
            if quantitat > 0:
                reposicions.append({"id": pid, "quantitat": quantitat})
                total += quantitat
            else:
                # Si l'objectiu ja s'ha superat amb l'estoc negatiu/baix, descarta
                descartats.append(pid)
        # Si l'estoc >= minim, però estoc < objectiu, el producte està en un estat "equilibrat"
        # que no requereix acció immediata segons la nostra lógica
        elif estoc >= objectiu:
            # Stock suficient o excessiu rispetto l'objectiu
            descartats.append(pid)
        # Si minim <= estoc < objectiu, el producte no necessita acció (surt buit d'aquestes 2 categories)

    informe = {
        "reposicions": reposicions,
        "unitats_totals": total,
        "descartats": descartats
    }

    return informe


def validar_informe(informe):
    """Valida un informe de reposició.

    Comprova:
    A. Camps obligatoris: reposicions, unitats_totals, descartats
    B. Quantitats han de ser enters estrictament positives
    C. Identificadors no poden repetir-se dins de les reposicions
    D. El total ha de coincidir amb la suma de les quantitats

    Retorna un diccionari amb 'valid' i 'errors'.
    """
    errors = []

    # A. Comprovar camps obligatoris
    required_fields = ["reposicions", "unitats_totals", "descartats"]
    for field in required_fields:
        if field not in informe:
            errors.append(f"Camp obligatori missing: {field}")

    # Si ja tenim errors crítics, no continuem amb algunes comprovacions
    if errors:
        return {"valid": False, "errors": errors}

    # B. Comprovar que les quantitats han de ser enters estrictament positives (no booleans)
    reposicions = informe["reposicions"]
    for i, reposicio in enumerate(reposicions):
        quantitat = reposicio.get("quantitat")
        # Reject booleans explicitly (bool is subclass of int in Python)
        if isinstance(quantitat, bool):
            errors.append(f"Quantitat no vàlida en reposicio {reposicio.get('id')}: valors boolean no acceptes")
        elif not isinstance(quantitat, int):
            errors.append(f"Quantitat no vàlida en reposicio {reposicio.get('id')}: esperat int, rebut {type(quantitat).__name__}")
        elif quantitat <= 0:
            errors.append(f"Quantitat ha de ser positiva en reposicio {reposicio.get('id')}: {quantitat}")

    # C. Comprovar que no hi ha identificadors repetits
    ids = [r.get("id") for r in reposicions if r.get("id")]
    if len(ids) != len(set(ids)):
        # Detectar els repetits
        ids_counts = {}
        for id_val in ids:
            ids_counts[id_val] = ids_counts.get(id_val, 0) + 1
        repetits = [id_val for id_val, count in ids_counts.items() if count > 1]
        errors.append(f"Identificadors repetits detectats: {repetits}")

    # D. Comprovar que el total coincideix amb la suma de les quantitats
    suma_quantitats = sum(r.get("quantitat", 0) for r in reposicions)
    if informe.get("unitats_totals") != suma_quantitats:
        errors.append(f"unitats_totals ({informe.get('unitats_totals')}) no coincideix amb la suma de les quantitats ({suma_quantitats})")

    # Si no hi ha errors, és valid
    if not errors:
        return {"valid": True, "errors": []}

    return {"valid": False, "errors": errors}