#!/usr/bin/env bash

set -euo pipefail

# Ruta absoluta de infra-podman/, sin importar desde dónde se ejecute el script.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

NETWORK_NAME="reservation-net"
COMPOSE_PROJECT_NAME="reservation-infrastructure"
KAFKA_CONTAINER="kafka-broker-1"
KAFKA_BOOTSTRAP_SERVER="localhost:19092"
KAFKA_TOPIC="reservation-created-topic"

echo ""
echo "############################################################"
echo "Creando red Podman: ${NETWORK_NAME}"
echo "############################################################"
podman network exists "${NETWORK_NAME}" || podman network create "${NETWORK_NAME}"

echo ""
echo "############################################################"
echo "Descargando imágenes requeridas"
echo "############################################################"
# --tls-verify=false solo para testing.
podman pull --tls-verify=false docker.io/bitnami/postgresql:latest
podman pull --tls-verify=false docker.io/confluentinc/cp-zookeeper:7.7.1
podman pull --tls-verify=false docker.io/confluentinc/cp-kafka:7.7.1
podman pull --tls-verify=false docker.io/confluentinc/cp-schema-registry:7.7.1
podman pull --tls-verify=false docker.io/confluentinc/cp-enterprise-control-center:7.7.1

echo ""
echo "############################################################"
echo "Levantando infraestructura Kafka + PostgreSQL"
echo "############################################################"
podman compose \
  -p "${COMPOSE_PROJECT_NAME}" \
  -f "${INFRA_DIR}/compose/01-kafka-server.yml" \
  -f "${INFRA_DIR}/compose/02-postgres.yml" \
  up -d

echo ""
echo "############################################################"
echo "Esperando disponibilidad de Kafka"
echo "############################################################"
MAX_ATTEMPTS=30
ATTEMPT=1

until podman exec "${KAFKA_CONTAINER}" \
  kafka-topics \
  --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" \
  --list >/dev/null 2>&1; do

  if [ "${ATTEMPT}" -ge "${MAX_ATTEMPTS}" ]; then
    echo "Error: Kafka no estuvo disponible después de ${MAX_ATTEMPTS} intentos."
    exit 1
  fi

  echo "Kafka aún no está listo. Intento ${ATTEMPT}/${MAX_ATTEMPTS}..."
  sleep 2
  ATTEMPT=$((ATTEMPT + 1))
done
echo "Kafka está disponible."

echo ""
echo "############################################################"
echo "Creando tópico Kafka: ${KAFKA_TOPIC}"
echo "############################################################"
podman exec "${KAFKA_CONTAINER}" \
  kafka-topics \
  --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" \
  --if-not-exists \
  --create \
  --topic "${KAFKA_TOPIC}" \
  --partitions 1 \
  --replication-factor 1

echo ""
echo "############################################################"
echo "Tópicos Kafka disponibles"
echo "############################################################"
podman exec "${KAFKA_CONTAINER}" \
  kafka-topics \
  --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" \
  --list

echo ""
echo "############################################################"
echo "Infraestructura iniciada correctamente"
echo "############################################################"

echo ""
echo "############################################################"
echo "Registrando esquema Avro de reservas"
echo "############################################################"

"${SCRIPT_DIR}/register-reservation-event-schema.sh"
