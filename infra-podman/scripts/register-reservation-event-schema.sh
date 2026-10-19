#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

SCHEMA_REGISTRY_URL="http://localhost:8081"
KAFKA_CONTAINER="kafka-broker-1"
KAFKA_BOOTSTRAP_SERVER="kafka-broker-1:9092"

TOPIC_NAME="reservation-created-topic"
SUBJECT_NAME="${TOPIC_NAME}-value"
SCHEMA_FILE="${INFRA_DIR}/schemas/ReservationCreatedEvent.avsc"

MAX_ATTEMPTS=30
ATTEMPT=1

echo ""
echo "############################################################"
echo "Validando archivo Avro"
echo "############################################################"

if [ ! -f "${SCHEMA_FILE}" ]; then
  echo "Error: no se encontró el archivo:"
  echo "${SCHEMA_FILE}"
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "Error: python3 es requerido para validar y preparar el esquema Avro."
  exit 1
fi

if ! python3 -m json.tool "${SCHEMA_FILE}" >/dev/null; then
  echo "Error: ReservationCreatedEvent.avsc no contiene JSON válido."
  exit 1
fi

echo "Esquema Avro encontrado y JSON válido."

echo ""
echo "############################################################"
echo "Verificando disponibilidad de Kafka"
echo "############################################################"

until podman exec "${KAFKA_CONTAINER}" \
  kafka-topics \
  --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" \
  --list >/dev/null 2>&1; do

  if [ "${ATTEMPT}" -ge "${MAX_ATTEMPTS}" ]; then
    echo "Error: Kafka no estuvo disponible después de ${MAX_ATTEMPTS} intentos."
    exit 1
  fi

  echo "Kafka aún no está disponible. Intento ${ATTEMPT}/${MAX_ATTEMPTS}..."
  sleep 2
  ATTEMPT=$((ATTEMPT + 1))
done

echo "Kafka está disponible."

echo ""
echo "############################################################"
echo "Verificando tópico Kafka: ${TOPIC_NAME}"
echo "############################################################"

if ! podman exec "${KAFKA_CONTAINER}" \
  kafka-topics \
  --bootstrap-server "${KAFKA_BOOTSTRAP_SERVER}" \
  --describe \
  --topic "${TOPIC_NAME}"; then

  echo "Error: no existe el tópico ${TOPIC_NAME}."
  echo "Ejecuta primero start-infrastructure.sh."
  exit 1
fi

echo ""
echo "############################################################"
echo "Verificando disponibilidad de Schema Registry"
echo "############################################################"

ATTEMPT=1

until curl --fail --silent --show-error \
  "${SCHEMA_REGISTRY_URL}/subjects" >/dev/null; do

  if [ "${ATTEMPT}" -ge "${MAX_ATTEMPTS}" ]; then
    echo "Error: Schema Registry no estuvo disponible después de ${MAX_ATTEMPTS} intentos."
    exit 1
  fi

  echo "Schema Registry aún no está disponible. Intento ${ATTEMPT}/${MAX_ATTEMPTS}..."
  sleep 2
  ATTEMPT=$((ATTEMPT + 1))
done

echo "Schema Registry está disponible."

echo ""
echo "############################################################"
echo "Registrando esquema Avro"
echo "############################################################"

PAYLOAD="$(
  python3 -c '
import json
import sys

schema_file = sys.argv[1]

with open(schema_file, encoding="utf-8") as file:
    schema = json.load(file)

print(json.dumps({"schema": json.dumps(schema)}))
' "${SCHEMA_FILE}"
)"

RESPONSE_FILE="$(mktemp)"

HTTP_STATUS="$(
  curl --silent --show-error \
    --output "${RESPONSE_FILE}" \
    --write-out "%{http_code}" \
    --request POST \
    "${SCHEMA_REGISTRY_URL}/subjects/${SUBJECT_NAME}/versions" \
    --header "Content-Type: application/vnd.schemaregistry.v1+json" \
    --data "${PAYLOAD}"
)"

if [[ "${HTTP_STATUS}" -lt 200 || "${HTTP_STATUS}" -ge 300 ]]; then
  echo "Error: Schema Registry respondió con HTTP ${HTTP_STATUS}."
  echo "Respuesta:"
  cat "${RESPONSE_FILE}"
  rm -f "${RESPONSE_FILE}"
  exit 1
fi

echo "Esquema registrado correctamente."
echo "Subject: ${SUBJECT_NAME}"
echo "Respuesta:"
cat "${RESPONSE_FILE}"

rm -f "${RESPONSE_FILE}"

echo ""
echo "############################################################"
echo "Validación de infraestructura completada correctamente"
echo "############################################################"
