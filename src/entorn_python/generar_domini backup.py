"""Genera domini_variables.json: un esborrany inferit de les dades més correccions humanes."""
import json

import pandas as pd

MAX_CATEGORIES = 20  # amb aquests valors únics o menys, la tractem com a categòrica

# Domini de negoci que el codi no pot endevinar. Cada entrada substitueix,
# per a aquella columna, les claus del mateix nom de l'esborrany inferit.
# Claus possibles: "valors" (llista), "rang" ([mínim, màxim]) i "patro"
# (expressió regular que ha de complir tot el text).
CORRECCIONS_HUMANES = {
    "id": {"patro": r"R\d{4}"},
    # Afegiu aquí la resta de columnes...
}


def infereix_domini(df):
    """Proposa un domini per columna a partir dels valors observats."""
    domini = {}
    for col in df.columns:
        s = df[col].dropna()
        if pd.api.types.is_numeric_dtype(s):
            domini[col] = {"rang": [float(s.min()), float(s.max())]}
        elif s.nunique() <= MAX_CATEGORIES:
            domini[col] = {"valors": sorted(s.unique().tolist())}
        else:
            domini[col] = {"tipus": "text", "exemple": str(s.iloc[0])}
    return domini


def aplica_correccions(domini, correccions):
    """Sobreescriu l'esborrany amb el que sap la persona experta."""
    for col, regla in correccions.items():
        domini.setdefault(col, {}).update(regla)
    return domini


if __name__ == "__main__":
    df = pd.read_csv("reserves_brut.csv")
    domini = aplica_correccions(infereix_domini(df), CORRECCIONS_HUMANES)
    with open("domini_variables.json", "w", encoding="utf-8") as f:
        json.dump(domini, f, ensure_ascii=False, indent=2)
    print(f"Generat: domini_variables.json ({len(domini)} columnes)")
