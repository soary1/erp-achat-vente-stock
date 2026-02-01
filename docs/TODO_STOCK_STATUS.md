# 📋 TO-DO LIST : MODULE DE GESTION DE STOCK
## État d'avancement - Analyse du 01/02/2026

**Légende :**
- ✅ = Implémenté (Code + BDD)
- ⚠️ = Partiellement implémenté
- ❌ = Non implémenté
- 🔧 = Bug / Problème détecté

---

# ================================================================================
# 1. ARCHITECTURE & DONNÉES DE BASE (Back-end / BDD)
# ================================================================================

### Structure hiérarchique Site > Dépôt > Emplacement
| Élément | Code Java | BDD | Statut |
|---------|-----------|-----|--------|
| Table `site` | ✅ `Site.java` | ✅ `new_database.sql` L108-117 | ✅ |
| Table `depot` | ✅ `Depot.java` | ✅ `new_database.sql` L119-127 | ✅ |
| Table `emplacement` | ✅ `Emplacement.java` | ✅ `new_database.sql` L129-137 | ✅ |
| Relations FK correctes | ✅ | ✅ | ✅ |

**Statut : ✅ COMPLET**

---

### Attributs de stock sur Article
| Élément | Code Java | BDD | Statut |
|---------|-----------|-----|--------|
| Gestion par Lot (Oui/Non) | ❌ Manque sur `Article.java` | ✅ `is_lot_obligatoire` sur `famille_article` | ⚠️ |
| Gestion par Série (Oui/Non) | ❌ Manque sur `Article.java` | ❌ Non présent | ❌ |
| DLC / DLUO obligatoire | ❌ Manque sur `Article.java` | ✅ `is_peremption_obligatoire` sur `famille_article` | ⚠️ |
| Méthode valorisation (FIFO/CUMP) | ❌ Manque sur `Article.java` | ✅ `methode_valorisation_code` sur `famille_article` | ⚠️ |

**Statut : ⚠️ PARTIEL**
> 📝 **Note** : Les attributs sont sur `famille_article` en BDD mais pas mappés dans le code Java. Il faut :
> - Ajouter les champs `gestionParLot`, `gestionParSerie`, `datesObligatoires`, `methodeValorisation` dans `Article.java`
> - OU mapper correctement la relation `Article → FamilleArticle → methodeValorisation`

---

### Unités de Mesure (Achat / Stockage / Vente) et conversions
| Élément | Code Java | BDD | Statut |
|---------|-----------|-----|--------|
| Table `unite_mesure` | ✅ `UniteMesure.java` | ✅ `new_database.sql` L34-37 | ✅ |
| Unité unique sur Article | ✅ `unite_code` | ✅ | ✅ |
| Unité Achat distincte | ❌ Non implémenté | ❌ | ❌ |
| Unité Vente distincte | ❌ Non implémenté | ❌ | ❌ |
| Table de conversion | ❌ Non implémenté | ❌ | ❌ |

**Statut : ❌ NON IMPLÉMENTÉ**
> 📝 **À faire** : Créer une table `conversion_unite` avec facteur de conversion

---

### Règles d'allocation automatique
| Élément | Code Java | BDD | Statut |
|---------|-----------|-----|--------|
| Allocation FIFO | ✅ `findAvailableStockFIFO()` + `getAllocationFIFO()` | N/A | ✅ |
| Allocation FEFO (périmés) | ✅ `findAvailableStockFEFO()` dans `StockRepository.java` | N/A | ✅ |
| Choix FIFO/FEFO configurable | ❌ | ❌ | ❌ |

**Statut : ✅ COMPLET** (FIFO et FEFO implémentés)

---

### Blocage automatique des lots
| Élément | Code Java | BDD | Statut |
|---------|-----------|-----|--------|
| Table `statut_qualite` | N/A | ✅ CONFORME/QUARANTAINE/REJETE | ✅ |
| Champ `statut_qualite_code` sur Lot | ✅ `Lot.java` | ✅ `lot` table | ✅ |
| Détection lots expirés | ✅ `findExpiredLots()` | N/A | ✅ |
| Blocage AUTOMATIQUE (scheduler) | ❌ Pas de `@Scheduled` job | ❌ | ❌ |

**Statut : ⚠️ PARTIEL**
> 📝 **À faire** : Créer un `@Scheduled` job qui bloque automatiquement les lots expirés

---

# ================================================================================
# 2. MOTEUR DE MOUVEMENTS (Cœur du système)
# ================================================================================

