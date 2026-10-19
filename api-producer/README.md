# api-producer

Define user credential in .env

## Reservations Examples
### Post http://localhost:8182/api/reservations
```json
{
  "customerName": "Ana Torres",
  "externalProductId": 1,
  "quantity": 1
}
```

### Get http://localhost:8182/api/reservations
```json
{
"id": "ac148942-990e-4033-8828-c45bfbcbfa59",
"customerName": "Ana Torres",
"externalProductId": 1,
"productTitle": "Essence Mascara Lash Princess",
"quantity": 1,
"status": "CREATED",
"createdAt": "2026-10-19T13:09:54.255307200Z",
"updatedAt": "2026-10-19T13:09:54.255307200Z"
}
```
### Get http://localhost:8182/api/reservations/ac148942-990e-4033-8828-c45bfbcbfa59
```json
{
"id": "ac148942-990e-4033-8828-c45bfbcbfa59",
"customerName": "Ana Torres",
"externalProductId": 1,
"productTitle": "Essence Mascara Lash Princess",
"quantity": 1,
"status": "CREATED",
"createdAt": "2026-10-19T13:09:54.255307200Z",
"updatedAt": "2026-10-19T13:09:54.255307200Z"
}
```
---
## Auth Examples
### Post http://localhost:8182/api/auth/token
```json
{
    "clientId": "reservation-operator",
    "clientSecret": "replace-with-local-operator-secret"
}

