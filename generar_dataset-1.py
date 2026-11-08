"""Genera el conjunt de dades sintètic d'aquesta pràctica (reserves + clients)."""
import numpy as np
import pandas as pd

rng = np.random.default_rng(2026)

# --- Taula clients ---
N_CLIENTS = 180
client_ids = [f"C{n:03d}" for n in range(1, N_CLIENTS + 1)]
antiguitat = rng.integers(0, 16, size=N_CLIENTS)

clients = pd.DataFrame({
    "client_id": client_ids,
    "antiguitat_anys": antiguitat,
})
clients.to_csv("clients.csv", index=False)

# --- Taula reserves ---
N_RESERVES = 800

zona_variants = {
    "PALMA": ["PALMA", "Palma", "palma "],
    "CALVIA": ["CALVIA", "Calvia", "calvia"],
    "LLEVANT": ["LLEVANT", "Llevant", "llevant "],
    "NORD": ["NORD", "Nord", "nord"],
}
zones_reals = list(zona_variants.keys())
pesos_zona = [0.4, 0.25, 0.2, 0.15]

zona_triada = rng.choice(zones_reals, size=N_RESERVES, p=pesos_zona)
zona = [rng.choice(zona_variants[z], p=[0.7, 0.2, 0.1]) for z in zona_triada]

# Els clients més nous reserven amb més freqüència que els de llarga
# antiguitat (efecte volgut, per fer visible la regla del gra més endavant)
pes_client = 1 / (antiguitat + 1)
pes_client = pes_client / pes_client.sum()
client_id_res = rng.choice(client_ids, size=N_RESERVES, p=pes_client)

km = rng.gamma(shape=3.0, scale=8.0, size=N_RESERVES).round(1)
import_num = (km * rng.uniform(2.8, 3.6, size=N_RESERVES)
              + rng.normal(0, 5, size=N_RESERVES)).clip(5).round(2)

estat = rng.choice(
    ["confirmada", "cancel·lada", "pendent"],
    size=N_RESERVES, p=[0.7, 0.2, 0.1],
)
passatgers = rng.choice([1, 2, 3, 4], size=N_RESERVES, p=[0.35, 0.35, 0.2, 0.1])

dates = pd.to_datetime("2026-01-01") + pd.to_timedelta(
    rng.integers(0, 365, size=N_RESERVES), unit="D"
)

codis_valids = ["07001", "07010", "07100", "07150", "07200",
                 "07300", "07400", "07500", "07600"]
codi_postal = rng.choice(codis_valids, size=N_RESERVES)

email = [f"client{cid[1:]}@correu.cat" for cid in client_id_res]

reserves = pd.DataFrame({
    "id": [f"R{n:04d}" for n in range(1, N_RESERVES + 1)],
    "client_id": client_id_res,
    "zona": zona,
    "km": km,
    "import": [f"{v:.2f}".replace(".", ",") for v in import_num],
    "estat": estat,
    "creada_el": dates.strftime("%Y-%m-%d"),
    "passatgers": passatgers,
    "codi_postal": codi_postal,
    "email": email,
})

# --- Defectes deliberats ---
# Sentinella a km (un "no ho sé" disfressat de número)
idx_sentinella = rng.choice(N_RESERVES, size=12, replace=False)
reserves.loc[idx_sentinella, "km"] = 9999

# Codis postals malformats (lletra afegida o zero inicial perdut)
idx_malformats = rng.choice(N_RESERVES, size=25, replace=False)
for i in idx_malformats:
    codi = str(reserves.loc[i, "codi_postal"])
    if rng.random() < 0.5:
        reserves.loc[i, "codi_postal"] = codi + "A"
    else:
        reserves.loc[i, "codi_postal"] = codi[1:]


def buidar(columna, proporcio):
    """Substitueix una proporció de valors d'una columna per absents (NaN)."""
    idx = rng.choice(N_RESERVES, size=int(N_RESERVES * proporcio), replace=False)
    reserves.loc[idx, columna] = np.nan


buidar("km", 0.02)
buidar("import", 0.03)
buidar("passatgers", 0.015)
buidar("codi_postal", 0.04)

reserves.to_csv("reserves_brut.csv", index=False)

print(f"Generats: reserves_brut.csv ({len(reserves)} files), "
      f"clients.csv ({len(clients)} files)")
