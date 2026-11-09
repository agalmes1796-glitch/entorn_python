---
name: auditar-reposicio
description: Provar les regles de reposició i validar l'informe amb l'eina personalitzada validar_informe (plugin d'OpenCode) abans del desament definitiu.
---

## Quan s'ha d'utilitzar
Per al subagent **verificador**, quan cal crear les proves automatitzades obligatòries, implementar l'eina personalitzada `validar_informe` i verificar el resultat abans de guardar l'informe definitiu per MCP.

## Entrades
- La tasca, les dades i el contracte de sortida lliurats pel coordinador.
- `enunciat.pdf` (seccions «Eina personalitzada d'OpenCode», «Flux que heu de demostrar» i «Proves i lliurament»: els sis grups de proves obligatòries).
- Codis ja implementats pels altres subagents: `src/negoci.py` (lògica), `mcp_server.py` (eines MCP), `mcp_client.py` (client de proves per al grup 6), `flux.py` (flux complet).

## Procediment
1. Llegeix `enunciat.pdf`, `src/negoci.py`, `mcp_server.py`, `mcp_client.py` i `flux.py`.
2. Implementa `src/validar_cli.py`:
   - `python3 src/validar_cli.py <ruta>` llegeix l'informe JSON de la ruta, aplica la validació (mateix contracte que l'eina `validar_informe` de l'enunciat) i imprimeix `{"valid": ..., "errors": [...]}` a stdout.
   - Codi de sortida: 0 si és vàlid, 1 si no ho és (o si la ruta/JSON no es pot llegir, amb el missatge d'error al JSON).
3. Implementa l'eina d'OpenCode com a **plugin de servidor V2** a `.opencode/plugins/validar-informe/index.ts`:
   - Registra l'eina `validar_informe` amb input `{"ruta": "string"}` (la ruta de l'informe).
   - L'execució delega a `python3 src/validar_cli.py <ruta>` (amb el directori del projecte com a cwd) per tenir un únic origen de veritat, i retorna el JSON de la validació com a contingut.
   - Equivalència amb la plantilla: la skill original menciona `.opencode/tools-lib/`; a OpenCode V2 el mecanisme d'eines personalitzades és un plugin de servidor a `.opencode/plugins/`. Documenta aquesta equivalència al resum final.
4. Crea `tests/test_reposicio.py` amb `unittest` (standard library) amb almenys un cas concret per a cadascun dels sis grups de l'enunciat:
   1. L'exemple d'entrada i el resultat esperat (compara amb `dades/esperat.json`).
   2. Un inventari buit → `{"reposicions": [], "unitats_totals": 0, "descartats": []}`.
   3. Un producte amb l'estoc exactament igual al mínim → sense compra.
   4. Dades invàlides i identificadors descartats, inclosos els tipus no admesos (booleans, negatius, `objectiu < minim`, camps amb tipus no admès) → `descartats` amb l'ordre d'entrada.
   5. L'ordre de les reposicions (id ascendent) i el càlcul del total d'unitats.
   6. El rebuig d'un informe invàlid per l'eina MCP `desar_informe`: uses `MCPClient` de `mcp_client.py` (arrel del projecte com a primer argument) i comproves també que un informe correcte anterior **no** es substitueix.
5. Executa les proves des de l'arrel del projecte: `python3 -m unittest discover -s tests -t . -v`.
   - Si un cas falla per un defecte en un fitxer prohibit per a tu (`src/negoci.py`, `mcp_server.py`...), **no el modifiquis**: documenta el defecte (entrada, sortida observada, esperada) i informa el coordinador per a la correcció.
6. **Abans de desar l'informe definitiu**, crida l'eina `validar_informe` amb la ruta de l'informe generat (p. ex. `artefactes/informe.json`). Només si retorna `valid: true` es pot procedir al desament per MCP amb `desar_informe`.

## Contracte de validació de l'informe
- Presència dels camps obligatoris: `reposicions` (llista), `unitats_totals` (enter no negatiu), `descartats` (llista de cadenes).
- Les quantitats de reposició són enters positives (booleans no vàlids).
- No es repeteixen identificadors a les reposicions (i `descartats` sense repeticions).
- El total d'unitats coincideix amb la suma de les quantitats.

## Eines
- `read` per a l'enunciat i els codis; `write`/`edit` per a `tests/`, `src/validar_cli.py` i `.opencode/plugins/validar-informe/`.
- `shell` per a `python3 -m unittest discover -s tests -t . -v` i `python3 src/validar_cli.py <ruta>`.
- Eines d'OpenCode: `validar_informe` (la que has creat) i, si són visibles al catàleg, `magatzem_desar_informe` / `magatzem_llegir_inventari` per a la demostració final.

## Resultat esperat
- Proves que passen (`Ran N tests ... OK`) cobrint els sis grups.
- Eina `validar_informe` registrada i **invocada** sobre l'informe definitiu amb `valid: true` abans del desament per MCP.
- Evidències a `evidencies/verificador_*.json` (o `evidencies/proves_*.json`): resultat de les proves i sortida de la validació.
- Resum final al coordinador: fitxers, ordres, nombre de proves i fallades, errors pendents (codi + missatge).