### Table Mouvements de Stock
| Élément | Code Java | BDD | Statut |
|---------|-----------|-----|--------|
| Table `mouvement_stock` | ✅ `MouvementStock.java` | ✅ `new_database.sql` L405-418 | ✅ |
| ID unique (UUID) | ✅ | ✅ | ✅ |
| Date/Heure (`created_at`) | ✅ | ✅ | ✅ |
| Utilisateur (`utilisateur_id`) | ✅ | ✅ | ✅ |
| Type mouvement | ✅ | ✅ `type_mouvement` table | ✅ |
| Article | ✅ | ✅ | ✅ |
| Quantité | ✅ | ✅ | ✅ |
| Emplacement (source/dest) | ✅ | ✅ | ✅ |
| Coût unitaire | ✅ | ✅ | ✅ |
| Référence document | ✅ | ✅ | ✅ |

**Statut : ✅ COMPLET**

---

### ✅ RÈGLE CRITIQUE : Historique NON modifiable / NON supprimable
| Élément | Code Java | BDD | Statut |
|---------|-----------|-----|---------|
| Annotation `@Immutable` | ✅ `@Immutable` sur `MouvementStock.java` | N/A | ✅ |
| Trigger BDD empêchant UPDATE/DELETE | N/A | ✅ `tr_mouvement_immutable` | ✅ |
| Blocage delete dans Repository | ✅ Override des méthodes delete | N/A | ✅ |

**Statut : ✅ COMPLET - SÉCURISÉ**
> ⚠️ **URGENT** : Les mouvements peuvent être modifiés/supprimés ! Ajouter :
> ```java
> @Entity
> @Immutable // Hibernate empêche les updates
> public class MouvementStock { ... }
> ```
> ET un trigger PostgreSQL :
> ```sql
> CREATE OR REPLACE FUNCTION prevent_mouvement_modification()
> RETURNS TRIGGER AS $$
> BEGIN
>     RAISE EXCEPTION 'Les mouvements de stock ne peuvent pas être modifiés ou supprimés';
> END;
> $$ LANGUAGE plpgsql;
> 
> CREATE TRIGGER tr_mouvement_immutable
> BEFORE UPDATE OR DELETE ON mouvement_stock
> FOR EACH ROW EXECUTE FUNCTION prevent_mouvement_modification();
> ```

---

### Numérotation séquentielle automatique
| Élément | Code Java | BDD | Statut |
|---------|-----------|-----|--------|
| Numéro séquentiel sur mouvement | ✅ Champ `numero` + `generateMouvementNumero()` | ✅ Colonne `numero VARCHAR(50)` | ✅ |
| Séquence PostgreSQL | ✅ `mouvement_stock_seq` | ✅ Créée | ✅ |
| Format MVT-AAMM-NNNNN | ✅ Ex: `MVT-2602-00001` | N/A | ✅ |

**Statut : ✅ COMPLET**

---

### Logique de Réservation
| Élément | Code Java | BDD | Statut |
|---------|-----------|-----|--------|
| Table `reservation_stock` | ✅ `ReservationStock.java` | ✅ `new_database.sql` L519-526 | ✅ |
| Champ `qty_reserve` sur Stock | ✅ `Stock.java` | ✅ | ✅ |
| Réservation auto à validation commande | ✅ `reserverStockPourLigne()` | N/A | ✅ |
| Libération à livraison | ✅ `validerLivraison()` | N/A | ✅ |
| Méthode `getQtyDisponible()` | ✅ `qtyReel - qtyReserve` | N/A | ✅ |
| Interdiction sortir stock réservé autre cmd | ⚠️ Vérifié via `getQtyDisponible` | N/A | ⚠️ |

**Statut : ✅ COMPLET** (logique de base OK)

---

### Traçabilité complète (Fournisseur → Lot → Client)
| Élément | Code Java | BDD | Statut |
|---------|-----------|-----|--------|
| Lien Lot → BonReception → CommandeAchat → Fournisseur | ✅ Relations | ✅ FK | ✅ |
| Lien Lot → LigneBonLivraison → CommandeClient → Client | ✅ Relations | ✅ FK | ✅ |
| Service de traçabilité dédié | ✅ `TracabiliteService.java` | N/A | ✅ |
| Méthode `getHistoriqueLot(lotId)` | ✅ | N/A | ✅ |
| Template traçabilité | ✅ `tracabilite-lot.html` | N/A | ✅ |

