# 📦 DONNÉES DE TEST - Application AVS (Achat-Vente-Stock)
## Guide de test avec les données pré-chargées

---

# 🔐 1. COMPTES UTILISATEURS (LOGIN)

| Username | Email | Mot de passe | Département | Rôle | Périmètre |
|----------|-------|--------------|-------------|------|-----------|
| `admin` | admin@madadis.mg | `admin` | IT | ADMIN | Accès total |
| `andry.dg` | dg@madadis.mg | `hash1` | Direction | DIRECTEUR | Accès total, approbation illimitée |
| `bakoly.daf` | daf@madadis.mg | `hash2` | Finance | MANAGER | Accès total, approbation 50M MGA |
| `faly.ach` | achat.mgr@madadis.mg | `hash4` | Achats | MANAGER | Site Tana, approbation 50M MGA |
| `sitraka.ach` | achat.op@madadis.mg | `hash5` | Achats | OPERATEUR | Site Tana, pas d'approbation |
| `tiana.stock` | stock.chef@madadis.mg | `hash6` | Magasin | SUPERVISEUR | Dépôt Tanjombato |
| `koto.stock` | magasinier@madadis.mg | `hash7` | Magasin | OPERATEUR | Dépôt Tanjombato |
| `soa.vte` | sales.mgr@madadis.mg | `hash8` | Ventes | MANAGER | Site Tana |
| `rivo.vte` | commercial@madadis.mg | `hash9` | Ventes | OPERATEUR | Site Tana |

### 🔑 Pour tester le login :
```
Username: admin
Password: admin
```

> ⚠️ **Note** : Les mots de passe `hash1`, `hash2`, etc. sont des placeholders. 
> En production, ils devraient être hashés avec BCrypt. Pour les tests, utiliser `admin/admin`.

---

# 🏢 2. ORGANISATION

## Groupe & Société
| Code | Nom |
|------|-----|
| `GRP_MADA` | Groupe Malagasy Distribution |
| `MADA_DIS` | Mada Distribution SA (NIF 3000123456) |

## Sites
| Code | Nom | Adresse | Coordonnées GPS |
|------|-----|---------|-----------------|
| `SITE_ANDRA` | Siège Andraharo | Zone Galaxy, Antananarivo 101 | -18.88, 47.51 |
| `SITE_TMM` | Hub Logistique Toamasina | Bd Joffre, Toamasina | -18.15, 49.40 |

## Dépôts
| Code | Nom | Site |
|------|-----|------|
| `DEP_TANJO` | Entrepôt Central Tanjombato | SITE_ANDRA |
| `DEP_PORT` | Entrepôt Douane Port | SITE_TMM |

## Emplacements (Dépôt Tanjombato)
| Code | Allée | Rack | Étagère | Usage |
|------|-------|------|---------|-------|
| `A-01-01` | A | 01 | 01 | Stock normal |
| `A-01-02` | A | 01 | 02 | Stock normal |
| `QUARANTAINE` | Z | 99 | 99 | Zone quarantaine |

---

# 🏷️ 3. RÉFÉRENTIELS

## Devises
| Code | Libellé | Symbole |
|------|---------|---------|
| `MGA` | Ariary Malgache | Ar |
| `EUR` | Euro | € |
| `USD` | US Dollar | $ |
| `CNY` | Yuan Chinois | CNY |

## Unités de Mesure
| Code | Libellé |
|------|---------|
| `PCE` | Pièce |
| `KG` | Kilogramme |
| `L` | Litre |
| `H` | Heure |
| `BOX` | Carton |
| `PAL` | Palette |

## Taxes
| Code | Libellé | Taux |
|------|---------|------|
| `TVA_20` | TVA 20% | 20% |
| `EXO` | Exonéré | 0% |
| `AIRSI_5` | AIRSI 5% | 5% |

## Modes de Paiement
| Code | Libellé |
|------|---------|
| `VIREMENT` | Virement Bancaire |
| `CHEQUE` | Chèque |
| `ESPECES` | Espèces |
| `MOBILE` | Mobile Money |
| `ORANGE_MONEY` | Orange Money |
| `LCR_30` | Lettre de Change 30j |

## Méthodes de Valorisation
| Code | Libellé |
|------|---------|
| `CUMP` | Coût Unitaire Moyen Pondéré |
| `FIFO` | Premier Entré Premier Sorti |
| `LIFO` | Dernier Entré Premier Sorti |

---

# 📦 4. ARTICLES & FAMILLES

## Familles d'Articles
| Code | Nom | Valorisation | Lot obligatoire | Péremption obligatoire |
|------|-----|--------------|-----------------|------------------------|
| `PPN` | Produits Première Nécessité | CUMP | ✅ Oui | Non |
| `HITECH` | Informatique & Technologie | FIFO | ✅ Oui | Non |
| `BOISSON` | Boissons & Liquides | FIFO | Non | Non |
| `HYGIENE` | Hygiène & Beauté | CUMP | Non | Non |

