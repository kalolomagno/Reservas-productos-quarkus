# Sistema de Reservas de Productos Seguras

## 1. Descripción del proyecto

Este proyecto implementa un sistema distribuido para gestionar **reservas de productos**. Su objetivo es permitir que un operador autorizado registre la reserva de un producto para un cliente, valide información externa del producto y publique un evento que será procesado de forma asíncrona.

El sistema está dividido en tres módulos principales:

```text
├── infra-podman/
├── api-producer/
└── api-consumer/
```

La solución aplica una arquitectura orientada a eventos: la API productora registra una reserva y publica un evento en Kafka; posteriormente, la API consumidora procesa el evento, registra una auditoría y actualiza un resumen de reservas por producto. Este enfoque desacopla los servicios y permite procesar información de manera asíncrona.

---

## 2. Problema de negocio

Una tienda necesita registrar reservas de productos solicitadas por clientes. El operador debe poder:

1. Autenticarse en el sistema.
2. Consultar o validar información de un producto externo.
3. Crear una reserva para un cliente.
4. Consultar, actualizar o cancelar una reserva.
5. Generar evidencia de que la reserva fue procesada.
6. Consultar un resumen de las reservas recibidas por producto.

La necesidad principal es que el registro de la reserva no dependa de que todos los procesos posteriores se completen en el mismo instante. Por ello, después de guardar la reserva, se publica un evento en Kafka para que otro servicio procese la auditoría y los indicadores de reservas.

---

## 3. Flujo completo del proceso de negocio

```text
[Operador]
    |
    | 1. Solicita token JWT local
    v
[API Producer - Auth]
    |
    | 2. Entrega JWT firmado con clave privada RSA
    v
[Operador autenticado]
    |
    | 3. Crea una reserva protegida
    v
[API Producer - Reservation Service]
    |
    | 4. Consulta DummyJSON para validar datos externos
    | 5. Guarda la reserva en PostgreSQL
    | 6. Publica ReservationCreatedEvent en Kafka
    v
[Kafka: reservation-created-topic]
    |
    | 7. Consume el evento
    v
[API Consumer - Audit Service]
    |
    | 8. Registra auditoría y actualiza resumen
    v
[PostgreSQL]
```

El productor utilizará la lógica de creación de un evento Avro, uso de una clave única para el mensaje, headers de Kafka y envío asíncrono al tópico configurado.

El consumidor se suscribirá al tópico, procesará los eventos recibidos, persistirá el resultado y realizará el `commit` del offset únicamente después del procesamiento exitoso.

---

## 4. Módulos del repositorio

## `infra-podman`

Esta carpeta contiene toda la infraestructura necesaria para ejecutar el proyecto mediante **Podman**.

### Responsabilidades

- Crear la red interna de contenedores.
- Levantar PostgreSQL.
- Levantar Zookeeper y Kafka.
- Levantar Schema Registry para los eventos Avro.
- Levantar Control Center para visualizar tópicos y mensajes.
- Crear los tópicos Kafka requeridos.
- Inicializar las tablas de base de datos.
- Iniciar y detener la infraestructura con scripts reutilizables.

### Estructura esperada

```text
infra-podman/
├── compose/
│   ├── database/
│   │   └── init.sql
│   ├── 01-kafka-server.yml
│   └── 02-postgres.yml
├── schemas/
│   └── ReservationCreatedEvent.avsc
├── scripts/
│   ├── start-infrastructure.sh
│   ├── stop-infrastructure.sh
│   └── register-reservation-event-schema.sh
└── volumes/
    └── zoo/
        ├── data/
        └── log/
```

### Tópico principal

```text
reservation-created-topic
```

Este tópico transportará los eventos generados cuando una reserva sea creada correctamente.

### Tablas principales

La base de datos del consumidor tendrá al menos las siguientes tablas:

- `tb_reservation_event_log`: almacena el historial de eventos consumidos, estado de procesamiento y posibles errores.
- `tb_product_reservation_summary`: mantiene el total de reservas y cantidades reservadas por producto.

Estas tablas permiten demostrar que el consumidor no solo lee el mensaje, sino que también aplica una acción de negocio persistente. La estructura registra eventos y mantiene totales agregados en tablas separadas.

---
## Despliegue y validación local de infraestructura

Esta sección permite validar la infraestructura antes de ejecutar `api-producer` y `api-consumer`.

