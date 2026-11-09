"""Client de proves/CLI. OpenCode utilitza el seu propi client MCP."""
import argparse
import json
import selectors
import subprocess
import sys
from infra import AppError


class MCPClient:
    def __init__(self, root, env=None):
        self.root, self.env, self.sequence = root, env, 0

    def __enter__(self):
        self.process = subprocess.Popen([sys.executable, "mcp_server.py"],
            cwd=self.root, env=self.env, stdin=subprocess.PIPE,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1)
        self.request("initialize", {"protocolVersion": "2025-11-25",
            "capabilities": {}, "clientInfo": {"name": "proves", "version": "1"}})
        self.send({"jsonrpc": "2.0", "method": "notifications/initialized"})
        return self

    def send(self, value):
        self.process.stdin.write(json.dumps(value) + "\n")
        self.process.stdin.flush()

    def request(self, method, params=None):
        self.sequence += 1
        request_id = self.sequence
        self.send({"jsonrpc": "2.0", "id": request_id,
                   "method": method, "params": params or {}})
        with selectors.DefaultSelector() as selector:
            selector.register(self.process.stdout, selectors.EVENT_READ)
            if not selector.select(45):
                raise AppError("TIMEOUT", "El servidor MCP no ha respost a temps.")
            line = self.process.stdout.readline()
        if not line:
            raise AppError("MCP_TANCAT", "El servidor MCP ha tancat la connexió.")
        response = json.loads(line)
        if response.get("id") != request_id:
            raise AppError("MCP_PROTOCOL", "Identificador de resposta inesperat.")
        if "error" in response:
            raise AppError("MCP_PROTOCOL", response["error"]["message"])
        return response["result"]

    def call(self, name, arguments=None):
        result = self.request("tools/call", {"name": name, "arguments": arguments or {}})
        value = result.get("structuredContent")
        if value is None:
            value = json.loads(result["content"][0]["text"])
        if result.get("isError"):
            raise AppError(value.get("codi", "MCP_ERROR"), value.get("missatge", "Error MCP"))
        return value

    def __exit__(self, *_):
        if self.process.stdin:
            self.process.stdin.close()
        try:
            self.process.wait(timeout=2)
        except subprocess.TimeoutExpired:
            self.process.terminate()
            self.process.wait(timeout=2)
        self.process.stdout.close()
        self.process.stderr.close()


def main(root):
    parser = argparse.ArgumentParser()
    parser.add_argument("tool")
    parser.add_argument("arguments", nargs="?", default="{}")
    args = parser.parse_args()
    try:
        with MCPClient(root) as client:
            result = client.call(args.tool, json.loads(args.arguments))
        print(json.dumps(result, ensure_ascii=False, indent=2))
    except AppError as exc:
        print(json.dumps(exc.as_dict(), ensure_ascii=False), file=sys.stderr)
        raise SystemExit(1)