## Articles
| SKU | Désignation | Famille | Unité | Poids | Taxe Vente | Taxe Achat |
|-----|-------------|---------|-------|-------|------------|------------|
| `RIZ-LUX-50` | Riz Luxury 50kg | PPN | KG | 50.0 kg | EXO | EXO |
| `HP-PROBOOK` | HP Probook 450 G9 | HITECH | PCE | 2.5 kg | TVA_20 | TVA_20 |
| `COCA-15L` | Coca-Cola 1.5L | BOISSON | PCE | - | TVA_20 | TVA_20 |

---

# 👥 5. TIERS (CLIENTS & FOURNISSEURS)

## Fournisseurs
| Code | Nom | NIF | Devise |
|------|-----|-----|--------|
| `FRS_CHINA` | Shenzhen Tech Export | CN-8899 | USD |
| `FRS_TIKO` | Tiko Agri | NIF 111222 | MGA |
| `FRS_STAR` | STAR Madagascar | NIF 777111 | MGA |

## Clients
| Code | Nom | NIF | Devise |
|------|-----|-----|--------|
| `CLI_JUMBO` | Jumbo Score | NIF 999888 | MGA |
| `CLI_SHOP` | Supermaki | NIF 777666 | MGA |

---

# 📋 6. FLUX ACHATS (Données de test)

## Demande d'Achat
| Numéro | Demandeur | Site | Statut |
|--------|-----------|------|--------|
| `DA-2401-001` | sitraka.ach | SITE_ANDRA | ✅ APPROUVÉE |

> **Workflow effectué** : SOUMISE → APPROUVÉE par faly.ach

## Commande d'Achat
| Numéro | Fournisseur | Montant HT | Montant TTC | Statut | Date |
|--------|-------------|------------|-------------|--------|------|
| `BC-2401-088` | FRS_TIKO (Tiko Agri) | 9 500 000 MGA | 9 500 000 MGA | ENVOYÉE | J-8 |

### Lignes de la commande :
| Article | Quantité | Prix Unitaire | Taxe |
|---------|----------|---------------|------|
| RIZ-LUX-50 | 100 | 95 000 MGA | EXO |

---

# 📥 7. RÉCEPTIONS (Données de test)

## Bon de Réception
| Numéro | Commande | Dépôt | Statut | Date |
|--------|----------|-------|--------|------|
| `BR-2401-088` | BC-2401-088 | DEP_TANJO | ✅ VALIDÉ | J-5 |

### Lignes réceptionnées :
| Article | Lot | Emplacement | Quantité |
|---------|-----|-------------|----------|
| RIZ-LUX-50 | LOT-RIZ-DEC23 | A-01-01 | 100 |

## Lot créé
| Numéro Lot | Article | Date Fabrication | Date Péremption | Statut Qualité |
|------------|---------|------------------|-----------------|----------------|
| `LOT-RIZ-DEC23` | RIZ-LUX-50 | 01/12/2023 | 01/12/2025 | ✅ CONFORME |

---

# 📊 8. ÉTAT DU STOCK (Données de test)

## Stock actuel (après réception et vente)
| Dépôt | Emplacement | Article | Lot | Qty Réelle | Qty Réservée | Qty Disponible |
|-------|-------------|---------|-----|------------|--------------|----------------|
| DEP_TANJO | A-01-01 | RIZ-LUX-50 | LOT-RIZ-DEC23 | **90** | **0** | **90** |

> **Calcul** : 100 reçus - 10 vendus = 90 restants

## Mouvements de stock enregistrés
| Type | Référence | Article | Lot | Dépôt | Qty | Coût Unit. | Utilisateur |
|------|-----------|---------|-----|-------|-----|------------|-------------|
| RECEPTION | BR-2401-088 | RIZ-LUX-50 | LOT-RIZ-DEC23 | → DEP_TANJO | +100 | 95 000 | tiana.stock |
| EXPEDITION | BL-CLI-500 | RIZ-LUX-50 | LOT-RIZ-DEC23 | DEP_TANJO → | -10 | - | koto.stock |

---

# 🛒 9. FLUX VENTES (Données de test)

## Commande Client
| Numéro | Client | Site | Statut |
|--------|--------|------|--------|
| `CMD-CLI-500` | CLI_JUMBO (Jumbo Score) | SITE_ANDRA | CONFIRMÉE |

### Lignes de la commande :
| Article | Quantité | Prix Unitaire |
|---------|----------|---------------|
| RIZ-LUX-50 | 10 | 120 000 MGA |

