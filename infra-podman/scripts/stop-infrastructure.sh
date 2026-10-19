#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

NETWORK_NAME="reservation-net"
COMPOSE_PROJECT_NAME="reservation-infrastructure"

echo ""
echo "############################################################"
echo "Deteniendo infraestructura de reservas"
echo "############################################################"

podman compose \
  -p "${COMPOSE_PROJECT_NAME}" \
  -f "${INFRA_DIR}/compose/01-kafka-server.yml" \
  -f "${INFRA_DIR}/compose/02-postgres.yml" \
  down

echo ""
echo "############################################################"
echo "Eliminando red Podman: ${NETWORK_NAME}"
echo "############################################################"

if podman network exists "${NETWORK_NAME}"; then
  if podman network rm "${NETWORK_NAME}"; then
    echo "Red ${NETWORK_NAME} eliminada."
  else
    echo "Advertencia: no se pudo eliminar ${NETWORK_NAME}."
    echo "Verifica que no existan contenedores de api-producer o api-consumer conectados a la red."
  fi
else
  echo "La red ${NETWORK_NAME} no existe."
fi

echo ""
echo "############################################################"
echo "Infraestructura detenida correctamente"
echo "Los volúmenes de PostgreSQL se conservaron."
echo "############################################################"
