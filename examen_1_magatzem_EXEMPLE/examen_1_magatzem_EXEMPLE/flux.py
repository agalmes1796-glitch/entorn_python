"""Prova tècnica bàsica. No substitueix la delegació real dins d’OpenCode."""
import json
from pathlib import Path
from infra import read_json, write_json
from mcp_client import MCPClient
from src.negoci import generar_informe, validar_informe

ROOT = Path(__file__).resolve().parent


def main():
    with MCPClient(ROOT) as client:
        dades = client.call("llegir_inventari")["inventari"]
        informe = generar_informe(dades)
        validacio = validar_informe(informe)
        if not validacio["valid"]:
            raise ValueError(validacio["errors"])
        write_json(ROOT / "artefactes/informe.json", informe)
        resultat = client.call("desar_informe", {"informe": informe})
    write_json(ROOT / "evidencies/prova_tecnica.json", {
        "origen": "prova_tecnica_python_sense_delegacio", "informe": informe,
        "validacio": validacio, "persistencia": resultat})
    print(json.dumps(resultat, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