## Réservation de Stock
| Commande | Article | Dépôt | Lot | Qty Réservée |
|----------|---------|-------|-----|--------------|
| CMD-CLI-500 | RIZ-LUX-50 | DEP_TANJO | LOT-RIZ-DEC23 | 10 |

## Bon de Livraison
| Numéro | Commande | Date Expédition |
|--------|----------|-----------------|
| `BL-CLI-500` | CMD-CLI-500 | Aujourd'hui |

### Lignes livrées :
| Article | Lot | Quantité |
|---------|-----|----------|
| RIZ-LUX-50 | LOT-RIZ-DEC23 | 10 |

---

# 🔍 10. INVENTAIRE (Données de test)

## Inventaire en cours
| Numéro | Type | Dépôt | Statut | Créé par | Date planif. |
|--------|------|-------|--------|----------|--------------|
| `INV-SPOT-001` | SPOT | DEP_TANJO | 🔄 ANALYSE | bakoly.daf | Aujourd'hui |

### Ligne d'inventaire avec écart :
| Article | Emplacement | Lot | Qty Théorique | Qty Comptée | Écart | Arbitré par | Notes |
|---------|-------------|-----|---------------|-------------|-------|-------------|-------|
| RIZ-LUX-50 | A-01-01 | LOT-RIZ-DEC23 | 90 | **88** | **-2** | andry.dg | Écart 2 sacs - Enquête vol potentiel |

### Saisie de comptage :
| Opérateur | Quantité comptée |
|-----------|------------------|
| koto.stock | 88 |

---

# 💰 11. FACTURES (Données de test)

## Facture Fournisseur
| Réf Interne | Réf Fournisseur | Fournisseur | Montant TTC | Statut | Date |
|-------------|-----------------|-------------|-------------|--------|------|
| `FAC-AGRI-001` | INV-12345 | FRS_TIKO | 9 500 000 MGA | 🔴 À PAYER | J-4 |

## Rapprochement (3-way match)
| Facture | Réception | Montant rapproché |
|---------|-----------|-------------------|
| FAC-AGRI-001 | BR-2401-088 | 9 500 000 MGA |

---

# 📝 12. AUDIT & WORKFLOW (Données de test)

## Historique Workflow
| Document | Étape | Acteur | Action | Commentaire |
|----------|-------|--------|--------|-------------|
| DA-2401-001 | → SOUMISE | sitraka.ach | SOUMISSION | Réappro stock urgence |
| DA-2401-001 | → APPROUVÉE | faly.ach | APPROBATION | OK Budget |

## Journal d'Audit
| Entité | Action | Utilisateur | Détails |
|--------|--------|-------------|---------|
| STOCK | ECART_INVENTAIRE | bakoly.daf | Détection écart de -2 unités sur Riz Luxury |

---

# 🧪 SCÉNARIOS DE TEST SUGGÉRÉS

## 1️⃣ Test Login
```
1. Ouvrir http://localhost:8080/login
2. Saisir : admin / admin
3. Vérifier redirection vers /dashboard
```

## 2️⃣ Test Consultation Stock
```
1. Se connecter avec tiana.stock
2. Aller sur /stock
3. Vérifier : RIZ-LUX-50 avec qty=90 dans dépôt DEP_TANJO
```

## 3️⃣ Test Création Réception
```
1. Se connecter avec tiana.stock
2. Aller sur /stock/receptions/add
3. Sélectionner une commande existante (BC-2401-088 déjà traitée - créer nouvelle)
4. Ajouter ligne avec article HP-PROBOOK ou COCA-15L
```

## 4️⃣ Test Inventaire
```
1. Se connecter avec bakoly.daf
2. Aller sur /inventaires
3. Voir l'inventaire INV-SPOT-001 en statut ANALYSE
4. Valider ou clôturer l'inventaire
```

## 5️⃣ Test Commande Client
```
1. Se connecter avec rivo.vte
2. Aller sur /ventes/commandes
3. Voir CMD-CLI-500 en statut CONFIRMÉE
4. Tester préparation et livraison
```

## 6️⃣ Test Transfert Stock
```
1. Se connecter avec tiana.stock
2. Aller sur /stock/transferts/add
3. Transférer du RIZ-LUX-50 de DEP_TANJO vers DEP_PORT
```

---

# ⚙️ RÈGLES D'APPROBATION CONFIGURÉES

| Document | Montant Min | Montant Max | Rôle requis | Niveau |
|----------|-------------|-------------|-------------|--------|
| DEMANDE_ACHAT | 0 | 50 000 000 MGA | MANAGER | 1 |
| DEMANDE_ACHAT | 50 000 000 | 9 999 999 999 MGA | DIRECTEUR | 2 |

---

*Document généré le 01/02/2026 - Basé sur new_data.sql*
