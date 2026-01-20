-- ============================================
-- ERP Achats / Ventes / Stock / Inventaires
-- Base de données PostgreSQL
-- ============================================

-- Extension pour UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- 1. TABLES REFERENTIELS
-- ============================================

-- Sites / Entités légales
CREATE TABLE sites (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    nom VARCHAR(100) NOT NULL,
    adresse TEXT,
    ville VARCHAR(100),
    pays VARCHAR(50),
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Dépôts / Entrepôts
CREATE TABLE depots (
    id SERIAL PRIMARY KEY,
    site_id INTEGER REFERENCES sites(id),
    code VARCHAR(20) UNIQUE NOT NULL,
    nom VARCHAR(100) NOT NULL,
    adresse TEXT,
    type_depot VARCHAR(50), -- principal, transit, quarantaine
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Emplacements dans les dépôts
CREATE TABLE emplacements (
    id SERIAL PRIMARY KEY,
    depot_id INTEGER REFERENCES depots(id),
    code VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    zone VARCHAR(50),
    capacite_max DECIMAL(15,3),
    actif BOOLEAN DEFAULT TRUE,
    UNIQUE(depot_id, code)
);

-- Unités de mesure
CREATE TABLE unites (
    id SERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    libelle VARCHAR(50) NOT NULL,
    type_unite VARCHAR(30) -- quantite, poids, volume
);

-- Familles d'articles
CREATE TABLE familles_articles (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    famille_parent_id INTEGER REFERENCES familles_articles(id),
    tracabilite_lot_obligatoire BOOLEAN DEFAULT FALSE,
    methode_valorisation VARCHAR(10) DEFAULT 'CUMP', -- FIFO, CUMP
    methode_sortie VARCHAR(10) DEFAULT 'FIFO' -- FIFO, FEFO
);

-- Articles
CREATE TABLE articles (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    designation VARCHAR(200) NOT NULL,
    description TEXT,
    famille_id INTEGER REFERENCES familles_articles(id),
    unite_stock_id INTEGER REFERENCES unites(id),
    unite_achat_id INTEGER REFERENCES unites(id),
    unite_vente_id INTEGER REFERENCES unites(id),
    coef_achat DECIMAL(10,4) DEFAULT 1,
    coef_vente DECIMAL(10,4) DEFAULT 1,
    prix_achat_standard DECIMAL(15,4),
    prix_vente_standard DECIMAL(15,4),
    stock_minimum DECIMAL(15,3) DEFAULT 0,
    stock_maximum DECIMAL(15,3),
    stock_securite DECIMAL(15,3) DEFAULT 0,
    delai_appro_jours INTEGER DEFAULT 0,
    perissable BOOLEAN DEFAULT FALSE,
    gestion_lot BOOLEAN DEFAULT FALSE,
    gestion_serie BOOLEAN DEFAULT FALSE,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Taxes (TVA, etc.)
CREATE TABLE taxes (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    taux DECIMAL(5,2) NOT NULL,
    type_taxe VARCHAR(30), -- TVA, taxe_import
    actif BOOLEAN DEFAULT TRUE
);

-- Fournisseurs
CREATE TABLE fournisseurs (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL,
    raison_sociale VARCHAR(200) NOT NULL,
    adresse TEXT,
    ville VARCHAR(100),
    pays VARCHAR(50),
    telephone VARCHAR(30),
    email VARCHAR(100),
    site_web VARCHAR(200),
    nif VARCHAR(50),
    stat VARCHAR(50),
    delai_paiement_jours INTEGER DEFAULT 30,
    conditions_paiement TEXT,
    note_qualite INTEGER, -- 1-5
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Clients
CREATE TABLE clients (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL,
    raison_sociale VARCHAR(200) NOT NULL,
    adresse TEXT,
    ville VARCHAR(100),
    pays VARCHAR(50),
    telephone VARCHAR(30),
    email VARCHAR(100),
    nif VARCHAR(50),
    stat VARCHAR(50),
    delai_paiement_jours INTEGER DEFAULT 30,
    plafond_credit DECIMAL(15,2),
    remise_globale DECIMAL(5,2) DEFAULT 0,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tarifs fournisseurs
CREATE TABLE tarifs_fournisseurs (
    id SERIAL PRIMARY KEY,
    fournisseur_id INTEGER REFERENCES fournisseurs(id),
    article_id INTEGER REFERENCES articles(id),
    prix_unitaire DECIMAL(15,4) NOT NULL,
    quantite_min DECIMAL(15,3) DEFAULT 1,
    date_debut DATE NOT NULL,
    date_fin DATE,
    devise VARCHAR(3) DEFAULT 'MGA',
    UNIQUE(fournisseur_id, article_id, date_debut)
);

-- Tarifs clients
CREATE TABLE tarifs_clients (
    id SERIAL PRIMARY KEY,
    client_id INTEGER REFERENCES clients(id),
    article_id INTEGER REFERENCES articles(id),
    prix_unitaire DECIMAL(15,4) NOT NULL,
    quantite_min DECIMAL(15,3) DEFAULT 1,
    date_debut DATE NOT NULL,
    date_fin DATE,
    devise VARCHAR(3) DEFAULT 'MGA'
);

-- ============================================
-- 2. GESTION DES UTILISATEURS ET ROLES
-- ============================================

-- Départements
CREATE TABLE departements (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    departement_parent_id INTEGER REFERENCES departements(id)
);

-- Utilisateurs
CREATE TABLE utilisateurs (
    id SERIAL PRIMARY KEY,
    matricule VARCHAR(30) UNIQUE NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100),
    email VARCHAR(150) UNIQUE NOT NULL,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    departement_id INTEGER REFERENCES departements(id),
    site_id INTEGER REFERENCES sites(id),
    responsable_id INTEGER REFERENCES utilisateurs(id),
    niveau_hierarchique INTEGER DEFAULT 1, -- 1=Operateur, 2=Superviseur, 3=Manager, 4=Directeur
    actif BOOLEAN DEFAULT TRUE,
    date_derniere_connexion TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Rôles
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    description TEXT,
    niveau_approbation INTEGER DEFAULT 0
);

-- Permissions
CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL, -- ACHAT, VENTE, STOCK, INVENTAIRE, ADMIN
    description TEXT
);

-- Association rôles-permissions
CREATE TABLE roles_permissions (
    role_id INTEGER REFERENCES roles(id),
    permission_id INTEGER REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

-- Association utilisateurs-rôles
CREATE TABLE utilisateurs_roles (
    utilisateur_id INTEGER REFERENCES utilisateurs(id),
    role_id INTEGER REFERENCES roles(id),
    date_debut DATE DEFAULT CURRENT_DATE,
    date_fin DATE,
    PRIMARY KEY (utilisateur_id, role_id, date_debut)
);

-- Restrictions ABAC (par attributs)
CREATE TABLE restrictions_utilisateurs (
    id SERIAL PRIMARY KEY,
    utilisateur_id INTEGER REFERENCES utilisateurs(id),
    type_restriction VARCHAR(50), -- SITE, DEPOT, FAMILLE, MONTANT
    valeur_restriction VARCHAR(100),
    montant_max DECIMAL(15,2)
);

-- Délégations temporaires
CREATE TABLE delegations (
    id SERIAL PRIMARY KEY,
    delegant_id INTEGER REFERENCES utilisateurs(id),
    delegataire_id INTEGER REFERENCES utilisateurs(id),
    role_id INTEGER REFERENCES roles(id),
    date_debut TIMESTAMP NOT NULL,
    date_fin TIMESTAMP NOT NULL,
    justification TEXT NOT NULL,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 3. MODULE ACHATS
-- ============================================

-- Demandes d'achat (DA)
CREATE TABLE demandes_achat (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_demande DATE DEFAULT CURRENT_DATE,
    demandeur_id INTEGER REFERENCES utilisateurs(id),
    departement_id INTEGER REFERENCES departements(id),
    site_id INTEGER REFERENCES sites(id),
    motif TEXT,
    date_besoin DATE,
    priorite VARCHAR(20) DEFAULT 'NORMALE', -- BASSE, NORMALE, HAUTE, URGENTE
    statut VARCHAR(30) DEFAULT 'BROUILLON', -- BROUILLON, SOUMIS, EN_APPROBATION, APPROUVE, REJETE, ANNULE
    montant_total DECIMAL(15,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Lignes demandes d'achat
CREATE TABLE lignes_demande_achat (
    id SERIAL PRIMARY KEY,
    demande_id INTEGER REFERENCES demandes_achat(id) ON DELETE CASCADE,
    article_id INTEGER REFERENCES articles(id),
    quantite DECIMAL(15,3) NOT NULL,
    unite_id INTEGER REFERENCES unites(id),
    prix_estime DECIMAL(15,4),
    fournisseur_suggere_id INTEGER REFERENCES fournisseurs(id),
    commentaire TEXT
);

-- Workflow approbations
CREATE TABLE approbations (
    id SERIAL PRIMARY KEY,
    type_document VARCHAR(30) NOT NULL, -- DA, BC, AJUSTEMENT, AVOIR
    document_id INTEGER NOT NULL,
    niveau_approbation INTEGER NOT NULL,
    approbateur_id INTEGER REFERENCES utilisateurs(id),
    statut VARCHAR(20) DEFAULT 'EN_ATTENTE', -- EN_ATTENTE, APPROUVE, REJETE
    date_action TIMESTAMP,
    commentaire TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Pro-forma fournisseurs
CREATE TABLE proforma_fournisseurs (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    demande_achat_id INTEGER REFERENCES demandes_achat(id),
    fournisseur_id INTEGER REFERENCES fournisseurs(id),
    date_proforma DATE,
    date_validite DATE,
    montant_ht DECIMAL(15,2),
    montant_tva DECIMAL(15,2),
    montant_ttc DECIMAL(15,2),
    devise VARCHAR(3) DEFAULT 'MGA',
    statut VARCHAR(20) DEFAULT 'RECU', -- RECU, SELECTIONNE, REJETE
    fichier_joint VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bons de commande fournisseur (BC)
CREATE TABLE commandes_fournisseur (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_commande DATE DEFAULT CURRENT_DATE,
    fournisseur_id INTEGER REFERENCES fournisseurs(id),
    demande_achat_id INTEGER REFERENCES demandes_achat(id),
    proforma_id INTEGER REFERENCES proforma_fournisseurs(id),
    acheteur_id INTEGER REFERENCES utilisateurs(id),
    site_id INTEGER REFERENCES sites(id),
    depot_id INTEGER REFERENCES depots(id),
    date_livraison_prevue DATE,
    conditions_paiement TEXT,
    montant_ht DECIMAL(15,2) DEFAULT 0,
    montant_tva DECIMAL(15,2) DEFAULT 0,
    montant_ttc DECIMAL(15,2) DEFAULT 0,
    devise VARCHAR(3) DEFAULT 'MGA',
    statut VARCHAR(30) DEFAULT 'BROUILLON', -- BROUILLON, EN_APPROBATION, APPROUVE, ENVOYE, PARTIEL, RECU, CLOS, ANNULE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Lignes commandes fournisseur
CREATE TABLE lignes_commande_fournisseur (
    id SERIAL PRIMARY KEY,
    commande_id INTEGER REFERENCES commandes_fournisseur(id) ON DELETE CASCADE,
    article_id INTEGER REFERENCES articles(id),
    quantite DECIMAL(15,3) NOT NULL,
    quantite_recue DECIMAL(15,3) DEFAULT 0,
    unite_id INTEGER REFERENCES unites(id),
    prix_unitaire DECIMAL(15,4) NOT NULL,
    taux_tva DECIMAL(5,2) DEFAULT 0,
    remise_pourcent DECIMAL(5,2) DEFAULT 0,
    montant_ht DECIMAL(15,2),
    montant_tva DECIMAL(15,2),
    montant_ttc DECIMAL(15,2)
);

-- Bons de réception
CREATE TABLE bons_reception (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_reception DATE DEFAULT CURRENT_DATE,
    commande_id INTEGER REFERENCES commandes_fournisseur(id),
    fournisseur_id INTEGER REFERENCES fournisseurs(id),
    depot_id INTEGER REFERENCES depots(id),
    receptionnaire_id INTEGER REFERENCES utilisateurs(id),
    numero_bl_fournisseur VARCHAR(50),
    date_bl_fournisseur DATE,
    statut VARCHAR(20) DEFAULT 'BROUILLON', -- BROUILLON, VALIDE, ANNULE
    commentaire TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Lignes bons de réception
CREATE TABLE lignes_bon_reception (
    id SERIAL PRIMARY KEY,
    bon_reception_id INTEGER REFERENCES bons_reception(id) ON DELETE CASCADE,
    ligne_commande_id INTEGER REFERENCES lignes_commande_fournisseur(id),
    article_id INTEGER REFERENCES articles(id),
    quantite_attendue DECIMAL(15,3),
    quantite_recue DECIMAL(15,3) NOT NULL,
    quantite_conforme DECIMAL(15,3),
    quantite_non_conforme DECIMAL(15,3) DEFAULT 0,
    unite_id INTEGER REFERENCES unites(id),
    emplacement_id INTEGER REFERENCES emplacements(id),
    lot_id INTEGER, -- FK vers lots
    commentaire TEXT
);

-- Factures fournisseur
CREATE TABLE factures_fournisseur (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    numero_facture_fournisseur VARCHAR(50),
    date_facture DATE,
    date_reception_facture DATE DEFAULT CURRENT_DATE,
    fournisseur_id INTEGER REFERENCES fournisseurs(id),
    commande_id INTEGER REFERENCES commandes_fournisseur(id),
    montant_ht DECIMAL(15,2),
    montant_tva DECIMAL(15,2),
    montant_ttc DECIMAL(15,2),
    devise VARCHAR(3) DEFAULT 'MGA',
    date_echeance DATE,
    statut VARCHAR(30) DEFAULT 'BROUILLON', -- BROUILLON, A_RAPPROCHER, VALIDE, PAYE, LITIGE, ANNULE
    statut_rapprochement VARCHAR(20), -- OK, ECART_QTE, ECART_PRIX
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Paiements fournisseur
CREATE TABLE paiements_fournisseur (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_paiement DATE DEFAULT CURRENT_DATE,
    facture_id INTEGER REFERENCES factures_fournisseur(id),
    montant DECIMAL(15,2) NOT NULL,
    mode_paiement VARCHAR(30), -- VIREMENT, CHEQUE, ESPECES
    reference_paiement VARCHAR(100),
    banque VARCHAR(100),
    validateur_id INTEGER REFERENCES utilisateurs(id),
    statut VARCHAR(20) DEFAULT 'BROUILLON', -- BROUILLON, VALIDE, ANNULE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 4. MODULE VENTES
-- ============================================

-- Devis / Pro-forma clients
CREATE TABLE devis (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_devis DATE DEFAULT CURRENT_DATE,
    client_id INTEGER REFERENCES clients(id),
    commercial_id INTEGER REFERENCES utilisateurs(id),
    site_id INTEGER REFERENCES sites(id),
    date_validite DATE,
    montant_ht DECIMAL(15,2) DEFAULT 0,
    montant_tva DECIMAL(15,2) DEFAULT 0,
    montant_ttc DECIMAL(15,2) DEFAULT 0,
    remise_globale DECIMAL(5,2) DEFAULT 0,
    devise VARCHAR(3) DEFAULT 'MGA',
    statut VARCHAR(20) DEFAULT 'BROUILLON', -- BROUILLON, ENVOYE, ACCEPTE, REFUSE, EXPIRE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Lignes devis
CREATE TABLE lignes_devis (
    id SERIAL PRIMARY KEY,
    devis_id INTEGER REFERENCES devis(id) ON DELETE CASCADE,
    article_id INTEGER REFERENCES articles(id),
    quantite DECIMAL(15,3) NOT NULL,
    unite_id INTEGER REFERENCES unites(id),
    prix_unitaire DECIMAL(15,4) NOT NULL,
    remise_pourcent DECIMAL(5,2) DEFAULT 0,
    taux_tva DECIMAL(5,2) DEFAULT 0,
    montant_ht DECIMAL(15,2),
    montant_tva DECIMAL(15,2)
);

-- Commandes clients
CREATE TABLE commandes_client (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_commande DATE DEFAULT CURRENT_DATE,
    client_id INTEGER REFERENCES clients(id),
    devis_id INTEGER REFERENCES devis(id),
    commercial_id INTEGER REFERENCES utilisateurs(id),
    site_id INTEGER REFERENCES sites(id),
    depot_id INTEGER REFERENCES depots(id),
    date_livraison_souhaitee DATE,
    adresse_livraison TEXT,
    montant_ht DECIMAL(15,2) DEFAULT 0,
    montant_tva DECIMAL(15,2) DEFAULT 0,
    montant_ttc DECIMAL(15,2) DEFAULT 0,
    remise_globale DECIMAL(5,2) DEFAULT 0,
    statut VARCHAR(30) DEFAULT 'BROUILLON', -- BROUILLON, CONFIRMEE, EN_PREPARATION, PARTIEL, LIVREE, CLOS, ANNULEE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Lignes commandes clients
CREATE TABLE lignes_commande_client (
    id SERIAL PRIMARY KEY,
    commande_id INTEGER REFERENCES commandes_client(id) ON DELETE CASCADE,
    article_id INTEGER REFERENCES articles(id),
    quantite DECIMAL(15,3) NOT NULL,
    quantite_reservee DECIMAL(15,3) DEFAULT 0,
    quantite_livree DECIMAL(15,3) DEFAULT 0,
    unite_id INTEGER REFERENCES unites(id),
    prix_unitaire DECIMAL(15,4) NOT NULL,
    remise_pourcent DECIMAL(5,2) DEFAULT 0,
    taux_tva DECIMAL(5,2) DEFAULT 0,
    montant_ht DECIMAL(15,2),
    montant_tva DECIMAL(15,2)
);

-- Bons de livraison
CREATE TABLE bons_livraison (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_livraison DATE DEFAULT CURRENT_DATE,
    commande_id INTEGER REFERENCES commandes_client(id),
    client_id INTEGER REFERENCES clients(id),
    depot_id INTEGER REFERENCES depots(id),
    preparateur_id INTEGER REFERENCES utilisateurs(id),
    livreur VARCHAR(100),
    adresse_livraison TEXT,
    statut VARCHAR(20) DEFAULT 'BROUILLON', -- BROUILLON, VALIDE, LIVRE, ANNULE
    date_expedition TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Lignes bons de livraison
CREATE TABLE lignes_bon_livraison (
    id SERIAL PRIMARY KEY,
    bon_livraison_id INTEGER REFERENCES bons_livraison(id) ON DELETE CASCADE,
    ligne_commande_id INTEGER REFERENCES lignes_commande_client(id),
    article_id INTEGER REFERENCES articles(id),
    quantite DECIMAL(15,3) NOT NULL,
    unite_id INTEGER REFERENCES unites(id),
    emplacement_id INTEGER REFERENCES emplacements(id),
    lot_id INTEGER
);

-- Factures clients
CREATE TABLE factures_client (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_facture DATE DEFAULT CURRENT_DATE,
    client_id INTEGER REFERENCES clients(id),
    commande_id INTEGER REFERENCES commandes_client(id),
    bon_livraison_id INTEGER REFERENCES bons_livraison(id),
    montant_ht DECIMAL(15,2),
    montant_tva DECIMAL(15,2),
    montant_ttc DECIMAL(15,2),
    montant_paye DECIMAL(15,2) DEFAULT 0,
    devise VARCHAR(3) DEFAULT 'MGA',
    date_echeance DATE,
    statut VARCHAR(20) DEFAULT 'BROUILLON', -- BROUILLON, VALIDE, PARTIEL, PAYE, ANNULE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Avoirs clients
CREATE TABLE avoirs_client (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_avoir DATE DEFAULT CURRENT_DATE,
    facture_id INTEGER REFERENCES factures_client(id),
    client_id INTEGER REFERENCES clients(id),
    motif VARCHAR(100), -- RETOUR, ERREUR_PRIX, CASSE, REMISE
    montant_ht DECIMAL(15,2),
    montant_tva DECIMAL(15,2),
    montant_ttc DECIMAL(15,2),
    statut VARCHAR(20) DEFAULT 'BROUILLON',
    validateur_id INTEGER REFERENCES utilisateurs(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Encaissements clients
CREATE TABLE encaissements (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_encaissement DATE DEFAULT CURRENT_DATE,
    facture_id INTEGER REFERENCES factures_client(id),
    client_id INTEGER REFERENCES clients(id),
    montant DECIMAL(15,2) NOT NULL,
    mode_paiement VARCHAR(30), -- VIREMENT, CHEQUE, ESPECES, CARTE
    reference_paiement VARCHAR(100),
    banque VARCHAR(100),
    statut VARCHAR(20) DEFAULT 'BROUILLON',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 5. MODULE STOCKS
-- ============================================

-- Lots / Séries
CREATE TABLE lots (
    id SERIAL PRIMARY KEY,
    article_id INTEGER REFERENCES articles(id),
    numero_lot VARCHAR(50) NOT NULL,
    numero_serie VARCHAR(50),
    date_fabrication DATE,
    date_peremption DATE,
    dluo DATE, -- Date Limite d'Utilisation Optimale
    dlc DATE, -- Date Limite de Consommation
    statut VARCHAR(20) DEFAULT 'DISPONIBLE', -- DISPONIBLE, BLOQUE, QUARANTAINE, EXPIRE
    fournisseur_id INTEGER REFERENCES fournisseurs(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(article_id, numero_lot)
);

-- Stock par emplacement
CREATE TABLE stock_emplacement (
    id SERIAL PRIMARY KEY,
    article_id INTEGER REFERENCES articles(id),
    depot_id INTEGER REFERENCES depots(id),
    emplacement_id INTEGER REFERENCES emplacements(id),
    lot_id INTEGER REFERENCES lots(id),
    quantite DECIMAL(15,3) DEFAULT 0,
    quantite_reservee DECIMAL(15,3) DEFAULT 0,
    cout_unitaire DECIMAL(15,4),
    date_derniere_maj TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(article_id, depot_id, emplacement_id, lot_id)
);

-- Mouvements de stock
CREATE TABLE mouvements_stock (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_mouvement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    type_mouvement VARCHAR(30) NOT NULL, -- ENTREE, SORTIE, TRANSFERT, AJUSTEMENT
    motif VARCHAR(50), -- RECEPTION, LIVRAISON, RETOUR_CLIENT, RETOUR_FOUR, REBUT, CONSO_INTERNE, AJUST_POS, AJUST_NEG
    article_id INTEGER REFERENCES articles(id),
    lot_id INTEGER REFERENCES lots(id),
    depot_source_id INTEGER REFERENCES depots(id),
    emplacement_source_id INTEGER REFERENCES emplacements(id),
    depot_dest_id INTEGER REFERENCES depots(id),
    emplacement_dest_id INTEGER REFERENCES emplacements(id),
    quantite DECIMAL(15,3) NOT NULL,
    unite_id INTEGER REFERENCES unites(id),
    cout_unitaire DECIMAL(15,4),
    cout_total DECIMAL(15,2),
    document_origine VARCHAR(30), -- BR, BL, INVENTAIRE, TRANSFERT
    document_origine_id INTEGER,
    utilisateur_id INTEGER REFERENCES utilisateurs(id),
    commentaire TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Réservations de stock
CREATE TABLE reservations_stock (
    id SERIAL PRIMARY KEY,
    article_id INTEGER REFERENCES articles(id),
    depot_id INTEGER REFERENCES depots(id),
    lot_id INTEGER REFERENCES lots(id),
    quantite DECIMAL(15,3) NOT NULL,
    type_document VARCHAR(30), -- COMMANDE_CLIENT
    document_id INTEGER,
    date_reservation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_expiration TIMESTAMP,
    statut VARCHAR(20) DEFAULT 'ACTIVE' -- ACTIVE, CONSOMMEE, ANNULEE, EXPIREE
);

-- Ordres de transfert
CREATE TABLE transferts_stock (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_transfert DATE DEFAULT CURRENT_DATE,
    depot_source_id INTEGER REFERENCES depots(id),
    depot_dest_id INTEGER REFERENCES depots(id),
    demandeur_id INTEGER REFERENCES utilisateurs(id),
    validateur_id INTEGER REFERENCES utilisateurs(id),
    statut VARCHAR(20) DEFAULT 'BROUILLON', -- BROUILLON, EN_ATTENTE, VALIDE, EN_COURS, TERMINE, ANNULE
    motif TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Lignes transfert
CREATE TABLE lignes_transfert (
    id SERIAL PRIMARY KEY,
    transfert_id INTEGER REFERENCES transferts_stock(id) ON DELETE CASCADE,
    article_id INTEGER REFERENCES articles(id),
    lot_id INTEGER REFERENCES lots(id),
    quantite_demandee DECIMAL(15,3) NOT NULL,
    quantite_envoyee DECIMAL(15,3) DEFAULT 0,
    quantite_recue DECIMAL(15,3) DEFAULT 0,
    emplacement_source_id INTEGER REFERENCES emplacements(id),
    emplacement_dest_id INTEGER REFERENCES emplacements(id)
);

-- ============================================
-- 6. MODULE INVENTAIRES
-- ============================================

-- Sessions d'inventaire
CREATE TABLE inventaires (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    date_inventaire DATE DEFAULT CURRENT_DATE,
    type_inventaire VARCHAR(30), -- ANNUEL, TOURNANT, PONCTUEL
    depot_id INTEGER REFERENCES depots(id),
    zone VARCHAR(50),
    famille_id INTEGER REFERENCES familles_articles(id),
    responsable_id INTEGER REFERENCES utilisateurs(id),
    statut VARCHAR(20) DEFAULT 'BROUILLON', -- BROUILLON, EN_COURS, TERMINE, VALIDE, ANNULE
    date_debut TIMESTAMP,
    date_fin TIMESTAMP,
    commentaire TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Lignes inventaire
CREATE TABLE lignes_inventaire (
    id SERIAL PRIMARY KEY,
    inventaire_id INTEGER REFERENCES inventaires(id) ON DELETE CASCADE,
    article_id INTEGER REFERENCES articles(id),
    lot_id INTEGER REFERENCES lots(id),
    emplacement_id INTEGER REFERENCES emplacements(id),
    quantite_theorique DECIMAL(15,3),
    quantite_comptee DECIMAL(15,3),
    ecart DECIMAL(15,3),
    cout_unitaire DECIMAL(15,4),
    valeur_ecart DECIMAL(15,2),
    compteur_id INTEGER REFERENCES utilisateurs(id),
    date_comptage TIMESTAMP,
    recompte BOOLEAN DEFAULT FALSE,
    commentaire TEXT
);

-- Demandes d'ajustement
CREATE TABLE ajustements_stock (
    id SERIAL PRIMARY KEY,
    numero VARCHAR(30) UNIQUE NOT NULL,
    inventaire_id INTEGER REFERENCES inventaires(id),
    date_demande DATE DEFAULT CURRENT_DATE,
    demandeur_id INTEGER REFERENCES utilisateurs(id),
    article_id INTEGER REFERENCES articles(id),
    depot_id INTEGER REFERENCES depots(id),
    lot_id INTEGER REFERENCES lots(id),
    quantite_avant DECIMAL(15,3),
    quantite_apres DECIMAL(15,3),
    ecart DECIMAL(15,3),
    valeur_ecart DECIMAL(15,2),
    motif TEXT,
    statut VARCHAR(20) DEFAULT 'EN_ATTENTE', -- EN_ATTENTE, APPROUVE, REJETE
    validateur_id INTEGER REFERENCES utilisateurs(id),
    date_validation TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 7. VALORISATION ET CLOTURE
-- ============================================

-- Clôtures mensuelles
CREATE TABLE clotures_mensuelles (
    id SERIAL PRIMARY KEY,
    annee INTEGER NOT NULL,
    mois INTEGER NOT NULL,
    depot_id INTEGER REFERENCES depots(id),
    date_cloture TIMESTAMP,
    utilisateur_id INTEGER REFERENCES utilisateurs(id),
    valeur_stock DECIMAL(15,2),
    statut VARCHAR(20) DEFAULT 'OUVERT', -- OUVERT, CLOTURE
    UNIQUE(annee, mois, depot_id)
);

-- Historique valorisation
CREATE TABLE valorisation_stock (
    id SERIAL PRIMARY KEY,
    date_valorisation DATE NOT NULL,
    article_id INTEGER REFERENCES articles(id),
    depot_id INTEGER REFERENCES depots(id),
    quantite DECIMAL(15,3),
    cout_unitaire_cump DECIMAL(15,4),
    cout_unitaire_fifo DECIMAL(15,4),
    valeur_stock DECIMAL(15,2),
    methode_utilisee VARCHAR(10)
);

-- ============================================
-- 8. AUDIT ET JOURNALISATION
-- ============================================

-- Journal d'audit (non modifiable)
CREATE TABLE journal_audit (
    id BIGSERIAL PRIMARY KEY,
    date_action TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    utilisateur_id INTEGER,
    utilisateur_nom VARCHAR(200),
    action VARCHAR(30), -- CREATE, UPDATE, DELETE, VALIDER, APPROUVER, REJETER
    module VARCHAR(30),
    table_concernee VARCHAR(50),
    enregistrement_id INTEGER,
    donnees_avant JSONB,
    donnees_apres JSONB,
    ip_address VARCHAR(50),
    user_agent TEXT
);

-- Verrouiller la table audit contre les modifications
CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Le journal d''audit ne peut pas être modifié';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER protect_audit_journal
BEFORE UPDATE OR DELETE ON journal_audit
FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

-- ============================================
-- 9. SEQUENCES POUR NUMEROTATION
-- ============================================

CREATE SEQUENCE seq_demande_achat START 1;
CREATE SEQUENCE seq_commande_fournisseur START 1;
CREATE SEQUENCE seq_bon_reception START 1;
CREATE SEQUENCE seq_facture_fournisseur START 1;
CREATE SEQUENCE seq_devis START 1;
CREATE SEQUENCE seq_commande_client START 1;
CREATE SEQUENCE seq_bon_livraison START 1;
CREATE SEQUENCE seq_facture_client START 1;
CREATE SEQUENCE seq_mouvement_stock START 1;
CREATE SEQUENCE seq_inventaire START 1;
CREATE SEQUENCE seq_transfert START 1;

-- ============================================
-- 10. INDEX POUR PERFORMANCE
-- ============================================

CREATE INDEX idx_articles_famille ON articles(famille_id);
CREATE INDEX idx_articles_code ON articles(code);
CREATE INDEX idx_mouvements_article ON mouvements_stock(article_id);
CREATE INDEX idx_mouvements_date ON mouvements_stock(date_mouvement);
CREATE INDEX idx_stock_article_depot ON stock_emplacement(article_id, depot_id);
CREATE INDEX idx_commandes_four_statut ON commandes_fournisseur(statut);
CREATE INDEX idx_commandes_client_statut ON commandes_client(statut);
CREATE INDEX idx_audit_date ON journal_audit(date_action);
CREATE INDEX idx_audit_utilisateur ON journal_audit(utilisateur_id);

-- ============================================
-- 11. DONNEES INITIALES - ROLES
-- ============================================

INSERT INTO roles (code, libelle, description, niveau_approbation) VALUES
('ADMIN', 'Administrateur', 'Accès complet au système', 99),
('DG', 'Directeur Général', 'Approbations finales, vision globale', 4),
('DAF', 'Directeur Administratif et Financier', 'Validation finance, KPI consolidés', 4),
('RESP_ACHATS', 'Responsable Achats', 'Validation BC, gestion fournisseurs', 3),
('ACHETEUR', 'Acheteur', 'Création BC, négociation', 2),
('DEMANDEUR_DA', 'Demandeur', 'Création demandes d''achat', 1),
('APPROBATEUR_N1', 'Approbateur Niveau 1', 'Validation DA jusqu''à seuil N1', 1),
('APPROBATEUR_N2', 'Approbateur Niveau 2', 'Validation DA jusqu''à seuil N2', 2),
('APPROBATEUR_N3', 'Approbateur Niveau 3', 'Validation DA jusqu''à seuil N3', 3),
('RESP_MAGASIN', 'Chef Magasin', 'Validation transferts, inventaires', 3),
('MAGASINIER_REC', 'Magasinier Réception', 'Réception marchandises', 1),
('MAGASINIER_SORT', 'Magasinier Sortie', 'Préparation et expédition', 1),
('RESP_VENTES', 'Responsable Ventes', 'Validation remises, annulations', 3),
('COMMERCIAL', 'Commercial', 'Devis et commandes clients', 2),
('COMPTABLE_FOUR', 'Comptable Fournisseur', 'Rapprochement factures', 2),
('COMPTABLE_CLIENT', 'Comptable Client', 'Facturation, encaissements', 2);

-- Départements
INSERT INTO departements (code, libelle) VALUES
('DIR', 'Direction'),
('ACH', 'Achats'),
('VTE', 'Ventes'),
('MAG', 'Magasin'),
('FIN', 'Finance');

-- Unités de mesure
INSERT INTO unites (code, libelle, type_unite) VALUES
('PCE', 'Pièce', 'quantite'),
('KG', 'Kilogramme', 'poids'),
('L', 'Litre', 'volume'),
('M', 'Mètre', 'quantite'),
('M2', 'Mètre carré', 'quantite'),
('M3', 'Mètre cube', 'volume'),
('BTE', 'Boîte', 'quantite'),
('CTN', 'Carton', 'quantite');