La validación comprueba que Podman, Kafka, Schema Registry, PostgreSQL, el tópico Kafka, el esquema Avro y las tablas de base de datos estén disponibles correctamente.

### Prerrequisitos

Se requiere tener instalados:

- Podman.
- Podman Compose.
- Git Bash, WSL o una terminal compatible con Bash.
- `curl`.
- Python 3, utilizado para validar el JSON del esquema Avro.
- Acceso a Internet para descargar las imágenes de contenedores la primera vez.

Verificar herramientas:

```bash
podman --version
podman compose version
curl --version
python3 --version
```

> En Windows con Git Bash, `python3` podría estar disponible como `python`.  
> Si el comando `python3 --version` falla, instalar Python 3 o adaptar el script `register-reservation-event-schema.sh` para utilizar `python`.

---

### Iniciar Podman en Windows

Cuando el proyecto se ejecuta desde Windows mediante Git Bash, Podman utiliza una máquina Linux basada en WSL.

Verificar el estado de la máquina:

```bash
podman machine ls
```

Si existe `podman-machine-default` y está detenida, iniciarla:

```bash
podman machine start podman-machine-default
```

Si no existe una máquina Podman, crearla e iniciarla:

```bash
podman machine init --now
```

Verificar que el motor de Podman funciona:

```bash
podman info
podman ps
```

Los comandos anteriores deben ejecutarse correctamente antes de continuar.

---

### Preparar permisos y directorios

Ubicarse en la raíz del repositorio y asegurar que los scripts tengan permiso de ejecución:

```bash
chmod +x infra-podman/scripts/start-infrastructure.sh
chmod +x infra-podman/scripts/stop-infrastructure.sh
chmod +x infra-podman/scripts/register-reservation-event-schema.sh
```

Crear los directorios locales requeridos por ZooKeeper:

```bash
mkdir -p infra-podman/volumes/zoo/data
mkdir -p infra-podman/volumes/zoo/log
```

---

### Validar la configuración Compose

Ingresar al módulo de infraestructura:

```bash
cd infra-podman
```

Validar que los archivos Compose sean correctos y puedan combinarse:

```bash
podman compose \
  -p reservation-infrastructure \
  -f compose/01-kafka-server.yml \
  -f compose/02-postgres.yml \
  config
```

El comando debe finalizar sin errores.

Los valores esperados son:

| Elemento | Valor esperado |
|---|---|
| Red Podman | `reservation-net` |
| Proyecto Compose | `reservation-infrastructure` |
| Contenedor Kafka | `kafka-broker-1` |
| Contenedor PostgreSQL | `pg-reservation` |
| Base de datos | `db_reservation` |
| Tópico Kafka | `reservation-created-topic` |
| Schema Registry | Puerto `8081` |
| Control Center | Puerto `9021` |
| PostgreSQL | Puerto `15432` |

---

### Iniciar la infraestructura

Ejecutar:

```bash
./scripts/start-infrastructure.sh
```

El script realiza las siguientes acciones:

1. Crea la red externa `reservation-net`.
2. Descarga las imágenes requeridas si aún no existen.
3. Inicia ZooKeeper, Kafka, Schema Registry, Control Center y PostgreSQL.
4. Espera la disponibilidad de Kafka.
5. Crea el tópico `reservation-created-topic`.
6. Muestra la lista de tópicos Kafka disponibles.

---

### 10.6. Validar los contenedores

Verificar que todos los contenedores se encuentren en ejecución:

```bash
podman ps
```

Deben aparecer, como mínimo, los siguientes contenedores:

```text
zookeeper
kafka-broker-1
schema-registry
control-center
pg-reservation
```

Si algún contenedor no aparece o se detiene, revisar sus logs.

Kafka:
```bash
podman logs kafka-broker-1 --tail 100
```

Schema Registry:
```bash
podman logs schema-registry --tail 100
```

PostgreSQL:
```bash
podman logs pg-reservations --tail 100
```

Control Center:
```bash
podman logs control-center --tail 100
```
---
### Validar la red Podman

Verificar la red de contenedores:

```bash
podman network inspect reservation-net
```

La salida debe mostrar la red y los contenedores de infraestructura conectados.

---

### Validar Kafka y el tópico de reservas

Listar los tópicos Kafka:

```bash
podman exec kafka-broker-1 \
  kafka-topics \
  --bootstrap-server kafka-broker-1:9092 \
  --list
```

