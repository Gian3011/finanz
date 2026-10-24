#!/usr/bin/env bash
# Publica FinanZ en Internet con un túnel gratuito de Cloudflare (no requiere cuenta).
# A diferencia del puerto público de Codespaces, este túnel no mezcla las cookies
# de distintos visitantes, así que varias personas pueden usar el sistema a la vez.
#
# Uso (en la terminal del Codespace, con FinanZ ya corriendo):  bash .devcontainer/tunel.sh
# El enlace de Cloudflare cambia cada vez que se ejecuta, pero el script lo publica en
# docs/app/destino.txt: el enlace fijo https://gian3011.github.io/finanz/app/ siempre
# redirige al túnel vigente. Registro: /tmp/tunel.log
#
# Antes de apagar el Codespace:  bash .devcontainer/tunel.sh --apagar
# (cierra el túnel y deja el enlace fijo mostrando "El sistema está apagado").
set -e
cd "$(dirname "$0")/.."

# Publica el destino del enlace fijo (GitHub Pages lo actualiza en 1-2 minutos)
publicar_destino() {
  echo "$1" > docs/app/destino.txt
  if git diff --quiet -- docs/app/destino.txt; then
    return 0
  fi
  git add docs/app/destino.txt
  git commit -q -m "Enlace fijo: $2" -- docs/app/destino.txt
  git pull -q --rebase && git push -q \
    && echo "🔗 Enlace fijo actualizado (tarda 1-2 min): https://gian3011.github.io/finanz/app/" \
    || echo "⚠️  No se pudo subir el enlace fijo a GitHub; el enlace directo sí funciona."
}

if [ "$1" = "--apagar" ]; then
  pkill -f "cloudflared tunnel" 2>/dev/null || true
  publicar_destino "apagado" "sistema apagado"
  echo "⏹️  Túnel cerrado."
  exit 0
fi

if ! command -v cloudflared >/dev/null 2>&1; then
  echo "⬇️  Instalando cloudflared (herramienta oficial de Cloudflare)..."
  curl -fsSL -o /tmp/cloudflared.deb \
    https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
  sudo dpkg -i /tmp/cloudflared.deb >/dev/null
fi

if ! curl -s -o /dev/null http://localhost:8080/Login.jsp; then
  echo "⚠️  FinanZ no está corriendo. Inícialo primero con:"
  echo "    setsid nohup bash .devcontainer/iniciar.sh > /tmp/finanz.log 2>&1 < /dev/null &"
  exit 1
fi

# Cierra un túnel anterior y abre uno nuevo, desacoplado de la terminal
pkill -f "cloudflared tunnel" 2>/dev/null || true
setsid nohup cloudflared tunnel --no-autoupdate --url http://localhost:8080 > /tmp/tunel.log 2>&1 < /dev/null &

echo "⏳ Abriendo el túnel..."
URL=""
for i in $(seq 1 40); do
  URL=$(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' /tmp/tunel.log | head -1 || true)
  [ -n "$URL" ] && break
  sleep 1
done

if [ -z "$URL" ]; then
  echo "❌ No se pudo abrir el túnel. Revisa /tmp/tunel.log"
  exit 1
fi

echo "$URL" > /tmp/tunel.url
echo ""
echo "✅ FinanZ está publicado en:"
echo "   $URL/Login.jsp"
echo ""
publicar_destino "$URL" "túnel actualizado"
echo ""
echo "Comparte el enlace fijo: https://gian3011.github.io/finanz/app/ (cada persona tiene su propia sesión)."
