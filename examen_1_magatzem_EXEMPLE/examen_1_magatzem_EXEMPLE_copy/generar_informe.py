"""Genera l'informe de reposicions a partir de l'inventari.

Lògica:
- Si estoc < 0 -> producte Descartat
- Si estoc < minim -> necessita reposició de (objectiu - estoc) unitats
- Si estoc >= minim -> sense reposició (già a l'estoc mínim)
"""

import json
import sys
from pathlib import Path
from infra import read_json, write_json, AppError


def validar_informe(informe):
    """Validar l'informe generat. Retorna dic amb clau 'valid' i 'errors'."""
    errors = []

    # Ha de contenir 'reposicions'
    if "reposicions" not in informe:
        errors.append("Falta el camp 'reposicions'")
        return {"valid": False, "errors": errors}

    # Ha de contenir 'unitats_totals'
    if "unitats_totals" not in informe:
        errors.append("Falta el camp 'unitats_totals'")
        return {"valid": False, "errors": errors}

    # Comprovar que les reposicions tenen quantitats positives
    reposicions = informe.get("reposicions", [])
    for reposicio in reposicions:
        if "quantitat" not in reposicio:
            errors.append(f"Reposició sense camp 'quantitat': {reposicio}")
        elif reposicio["quantitat"] <= 0:
            errors.append(f"Quantitat no positiva en {reposicio.get('id', 'desconegut')}")

    # Comprovar que el total coincideix amb la suma
    if reposicions:
        suma = sum(r["quantitat"] for r in reposicions)
        if suma != informe["unitats_totals"]:
            errors.append(f"La suma dels reposicions ({suma}) no coincideix amb unitats_totals ({informe['unitats_totals']})")

    # Comprobar que'hi ha descartats si cal
    # (no és obligatori però s'ha de tenir en compte)

    valid = len(errors) == 0
    return {"valid": valid, "errors": errors}


def generar_inventari(ruta_entrada):
    """Llegir l'inventari i generar l'informe de reposicions."""
    try:
        inventari = read_json(ruta_entrada)
    except FileNotFoundError:
        raise AppError("ENTRADA_NO_ENCONTRO", f"Fitxer d'entrada no trobat: {ruta_entrada}")
    except json.JSONDecodeError as e:
        raise AppError("ENTRADA_INVALIDE", f"Error parsing JSON a {ruta_entrada}: {e}")

    reposicions = []
    descartats = []

    for producte in inventari:
        id = producte["id"]
        estoc = producte["estoc"]
        minim = producte["minim"]
        objectiu = producte["objectiu"]

        if estoc < 0:
            descartats.append(id)
        elif estoc < minim:
            # Necessita reposició per arribar a l'objectiu
            quantitat = objectiu - estoc
            reposicions.append({"id": id, "quantitat": quantitat})
        # else: estoc >= minim, sense reposició

    unitats_totals = sum(r["quantitat"] for r in reposicions)

    informe = {
        "reposicions": reposicions,
        "unitats_totals": unitats_totals,
        "descartados": descartats,
    }

    return informe


def main():
    if len(sys.argv) < 2:
        print("Ús: generar_informe.py <ruta_entrada_json>", file=sys.stderr)
        sys.exit(1)

    ruta_entrada = sys.argv[1]

    # Generar l'informe
    informe = generar_inventari(ruta_entrada)

    # Validar l'informe
    validacio = validar_informe(informe)
    if not validacio["valid"]:
        print(f"Error de validació: {validacio['errors']}", file=sys.stderr)
        sys.exit(1)

    # Escriure l'informe a artefactes/informe.json
    ROOT = Path(__file__).resolve().parent
    ruta_sortida = ROOT / "artefactes" / "informe.json"
    write_json(ruta_sortida, informe)

    print(json.dumps(informe, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()