**Statut : ✅ COMPLET**

---

# ================================================================================
# 3. FONCTIONNALITÉS OPÉRATIONNELLES (Front-end Magasinier)
# ================================================================================

## 3.1 Module RÉCEPTION (Entrées)

| Élément | Code Java | Template | BDD | Statut |
|---------|-----------|----------|-----|--------|
| Table `bon_reception` | ✅ `BonReception.java` | N/A | ✅ | ✅ |
| Table `ligne_bon_reception` | ✅ `LigneBonReception.java` | N/A | ✅ | ✅ |
| Interface liée au BC | ✅ `commandeAchat` FK | ✅ `reception-form.html` | ✅ | ✅ |
| Champ Lot obligatoire | ✅ | ⚠️ Non validé côté front | ✅ | ⚠️ |
| Champ Date expiration | ✅ via Lot | ⚠️ | ✅ | ⚠️ |
| Champ Emplacement | ✅ | ⚠️ | ✅ | ⚠️ |
| Réceptions partielles | ✅ `qtyReceived` vs `qtyOrdered` | N/A | ✅ | ✅ |
| Génération BR | ✅ `generateReceptionNumero()` | N/A | ✅ | ✅ |
| Mise à jour stock auto | ✅ `validerReception()` | N/A | N/A | ✅ |
| Contrôle qualité | ✅ `ControleQualite.java` | ❌ Pas de template | ✅ | ⚠️ |

**Statut : ✅ QUASI COMPLET** (manque validation front + interface QC)

---

## 3.2 Module EXPÉDITION (Sorties)

| Élément | Code Java | Template | BDD | Statut |
|---------|-----------|----------|-----|--------|
| Table `bon_livraison` | ✅ `BonLivraison.java` | N/A | ✅ | ✅ |
| Table `ligne_bon_livraison` | ✅ `LigneBonLivraison.java` | N/A | ✅ | ✅ |
| Génération BL | ✅ `generateLivraisonNumero()` | N/A | ✅ | ✅ |
| Lien commande client | ✅ | ✅ `livraisons.html` | ✅ | ✅ |
| Déstockage automatique | ✅ `validerLivraison()` | N/A | N/A | ✅ |
| **Interface de Préparation (Picking)** | ❌ NON IMPLÉMENTÉ | ❌ | N/A | ❌ |
| **Liste de picking générée** | ❌ | ❌ | N/A | ❌ |
| **Scan validation Article/Lot/Empl** | ❌ | ❌ | N/A | ❌ |
| **Statut PREPARATION** | ✅ `preparerCommande()` | N/A | ✅ | ✅ |

**Statut : ⚠️ PARTIEL**
> 📝 **À faire** :
> - Créer template `picking.html` avec liste des articles à préparer
> - Ajouter interface de scan (ou saisie manuelle avec validation)
> - Workflow : Commande CONFIRMÉE → PRÉPARATION → BL créé → EXPÉDIÉE

---

## 3.3 Module TRANSFERTS INTERNES

| Élément | Code Java | Template | BDD | Statut |
|---------|-----------|----------|-----|--------|
| Type mouvement TRANSFERT | ✅ | N/A | ✅ `type_mouvement` | ✅ |
| Transfert direct | ✅ `transfererStock()` | ✅ `transfert-form.html` | N/A | ✅ |
| Mouvement enregistré | ✅ | N/A | ✅ | ✅ |
| **Demande de transfert** | ❌ NON IMPLÉMENTÉ | ❌ | ❌ Pas de table | ❌ |
| **Validation Chef avant exécution** | ❌ | ❌ | ❌ | ❌ |
| **Workflow Demande → Validation → Exécution** | ❌ | ❌ | ❌ | ❌ |
| **Double mouvement tracé (Sortie A + Entrée B)** | ✅ 2 mouvements créés | N/A | ✅ | ✅ |

**Statut : ⚠️ PARTIEL** (Double mouvement OK, workflow demande manquant)
> 📝 **À faire** :
> - Créer table `demande_transfert` avec statuts (BROUILLON, SOUMISE, APPROUVEE, EXECUTEE)
> - Ajouter workflow d'approbation

---

# ================================================================================
# 4. INVENTAIRES & CONTRÔLES
# ================================================================================

