# 📋 Liste des Fonctionnalités à Implémenter

## Application Web ERP — Achats / Ventes / Stock / Inventaires

---

## 🎯 Objectifs du Projet

| Objectif         | Description                                                                                  |
| ---------------- | -------------------------------------------------------------------------------------------- |
| Centralisation   | Unifier les flux : achats → réception → stockage → sortie → vente → facturation → inventaire |
| Standardisation  | Méthodes de gestion de stock (entrées/sorties, valorisation, inventaires)                    |
| Contrôle interne | Séparation des tâches, validations multi-niveaux                                             |
| Pilotage         | KPI par rôle / service / site / entité                                                       |
| Performance      | Réduction des délais, erreurs et écarts d'inventaire                                         |

---

## 📦 Module 1 : Référentiels

### 1.1 Gestion des Articles

- [ ] CRUD articles (code, désignation, famille, unités)
- [ ] Configuration unité stock / achat / vente avec coefficients
- [ ] Paramétrage stock min/max/sécurité
- [ ] Gestion articles périssables (lot, série, DLUO, DLC)
- [ ] Activation/désactivation articles

### 1.2 Gestion des Familles d'Articles

- [ ] CRUD familles avec hiérarchie (parent/enfant)
- [ ] Configuration traçabilité lot obligatoire par famille
- [ ] Méthode de valorisation par famille (FIFO/CUMP)
- [ ] Méthode de sortie par famille (FIFO/FEFO)

### 1.3 Gestion des Fournisseurs

- [ ] CRUD fournisseurs (raison sociale, coordonnées, NIF, STAT)
- [ ] Conditions de paiement par fournisseur
- [ ] Note qualité fournisseur
- [ ] Historique des transactions

### 1.4 Gestion des Clients

- [ ] CRUD clients (raison sociale, coordonnées, NIF, STAT)
- [ ] Plafond crédit client
- [ ] Remise globale par client
- [ ] Historique des ventes

### 1.5 Gestion des Tarifs

- [ ] Tarifs fournisseurs par article (prix, quantité min, validité)
- [ ] Tarifs clients par article
- [ ] Gestion multi-devises

### 1.6 Organisation

- [ ] CRUD sites / entités légales
- [ ] CRUD dépôts par site
- [ ] CRUD emplacements par dépôt (zone, capacité)
- [ ] CRUD unités de mesure
- [ ] CRUD taxes (TVA, autres)

---

## 🛒 Module 2 : Achats

### 2.1 Demandes d'Achat (DA)

- [ ] Création DA (demandeur, articles, quantités, date besoin)
- [ ] Affectation priorité (Basse, Normale, Haute, Urgente)
- [ ] Soumission DA pour approbation
- [ ] Suivi statut DA (Brouillon → Soumis → En approbation → Approuvé/Rejeté)
- [ ] Annulation DA

### 2.2 Workflow d'Approbation

- [ ] Approbation multi-niveaux (N1/N2/N3) selon seuils montant
- [ ] Règle : créateur ≠ approbateur
- [ ] Validation Finance (disponibilité fonds)
- [ ] Historique des approbations avec commentaires
- [ ] Notifications aux approbateurs

### 2.3 Pro-forma Fournisseurs

- [ ] Réception et saisie pro-forma
- [ ] Comparaison multi-fournisseurs
- [ ] Sélection pro-forma pour commande
- [ ] Pièce jointe (scan pro-forma)

### 2.4 Bons de Commande Fournisseur (BC)

- [ ] Création BC depuis DA approuvée
- [ ] Transformation pro-forma → BC
- [ ] Workflow approbation BC (seuils)
- [ ] Envoi BC au fournisseur
- [ ] Suivi statut (Brouillon → Approuvé → Envoyé → Réception)
- [ ] Règle : créateur ≠ approbateur final

### 2.5 Réception Marchandises

- [ ] Création bon de réception lié au BC
- [ ] Contrôle quantités (commandé vs reçu)
- [ ] Réception partielle autorisée
- [ ] Saisie référence BL fournisseur
- [ ] Affectation emplacement/lot
- [ ] Contrôle qualité (conforme/non conforme)
- [ ] Génération mouvement stock (entrée)
- [ ] Règle : réceptionnaire ≠ validateur facture

### 2.6 Factures Fournisseur

- [ ] Saisie facture fournisseur
- [ ] Rapprochement 3-way match (BC ↔ Réception ↔ Facture)
- [ ] Détection écarts (quantité, prix)
- [ ] Gestion litiges
- [ ] Validation facture

### 2.7 Paiements Fournisseur

- [ ] Enregistrement paiements (modes : virement, chèque, espèces)
- [ ] Validation paiement
- [ ] Suivi échéances

