
"""Genera domini_variables.json: un esborrany inferit de les dades més
les correccions humanes definides segons la norma de negoci.
"""
import json
import re

import pandas as pd

MAX_CATEGORIES = 20

# Domini de negoci definit explícitament.
CORRECCIONS_HUMANES = {
    "id": {
        "patro": r"^R\d{4}$",
        "unic": True,
    },
    "client_id": {
        "patro": r"^C\d{3}$",
    },
    "zona": {
        "valors": ["PALMA", "CALVIA", "LLEVANT", "NORD"],
    },
    "km": {
        "rang": [1, 400],
    },
    "import": {
        "rang": [0, 1500],
        "regla": "Si estat és cancel·lada, import ha de ser 0",
    },
    "estat": {
        "valors": ["confirmada", "cancel·lada", "pendent"],
    },
    "canal": {
        "valors": ["WEB", "TELEFON", "APP"],
    },
    "creada_el": {
        "patro": r"^\d{4}-\d{2}-\d{2}$",
        "data_maxima": "2026-10-01",
    },
    "recollida_el": {
        "patro": r"^\d{4}-\d{2}-\d{2}$",
        "regla": "Data igual o posterior a creada_el",
    },
    "passatgers": {
        "rang": [1, 8],
        "tipus": "enter",
    },
    "codi_postal": {
        "patro": r"^07\d{3}$",
    },
    "email": {
        "patro": r"^[^@\s]+@[^@\s]+$",
    },
    "pagat": {
        "valors": ["si", "no"],
    },
}


def infereix_domini(df):
    """Proposa un domini per columna a partir dels valors observats."""
    domini = {}

    for col in df.columns:
        s = df[col].dropna()

        if s.empty:
            domini[col] = {"tipus": "desconegut"}
        elif pd.api.types.is_numeric_dtype(s):
            domini[col] = {
                "rang": [float(s.min()), float(s.max())]
            }
        elif s.nunique() <= MAX_CATEGORIES:
            domini[col] = {
                "valors": sorted(s.unique().tolist())
            }
        else:
            domini[col] = {
                "tipus": "text",
                "exemple": str(s.iloc[0]),
            }

    return domini


def aplica_correccions(domini, correccions):
    """Sobreescriu les regles inferides amb les de negoci."""
    for col, regla in correccions.items():
        domini.setdefault(col, {}).update(regla)

    return domini


def valida_reserves(df):
    """Valida les dades contra les regles de negoci."""
    errors = {}

    def afegeix_error(col, mascara, missatge):
        files = df.index[mascara.fillna(False)].tolist()
        if files:
            errors.setdefault(col, []).append({
                "regla": missatge,
                "files": files,
            })

    # Patrons de text: han de coincidir amb tota la cadena.
    patrons = {
        "id": r"R\d{4}",
        "client_id": r"C\d{3}",
        "codi_postal": r"07\d{3}",
        "email": r"[^@\s]+@[^@\s]+",
    }

    for col, patro in patrons.items():
        if col in df.columns:
            valids = df[col].astype("string").str.fullmatch(
                patro, na=False
            )
            afegeix_error(
                col,
                df[col].notna() & ~valids,
                f"Ha de complir el patró {patro}",
            )

    # ID únic per reserva.
    if "id" in df.columns:
        duplicats = df["id"].notna() & df["id"].duplicated(
            keep=False
        )
        afegeix_error("id", duplicats, "Ha de ser únic")

    # Valors categòrics.
    categories = {
        "zona": ["PALMA", "CALVIA", "LLEVANT", "NORD"],
        "estat": ["confirmada", "cancel·lada", "pendent"],
        "canal": ["WEB", "TELEFON", "APP"],
        "pagat": ["si", "no"],
    }

    for col, valors in categories.items():
        if col in df.columns:
            mascara = df[col].notna() & ~df[col].isin(valors)
            afegeix_error(
                col, mascara, f"Valors permesos: {valors}"
            )

    # Rang de quilòmetres i import.
    for col, minim, maxim in [
        ("km", 1, 400),
        ("import", 0, 1500),
        ("passatgers", 1, 8),
    ]:
        if col in df.columns:
            numerics = pd.to_numeric(df[col], errors="coerce")
            mascara = (
                df[col].notna()
                & (
                    numerics.isna()
                    | (numerics < minim)
                    | (numerics > maxim)
                )
            )
            afegeix_error(
                col, mascara, f"Ha d'estar entre {minim} i {maxim}"
            )

    # Els passatgers han de ser un nombre enter.
    if "passatgers" in df.columns:
        numerics = pd.to_numeric(
            df["passatgers"], errors="coerce"
        )
        mascara = (
            df["passatgers"].notna()
            & numerics.notna()
            & (numerics % 1 != 0)
        )
        afegeix_error(
            "passatgers", mascara, "Ha de ser un nombre enter"
        )

    # Dates vàlides en format AAAA-MM-DD.
    dates = {}
    for col in ["creada_el", "recollida_el"]:
        if col in df.columns:
            text_dates = df[col].astype("string")
            format_ok = text_dates.str.fullmatch(
                r"\d{4}-\d{2}-\d{2}", na=False
            )
            parsed = pd.to_datetime(
                df[col].where(format_ok),
                format="%Y-%m-%d",
                errors="coerce",
            )
            dates[col] = parsed

            mascara = df[col].notna() & (
                ~format_ok | parsed.isna()
            )
            afegeix_error(
                col,
                mascara,
                "Ha de ser una data vàlida en format AAAA-MM-DD",
            )

    if "creada_el" in dates:
        maxima = pd.Timestamp("2026-10-01")
        mascara = dates["creada_el"].notna() & (
            dates["creada_el"] > maxima
        )
        afegeix_error(
            "creada_el",
            mascara,
            "No pot ser posterior a 2026-10-01",
        )

    if "creada_el" in dates and "recollida_el" in dates:
        mascara = (
            dates["creada_el"].notna()
            & dates["recollida_el"].notna()
            & (
                dates["recollida_el"]
                < dates["creada_el"]
            )
        )
        afegeix_error(
            "recollida_el",
            mascara,
            "No pot ser anterior a creada_el",
        )

    # Una reserva cancel·lada no es cobra.
    if {"estat", "import"}.issubset(df.columns):
        imports = pd.to_numeric(df["import"], errors="coerce")
        mascara = (
            df["estat"].eq("cancel·lada")
            & imports.notna()
            & imports.ne(0)
        )
        afegeix_error(
            "import",
            mascara,
            "Una reserva cancel·lada ha de tenir import 0",
        )

    return errors


if __name__ == "__main__":
    df = pd.read_csv("reserves_brut.csv")

    domini = aplica_correccions(
        infereix_domini(df),
        CORRECCIONS_HUMANES,
    )

    with open(
        "domini_variables.json", "w", encoding="utf-8"
    ) as f:
        json.dump(domini, f, ensure_ascii=False, indent=2)

    errors = valida_reserves(df)

    print(
        f"Generat: domini_variables.json "
        f"({len(domini)} columnes)"
    )

    if errors:
        print("\nS'han detectat errors de validació:")
        print(json.dumps(errors, ensure_ascii=False, indent=2))
    else:
        print("Totes les dades compleixen la norma de negoci.")