| Élément | Code Java | Template | BDD | Statut |
|---------|-----------|----------|-----|--------|
| Table `inventaire` | ✅ `Inventaire.java` | N/A | ✅ | ✅ |
| Table `ligne_inventaire` | ✅ `LigneInventaire.java` | N/A | ✅ | ✅ |
| Table `saisie_inventaire` | ✅ `SaisieInventaire.java` | N/A | ✅ | ✅ |
| Types : ANNUEL, TOURNANT, SPOT | ✅ `typeCode` | ✅ `inventaire-form.html` | ✅ CHECK constraint | ✅ |
| Workflow PLANIFIE→EN_COURS→ANALYSE→VALIDE | ✅ `InventaireService.java` | ✅ | ✅ `statut_inventaire` | ✅ |
| Calcul écarts automatique | ✅ `getEcartFinal()` | N/A | ✅ colonne GENERATED | ✅ |
| Multi-comptage (tours) | ✅ `tourComptage` | ⚠️ | ✅ | ✅ |
| Séparation des tâches (opérateur ≠ validateur) | ✅ `validerEcart()` vérifie | N/A | N/A | ✅ |
| Ajustements à la clôture | ✅ `cloturerInventaire()` | N/A | N/A | ✅ |
| **Mode "Saisie à l'aveugle"** | ❌ NON IMPLÉMENTÉ | ❌ | ❌ Pas de flag | ❌ |
| **Seuil écart validation auto** | ❌ NON IMPLÉMENTÉ | ❌ | ❌ | ❌ |
| **Seuil écart blocage Manager** | ❌ NON IMPLÉMENTÉ | ❌ | ❌ | ❌ |

**Statut : ⚠️ PARTIEL (70%)**
> 📝 **À faire** :
> - Ajouter champ `mode_aveugle BOOLEAN` sur inventaire
> - Modifier template pour cacher `qty_theorique` si mode aveugle activé
> - Ajouter table/config `seuil_ecart_inventaire` avec `auto_validation_threshold` et `manager_approval_threshold`

---

# ================================================================================
# 5. VALORISATION & FINANCE
# ================================================================================

| Élément | Code Java | BDD | Statut |
|---------|-----------|-----|--------|
| Table `methode_valorisation` | N/A | ✅ CUMP/FIFO/LIFO définis | ✅ BDD seule |
| Champ `unit_cost` sur mouvement | ✅ | ✅ | ✅ |
| **Calcul CUMP à chaque entrée** | ❌ NON IMPLÉMENTÉ | ❌ Pas de colonne `cump` sur stock | ❌ |
| **Pile de coûts FIFO** | ❌ NON IMPLÉMENTÉ | ❌ Pas de table | ❌ |
| **Clôture mensuelle** | ❌ NON IMPLÉMENTÉ | ❌ | ❌ |
| **Gel des mouvements période passée** | ❌ | ❌ | ❌ |
| **Gel valeur du stock** | ❌ | ❌ | ❌ |
| **Rapport écart valorisation** | ❌ | ❌ | ❌ |

**Statut : ❌ NON IMPLÉMENTÉ (10%)**
> ⚠️ **Critique pour la comptabilité !**
> 📝 **À faire** :
> - Ajouter colonne `cump DECIMAL(19,4)` sur table `stock`
> - Créer table `pile_fifo` (stock_id, date_entree, qty_restante, unit_cost)
> - Créer table `cloture_stock` (periode, date_cloture, valeur_totale, est_cloturee)
> - Implémenter `ValorisationService` avec calculs CUMP et FIFO
> - Ajouter trigger/check empêchant mouvements sur période clôturée

---

# ================================================================================
# 6. GESTION DES RÔLES & SÉCURITÉ (RBAC)
# ================================================================================

### Rôles définis
| Rôle | Code Java | BDD | Statut |
|------|-----------|-----|--------|
| ADMIN | ✅ `SecurityConfig.java` | ✅ `role` table | ✅ |
| MAGASINIER | ✅ | ⚠️ Manque (OPERATEUR utilisé) | ⚠️ |
| MANAGER_STOCK | ✅ | ⚠️ Manque (SUPERVISEUR utilisé) | ⚠️ |
| ACHETEUR | ✅ | ⚠️ | ⚠️ |
| COMMERCIAL | ✅ | ⚠️ | ⚠️ |

**Statut : ⚠️ PARTIEL**
> 📝 Les rôles en BDD sont génériques (OPERATEUR, MANAGER). Le code Java attend des rôles spécifiques (MAGASINIER, etc.)

---

### Restrictions par rôle

