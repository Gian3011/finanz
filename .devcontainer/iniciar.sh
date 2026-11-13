#!/usr/bin/env bash
# Inicia FinanZ dentro de GitHub Codespaces: espera a MySQL y levanta el servidor en el puerto 8080.
# Se ejecuta automáticamente en segundo plano al abrir el Codespace
# (postAttachCommand). Registro: /tmp/finanz.log
cd "$(dirname "$0")/../Sistema"

if curl -s -o /dev/null http://localhost:8080/Login.jsp; then
  echo "✅ FinanZ ya está corriendo en http://localhost:8080"
  exit 0
fi

# JasperReports necesita las librerías de fuentes del sistema (libfreetype y fontconfig)
# para medir el texto de los reportes; la imagen base del Codespace no las trae.
if ! ldconfig -p | grep -q libfreetype.so.6 || ! command -v fc-list >/dev/null; then
  echo "🔤 Instalando librerías de fuentes para los reportes (libfreetype6, fontconfig)..."
  sudo rm -f /etc/apt/sources.list.d/yarn.list
  sudo apt-get update -qq && sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq libfreetype6 fontconfig > /dev/null
fi

echo "⏳ Esperando a que MySQL esté listo..."
for i in $(seq 1 90); do
  if (exec 3<>/dev/tcp/db/3306) 2>/dev/null; then
    break
  fi
  sleep 2
done
sleep 3

echo "🚀 Iniciando FinanZ en http://localhost:8080 (cuenta demo: juan@correo.com / Finanz2026)"
exec mvn -q -DskipTests jetty:run