Debe aparecer:

```text
reservation-created-topic
```

Consultar el detalle del tópico:

```bash
podman exec kafka-broker-1 \
  kafka-topics \
  --bootstrap-server kafka-broker-1:9092 \
  --describe \
  --topic reservation-created-topic
```

El resultado debe indicar:

```text
Topic: reservation-created-topic
PartitionCount: 1
ReplicationFactor: 1
```

---

### Registrar y validar el esquema Avro

Si el script `start-infrastructure.sh` no registra automáticamente el esquema Avro, ejecutar:

```bash
./scripts/register-reservation-event-schema.sh
```

El resultado esperado incluye mensajes similares a:

```text
Esquema Avro encontrado y JSON válido.
Kafka está disponible.
Schema Registry está disponible.
Esquema registrado correctamente.
Subject: reservation-created-topic-value
```

Verificar los subjects registrados en Schema Registry:

```bash
curl --silent http://localhost:8081/subjects
```

Debe aparecer:

```json
["reservation-created-topic-value"]
```

Consultar la última versión registrada:

```bash
curl --silent \
  http://localhost:8081/subjects/reservation-created-topic-value/versions/latest
```

La respuesta debe incluir el esquema `ReservationCreatedEvent` y sus campos de negocio:

```text
eventId
reservationId
customerName
externalProductId
productTitle
quantity
status
createdAt
```

---

### Validar PostgreSQL y las tablas iniciales

Conectarse a PostgreSQL:

```bash
podman exec -it pg-reservation \
  env PGPASSWORD=reservation_password \
  psql \
  -U reservation_user \
  -d db_reservation
```

Dentro de PostgreSQL, listar las tablas:

```sql
\dt
```

Deben aparecer las siguientes tablas:

```text
tb_reservation
tb_reservation_event_log
tb_product_reservation_summary
```

Consultar la estructura de las tablas:

```sql
\d tb_reservation
\d tb_reservation_event_log
\d tb_product_reservation_summary
```

Salir de PostgreSQL:

```sql
\q
```

También se puede validar directamente con un único comando:

```bash
podman exec pg-reservation \
  env PGPASSWORD=reservation_password \
  psql \
  -U reservation_user \
  -d db_reservation \
  -c "\dt"
```

---

### Validar Kafka Control Center

Abrir en el navegador:

```text
http://localhost:9021
```

En Control Center se debe validar:

1. La visualización del clúster Kafka.
2. La existencia del tópico `reservation-created-topic`.
3. La disponibilidad de Schema Registry.
4. La existencia inicial de cero mensajes en el tópico, antes de ejecutar `api-producer`.

---

### Detener la infraestructura

Al finalizar las pruebas, ejecutar:

```bash
./scripts/stop-infrastructure.sh
```

Este script detiene los contenedores de Kafka, ZooKeeper, Schema Registry, Control Center y PostgreSQL, además de intentar eliminar la red `reservation-net`.

Los datos de PostgreSQL se conservan por defecto porque el volumen no se elimina.

---

### Reinicio limpio de PostgreSQL para desarrollo

El archivo `init.sql` se ejecuta solamente al crear un volumen PostgreSQL nuevo.

Si se modificó `init.sql` y se requiere reconstruir las tablas desde cero:

```bash
./scripts/stop-infrastructure.sh
podman volume ls
```

Identificar el volumen asociado a PostgreSQL y eliminarlo:

```bash
podman volume rm reservation-infrastructure_pg-reservations-data
```

Luego iniciar nuevamente la infraestructura:

```bash
./scripts/start-infrastructure.sh
```

> Advertencia: eliminar el volumen borra toda la información almacenada en PostgreSQL.

---

## `api-producer`

Este módulo representa el servicio principal del negocio: la **API de Reservas**.

Su responsabilidad es recibir las solicitudes REST de los operadores autorizados, validar los datos, consultar la API externa, registrar la reserva y publicar un evento Kafka.

### Responsabilidades principales

- Generar tokens JWT locales.
- Proteger endpoints mediante roles.
- Exponer operaciones CRUD de reservas.
- Validar solicitudes con Bean Validation.
- Manejar errores de negocio y errores HTTP de forma centralizada.
- Consumir la API externa DummyJSON.
- Publicar eventos Avro en Kafka.

### Estructura esperada

