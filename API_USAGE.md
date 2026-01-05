# Guide d'utilisation des API ERP

Ce document présente des exemples d'utilisation des API REST de l'application ERP.

## Base URL

```
http://localhost:8080
```

## 1. Authentification

### Connexion (Login)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Réponse:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "admin",
  "email": "admin@erp.com"
}
```

### Inscription (Register)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password123",
    "email": "user1@example.com",
    "firstName": "Jean",
    "lastName": "Dupont",
    "roles": ["ROLE_USER"]
  }'
```

## 2. Gestion des Produits

**Note:** Toutes les requêtes suivantes nécessitent le header d'authentification:
```
Authorization: Bearer {votre-token-jwt}
```

### Créer un produit

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "LAPTOP001",
    "name": "Ordinateur Portable Dell",
    "description": "Dell Latitude 15 pouces",
    "purchasePrice": 800.00,
    "salePrice": 1200.00,
    "stockQuantity": 10,
    "minStockLevel": 5,
    "maxStockLevel": 50,
    "unit": "pièce",
    "category": "Informatique",
    "active": true
  }'
```

### Lister tous les produits

```bash
curl -X GET http://localhost:8080/api/products \
  -H "Authorization: Bearer {token}"
```

### Obtenir un produit par code

```bash
curl -X GET http://localhost:8080/api/products/code/LAPTOP001 \
  -H "Authorization: Bearer {token}"
```

### Obtenir les produits en stock faible

```bash
curl -X GET http://localhost:8080/api/products/low-stock \
  -H "Authorization: Bearer {token}"
```

## 3. Gestion des Achats

### Créer un achat

```bash
curl -X POST http://localhost:8080/api/purchases \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "referenceNumber": "ACH-2024-001",
    "purchaseDate": "2024-01-15",
    "supplier": "Fournisseur Tech SA",
    "notes": "Commande urgente",
    "items": [
      {
        "product": {"id": 1},
        "quantity": 5,
        "unitPrice": 800.00
      }
    ]
  }'
```

### Réceptionner un achat (met à jour le stock)

```bash
curl -X PUT http://localhost:8080/api/purchases/1/receive \
  -H "Authorization: Bearer {token}"
```

### Lister les achats

```bash
curl -X GET http://localhost:8080/api/purchases \
  -H "Authorization: Bearer {token}"
```

### Filtrer les achats par statut

```bash
curl -X GET "http://localhost:8080/api/purchases/status/PENDING" \
  -H "Authorization: Bearer {token}"
```

## 4. Gestion des Ventes

### Créer une vente

```bash
curl -X POST http://localhost:8080/api/sales \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "referenceNumber": "VTE-2024-001",
    "saleDate": "2024-01-16",
    "customer": "Client ABC SARL",
    "notes": "Livraison express",
    "items": [
      {
        "product": {"id": 1},
        "quantity": 2,
        "unitPrice": 1200.00
      }
    ]
  }'
```

### Finaliser une vente (met à jour le stock)

```bash
curl -X PUT http://localhost:8080/api/sales/1/complete \
  -H "Authorization: Bearer {token}"
```

### Lister les ventes

```bash
curl -X GET http://localhost:8080/api/sales \
  -H "Authorization: Bearer {token}"
```

### Obtenir les ventes par période

```bash
curl -X GET "http://localhost:8080/api/sales/date-range?startDate=2024-01-01&endDate=2024-12-31" \
  -H "Authorization: Bearer {token}"
```

## 5. Gestion des Inventaires

### Créer un inventaire

```bash
curl -X POST http://localhost:8080/api/inventories \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "referenceNumber": "INV-2024-001",
    "inventoryDate": "2024-01-20",
    "notes": "Inventaire trimestriel",
    "items": [
      {
        "product": {"id": 1},
        "actualQuantity": 12,
        "remarks": "Stock correct"
      }
    ]
  }'
```

### Finaliser un inventaire (ajuste le stock)

```bash
curl -X PUT http://localhost:8080/api/inventories/1/complete \
  -H "Authorization: Bearer {token}"
```

### Lister les inventaires

```bash
curl -X GET http://localhost:8080/api/inventories \
  -H "Authorization: Bearer {token}"
```

## 6. Traçabilité des Mouvements de Stock

### Obtenir tous les mouvements

```bash
curl -X GET http://localhost:8080/api/stock-movements \
  -H "Authorization: Bearer {token}"
```

### Obtenir les mouvements d'un produit

```bash
curl -X GET http://localhost:8080/api/stock-movements/product/1 \
  -H "Authorization: Bearer {token}"
```

### Filtrer par type de mouvement

```bash
curl -X GET http://localhost:8080/api/stock-movements/type/PURCHASE \
  -H "Authorization: Bearer {token}"
```

## 7. Documentation Interactive

Accédez à la documentation Swagger UI pour tester les API de manière interactive:

```
http://localhost:8080/swagger-ui.html
```

## Codes de Statut HTTP

- `200 OK` - Requête réussie
- `201 Created` - Ressource créée avec succès
- `204 No Content` - Suppression réussie
- `400 Bad Request` - Données invalides
- `401 Unauthorized` - Authentification requise
- `403 Forbidden` - Accès refusé (permissions insuffisantes)
- `404 Not Found` - Ressource non trouvée
- `500 Internal Server Error` - Erreur serveur

## Permissions par Rôle

| Rôle                   | Permissions                                      |
|------------------------|--------------------------------------------------|
| ROLE_ADMIN             | Accès complet à toutes les fonctionnalités      |
| ROLE_MANAGER           | Gestion des produits, achats, ventes, stocks    |
| ROLE_PURCHASE_AGENT    | Gestion des achats et consultation              |
| ROLE_SALES_AGENT       | Gestion des ventes et consultation              |
| ROLE_INVENTORY_MANAGER | Gestion des inventaires et stocks                |
| ROLE_USER              | Consultation uniquement                          |
