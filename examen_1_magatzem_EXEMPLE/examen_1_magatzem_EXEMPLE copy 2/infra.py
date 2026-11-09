"""Utilitats comunes. Només biblioteca estàndard de Python 3.9+."""
import json
import os
import tempfile
from pathlib import Path


class AppError(Exception):
    def __init__(self, codi, missatge):
        super().__init__(missatge)
        self.codi = codi

    def as_dict(self):
        return {"ok": False, "codi": self.codi, "missatge": str(self)}


def read_json(path):
    with open(path, encoding="utf-8") as stream:
        return json.load(stream)


def write_json(path, data):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8",
                                         dir=path.parent, delete=False) as stream:
            temporary = Path(stream.name)
            json.dump(data, stream, ensure_ascii=False, indent=2, allow_nan=False)
            stream.write("\n")
        os.replace(temporary, path)
    finally:
        if temporary is not None and temporary.exists():
            temporary.unlink()


def project_path(root, path):
    root = Path(root).resolve()
    candidate = (root / path).resolve()
    if candidate != root and root not in candidate.parents:
        raise AppError("RUTA_INVALIDA", "La ruta ha de ser dins del projecte.")
    return candidate
