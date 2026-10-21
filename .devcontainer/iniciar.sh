#!/usr/bin/env bash
# Inicia FinanZ dentro de GitHub Codespaces: espera a MySQL y levanta el servidor en el puerto 8080.
# Se ejecuta automáticamente en segundo plano al abrir el Codespace
# (postAttachCommand). Registro: /tmp/finanz.log
cd "$(dirname "$0")/../Sistema"

if curl -s -o /dev/null http://localhost:8080/Login.jsp; then
  echo "✅ FinanZ ya está corriendo en http://localhost:8080"
  exit 0
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
