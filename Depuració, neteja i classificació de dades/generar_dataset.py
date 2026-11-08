"""Genera el conjunt de dades sintètic d'aquesta pràctica (reserves amb els quatre pecats)."""
import numpy as np
import pandas as pd

rng = np.random.default_rng(2026)

N_RESERVES = 800
N_CLIENTS = 180
EXTRACCIO = pd.Timestamp("2026-10-01")  # data d'extracció de l'històric

# --- Base: la mateixa lògica que la pràctica de la UT1 ---
zona_variants = {
    "PALMA": ["PALMA", "Palma", "palma "],
    "CALVIA": ["CALVIA", "Calvia", "Calvià"],
    "LLEVANT": ["LLEVANT", "Llevant", "llevant "],
    "NORD": ["NORD", "Nord", "nord"],
}
zona_real = rng.choice(list(zona_variants), size=N_RESERVES,
                       p=[0.4, 0.25, 0.2, 0.15])
zona = [rng.choice(zona_variants[z], p=[0.7, 0.2, 0.1]) for z in zona_real]

client_id = rng.choice([f"C{n:03d}" for n in range(1, N_CLIENTS + 1)],
                       size=N_RESERVES)
km = rng.gamma(shape=3.0, scale=8.0, size=N_RESERVES).clip(1).round(1)
import_num = (km * rng.uniform(2.8, 3.6, size=N_RESERVES)
              + rng.normal(0, 5, size=N_RESERVES)).clip(5).round(2)
estat = rng.choice(["confirmada", "cancel·lada", "pendent"],
                   size=N_RESERVES, p=[0.7, 0.2, 0.1]).astype(object)
import_num[estat == "cancel·lada"] = 0.0  # una cancel·lació no es cobra
canal = rng.choice(["WEB", "TELEFON", "APP"], size=N_RESERVES,
                   p=[0.55, 0.30, 0.15])
passatgers = rng.choice([1, 2, 3, 4], size=N_RESERVES,
                        p=[0.35, 0.35, 0.2, 0.1]).astype(float)

creada = pd.Timestamp("2026-01-01") + pd.to_timedelta(
    rng.integers(0, 273, size=N_RESERVES), unit="D")
recollida = creada + pd.to_timedelta(
    rng.integers(0, 31, size=N_RESERVES), unit="D")

codis_valids = ["07001", "07010", "07100", "07150", "07200",
                "07300", "07400", "07500", "07600"]
codi_postal = rng.choice(codis_valids, size=N_RESERVES).astype(object)

# Booleà «pagat» escrit de maneres diferents segons qui l'introdueix
pagat_si = ["si", "Sí", "1", "true", "x"]
pagat_no = ["no", "No", "0", "false"]
pagat = [rng.choice(pagat_si) if e == "confirmada" else rng.choice(pagat_no)
         for e in estat]

email = np.array([f"client{c[1:]}@correu.cat" for c in client_id],
                 dtype=object)

# --- Defectes deliberats, en files disjuntes perquè els recomptes siguin nets ---
lliures = set(range(N_RESERVES))


def tria(n, candidates=None):
    """Tria n files encara no usades per cap altre defecte."""
    pool = sorted(lliures if candidates is None
                  else lliures & set(np.flatnonzero(candidates)))
    idx = rng.choice(pool, size=n, replace=False)
    lliures.difference_update(idx)
    return idx


# Pecat 1: valors incorrectes
idx = tria(6, estat == "cancel·lada")  # cancel·lades amb import cobrat
import_num[idx] = (km[idx] * 3.2).round(2)
idx = tria(5)  # recollida anterior a la creació
recollida = recollida.to_numpy().copy()
recollida[idx] = (creada[idx] - pd.to_timedelta(
    rng.integers(1, 6, size=5), unit="D")).to_numpy()
idx = tria(3)  # km negatius
km[idx] = -km[idx]
idx = tria(3)  # més passatgers que places
passatgers[idx] = [12, 15, 20]
creada = creada.to_numpy().copy()
idx = tria(2)  # dates posteriors a l'extracció
creada[idx] = np.array(["2026-11-15", "2027-01-10"], dtype="datetime64[ns]")
recollida[idx] = creada[idx]

# Pecat 2: absència de valors
km[tria(12)] = 9999  # sentinella
km[tria(16)] = np.nan  # absència a l'atzar (MCAR)
import_num[tria(24)] = np.nan
passatgers[tria(12)] = np.nan
codi_postal[tria(32)] = np.nan
passatgers[tria(8, canal == "TELEFON")] = 0  # sentinella del formulari
passatgers[tria(2, canal != "TELEFON")] = 0
for i in sorted(lliures):  # email absent sobretot per telèfon (MAR)
    if rng.random() < (0.25 if canal[i] == "TELEFON" else 0.02):
        email[i] = np.nan
lliures -= set(np.flatnonzero(pd.isna(email)))
email[tria(10)] = rng.choice(["-", " ", "desconegut", "no en té"], size=10)

# Pecat 3: mala codificació
idx = tria(15, estat == "cancel·lada")  # UTF-8 llegit com a Latin-1
estat[idx] = "cancel·lada".encode("utf-8").decode("latin-1")
for i in tria(25):  # codis postals amb lletra afegida o zero perdut
    codi_postal[i] = (codi_postal[i] + "A" if rng.random() < 0.5
                      else codi_postal[i][1:])
data_europea = tria(56)  # dates en format dd/mm/aaaa

# Pecat 4: atípics legítims i errors d'unitats
idx = tria(6, estat == "confirmada")  # trasllats llargs reals
km[idx] = rng.uniform(120, 190, size=6).round(1)
import_num[idx] = (km[idx] * 3.2).round(2)
idx = tria(4, (estat == "confirmada") & ~np.isnan(import_num))
import_num[idx] = (import_num[idx] * 100).round(2)  # imports en cèntims

# --- Muntatge ---
creada_txt = pd.Series(pd.to_datetime(creada).strftime("%Y-%m-%d"))
creada_txt[data_europea] = pd.to_datetime(
    creada[data_europea]).strftime("%d/%m/%Y")

reserves = pd.DataFrame({
    "id": [f"R{n:04d}" for n in range(1, N_RESERVES + 1)],
    "client_id": client_id,
    "zona": zona,
    "km": km,
    "import": [np.nan if np.isnan(v) else f"{v:.2f}".replace(".", ",")
               for v in import_num],
    "estat": estat,
    "canal": canal,
    "creada_el": creada_txt,
    "recollida_el": pd.to_datetime(recollida).strftime("%Y-%m-%d"),
    "passatgers": passatgers,
    "codi_postal": codi_postal,
    "email": email,
    "pagat": pagat,
})

# Pecat 1: duplicats lògics (doble clic: id nou) i exactes (càrrega repetida)
logics = reserves.loc[tria(8)].copy()
logics["id"] = [f"R{n:04d}" for n in range(N_RESERVES + 1,
                                            N_RESERVES + 9)]
exactes = reserves.loc[tria(5)].copy()
reserves = pd.concat([reserves, logics, exactes], ignore_index=True)
reserves = reserves.sample(frac=1, random_state=2026).reset_index(drop=True)

reserves.to_csv("reserves_brut.csv", index=False)
print(f"Generat: reserves_brut.csv ({len(reserves)} files)")