```text
api-producer/
└── src/main/java/.../
    ├── resource/
    │   ├── AuthResource.java
    │   └── ReservationResource.java
    ├── service/
    │   ├── AuthService.java
    │   ├── ReservationService.java
    │   └── ExternalProductService.java
    ├── repository/
    │   └── ReservationRepository.java
    ├── entity/
    │   └── Reservation.java
    ├── dto/
    │   ├── TokenRequest.java
    │   ├── TokenResponse.java
    │   ├── ReservationRequest.java
    │   ├── ReservationUpdateRequest.java
    │   └── ReservationResponse.java
    ├── client/
    │   └── DummyJsonClient.java
    ├── mapper/
    │   └── ReservationMapper.java
    ├── kafka/
    │   └── ReservationKafkaProducer.java
    ├── exception/
    │   └── ApiExceptionMapper.java
    └── security/
        └── JwtService.java
```

### API REST propuesta

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `POST` | `/api/auth/token` | Genera un JWT local firmado con RSA. | Público |
| `POST` | `/api/reservations` | Crea una reserva y publica un evento Kafka. | `OPERATOR`, `ADMIN` |
| `GET` | `/api/reservations` | Lista las reservas registradas. | `ADMIN` |
| `GET` | `/api/reservations/{id}` | Consulta una reserva por identificador. | `OPERATOR`, `ADMIN` |
| `PUT` | `/api/reservations/{id}` | Actualiza una reserva existente. | `OPERATOR`, `ADMIN` |
| `DELETE` | `/api/reservations/{id}` | Cancela o elimina una reserva. | `ADMIN` |
| `GET` | `/api/external/products/{id}` | Consulta un producto externo validado por DummyJSON. | `OPERATOR`, `ADMIN` |

### Entidad principal: Reserva

Una reserva tendrá, como mínimo, los siguientes datos:

| Campo | Descripción |
|---|---|
| `id` | Identificador único de la reserva. |
| `customerName` | Nombre del cliente que solicita la reserva. |
| `externalProductId` | Identificador del producto obtenido desde DummyJSON. |
| `productTitle` | Nombre del producto externo validado. |
| `quantity` | Cantidad solicitada. |
| `status` | Estado de la reserva: `CREATED` o `CANCELLED`. |
| `createdAt` | Fecha y hora de creación. |

### Persistencia y Panache

La API productora utilizará Hibernate ORM con Panache y el patrón **Repository**. La entidad `Reservation` representará la tabla de reservas, mientras que `ReservationRepository` concentrará las consultas específicas necesarias para el negocio.

La separación entre entidad, repositorio, servicio, DTO y recurso REST evita que el controlador concentre reglas de negocio o consultas de base de datos. El material de clase presenta Panache como una forma de simplificar persistencia y soportar tanto Repository Pattern como Active Record; para este proyecto se utilizará Repository Pattern por su mejor separación de responsabilidades.

---

## 5. Integración con DummyJSON

DummyJSON será utilizado como API externa para validar productos y demostrar una integración real mediante MicroProfile REST Client.

La API productora implementará un cliente REST que consulte información de productos externos.

---

## 6. Seguridad con JWT y RSA

El proyecto implementará autenticación y autorización mediante JWT.

### Flujo de seguridad

1. El operador solicita un token en `POST /api/auth/token`.
2. La API productora valida credenciales de cliente configuradas para la demostración.
3. La API productora genera un JWT con:
   - Emisor.
   - Subject.
   - Fecha de expiración.
   - Roles o grupos: `OPERATOR` y/o `ADMIN`.
4. El JWT se firma con la **clave privada RSA**.
5. El productor y el consumidor validan el JWT con la **clave pública RSA**.
6. Los endpoints restringidos usan `@RolesAllowed`.

### Manejo de llaves

```text
security/
├── privateKey.pem   # Solo firma tokens; no se sube al repositorio.
├── publicKey.pem    # Verifica tokens; puede incluirse para desarrollo.
└── README.md        # Instrucciones para generar llaves locales.
```

La clave privada se agregará a `.gitignore`. El repositorio incluirá únicamente un archivo de ejemplo o instrucciones para generarla localmente.

---

## `api-consumer`

Este módulo representa el servicio de **Auditoría y Resumen de Reservas**.

No crea reservas directamente. Su responsabilidad es consumir los eventos producidos por `api-producer`, registrar evidencia de procesamiento y actualizar información agregada para consultas administrativas.