| Règle | Implémentation | Statut |
|-------|----------------|--------|
| MAGASINIER : Réception ✓ | ✅ URL `/stock/**` | ✅ |
| MAGASINIER : Picking ✓ | ✅ | ✅ |
| MAGASINIER : Transfert saisie ✓ | ✅ | ✅ |
| MAGASINIER : Validation financière ✗ | ⚠️ Pas de contrôle fin | ⚠️ |
| MAGASINIER : Ajustement manuel ✗ | ❌ NON VÉRIFIÉ | ❌ |
| MAGASINIER : Modification prix ✗ | ❌ NON VÉRIFIÉ | ❌ |
| CHEF MAGASIN : Validation transferts | ❌ Pas de workflow | ❌ |
| CHEF MAGASIN : Création inventaires | ✅ | ✅ |
| ACHAT/VENTE : Lecture seule stock | ⚠️ Accès URL complet | ⚠️ |

**Statut : ⚠️ PARTIEL**
> 📝 **À faire** : Ajouter `@PreAuthorize` sur les méthodes sensibles

---

# ================================================================================
# 7. TABLEAUX DE BORD & KPIs
# ================================================================================

### Dashboard Opérationnel
| KPI | Code Java | Template | Statut |
|-----|-----------|----------|--------|
| Articles en stock | ✅ `getKPIsStock()` | ✅ `dashboard.html` | ✅ |
| Articles faible stock | ✅ Seuil fixe 10 | ✅ | ✅ |
| Lots périmés | ✅ | ✅ | ✅ |
| Lots expirant bientôt (30j) | ✅ | ✅ | ✅ |
| Inventaires en cours | ✅ | ✅ | ✅ |
| **Réceptions du jour** | ❌ NON IMPLÉMENTÉ | ❌ | ❌ |
| **Réceptions en retard** | ❌ | ❌ | ❌ |
| **Commandes à préparer (priorité)** | ❌ | ❌ | ❌ |

**Statut : ⚠️ PARTIEL (50%)**

---

### Dashboard de Pilotage (KPIs avancés)
| KPI | Code Java | Statut |
|-----|-----------|--------|
| **Taux de précision du stock (%)** | ❌ NON IMPLÉMENTÉ | ❌ |
| **Valeur stock totale** | ⚠️ `getStockValueByDepot` incomplet | ⚠️ |
| **Valeur stock dormant** | ❌ | ❌ |
| **Valeur stock obsolète** | ❌ | ❌ |
| **Rotation du stock** | ❌ | ❌ |

**Statut : ❌ NON IMPLÉMENTÉ (10%)**

---

# ================================================================================
# 📊 RÉSUMÉ GLOBAL
# ================================================================================

| Section | Score | Statut |
|---------|-------|--------|
| 1. Architecture & Données | **65%** | ⚠️ |
| 2. Moteur Mouvements | **70%** | ⚠️ (🔧 problème critique) |
| 3.1 Réception | **90%** | ✅ |
| 3.2 Expédition | **50%** | ⚠️ |
| 3.3 Transferts | **40%** | ⚠️ |
| 4. Inventaires | **70%** | ⚠️ |
| 5. Valorisation | **10%** | ❌ |
| 6. Rôles & Sécurité | **60%** | ⚠️ |
| 7. KPIs | **40%** | ⚠️ |

## **Score global estimé : ~55%**

---

# 🚨 ACTIONS PRIORITAIRES

## 🔴 CRITIQUE (Sécurité/Intégrité)
1. [ ] Protéger `mouvement_stock` contre modification/suppression (trigger + @Immutable)
2. [ ] Ajouter rôles spécifiques en BDD (MAGASINIER, MANAGER_STOCK, etc.)

## 🟠 HAUTE PRIORITÉ (Fonctionnel core)
3. [ ] Implémenter valorisation CUMP (calcul + stockage)
4. [ ] Créer workflow demande de transfert avec validation
5. [ ] Compléter module picking/expédition

## 🟡 MOYENNE PRIORITÉ
6. [ ] Ajouter mode inventaire "à l'aveugle"
7. [ ] Implémenter seuils d'écart inventaire
8. [ ] Ajouter scheduler blocage lots expirés
9. [ ] Créer service traçabilité lot complète

## 🟢 BASSE PRIORITÉ
10. [ ] KPIs avancés (rotation, précision)
11. [ ] Unités de mesure multiples + conversions
12. [ ] Allocation FIFO (en plus de FEFO)

---

*Document généré automatiquement - Analyse du code source et de la BDD*
