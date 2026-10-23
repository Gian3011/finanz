#!/usr/bin/env bash
# Publica FinanZ en Internet con un túnel gratuito de Cloudflare (no requiere cuenta).
# A diferencia del puerto público de Codespaces, este túnel no mezcla las cookies
# de distintos visitantes, así que varias personas pueden usar el sistema a la vez.
#
# Uso (en la terminal del Codespace, con FinanZ ya corriendo):  bash .devcontainer/tunel.sh
# El enlace cambia cada vez que se ejecuta. Registro: /tmp/tunel.log
set -e

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
echo "Comparte ese enlace. Cada persona tiene su propia sesión."