### Responsabilidades principales

- Suscribirse a `reservation-created-topic`.
- Deserializar `ReservationCreatedEvent`.
- Registrar cada evento procesado.
- Evitar duplicados usando `eventId`.
- Actualizar el resumen por producto.
- Exponer consultas protegidas de auditoría y resumen.
- Persistir información con Hibernate ORM y Panache.
- Validar tokens JWT mediante la clave pública RSA.

### Estructura esperada

```text
api-consumer/
└── src/main/java/.../
    ├── config/
    │   └── KafkaConsumerConfig.java
    ├── consumer/
    │   └── ReservationKafkaConsumer.java
    ├── service/
    │   └── ReservationAuditService.java
    ├── repository/
    │   ├── ReservationEventLogRepository.java
    │   └── ProductReservationSummaryRepository.java
    ├── entity/
    │   ├── ReservationEventLog.java
    │   └── ProductReservationSummary.java
    ├── dto/
    │   ├── ReservationEventLogResponse.java
    │   └── ProductReservationSummaryResponse.java
    ├── resource/
    │   └── ReservationAuditResource.java
    ├── mapper/
    │   └── ReservationAuditMapper.java
    └── exception/
        └── ApiExceptionMapper.java
```

### API REST propuesta

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/reservation-events` | Lista los eventos procesados y auditados. | `ADMIN` |
| `GET` | `/api/reservation-events/{reservationId}` | Consulta los eventos de una reserva. | `ADMIN` |
| `GET` | `/api/reservation-summary` | Muestra el total de reservas por producto. | `ADMIN` |

---

## 7. Evento Kafka

El evento principal será `ReservationCreatedEvent`.

### Ejemplo conceptual

```json
{
  "eventId": "uuid-del-evento",
  "reservationId": "uuid-de-la-reserva",
  "customerName": "Nombre del cliente",
  "externalProductId": 101,
  "productTitle": "Producto validado",
  "quantity": 2,
  "status": "CREATED",
  "createdAt": "2026-10-16T10:30:00"
}
```

El esquema Avro se ubicará en:

```text
infra-podman/schemas/ReservationCreatedEvent.avsc
```

---

## 8. Arquitectura y buenas prácticas

El proyecto aplicará las siguientes decisiones:

- **Arquitectura por capas:** recursos REST, servicios, repositorios, entidades, DTOs, mappers, clientes externos y componentes Kafka.
- **DTOs:** las entidades JPA no se expondrán directamente en las APIs.
- **Repository Pattern:** acceso a datos encapsulado con Panache.
- **Adapter Pattern:** el cliente DummyJSON transforma la respuesta externa al modelo interno del negocio.
- **Builder o Factory:** creación controlada del evento de reserva.
- **Manejo global de excepciones:** respuestas consistentes para validación, recurso inexistente, acceso denegado y fallos externos.
- **Validaciones:** uso de restricciones como `@NotBlank`, `@NotNull`, `@Positive` y `@Size`.
- **Métodos pequeños y responsabilidades únicas:** para evitar clases extensas, lógica duplicada y controladores con reglas de negocio.
- **Configuración externa:** propiedades en `application.yml` y perfiles para desarrollo o producción.

---

## 9. Ejecución esperada

### 1. Levantar infraestructura

```bash
cd infra-podman
chmod +x start-infrastructure.sh
./start-infrastructure.sh
```

### 2. Ejecutar API productora

```bash
cd api-producer
./mvnw quarkus:dev
```

### 3. Ejecutar API consumidora

```bash
cd api-consumer
./mvnw quarkus:dev
```

### 4. Demostración funcional

1. Obtener JWT local.
2. Crear una reserva enviando el JWT en el header `Authorization: Bearer <token>`.
3. Verificar que la reserva fue persistida en la base de datos del productor.
4. Verificar que el evento fue enviado al tópico Kafka.
5. Verificar que el consumidor registró la auditoría.
6. Consultar el resumen de reservas desde la API consumidora con un token de administrador.
7. Detener la infraestructura con el script correspondiente.

---

## 10. Seguridad del repositorio

No se deben publicar secretos ni llaves privadas.

Se incluirán en `.gitignore`:

```gitignore
security/privateKey.pem
security/private.key
.env
application-local.yml
```

Para generar las llaves RSA y configurar variables locales, sin exponer credenciales reales.
Se tiene que:

1. a
2. b
3. c
