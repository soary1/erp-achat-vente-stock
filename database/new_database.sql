\c postgres;
DROP DATABASE IF EXISTS avs_db;
CREATE DATABASE avs_db;
\c avs_db;

-- ==============================================================================
-- 0. CONFIGURATION & EXTENSIONS
-- ==============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==============================================================================
-- 1. TABLES DE RÉFÉRENCE GLOBALES
-- ==============================================================================

-- 1.1 Géographie & Standards
CREATE TABLE devise (
    code VARCHAR(3) PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    symbol VARCHAR(5)
);
INSERT INTO devise (code, label, symbol) VALUES ('MGA', 'Ariary Malgache', 'Ar'), ('EUR', 'Euro', '€'), ('USD', 'US Dollar', '$');

CREATE TABLE pays (
    code VARCHAR(2) PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);
INSERT INTO pays (code, label) VALUES ('MG', 'Madagascar'), ('FR', 'France');

-- 1.2 Unités et Taxes
CREATE TABLE unite_mesure (
    code VARCHAR(20) PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);
INSERT INTO unite_mesure (code, label) VALUES ('PCE', 'Pièce'), ('KG', 'Kilogramme'), ('L', 'Litre'), ('H', 'Heure');

CREATE TABLE type_taxe (
    code VARCHAR(20) PRIMARY KEY,
    rate DECIMAL(5, 4) NOT NULL,
    label VARCHAR(100) NOT NULL
);
INSERT INTO type_taxe (code, label, rate) VALUES ('TVA_20', 'TVA 20%', 0.20), ('EXO', 'Exonéré', 0.00);

-- 1.3 Méthodes de Gestion
CREATE TABLE methode_valorisation (
    code VARCHAR(20) PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    description TEXT
);
INSERT INTO methode_valorisation (code, label) VALUES 
('CUMP', 'Coût Unitaire Moyen Pondéré'), 
('FIFO', 'Premier Entré Premier Sorti'), 
('LIFO', 'Dernier Entré Premier Sorti');

CREATE TABLE mode_paiement (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);
INSERT INTO mode_paiement (code, label) VALUES ('VIREMENT', 'Virement Bancaire'), ('CHEQUE', 'Chèque'), ('ESPECES', 'Espèces'), ('MOBILE', 'Mobile Money');

-- ==============================================================================
-- 2. TABLES DE STATUTS (WORKFLOW)
-- ==============================================================================

CREATE TABLE statut_demande_achat ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_demande_achat VALUES ('BROUILLON', 'Brouillon'), ('SOUMISE', 'Soumise'), ('APPROUVEE', 'Approuvée'), ('REJETEE', 'Rejetée');

CREATE TABLE statut_commande_achat ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_commande_achat VALUES ('BROUILLON', 'Brouillon'), ('VALIDEE', 'Validée'), ('ENVOYEE', 'Envoyée Frs'), ('PARTIEL', 'Reçu Partiel'), ('CLOTUREE', 'Clôturée');

CREATE TABLE statut_reception ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_reception VALUES ('BROUILLON', 'Brouillon'), ('CONTROLE', 'Contrôle Qualité'), ('VALIDE', 'Validé / En Stock');

CREATE TABLE statut_commande_vente ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_commande_vente VALUES ('BROUILLON', 'Brouillon'), ('CONFIRMEE', 'Confirmée'), ('PREPARATION', 'En préparation'), ('EXPEDIEE', 'Expédiée');

CREATE TABLE statut_facture ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_facture VALUES ('BROUILLON', 'Brouillon'), ('A_PAYER', 'Validée / À Payer'), ('PAYEE_PARTIEL', 'Payée Partiellement'), ('PAYEE', 'Soldée'), ('ANNULEE', 'Annulée');

CREATE TABLE statut_inventaire ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_inventaire VALUES ('PLANIFIE', 'Planifié'), ('EN_COURS', 'Comptage en cours'), ('ANALYSE', 'Analyse des écarts'), ('VALIDE', 'Validé');

CREATE TABLE statut_qualite ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_qualite VALUES ('CONFORME', 'Bon'), ('QUARANTAINE', 'En attente contrôle'), ('REJETE', 'Rebut / Non conforme');

