#!/usr/bin/env bash
# Inicia FinanZ dentro de GitHub Codespaces: espera a MySQL y levanta el servidor en el puerto 8080.
set -e
cd "$(dirname "$0")/../Sistema"

echo "⏳ Esperando a que MySQL esté listo..."
for i in $(seq 1 60); do
  if (exec 3<>/dev/tcp/db/3306) 2>/dev/null; then
    break
  fi
  sleep 2
done
sleep 3

echo "🚀 Iniciando FinanZ en http://localhost:8080 (cuenta demo: juan@correo.com / Finanz2026)"
echo "   Para detenerlo presiona Ctrl+C. Para volver a iniciarlo: bash .devcontainer/iniciar.sh"
mvn -q jetty:run