---

## 💰 Module 3 : Ventes

### 3.1 Devis / Pro-forma Clients

- [ ] Création devis (client, articles, prix, remises)
- [ ] Application remise plafonnée par commercial
- [ ] Date de validité
- [ ] Envoi devis client
- [ ] Suivi statut (Brouillon → Envoyé → Accepté/Refusé/Expiré)
- [ ] Transformation devis → commande

### 3.2 Commandes Clients

- [ ] Création commande depuis devis ou directe
- [ ] Vérification disponibilité stock
- [ ] Réservation stock automatique (configurable)
- [ ] Validation remises exceptionnelles par responsable
- [ ] Suivi statut commande
- [ ] Annulation avec validation responsable

### 3.3 Préparation et Livraison

- [ ] Génération bon de préparation (picking)
- [ ] Informations picking : article, quantité, emplacement, lot, priorité
- [ ] Blocage livraison si stock insuffisant
- [ ] Création bon de livraison
- [ ] Livraison partielle
- [ ] Génération mouvement stock (sortie)

### 3.4 Facturation Client

- [ ] Génération facture depuis BL
- [ ] Calcul automatique TVA
- [ ] Gestion échéances
- [ ] Impression/export facture

### 3.5 Avoirs Clients

- [ ] Création avoir (retour, erreur prix, casse, remise)
- [ ] Double validation (créateur ≠ validateur)
- [ ] Règle : créateur ≠ validateur ≠ encaisseur

### 3.6 Encaissements

- [ ] Enregistrement encaissements multi-modes
- [ ] Affectation aux factures
- [ ] Suivi solde client

---

## 📦 Module 4 : Stocks

### 4.1 Gestion des Lots/Séries

- [ ] Création lots à la réception
- [ ] Numéro lot/série unique
- [ ] Dates : fabrication, péremption, DLUO, DLC
- [ ] Statut lot (Disponible, Bloqué, Quarantaine, Expiré)
- [ ] Blocage automatique lots expirés

### 4.2 Stock par Emplacement

- [ ] Visualisation stock par article/dépôt/emplacement/lot
- [ ] Stock disponible = stock physique - réservé
- [ ] Coût unitaire par emplacement

### 4.3 Mouvements de Stock

- [ ] Traçabilité complète (qui, quoi, quand, pourquoi)
- [ ] Types : Entrée, Sortie, Transfert, Ajustement
- [ ] Motifs : Réception, Livraison, Retour, Rebut, Conso interne
- [ ] Numérotation automatique non réutilisable
- [ ] Lien document origine
- [ ] Historique non modifiable

### 4.4 Réservations

- [ ] Réservation à la commande client (configurable)
- [ ] Allocation FIFO ou FEFO selon produit
- [ ] Expiration réservation
- [ ] Annulation réservation

### 4.5 Transferts Inter-dépôts

- [ ] Création ordre de transfert
- [ ] Validation chef magasin
- [ ] Expédition source
- [ ] Réception destination
- [ ] Traçabilité complète

---

## 📊 Module 5 : Inventaires

### 5.1 Sessions d'Inventaire

- [ ] Création inventaire (Annuel, Tournant, Ponctuel)
- [ ] Périmètre : dépôt, zone, famille
- [ ] Gel des mouvements pendant inventaire (optionnel)

### 5.2 Comptage

- [ ] Liste articles à compter
- [ ] Saisie quantités comptées
- [ ] Calcul automatique écarts
- [ ] Recomptage si écart significatif
- [ ] Règle : compteur ≠ validateur ajustement

### 5.3 Ajustements de Stock

- [ ] Génération demandes d'ajustement
- [ ] Double validation pour ajustements importants
- [ ] Justification obligatoire
- [ ] Génération mouvements correctifs

---

## 💹 Module 6 : Valorisation

### 6.1 Méthodes de Valorisation

- [ ] FIFO (Premier Entré, Premier Sorti)
- [ ] CUMP (Coût Unitaire Moyen Pondéré)
- [ ] Configuration par famille d'articles

### 6.2 Clôture Mensuelle

- [ ] Gel des coûts en fin de mois
- [ ] Blocage mouvements rétrodatés
- [ ] Calcul valeur stock à date

### 6.3 Rapports Valorisation

- [ ] Variation de coût
- [ ] Inventaire vs valorisation
- [ ] Écarts de marge

---

## 👥 Module 7 : Administration & Sécurité

### 7.1 Gestion des Utilisateurs

- [ ] CRUD utilisateurs (matricule, nom, email, mot de passe)
- [ ] Affectation département/site
- [ ] Niveau hiérarchique (Opérateur → Directeur)
- [ ] Désactivation compte

