"""MCP inicial del magatzem. Les dues operacions són a completar."""
from pathlib import Path
from infra import AppError
from mcp_stdio import MCPServer, definition

ROOT = Path(__file__).resolve().parent

def llegir_inventari():
    raise AppError("PENDENT_IMPLEMENTACIO", "Completeu la lectura de l’inventari.")

def desar_informe(informe):
    raise AppError("PENDENT_IMPLEMENTACIO", "Completeu la validació i el desament.")

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
