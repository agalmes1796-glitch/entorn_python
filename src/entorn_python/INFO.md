# Els quatre pecats de les dades

1. **Pecat 1:** valors incorrectes.
2. **Pecat 2:** absència de valors.
3. **Pecat 3:** mala codificació.
4. **Pecat 4:** valors atípics.

## Pecat 1: valors incorrectes

- **El tipus** diu quina mena de valor és: `km` és un número decimal (`float64`).

- **El domini** diu quins valors tenen sentit per al negoci: un trasllat a Mallorca té més de 0 i menys d'uns 200 km.

- **Fora de domini:** el valor és impossible per si sol.
  - Zona EIVISSA (no és una de les 4).
  - `passatgers = 0`.
  - Una data futura en un històric.

- **Inconsistència entre camps:** cada valor és plausible, però la combinació no.
  - R003 està cancel·lada i té un import de 78,50 € cobrat.

- **Duplicats:**
  - **Exactes:** la mateixa fila dues vegades (error en carregar les dades).
  - **Lògics:** ID diferent, però mateix client, trajecte i moment (un doble clic al formulari). Exemples: R001 i R011.



## Pecat 2: absència de valors

1. **NaN honestos:** Pandas els reconeix com a absents (`isna()`) i els exclou dels càlculs.  
   - Exemples: R003 i R007 a `email`.

2. **Pseudo-buits:** text que vol dir «no hi ha res»: `""`, `" "`, `"N/A"`, `"-"`, `"NULL"`. Per a Pandas, són text normal.  
   - Exemple: R005 a `email`.

3. **Sentinelles:** números legals per al tipus, però impossibles per al domini: `-1`, `0`, `9999`.  
   - Exemple: R005 a `km`.

| Patró | Què vol dir | Exemple de trasllats |
|---|---|---|
| MCAR | L’absència no depèn de res: és pur atzar. | La tauleta del xofer perd la connexió de tant en tant i no registra els km. |
| MAR | Depèn d’una altra variable que sí que tenim. | Les reserves per telèfon no solen portar email: la falta depèn del canal. |
| MNAR | Depèn del mateix valor que falta. | Els clients amb imports alts prefereixen no deixar el telèfon. |

### Com es diagnostica el patró

- Es compara el **% d’absents** entre segments (canal, zona, mes...).
- Si el % és semblant a tots els segments, pot ser **MCAR**.
- Si un segment en té molts més, és un indici de **MAR**: la falta depèn d’aquella variable.
- **MNAR** no es pot demostrar només amb les dades (el valor que ens falta no el veiem): cal coneixement del negoci.



## Pecat 3: mala codificació

### La dada és bona, el vestit no

| Símptoma | Causa típica | Com es detecta |
|---|---|---|
| Número llegit com a text | Coma decimal, punt de milers, símbol € | La columna és `str` quan hauria de ser numèrica |
| Dates en formats barrejats | `2026-03-05`, `05/03/2026`, `5-mar-26` | % de dates que no encaixen amb el format esperat |
| Categories inconsistents | Majúscules, espais, accents (`Calvià`, `CALVIA `) | Cardinalitat més gran que el domini |
| Caràcters estranys (`cancelÂ·lada`) | Text UTF-8 llegit com a Latin-1 | Seqüències `Ã` o `Â` sospitoses |
| Booleans creatius | `si`, `Sí`, `1`, `true`, `x` | Inventari de valors únics |