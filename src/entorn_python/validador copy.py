
import json
import pandas as pd

# 1. Llegim les dades
df = pd.read_csv("reserves_brut.csv")

# 2. Convertim l'import a número
df["import"] = pd.to_numeric(
    df["import"].astype("string").str.replace(",", ".", regex=False),
    errors="coerce"
)

# 3. Convertim les dates
iso = pd.to_datetime(
    df["creada_el"], format="%Y-%m-%d", errors="coerce"
)
creada = iso.fillna(
    pd.to_datetime(
        df["creada_el"], format="%d/%m/%Y", errors="coerce"
    )
)

recollida = pd.to_datetime(
    df["recollida_el"], format="%Y-%m-%d", errors="coerce"
)

# 4. Llegim el domini de negoci
with open("domini_variables.json", encoding="utf-8") as f:
    domini = json.load(f)

# 5. Creem les regles de validació
REGLES = {}

for col, regla in domini.items():
    s = df[col]

    # Els valors absents no es compten com a errors
    presents = s.notna()

    if "valors" in regla:
        REGLES[f"{col}_fora_valors"] = (
            presents & ~s.isin(regla["valors"])
        )

    if "rang" in regla:
        minim, maxim = regla["rang"]
        nums = pd.to_numeric(
            s.astype("string").str.replace(",", ".", regex=False),
            errors="coerce"
        )

        REGLES[f"{col}_fora_rang"] = (
            presents
            & (
                nums.isna()
                | (nums < minim)
                | (nums > maxim)
            )
        )

    if "patro" in regla:
        compleix = s.astype("string").str.fullmatch(
            regla["patro"], na=False
        )

        REGLES[f"{col}_fora_patro"] = (
            presents & ~compleix
        )

# 6. Regles de consistència entre camps

# Reserva cancel·lada amb un import superior a zero
REGLES["cancelada_amb_import"] = (
    df["estat"].eq("cancel·lada")
    & df["import"].notna()
    & (df["import"] > 0)
)

# Data de recollida anterior a la data de creació
REGLES["recollida_abans_creada"] = (
    recollida.notna()
    & creada.notna()
    & (recollida < creada)
)

# Reserva creada després de la data d'extracció
data_extraccio = pd.Timestamp("2026-10-01")

REGLES["creada_futura"] = (
    creada.notna()
    & (creada > data_extraccio)
)

# 7. Mostrem el nombre de files sospitoses i el percentatge
total = len(df)

for nom, mascara in REGLES.items():
    files = int(mascara.sum())
    percentatge = files / total * 100 if total else 0

    print(f"{nom:26s} {files:5d} {percentatge:6.2f} %")
