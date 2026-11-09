"""MCP inicial del magatzem. Les dues operacions són a completar."""
import json
from pathlib import Path
from infra import AppError, read_json, write_json
from mcp_stdio import MCPServer, definition

ROOT = Path(__file__).resolve().parent

def llegir_inventari():
    """Llegir l'inventari actual des de dades/inventari.json."""
    try:
        inventari = read_json(ROOT / "dades" / "inventari.json")
        return {"ok": True, "inventari": inventari}
    except FileNotFoundError:
        return {"ok": False, "codi": "INVENTARI_NO_ENCONTRA", "missatge": "Fitxer inventari.json no trobat"}
    except json.JSONDecodeError as e:
        return {"ok": False, "codi": "INVENTARI_INVALIDE", "missatge": f"Error parsing JSON: {e}"}
    except Exception as e:
        return {"ok": False, "codi": "ERROR_INEXPECTAT", "missatge": str(e)}

def desar_informe(informe):
    """Validar i desar l’informe. Si és correcte, desar-lo i retornar la ubicació."""
    # Comprovar que té els camps obligatoris
    if "reposicions" not in informe or "unitats_totals" not in informe:
        raise AppError("INFORME_INCOMPLETO", "L'informe ha de contenir els camps 'reposicions' i 'unitats_totals'.")

    # Comprobar que les reposicions tenen quantitats positives
    if informe["reposicions"]:
        for reposicio in informe["reposicions"]:
            if "quantitat" not in reposicio or reposicio["quantitat"] <= 0:
                raise AppError("QUANTITAT_NO_POSITIVA", "Les quantitats de les reposicions de han de ser positives.")
            if "id" not in reposicio:
                raise AppError("ID_FALTANT", "Cadena de reposició ha de contenir un camp 'id'.")

    # Comprobar que el total coincideix amb la suma
    if informe["reposicions"]:
        suma = sum(r["quantitat"] for r in informe["reposicions"])
        if suma != informe["unitats_totals"]:
            raise AppError("TOTAL_MISMATCH", f"La suma dels reposicions ({suma}) no coincideix amb unitats_totals ({informe['unitats_totals']}).")

    # Desar l'informe correcte
    try:
        write_json(ROOT / "artefactes" / "informe.json", informe)
        return {"ok": True, "locacio": str(ROOT / "artefactes" / "informe.json")}
    except Exception as e:
        raise AppError("ERROR_DESARTA", f"Error al desar l'informe: {e}")

TOOLS = [
    definition("llegir_inventari", "Consulta l’inventari actual."),
    definition("desar_informe", "Valida i desa l’informe.",
               {"informe": {"type": "object"}}, ["informe"]),
]

def dispatch(name, arguments):
    if name == "llegir_inventari":
        return llegir_inventari()
    return desar_informe(arguments["informe"])

if __name__ == "__main__":
    MCPServer("magatzem", TOOLS, dispatch).serve()
