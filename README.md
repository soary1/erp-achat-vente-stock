# ERP Achats Ventes Stock

Application web ERP dédiée à la gestion intégrée des achats, ventes, stocks et inventaires. Elle centralise les flux opérationnels, assure une traçabilité complète des mouvements, intègre la gestion des rôles et propose des tableaux de bord pour le pilotage et le contrôle interne.

## 🚀 Fonctionnalités

- **Authentification & Autorisation** : Système JWT avec gestion des rôles (RBAC)
- **Gestion des Produits** : CRUD complet avec alertes de stock bas
- **Gestion des Achats** : Création, réception et suivi des commandes d'achat
- **Gestion des Ventes** : Création, validation et suivi des ventes
- **Gestion des Stocks** : Mouvements de stock avec traçabilité complète
- **Inventaires** : Création et validation d'inventaires avec ajustements automatiques
- **API REST sécurisée** : Documentation Swagger/OpenAPI
- **Audit Trail** : Traçabilité automatique de toutes les opérations

## 🏗️ Architecture

### Technologies utilisées

- **Java 17**
- **Spring Boot 3.2.0**
  - Spring Web
  - Spring Data JPA
  - Spring Security
  - Spring Validation
- **Base de données** : H2 (développement) / PostgreSQL (production)
- **Sécurité** : JWT (JSON Web Tokens)
- **Documentation API** : SpringDoc OpenAPI 3
- **Build** : Maven

### Structure du projet

```
src/
├── main/
│   ├── java/com/erp/achats/ventes/
│   │   ├── config/         # Configurations (Security, JPA, OpenAPI)
│   │   ├── controller/     # REST Controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── exception/      # Gestion des exceptions
│   │   ├── model/          # Entités JPA
│   │   ├── repository/     # Repositories Spring Data
│   │   ├── security/       # JWT et UserDetails
│   │   └── service/        # Logique métier
│   └── resources/
│       └── application.yml # Configuration de l'application
└── test/                   # Tests unitaires et d'intégration
```

## 📋 Prérequis

- Java 17 ou supérieur
- Maven 3.6 ou supérieur

## 🔧 Installation

1. Cloner le dépôt :
```bash
git clone https://github.com/soary1/erp-achat-vente-stock.git
cd erp-achat-vente-stock
```

2. Compiler le projet :
```bash
mvn clean install
```

3. Lancer l'application :
```bash
mvn spring-boot:run
```

L'application sera accessible sur `http://localhost:8080`

## 📚 Documentation API

Une fois l'application lancée, accédez à la documentation Swagger UI :
- **Swagger UI** : http://localhost:8080/swagger-ui.html
- **API Docs** : http://localhost:8080/api-docs

## 🔐 Authentification

### Utilisateur par défaut

- **Username** : `admin`
- **Password** : `admin123`
- **Rôle** : ROLE_ADMIN

### Rôles disponibles

- `ROLE_ADMIN` : Accès complet à toutes les fonctionnalités
- `ROLE_MANAGER` : Gestion des opérations et consultation
- `ROLE_PURCHASE_AGENT` : Gestion des achats
- `ROLE_SALES_AGENT` : Gestion des ventes
- `ROLE_INVENTORY_MANAGER` : Gestion des inventaires et stocks
- `ROLE_USER` : Consultation uniquement

### Utilisation de l'API

1. **Se connecter** :
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

2. **Utiliser le token JWT** dans les requêtes suivantes :
```bash
curl -X GET http://localhost:8080/api/products \
  -H "Authorization: Bearer {votre-token-jwt}"
```

## 🗄️ Base de données

### H2 Console (Développement)

Accédez à la console H2 : http://localhost:8080/h2-console

- **JDBC URL** : `jdbc:h2:mem:erpdb`
- **Username** : `sa`
- **Password** : *(vide)*

### PostgreSQL (Production)

Modifiez `application.yml` pour configurer PostgreSQL :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/erpdb
    username: your_username
    password: your_password
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

## 🔄 Endpoints principaux

### Authentification
- `POST /api/auth/login` - Connexion
- `POST /api/auth/register` - Inscription

### Produits
- `GET /api/products` - Liste des produits
- `POST /api/products` - Créer un produit
- `GET /api/products/{id}` - Détails d'un produit
- `PUT /api/products/{id}` - Modifier un produit
- `DELETE /api/products/{id}` - Supprimer un produit

### Achats
- `GET /api/purchases` - Liste des achats
- `POST /api/purchases` - Créer un achat
- `PUT /api/purchases/{id}/receive` - Réceptionner un achat
- `PUT /api/purchases/{id}/cancel` - Annuler un achat

### Ventes
- `GET /api/sales` - Liste des ventes
- `POST /api/sales` - Créer une vente
- `PUT /api/sales/{id}/complete` - Finaliser une vente
- `PUT /api/sales/{id}/cancel` - Annuler une vente

### Inventaires
- `GET /api/inventories` - Liste des inventaires
- `POST /api/inventories` - Créer un inventaire
- `PUT /api/inventories/{id}/complete` - Finaliser un inventaire

### Mouvements de stock
- `GET /api/stock-movements` - Historique des mouvements
- `GET /api/stock-movements/product/{id}` - Mouvements par produit

## 🧪 Tests

Lancer les tests :
```bash
mvn test
```

## 📝 Licence

Ce projet est sous licence MIT.

## 👥 Contributeurs

- Développé pour la gestion d'entreprise moderne
