import json
import pandas as pd
df = pd.read_csv("reserves_brut.csv")
df["import"] = pd.to_numeric(df["import"].str.replace(",", "."),

errors="coerce")

iso = pd.to_datetime(df["creada_el"], format="%Y-%m-%d", errors="coerce")
creada = iso.fillna(pd.to_datetime(df["creada_el"], format="%d/%m/%Y", errors="coerce"))

recollida = pd.to_datetime(df["recollida_el"], format="%Y-%m-%d")
with open("domini_variables.json", encoding="utf-8") as f:domini = json.load(f)

REGLES = {} # nom -> màscara de files sospitoses

for col, regla in domini.items():
    s = df[col]
    # Els absents no són valors incorrectes (són el pecat 2): excloeu-los
    if "valors" in regla:
        REGLES[f"{col}_fora_valors"] = s.notna() & ~s.isin(regla["valors"])
        REGLES[f"{col}_fora_rang"] = s.notna() & ~s.between(*regla["rang"])
    
    if "patro" in regla:
        REGLES[f"{col}_fora_patro"] = s.notna() & ~s.astype(str).str.fullmatch(regla["patro"])


# imp = pd.to_numeric(df["import"].str.replace(",", "."))

REGLES["cancelada_amb_import"] = (df["estat"] == "cancel·lada") & (df["import"] > 0)
REGLES["recollida_abans_creada"] = recollida < creada
REGLES["creada_futura"] = pd.Timestamp("2026-10-01")< creada

for nom, mascara in REGLES.items():
    print(f"{nom:26s} {mascara.sum():5d} {100 * mascara.mean():6.2f} %") # files i percentatge