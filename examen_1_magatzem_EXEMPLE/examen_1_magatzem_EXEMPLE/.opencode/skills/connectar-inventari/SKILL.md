---
name: connectar-inventari
description: Integrar i verificar el servidor MCP del magatzem: les dues eines llegir_inventari i desar_informe, i el seu registre a opencode.json.
---

## Quan s'ha d'utilitzar
Per al subagent **integrador**, quan cal implementar, corregir o verificar les dues eines MCP del magatzem (`llegir_inventari` i `desar_informe`) o el registre del servidor a `opencode.json`.

## Entrades
- La tasca, les dades i el contracte de sortida lliurats pel coordinador.
- `enunciat.pdf` (secció «Subagents, MCP i eines»): contracte de les dues operacions.
- Esquelets: `mcp_server.py` (eines pendents), `mcp_stdio.py` (protocol, no tocar), `mcp_client.py` (client de proves), `infra.py` (utilitats), `opencode.json` (registre MCP).
- `dades/inventari.json` (dades de lectura) i `dades/esperat.json` (exemple de resultat vàlid per a les proves).

## Procediment
1. Llegeix `enunciat.pdf`, `mcp_server.py` i `opencode.json`. No modifiquis `mcp_stdio.py` (el protocol stdio ja és complet).
2. Implementa `llegir_inventari()` a `mcp_server.py`:
   - Llegeix `dades/inventari.json` amb `infra.read_json` (ruta relativa a l'arrel del projecte, `ROOT`).
   - Retorna `{"inventari": [...]}`.
   - Fitxer inexistente o JSON invàlid → `AppError("INVENTARI_NO_DISPONIBLE", ...)`; contingut que no sigui una llista → `AppError("INVENTARI_INVALID", ...)`.
3. Implementa `desar_informe(informe)` a `mcp_server.py`:
   - Valida l'estructura amb el «Contracte de validació» d'aquesta skill.
   - Si és vàlid: desa amb `infra.write_json` a `artefactes/informe.json` i retorna
     `{"ok": true, "ruta": "artefactes/informe.json", "unitats_totals": N, "num_reposicions": K, "num_descartats": M}`.
   - Si és invàlid: llança `AppError("INFORME_INVALIDE", ...)` amb la **llista de tots els motius** i **no escriguis** el fitxer (cap informe correcte anterior no ha de ser substituït).
4. Comprova el registre a `opencode.json`: `mcp.servers.magatzem` amb `type: "local"`, `command: ["python3", "mcp_server.py"]`, `cwd: "."`, `codemode: false`. Executa `opencode mcp list` i verifica `magatzem connected`.
5. Prova amb el client de proves des de l'arrel del projecte:
   - `python3 mcp_call.py llegir_inventari`
   - `python3 mcp_call.py desar_informe '{"reposicions": [], "unitats_totals": 0, "descartats": []}'` (informe vàlid)
   - Un informe invàlid per comprovar el rebut i que l'anterior no canvia (llegeix `artefactes/informe.json` abans i després).

## Contracte de validació de l'informe (idèntic a l'eina personalitzada `validar_informe`)
- Camps obligatoris a nivell superior: `reposicions` (llista), `unitats_totals` (enter no negatiu), `descartats` (llista de cadenes). No s'admeten camps extra.
- Cada reposició: objecte amb `id` (cadena no buida) i `quantitat` (enter **positiu**; els booleans no són enters vàlids).
- Cap `id` repetit dins de `reposicions`; `descartats` sense repeticions.
- `unitats_totals` ha de coincidir amb la suma de les quantitats.

## Eines
- `read` / `edit` / `write` per a `mcp_server.py` i `opencode.json`.
- `shell` per a `python3 mcp_call.py ...`, `opencode mcp list` i lectures comparatives de `artefactes/informe.json`.
- Dins d'OpenCode, les eines del servidor es publiquen com `magatzem_llegir_inventari` i `magatzem_desar_informe` (codemode desactivat): si són visibles al catàleg de la sessió, la prova definitiva és cridar-les.

## Resultat esperat
- Fitxers modificats: `mcp_server.py` (i `opencode.json` només si el registre era erroni).
- Evidències a `evidencies/integrador_*.json`: sortida de `llegir_inventari`, desament d'un informe vàlid, rebut d'un informe invàlid i comprovació que l'informe anterior no s'ha substituït.
- Resum final al coordinador: fitxers, ordres, sortides observades i errors pendents (codi + missatge).