### 7.2 Gestion des Rôles (RBAC)

- [ ] Rôles prédéfinis (Acheteur, Magasinier, Commercial, DAF, etc.)
- [ ] Association rôles ↔ permissions
- [ ] Association utilisateurs ↔ rôles

### 7.3 Restrictions par Attributs (ABAC)

- [ ] Restriction par site
- [ ] Restriction par dépôt
- [ ] Restriction par famille d'articles
- [ ] Plafond montant par utilisateur

### 7.4 Délégations Temporaires

- [ ] Délégation de droits avec date début/fin
- [ ] Justification obligatoire
- [ ] Expiration automatique

### 7.5 Audit et Traçabilité

- [ ] Journal d'audit non modifiable
- [ ] Enregistrement : action, utilisateur, date, avant/après
- [ ] IP et user-agent
- [ ] Consultation historique par document

---

## 📈 Module 8 : Tableaux de Bord & KPI

### 8.1 Direction Générale

- [ ] CA, marge brute, marge % (global + par site)
- [ ] Valeur stock total + évolution (M-1, M-12)
- [ ] Rotation stock (turnover)
- [ ] Top 5 surstocks / obsolescence
- [ ] Taux d'écarts inventaire

### 8.2 Responsable Achats / Supply Chain

- [ ] Cycle time DA → BC (médiane, P90)
- [ ] Respect délais fournisseurs (OTD)
- [ ] Taux réception conforme
- [ ] Taux litiges facture (3-way mismatch)
- [ ] Concentration fournisseurs
- [ ] Évolution prix d'achat
- [ ] Taux commandes urgentes

### 8.3 Magasin / Responsable Stock

- [ ] Taux de précision stock (théorique vs physique)
- [ ] Obsolescence / péremption (valeur, lots à risque)
- [ ] Productivité préparation (lignes/heure, erreurs picking)
- [ ] Temps de traitement réception (dock-to-stock)

### 8.4 Ventes / Responsable Commercial

- [ ] Commandes en cours, livrées, en retard
- [ ] Taux annulation commandes + motifs
- [ ] Remises accordées vs plafond
- [ ] Avoirs : volume, valeur, causes
- [ ] Backlog non servi (stock insuffisant)

### 8.5 Finance / DAF

- [ ] Factures bloquées (3-way mismatch)
- [ ] Valeur stock comptable vs opérationnelle
- [ ] Variation de marge (prix vente vs coût)

---

## 🔐 Règles de Contrôle Interne (Transverses)

### Séparation des Tâches

| Règle | Description                                     |
| ----- | ----------------------------------------------- |
| R1    | Créateur DA ≠ Approbateur DA                    |
| R2    | Créateur BC ≠ Approbateur BC                    |
| R3    | Réceptionnaire ≠ Validateur facture fournisseur |
| R4    | Créateur client ≠ Validateur avoir ≠ Encaisseur |
| R5    | Compteur inventaire ≠ Validateur ajustement     |

### Principe du Moindre Privilège

- Magasinier : réception/sortie, pas de modification coûts
- Commercial : devis/commande, remise ≤ plafond
- Acheteur : création BC, pas d'auto-approbation

---

## 🛠️ Contraintes Techniques

| Contrainte   | Description                                           |
| ------------ | ----------------------------------------------------- |
| Multi-sites  | Support multi-sites / multi-dépôts / multi-entités    |
| Volumétrie   | Haute volumétrie (articles, mouvements, utilisateurs) |
| Numérotation | Automatique et non réutilisable                       |
| Traçabilité  | Journalisation complète non modifiable                |
| Intégration  | API pour intégration RH et autres systèmes            |

---

## 📅 Priorités d'Implémentation

### Phase 1 - Fondations

1. Référentiels (articles, fournisseurs, clients, dépôts)
2. Gestion utilisateurs et rôles
3. Journal d'audit

### Phase 2 - Flux Achats

4. Demandes d'achat + workflow approbation
5. Bons de commande fournisseur
6. Réception marchandises
7. Factures et paiements fournisseur

### Phase 3 - Flux Ventes

8. Devis clients
9. Commandes clients + réservation stock
10. Livraison et facturation
11. Avoirs et encaissements

### Phase 4 - Gestion Stock Avancée

12. Lots/séries et traçabilité
13. Transferts inter-dépôts
14. Inventaires et ajustements
15. Valorisation stock

### Phase 5 - Pilotage

16. Tableaux de bord par rôle
17. KPI et rapports
18. Alertes automatiques

---

## 📝 Notes

- **Base de données** : PostgreSQL (voir `database.sql`)
- **Architecture** : Application Web
- **Sécurité** : RBAC + ABAC, double validation opérations sensibles