-- ==============================================================================
-- 3. ORGANISATION
-- ==============================================================================

CREATE TABLE groupe_societe (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE societe (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    groupe_id UUID NOT NULL REFERENCES groupe_societe(id),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    tax_id VARCHAR(50),
    pays_code VARCHAR(2) REFERENCES pays(code),
    devise_code VARCHAR(3) NOT NULL REFERENCES devise(code)
);

CREATE TABLE site (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    societe_id UUID NOT NULL REFERENCES societe(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    address TEXT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(11, 7),
    is_active BOOLEAN DEFAULT TRUE,
    UNIQUE(societe_id, code)
);

CREATE TABLE depot (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_id UUID NOT NULL REFERENCES site(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(11, 7),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE emplacement (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    depot_id UUID NOT NULL REFERENCES depot(id),
    code VARCHAR(50) NOT NULL,
    aisle VARCHAR(20),
    rack VARCHAR(20),
    shelf VARCHAR(20),
    UNIQUE(depot_id, code)
);

-- ==============================================================================
-- 4. GOUVERNANCE & SÉCURITÉ
-- ==============================================================================

CREATE TABLE departement (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE,
    name VARCHAR(100)
);

CREATE TABLE role (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(100)
);

CREATE TABLE utilisateur (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(200) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    departement_id UUID REFERENCES departement(id),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE utilisateur_role (
    utilisateur_id UUID NOT NULL REFERENCES utilisateur(id),
    role_id UUID NOT NULL REFERENCES role(id),
    PRIMARY KEY (utilisateur_id, role_id)
);

-- ABAC (Permissions fines)
CREATE TABLE perimetre_acces (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    utilisateur_id UUID NOT NULL REFERENCES utilisateur(id),
    societe_id UUID REFERENCES societe(id),
    site_id UUID REFERENCES site(id),
    depot_id UUID REFERENCES depot(id),
    max_amount_approval DECIMAL(19, 2),
    active BOOLEAN DEFAULT TRUE
);

-- Délégation
CREATE TABLE delegation_acces (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    donneur_id UUID NOT NULL REFERENCES utilisateur(id),
    receveur_id UUID NOT NULL REFERENCES utilisateur(id),
    role_id UUID NOT NULL REFERENCES role(id),
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    reason TEXT
);

-- Audit Technique (Logs CRUD)
CREATE TABLE journal_audit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_name VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    utilisateur_id UUID REFERENCES utilisateur(id),
    changes JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 5. AUDIT FONCTIONNEL & WORKFLOW (NOUVEAU)
-- ==============================================================================

-- Cette table permet de savoir QUI a validé une commande ou un inventaire
-- C'est ici qu'on stocke les signatures électroniques.
CREATE TABLE historique_workflow (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Lien polymorphique
    document_type VARCHAR(50) NOT NULL, -- Ex: 'COMMANDE_ACHAT', 'INVENTAIRE', 'BON_RECEPTION'
    document_id UUID NOT NULL, 
    
    etape_precedente VARCHAR(50),
    etape_nouvelle VARCHAR(50) NOT NULL,
    
    acteur_id UUID NOT NULL REFERENCES utilisateur(id), -- Celui qui a cliqué
    
    action VARCHAR(50) NOT NULL, -- 'SOUMISSION', 'APPROBATION', 'REJET', 'ANNULATION'
    commentaire TEXT, -- Raison du rejet ou note d'approbation
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_workflow_doc ON historique_workflow(document_type, document_id);

-- ==============================================================================
-- 6. RÉFÉRENTIELS ARTICLES & TIERS
-- ==============================================================================

CREATE TABLE famille_article (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    methode_valorisation_code VARCHAR(20) NOT NULL REFERENCES methode_valorisation(code),
    is_lot_obligatoire BOOLEAN DEFAULT FALSE,
    is_peremption_obligatoire BOOLEAN DEFAULT FALSE
);

CREATE TABLE article (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    societe_id UUID NOT NULL REFERENCES societe(id),
    famille_id UUID NOT NULL REFERENCES famille_article(id),
    unite_code VARCHAR(20) NOT NULL REFERENCES unite_mesure(code),
    taxe_vente_code VARCHAR(20) REFERENCES type_taxe(code),
    taxe_achat_code VARCHAR(20) REFERENCES type_taxe(code),
    sku VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    weight DECIMAL(10, 3),
    is_active BOOLEAN DEFAULT TRUE,
    UNIQUE(societe_id, sku)
);

CREATE TABLE client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    tax_id VARCHAR(50),
    email VARCHAR(200),
    telephone VARCHAR(50),
    adresse TEXT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(11, 7),
    devise_code VARCHAR(3) REFERENCES devise(code),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE fournisseur (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    tax_id VARCHAR(50),
    email VARCHAR(200),
    telephone VARCHAR(50),
    adresse TEXT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(11, 7),
    devise_code VARCHAR(3) REFERENCES devise(code),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE liste_tarifaire (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE,
    name VARCHAR(100),
    devise_code VARCHAR(3) NOT NULL REFERENCES devise(code),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE ligne_liste_tarifaire (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    liste_tarifaire_id UUID NOT NULL REFERENCES liste_tarifaire(id),
    article_id UUID NOT NULL REFERENCES article(id),
    price DECIMAL(19, 4) NOT NULL,
    min_qty DECIMAL(19, 4) DEFAULT 1,
    start_date DATE NOT NULL,
    end_date DATE
);

-- ==============================================================================
-- 7. ACHATS (P2P)
-- ==============================================================================

CREATE TABLE regle_approbation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    societe_id UUID REFERENCES societe(id),
    document_type VARCHAR(50) NOT NULL,
    min_amount DECIMAL(19, 2),
    max_amount DECIMAL(19, 2),
    role_id UUID NOT NULL REFERENCES role(id),
    level_index INT NOT NULL
);

CREATE TABLE demande_achat (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) NOT NULL UNIQUE,
    demandeur_id UUID NOT NULL REFERENCES utilisateur(id),
    site_id UUID NOT NULL REFERENCES site(id),
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_demande_achat(code),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE commande_achat (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) NOT NULL UNIQUE,
    demande_achat_id UUID REFERENCES demande_achat(id),
    fournisseur_id UUID NOT NULL REFERENCES fournisseur(id),
    site_id UUID NOT NULL REFERENCES site(id),
    acheteur_id UUID REFERENCES utilisateur(id),
    devise_code VARCHAR(3) NOT NULL REFERENCES devise(code),
    total_ht DECIMAL(19, 2) DEFAULT 0,
    total_ttc DECIMAL(19, 2) DEFAULT 0,
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_commande_achat(code),
    date_commande DATE DEFAULT CURRENT_DATE
);

CREATE TABLE ligne_commande_achat (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    commande_id UUID NOT NULL REFERENCES commande_achat(id),
    article_id UUID NOT NULL REFERENCES article(id),
    qty_ordered DECIMAL(19, 4) NOT NULL,
    unit_price DECIMAL(19, 4) NOT NULL,
    taxe_code VARCHAR(20) REFERENCES type_taxe(code)
);

-- ==============================================================================
-- 8. STOCK & LOGISTIQUE
-- ==============================================================================

CREATE TABLE lot (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    article_id UUID NOT NULL REFERENCES article(id),
    numero_lot VARCHAR(100) NOT NULL,
    numero_serie VARCHAR(100),
    date_fabrication DATE,
    date_peremption DATE,
    statut_qualite_code VARCHAR(50) NOT NULL REFERENCES statut_qualite(code),
    UNIQUE(article_id, numero_lot, numero_serie)
);

-- 8.1 Réception
CREATE TABLE bon_reception (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) NOT NULL UNIQUE,
    commande_achat_id UUID REFERENCES commande_achat(id),
    site_id UUID NOT NULL REFERENCES site(id),
    depot_id UUID NOT NULL REFERENCES depot(id),
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_reception(code),
    date_reception TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE ligne_bon_reception (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bon_reception_id UUID NOT NULL REFERENCES bon_reception(id),
    article_id UUID NOT NULL REFERENCES article(id),
    lot_id UUID REFERENCES lot(id),
    emplacement_id UUID REFERENCES emplacement(id),
    qty_received DECIMAL(19, 4) NOT NULL
);

-- 8.2 Contrôle Qualité (NOUVEAU)
-- Permet de tracer qui a vérifié la marchandise et les preuves de non-conformité
CREATE TABLE controle_qualite (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ligne_reception_id UUID NOT NULL REFERENCES ligne_bon_reception(id),
    
    controleur_id UUID NOT NULL REFERENCES utilisateur(id),
    
    qty_inspectee DECIMAL(19,4) NOT NULL,
    qty_acceptee DECIMAL(19,4) NOT NULL,
    qty_rejetee DECIMAL(19,4) NOT NULL,
    
    motif_rejet_code VARCHAR(50), -- ex: 'CASSE', 'PERIME'
    photo_preuve_url TEXT, -- Lien S3 ou autre
    commentaires TEXT,
    
    date_controle TIMESTAMPTZ DEFAULT NOW()
);

-- 8.3 Mouvements & Stock
CREATE TABLE type_mouvement (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    sens INT NOT NULL
);
INSERT INTO type_mouvement VALUES ('RECEPTION', 'Réception Fournisseur', 1), ('EXPEDITION', 'Livraison Client', -1), ('TRANSFERT', 'Transfert Interne', 0), ('AJUSTEMENT', 'Ajustement Inventaire', 0);

CREATE TABLE mouvement_stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type_mouvement_code VARCHAR(50) NOT NULL REFERENCES type_mouvement(code),
    reference_doc VARCHAR(100),
    article_id UUID NOT NULL REFERENCES article(id),
    lot_id UUID REFERENCES lot(id),
    depot_source_id UUID REFERENCES depot(id),
    emplacement_source_id UUID REFERENCES emplacement(id),
    depot_dest_id UUID REFERENCES depot(id),
    emplacement_dest_id UUID REFERENCES emplacement(id),
    qty DECIMAL(19, 4) NOT NULL,
    unit_cost DECIMAL(19, 4),
    utilisateur_id UUID REFERENCES utilisateur(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    depot_id UUID NOT NULL REFERENCES depot(id),
    emplacement_id UUID REFERENCES emplacement(id),
    article_id UUID NOT NULL REFERENCES article(id),
    lot_id UUID REFERENCES lot(id),
    qty_reel DECIMAL(19, 4) DEFAULT 0,
    qty_reserve DECIMAL(19, 4) DEFAULT 0,
    version BIGINT DEFAULT 0,
    UNIQUE(depot_id, emplacement_id, article_id, lot_id)
);

-- 8.4 Inventaire
CREATE TABLE inventaire (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) NOT NULL UNIQUE,
    site_id UUID NOT NULL REFERENCES site(id),
    depot_id UUID REFERENCES depot(id),
    description VARCHAR(200),
    type_code VARCHAR(50) CHECK (type_code IN ('ANNUEL', 'TOURNANT', 'SPOT')),
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_inventaire(code),
    date_planification DATE,
    date_debut TIMESTAMPTZ,
    date_cloture TIMESTAMPTZ,
    cree_par UUID REFERENCES utilisateur(id),
    valide_par UUID REFERENCES utilisateur(id)
);

-- AJOUT AUDIT: 'arbitrage_par' pour tracer la décision financière de l'écart
CREATE TABLE ligne_inventaire (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    inventaire_id UUID NOT NULL REFERENCES inventaire(id),
    article_id UUID NOT NULL REFERENCES article(id),
    emplacement_id UUID REFERENCES emplacement(id),
    lot_id UUID REFERENCES lot(id),
    
    qty_theorique DECIMAL(19, 4) NOT NULL DEFAULT 0,
    qty_reelle_retenue DECIMAL(19, 4), 
    ecart_final DECIMAL(19, 4) GENERATED ALWAYS AS (qty_reelle_retenue - qty_theorique) STORED,
    
    est_traitee BOOLEAN DEFAULT FALSE,
    est_validee BOOLEAN DEFAULT FALSE,
    notes_arbitrage TEXT,
    
    -- Qui a validé cet écart spécifique ?
    arbitrage_par UUID REFERENCES utilisateur(id),
    date_arbitrage TIMESTAMPTZ
);

-- AJOUT AUDIT: 'superviseur_id' pour le double check
CREATE TABLE saisie_inventaire (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ligne_inventaire_id UUID NOT NULL REFERENCES ligne_inventaire(id),
    operateur_id UUID NOT NULL REFERENCES utilisateur(id),
    
    -- Si un chef a supervisé le comptage (pour les articles haute valeur)
    superviseur_id UUID REFERENCES utilisateur(id),
    
    qty_comptee DECIMAL(19, 4) NOT NULL,
    date_saisie TIMESTAMPTZ DEFAULT NOW(),
    tour_comptage INT DEFAULT 1,
    est_retenue BOOLEAN DEFAULT FALSE
);

-- ==============================================================================
-- 9. FINANCE (Facturation & Paiement)
-- ==============================================================================

CREATE TABLE facture_fournisseur (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ref_interne VARCHAR(50) UNIQUE,
    ref_fournisseur VARCHAR(100),
    fournisseur_id UUID NOT NULL REFERENCES fournisseur(id),
    commande_achat_id UUID REFERENCES commande_achat(id),
    montant_ht DECIMAL(19, 2) NOT NULL,
    montant_ttc DECIMAL(19, 2) NOT NULL,
    devise_code VARCHAR(3) NOT NULL REFERENCES devise(code),
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_facture(code),
    date_facture DATE NOT NULL,
    date_echeance DATE
);

CREATE TABLE rapprochement_achat (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facture_id UUID NOT NULL REFERENCES facture_fournisseur(id),
    reception_id UUID REFERENCES bon_reception(id),
    montant_rapproche DECIMAL(19, 2) NOT NULL
);

CREATE TABLE paiement_fournisseur (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facture_id UUID NOT NULL REFERENCES facture_fournisseur(id),
    montant DECIMAL(19, 2) NOT NULL,
    mode_paiement_code VARCHAR(50) NOT NULL REFERENCES mode_paiement(code),
    date_paiement DATE NOT NULL
);

-- ==============================================================================
-- 10. VENTES (Order to Cash)
-- ==============================================================================

CREATE TABLE devis_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) UNIQUE,
    client_id UUID NOT NULL REFERENCES client(id),
    site_id UUID NOT NULL REFERENCES site(id),
    statut_code VARCHAR(50) DEFAULT 'BROUILLON',
    total_ttc DECIMAL(19, 2)
);

CREATE TABLE commande_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) UNIQUE,
    devis_id UUID REFERENCES devis_client(id),
    client_id UUID NOT NULL REFERENCES client(id),
    site_id UUID NOT NULL REFERENCES site(id),
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_commande_vente(code),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE ligne_commande_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    commande_id UUID NOT NULL REFERENCES commande_client(id),
    article_id UUID NOT NULL REFERENCES article(id),
    qty_ordered DECIMAL(19, 4) NOT NULL,
    qty_delivered DECIMAL(19, 4) DEFAULT 0,
    price_unit DECIMAL(19, 2) NOT NULL
);

CREATE TABLE reservation_stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ligne_commande_id UUID NOT NULL REFERENCES ligne_commande_client(id),
    article_id UUID NOT NULL REFERENCES article(id),
    depot_id UUID NOT NULL REFERENCES depot(id),
    lot_id UUID REFERENCES lot(id),
    qty_reservee DECIMAL(19, 4) NOT NULL
);

CREATE TABLE bon_livraison (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) UNIQUE,
    commande_id UUID NOT NULL REFERENCES commande_client(id),
    date_expedition TIMESTAMPTZ
);

CREATE TABLE ligne_bon_livraison (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    livraison_id UUID NOT NULL REFERENCES bon_livraison(id),
    article_id UUID NOT NULL REFERENCES article(id),
    lot_id UUID REFERENCES lot(id),
    qty_livree DECIMAL(19, 4) NOT NULL
);

CREATE TABLE facture_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) UNIQUE,
    client_id UUID NOT NULL REFERENCES client(id),
    commande_id UUID REFERENCES commande_client(id),
    montant_ttc DECIMAL(19, 2) NOT NULL,
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_facture(code)
);

CREATE TABLE encaissement_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facture_id UUID NOT NULL REFERENCES facture_client(id),
    montant DECIMAL(19, 2) NOT NULL,
    mode_paiement_code VARCHAR(50) NOT NULL REFERENCES mode_paiement(code)
);