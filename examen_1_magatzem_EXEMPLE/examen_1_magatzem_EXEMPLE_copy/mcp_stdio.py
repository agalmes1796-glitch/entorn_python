"""Servidor MCP stdio educatiu: initialize, tools/list, tools/call i ping.

Missatges JSON-RPC delimitats per salts de línia. No és un servidor HTTP.
La sortida estàndard es reserva exclusivament al protocol.
"""
import json
import sys
from infra import AppError

VERSIONS = ("2024-11-05", "2025-03-26", "2025-06-18", "2025-11-25")


class MCPServer:
    def __init__(self, name, tools, dispatch):
        self.name, self.tools, self.dispatch = name, tools, dispatch
        self.initialized = False

    def handle(self, request):
        if not isinstance(request, dict) or request.get("jsonrpc") != "2.0":
            return self.error(None, -32600, "Sol·licitud JSON-RPC invàlida")
        method, request_id = request.get("method"), request.get("id")
        if "id" not in request:
            return None
        params = request.get("params", {})
        if not isinstance(params, dict):
            return self.error(request_id, -32602, "params ha de ser un objecte")
        if method == "initialize":
            version = params.get("protocolVersion")
            self.initialized = True
            result = {
                "protocolVersion": version if version in VERSIONS else VERSIONS[-1],
                "capabilities": {"tools": {"listChanged": False}},
                "serverInfo": {"name": self.name, "version": "1.0.0"},
            }
        elif method == "ping":
            result = {}
        elif not self.initialized:
            return self.error(request_id, -32000, "Inicialitzeu primer la connexió")
        elif method == "tools/list":
            result = {"tools": self.tools}
        elif method == "tools/call":
            tool = next((t for t in self.tools if t["name"] == params.get("name")), None)
            if tool is None:
                return self.error(request_id, -32602, "Eina desconeguda")
            arguments = params.get("arguments", {})
            schema = tool["inputSchema"]
            if not isinstance(arguments, dict):
                return self.error(request_id, -32602, "arguments ha de ser un objecte")
            if set(arguments) - set(schema.get("properties", {})):
                return self.error(request_id, -32602, "Arguments desconeguts")
            if not set(schema.get("required", [])).issubset(arguments):
                return self.error(request_id, -32602, "Falten arguments obligatoris")
            try:
                value = self.dispatch(tool["name"], arguments)
                result = self.tool_result(value, False)
            except AppError as exc:
                result = self.tool_result(exc.as_dict(), True)
            except (ValueError, TypeError, KeyError, OSError):
                result = self.tool_result({"ok": False, "codi": "DADES_INVALIDES",
                    "missatge": "No s’ha pogut processar l’eina; reviseu les dades i les rutes."}, True)
        else:
            return self.error(request_id, -32601, "Mètode no admès")
        return {"jsonrpc": "2.0", "id": request_id, "result": result}

    @staticmethod
    def tool_result(value, error):
        return {"content": [{"type": "text", "text": json.dumps(value, ensure_ascii=False)}],
                "structuredContent": value, "isError": error}

    @staticmethod
    def error(request_id, code, message):
        return {"jsonrpc": "2.0", "id": request_id,
                "error": {"code": code, "message": message}}

    def serve(self):
        for line in sys.stdin:
            try:
                response = self.handle(json.loads(line))
            except (ValueError, TypeError):
                response = self.error(None, -32700, "JSON invàlid")
            if response is not None:
                print(json.dumps(response, ensure_ascii=False), flush=True)


def definition(name, description, properties=None, required=None):
    return {"name": name, "description": description, "inputSchema": {
        "type": "object", "properties": properties or {},
        "required": required or [], "additionalProperties": False}}
