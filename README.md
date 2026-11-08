# entorn_python

Guía rápida para trabajar en este proyecto desde **casa (Windows)** y desde **clase (Ubuntu)**.

GitHub es la copia que manda: empieza siempre con `git pull` y termina siempre con `git push`.

---

## 1. Rutina de cada día

**Al empezar** (dentro de la carpeta del proyecto):

```bash
git pull      # bajar los últimos cambios de GitHub
uv sync       # instalar o actualizar las librerías en esta máquina
```

**Al terminar:**

```bash
git add .
git commit -m "Qué he hecho"
git push      # subir los cambios a GitHub
```

> Si no haces `push` al terminar, en el otro ordenador no tendrás los cambios.

---

## 2. Primera vez en un ordenador nuevo

1. Instalar `uv` (solo una vez por ordenador):
   - **Ubuntu:** `curl -LsSf https://astral.sh/uv/install.sh | sh`. Después cierra y vuelve a abrir la terminal.
   - **Windows (PowerShell):** `powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"`
2. Descargar el proyecto:
   ```bash
   git clone https://github.com/agalmes1796-glitch/entorn_python.git
   cd entorn_python
   uv sync
   ```
   `uv sync` crea la carpeta `.venv` e instala Python 3.14 si no lo tienes.
3. Abrir la carpeta en VS Code y elegir el Python del proyecto (mira el punto 4).

---

## 3. Instalar una librería nueva

```bash
uv add nombre_libreria
```

- **Nunca uses `pip install`**: no queda apuntado y en el otro ordenador no estará.
- Después, haz `commit` y `push` de `pyproject.toml` y `uv.lock`.
- En el otro ordenador, `git pull` + `uv sync` y ya la tendrás.

---

## 4. Si VS Code no reconoce los `import`

Casi siempre es porque VS Code no usa el Python del proyecto.

- **Notebook (`.ipynb`):** arriba a la derecha, **Select Kernel** → **Python Environments** → `.venv`
- **Fichero `.py`:** `Ctrl+Shift+P` → **Python: Select Interpreter** → `.venv`

Si aún falla, ejecuta `uv sync`. Si la librería no está en `pyproject.toml`, añádela con `uv add`.

> `from google.colab import ...` solo funciona dentro de Google Colab. En VS Code siempre dará error: lee los ficheros directamente, por ejemplo con `pd.read_csv("fichero.csv")`.

---

## 5. Windows y Ubuntu: lo que hay que vigilar

- **No hace falta un entorno especial para Ubuntu.** Cada ordenador tiene su propio `.venv`, que no se sube a GitHub, y `uv sync` lo deja igual en los dos.
- **Mayúsculas en los nombres de fichero:** Linux distingue `Clients.csv` de `clients.csv`, y Windows no. Escribe los nombres exactamente como son.
- **Rutas:** usa `/` o `pathlib`, nunca `\` ni rutas como `C:\Users\...`.
- **Los avisos de `LF will be replaced by CRLF`** son normales. Ignóralos.

---

## 6. Problemas habituales

| Mensaje / situación | Qué hacer |
|---|---|
| `git push` dice `rejected` o `fetch first` | Haz `git pull` y vuelve a hacer `git push`. |
| `git pull` dice `CONFLICT` | El mismo fichero ha cambiado en los dos sitios. Ábrelo en VS Code, elige qué versión te quedas (**Accept Current / Incoming / Both**), guarda y haz `git add .`, `git commit` y `git push`. |
| `git pull` dice que tienes cambios locales sin guardar | Haz primero `git add .` y `git commit -m "..."`, y después `git pull`. |
| `Permission denied` / `403` al hacer `push` | La cuenta de GitHub de ese ordenador no tiene acceso al repo. Mira el punto 7. |
| `uv: command not found` | Instala `uv` (punto 2) y vuelve a abrir la terminal. |

---

## 7. Dar acceso a otra cuenta de GitHub (repo a repo)

1. Con la cuenta propietaria, ve al repo en GitHub → **Settings** → **Collaborators** → **Add people**.
2. Escribe el nombre de la otra cuenta y envía la invitación.
3. Con la otra cuenta, acepta la invitación (llega por correo o en `https://github.com/<propietario>/<repo>/invitations`).

---

## 8. Ramas de este repo

- `main` → la versión buena, la que se usa siempre.
- `versio-local-antiga` → copia de seguridad de unos ficheros antiguos de la carpeta local. No hay que tocarla.
