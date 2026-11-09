---
name: calcular-reposicio
description: Implementar les regles de reposició del magatzem a src/negoci.py (generar_informe i validar_informe).
---

## Quan s'ha d'utilitzar
Per al subagent **desenvolupador**, quan cal implementar o corregir les regles de reposició del magatzem o la validació de l'informe en memòria.

## Entrades
- La tasca, les dades i el contracte de sortida lliurats pel coordinador.
- `enunciat.pdf` (secció «Dades d'entrada i regles»): regles, exemple d'entrada i resultat esperat.
- `flux.py`: mostra com es consumeixen `generar_informe` i `validar_informe` (`from src.negoci import ...`).
- `dades/inventari.json` (exemple d'entrada) i `dades/esperat.json` (resultat esperat de l'exemple).

## Procediment
1. Llegeix `enunciat.pdf`, `flux.py` i `dades/esperat.json`.
2. Crea `src/__init__.py` (buit) si no existeix, perquè `src` sigui un paquet importable.
3. Implementa `generar_informe(inventari)` a `src/negoci.py`:
   - Valida cada producte (veure «Regles de negoci»); no mutis la llista d'entrada.
   - Producte invàlid → el seu `id` a `descartats`, conservant l'ordre d'entrada.
   - Producte vàlid amb `estoc < minim` → reposició de `objectiu - estoc`.
   - `reposicions` ordenades per `id` ascendent; `unitats_totals` = suma de les quantitats.
   - Retorna `{"reposicions": [...], "unitats_totals": N, "descartats": [...]}`.
4. Implementa `validar_informe(informe)` al mateix fitxer, amb el «Contracte de validació» d'aquesta skill; retorna `{"valid": bool, "errors": [str]}` acumulant **tots** els errors trobats (no només el primer).
5. Prova la lògica de forma aïllada des de l'arrel del projecte (`python3 -c "..."` o un script de prova temporal) amb:
   - l'exemple de l'enunciat (ha de coincidir exactament amb `dades/esperat.json`),
   - inventari buit, estoc igual al mínim, dades invàlides i ordenació.
   - Si el servidor MCP encara no està implementat (error `PENDENT_IMPLEMENTACIO`), **no** executis `flux.py` sencer: prova la lògica sense el pas MCP i informa-ho al coordinador.

## Regles de negoci (contracte de l'enunciat)
- `id`: cadena de text no buida.
- `estoc`, `minim`, `objectiu`: enters no negatius. Els booleans no són vàlids (comprova `type(x) is bool` abans de la comprovació d'enter).
- `objectiu >= minim`.
- Si `estoc < minim` → compra de `objectiu - estoc`. Si `estoc >= minim` → sense compra.
- `reposicions` per `id` ascendent; `descartats` amb l'ordre d'entrada; no es modifiquen les dades originals.

## Contracte de validació de l'informe (idèntic a l'eina personalitzada `validar_informe`)
- Camps obligatoris: `reposicions` (llista), `unitats_totals` (enter no negatiu), `descartats` (llista de cadenes).
- Cada reposició: `id` cadena no buida; `quantitat` enter positiu (booleans no vàlids).
- Cap `id` repetit a `reposicions`; `descartats` sense repeticions.
- `unitats_totals` == suma de les quantitats.

## Eines
- `read` per a l'enunciat i l'esquelet; `write`/`edit` per a `src/negoci.py` i `src/__init__.py`.
- `shell` per a `python3 -c "..."` o petits scripts de prova de la lògica, des de l'arrel del projecte.

## Resultat esperat
- `src/negoci.py` amb `generar_informe` i `validar_informe`; `src/__init__.py` present.
- Evidència a `evidencies/desenvolupador_*.json`: sortida de `generar_informe` amb l'exemple de l'enunciat i la seva igualtat amb `dades/esperat.json`.
- Resum final al coordinador: fitxers, ordres, resultats observats i errors pendents.
