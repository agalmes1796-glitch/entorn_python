import requests

resposta = requests.get("https://api.github.com")

from rich.console import Console

console = Console()
console.print("Entorn Python gestionat amb uv", resposta.json()["current_user_url"], style="bold green")
