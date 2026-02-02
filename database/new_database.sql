\c postgres;
DROP DATABASE IF EXISTS avs_db;
CREATE DATABASE avs_db;
\c avs_db;

-- ==============================================================================
-- 0. CONFIGURATION & EXTENSIONS
-- ==============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==============================================================================
-- 1. TABLES DE ReFeRENCE GLOBALES
-- ==============================================================================

-- 1.1 Geographie & Standards
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

-- 1.2 Unites et Taxes
CREATE TABLE unite_mesure (
    code VARCHAR(20) PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);
INSERT INTO unite_mesure (code, label) VALUES ('PCE', 'Piece'), ('KG', 'Kilogramme'), ('L', 'Litre'), ('H', 'Heure');

CREATE TABLE type_taxe (
    code VARCHAR(20) PRIMARY KEY,
    rate DECIMAL(5, 4) NOT NULL,
    label VARCHAR(100) NOT NULL
);
INSERT INTO type_taxe (code, label, rate) VALUES ('TVA_20', 'TVA 20%', 0.20), ('EXO', 'Exonere', 0.00);

-- 1.3 Methodes de Gestion
CREATE TABLE methode_valorisation (
    code VARCHAR(20) PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    description TEXT
);
INSERT INTO methode_valorisation (code, label) VALUES 
('CUMP', 'Coût Unitaire Moyen Pondere'), 
('FIFO', 'Premier Entre Premier Sorti'), 
('LIFO', 'Dernier Entre Premier Sorti');

CREATE TABLE mode_paiement (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);
INSERT INTO mode_paiement (code, label) VALUES ('VIREMENT', 'Virement Bancaire'), ('CHEQUE', 'Cheque'), ('ESPECES', 'Especes'), ('MOBILE', 'Mobile Money');

-- ==============================================================================
-- 2. TABLES DE STATUTS (WORKFLOW)
-- ==============================================================================

CREATE TABLE statut_demande_achat ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_demande_achat VALUES ('BROUILLON', 'Brouillon'), ('SOUMISE', 'Soumise'), ('APPROUVEE', 'Approuvee'), ('REJETEE', 'Rejetee');

CREATE TABLE statut_commande_achat ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_commande_achat VALUES ('BROUILLON', 'Brouillon'), ('VALIDEE', 'Validee'), ('ENVOYEE', 'Envoyee Frs'), ('PARTIEL', 'Reçu Partiel'), ('CLOTUREE', 'Clôturee');

CREATE TABLE statut_reception ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_reception VALUES ('BROUILLON', 'Brouillon'), ('CONTROLE', 'Contrôle Qualite'), ('VALIDE', 'Valide / En Stock');

-- Statuts devis client
CREATE TABLE statut_devis_client ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_devis_client VALUES 
('BROUILLON', 'Brouillon'), 
('EN_ATTENTE_VALIDATION', 'En attente validation remise'), 
('VALIDE', 'Valide'),
('REFUSE', 'Refuse'),
('TRANSFORME', 'Transforme en commande');

CREATE TABLE statut_commande_vente ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_commande_vente VALUES 
('BROUILLON', 'Brouillon'), 
('EN_ATTENTE_VALIDATION', 'En attente validation remise'),
('CONFIRMEE', 'Confirmee'), 
('EN_ATTENTE_STOCK', 'En attente de stock'),
('PREPARATION', 'En preparation'), 
('PRETE', 'Prête pour expedition'),
('EXPEDIEE', 'Expediee'),
('CLOTUREE', 'Clôturee');

CREATE TABLE statut_facture ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_facture VALUES ('BROUILLON', 'Brouillon'), ('A_PAYER', 'Validee / a Payer'), ('PAYEE_PARTIEL', 'Payee Partiellement'), ('PAYEE', 'Soldee'), ('ANNULEE', 'Annulee');

CREATE TABLE statut_inventaire ( code VARCHAR(50) PRIMARY KEY, label VARCHAR(100) );
INSERT INTO statut_inventaire VALUES ('PLANIFIE', 'Planifie'), ('EN_COURS', 'Comptage en cours'), ('ANALYSE', 'Analyse des ecarts'), ('VALIDE', 'Valide');

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
-- 4. GOUVERNANCE & SeCURITe
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
    max_remise_pct DECIMAL(5, 2) DEFAULT 5.00, -- Plafond de remise autorisee (%)
    active BOOLEAN DEFAULT TRUE
);

-- Table pour les plafonds de remise par rôle
CREATE TABLE plafond_remise_role (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id UUID NOT NULL REFERENCES role(id),
    max_remise_pct DECIMAL(5, 2) NOT NULL DEFAULT 10.00,
    description TEXT
);

-- Insertion des plafonds par defaut
INSERT INTO plafond_remise_role (role_id, max_remise_pct, description)
SELECT id, 5.00, 'Commercial - Remise max 5%' FROM role WHERE code = 'COMMERCIAL'
UNION ALL
SELECT id, 15.00, 'Responsable Ventes - Remise max 15%' FROM role WHERE code = 'MANAGER'
UNION ALL
SELECT id, 30.00, 'Directeur Commercial - Remise max 30%' FROM role WHERE code = 'ADMIN';

-- Delegation
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

-- Cette table permet de savoir QUI a valide une commande ou un inventaire
-- C'est ici qu'on stocke les signatures electroniques.
CREATE TABLE historique_workflow (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Lien polymorphique
    document_type VARCHAR(50) NOT NULL, -- Ex: 'COMMANDE_ACHAT', 'INVENTAIRE', 'BON_RECEPTION'
    document_id UUID NOT NULL, 
    
    etape_precedente VARCHAR(50),
    etape_nouvelle VARCHAR(50) NOT NULL,
    
    acteur_id UUID NOT NULL REFERENCES utilisateur(id), -- Celui qui a clique
    
    action VARCHAR(50) NOT NULL, -- 'SOUMISSION', 'APPROBATION', 'REJET', 'ANNULATION'
    commentaire TEXT, -- Raison du rejet ou note d'approbation
    
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_workflow_doc ON historique_workflow(document_type, document_id);

-- ==============================================================================
-- 6. ReFeRENTIELS ARTICLES & TIERS
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

CREATE TABLE ligne_demande_achat (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    demande_achat_id UUID NOT NULL REFERENCES demande_achat(id),
    article_id UUID NOT NULL REFERENCES article(id),
    qty_demandee DECIMAL(19, 4) NOT NULL,
    description TEXT
);

-- Séquence pour numérotation automatique des commandes d'achat
CREATE SEQUENCE commande_achat_seq START 1;

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
    unit_price DECIMAL(19, 4) NOT NULL
);

CREATE TABLE ligne_commande_achat_taxe (
    ligne_id UUID NOT NULL REFERENCES ligne_commande_achat(id),
    taxe_code VARCHAR(20) NOT NULL REFERENCES type_taxe(code),
    PRIMARY KEY (ligne_id, taxe_code)
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

-- 8.1 Reception
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

-- 8.2 Contrôle Qualite (NOUVEAU)
-- Permet de tracer qui a verifie la marchandise et les preuves de non-conformite
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
INSERT INTO type_mouvement VALUES 
('RECEPTION', 'Reception Fournisseur', 1), 
('EXPEDITION', 'Livraison Client', -1), 
('RETOUR_CLIENT', 'Retour Client (SAV)', 1),
('TRANSFERT_SORTIE', 'Transfert Sortant (Depart)', -1),
('TRANSFERT_ENTREE', 'Transfert Entrant (Arrivee)', 1),
('TRANSFERT_EMPLACEMENT', 'Transfert d emplacement', 0),
('CONSOMMATION', 'Consommation Interne', -1),
('REBUT', 'Mise au Rebut', -1),
('AJUSTEMENT_POS', 'Ajustement Positif', 1),
('AJUSTEMENT_NEG', 'Ajustement Negatif', -1),
('AJUSTEMENT', 'Ajustement Inventaire', 0);

CREATE TABLE mouvement_stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) UNIQUE NOT NULL,
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

-- Sequence pour numerotation automatique des mouvements
CREATE SEQUENCE mouvement_stock_seq START 1;

-- TRIGGER : Empêcher modification/suppression des mouvements (AUDIT TRAIL)
CREATE OR REPLACE FUNCTION prevent_mouvement_modification()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Les mouvements de stock ne peuvent pas être modifies (ID: %)', OLD.id;
    ELSIF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Les mouvements de stock ne peuvent pas être supprimes (ID: %)', OLD.id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_mouvement_immutable
BEFORE UPDATE OR DELETE ON mouvement_stock
FOR EACH ROW EXECUTE FUNCTION prevent_mouvement_modification();

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

-- AJOUT AUDIT: 'arbitrage_par' pour tracer la decision financiere de l'ecart
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
    
    -- Qui a valide cet ecart specifique ?
    arbitrage_par UUID REFERENCES utilisateur(id),
    date_arbitrage TIMESTAMPTZ
);

-- AJOUT AUDIT: 'superviseur_id' pour le double check
CREATE TABLE saisie_inventaire (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ligne_inventaire_id UUID NOT NULL REFERENCES ligne_inventaire(id),
    operateur_id UUID NOT NULL REFERENCES utilisateur(id),
    
    -- Si un chef a supervise le comptage (pour les articles haute valeur)
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
    commercial_id UUID REFERENCES utilisateur(id),
    validateur_id UUID REFERENCES utilisateur(id), -- Pour validation remise
    statut_code VARCHAR(50) DEFAULT 'BROUILLON' REFERENCES statut_devis_client(code),
    total_ht DECIMAL(19, 2),
    total_ttc DECIMAL(19, 2),
    remise_globale_pct DECIMAL(5, 2) DEFAULT 0,
    date_validite DATE,
    date_validation TIMESTAMPTZ,
    motif_refus TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    notes TEXT
);

CREATE TABLE ligne_devis_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    devis_id UUID NOT NULL REFERENCES devis_client(id) ON DELETE CASCADE,
    article_id UUID NOT NULL REFERENCES article(id),
    qty DECIMAL(19, 4) NOT NULL,
    price_unit DECIMAL(19, 2) NOT NULL,
    remise_pct DECIMAL(5, 2) DEFAULT 0
);

CREATE TABLE commande_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) UNIQUE,
    devis_id UUID REFERENCES devis_client(id),
    client_id UUID NOT NULL REFERENCES client(id),
    site_id UUID NOT NULL REFERENCES site(id),
    commercial_id UUID REFERENCES utilisateur(id),
    validateur_id UUID REFERENCES utilisateur(id), -- Pour validation remise
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_commande_vente(code),
    total_ht DECIMAL(19, 2),
    total_ttc DECIMAL(19, 2),
    remise_globale_pct DECIMAL(5, 2) DEFAULT 0,
    date_validation TIMESTAMPTZ,
    motif_refus TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    notes TEXT
);

CREATE TABLE ligne_commande_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    commande_id UUID NOT NULL REFERENCES commande_client(id) ON DELETE CASCADE,
    article_id UUID NOT NULL REFERENCES article(id),
    qty_ordered DECIMAL(19, 4) NOT NULL,
    qty_delivered DECIMAL(19, 4) DEFAULT 0,
    price_unit DECIMAL(19, 2) NOT NULL,
    remise_pct DECIMAL(5, 2) DEFAULT 0
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
    statut_code VARCHAR(50) DEFAULT 'BROUILLON',
    date_expedition TIMESTAMPTZ,
    preparateur_id UUID REFERENCES utilisateur(id),
    validateur_id UUID REFERENCES utilisateur(id),
    date_preparation TIMESTAMPTZ,
    date_validation TIMESTAMPTZ
);

CREATE TABLE ligne_bon_livraison (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    livraison_id UUID NOT NULL REFERENCES bon_livraison(id) ON DELETE CASCADE,
    article_id UUID NOT NULL REFERENCES article(id),
    lot_id UUID REFERENCES lot(id),
    depot_id UUID REFERENCES depot(id),
    emplacement_id UUID REFERENCES emplacement(id),
    qty_commandee DECIMAL(19, 4),
    qty_livree DECIMAL(19, 4) NOT NULL,
    qty_preparee DECIMAL(19, 4) DEFAULT 0
);

-- Table pour le picking/preparation de commande
CREATE TABLE ordre_preparation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) UNIQUE NOT NULL,
    commande_id UUID NOT NULL REFERENCES commande_client(id),
    bon_livraison_id UUID REFERENCES bon_livraison(id),
    statut_code VARCHAR(50) DEFAULT 'EN_ATTENTE', -- EN_ATTENTE, EN_COURS, TERMINE, ANNULE
    preparateur_id UUID REFERENCES utilisateur(id),
    date_creation TIMESTAMPTZ DEFAULT NOW(),
    date_debut_preparation TIMESTAMPTZ,
    date_fin_preparation TIMESTAMPTZ,
    priorite INT DEFAULT 0
);

CREATE TABLE ligne_ordre_preparation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ordre_id UUID NOT NULL REFERENCES ordre_preparation(id) ON DELETE CASCADE,
    ligne_commande_id UUID NOT NULL REFERENCES ligne_commande_client(id),
    article_id UUID NOT NULL REFERENCES article(id),
    lot_id UUID REFERENCES lot(id), -- Lot alloue par FIFO
    emplacement_id UUID REFERENCES emplacement(id),
    qty_a_preparer DECIMAL(19, 4) NOT NULL,
    qty_preparee DECIMAL(19, 4) DEFAULT 0,
    date_scan TIMESTAMPTZ,
    scanne BOOLEAN DEFAULT FALSE,
    forcage_fifo BOOLEAN DEFAULT FALSE, -- True si l'utilisateur a force un lot different
    forcage_validateur_id UUID REFERENCES utilisateur(id),
    forcage_motif TEXT
);

CREATE TABLE facture_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) UNIQUE,
    client_id UUID NOT NULL REFERENCES client(id),
    commande_id UUID REFERENCES commande_client(id),
    bon_livraison_id UUID REFERENCES bon_livraison(id),
    montant_ht DECIMAL(19, 2) NOT NULL,
    montant_ttc DECIMAL(19, 2) NOT NULL,
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_facture(code),
    date_facture DATE NOT NULL,
    date_echeance DATE,
    montant_encaisse DECIMAL(19, 2) DEFAULT 0,
    createur_id UUID REFERENCES utilisateur(id),
    date_creation TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE encaissement_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facture_id UUID NOT NULL REFERENCES facture_client(id),
    montant DECIMAL(19, 2) NOT NULL,
    mode_paiement_code VARCHAR(50) NOT NULL REFERENCES mode_paiement(code),
    date_encaissement DATE NOT NULL,
    reference VARCHAR(100),
    encaisseur_id UUID REFERENCES utilisateur(id),
    date_creation TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 11. RETOURS CLIENT (SAV)
-- ==============================================================================

CREATE TABLE statut_retour_client (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);
INSERT INTO statut_retour_client VALUES 
('DEMANDE', 'Demande de retour'),
('APPROUVE', 'Retour approuve'),
('RECEPTIONNE', 'Marchandise receptionnee'),
('CONTROLE', 'En contrôle qualite'),
('INTEGRE', 'Reintegre au stock'),
('REBUTE', 'Mis au rebut'),
('REMBOURSE', 'Client rembourse'),
('REFUSE', 'Retour refuse');

CREATE TABLE motif_retour (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);
INSERT INTO motif_retour VALUES 
('DEFECTUEUX', 'Produit defectueux'),
('NON_CONFORME', 'Non conforme a la commande'),
('ERREUR_LIVRAISON', 'Erreur de livraison'),
('ENDOMMAGE', 'Produit endommage'),
('PERIME', 'Produit perime ou proche peremption'),
('REPENTIR', 'Droit de retractation'),
('AUTRE', 'Autre motif');

CREATE TABLE retour_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) NOT NULL UNIQUE,
    client_id UUID NOT NULL REFERENCES client(id),
    commande_id UUID REFERENCES commande_client(id),
    facture_id UUID REFERENCES facture_client(id),
    bon_livraison_id UUID REFERENCES bon_livraison(id),
    
    motif_code VARCHAR(50) NOT NULL REFERENCES motif_retour(code),
    description TEXT,
    
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_retour_client(code),
    
    demandeur_id UUID REFERENCES utilisateur(id),
    approbateur_id UUID REFERENCES utilisateur(id),
    
    date_demande TIMESTAMPTZ DEFAULT NOW(),
    date_approbation TIMESTAMPTZ,
    date_reception TIMESTAMPTZ,
    
    depot_retour_id UUID REFERENCES depot(id),
    
    montant_rembourse DECIMAL(19, 2),
    notes TEXT
);

CREATE TABLE ligne_retour_client (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    retour_id UUID NOT NULL REFERENCES retour_client(id),
    ligne_livraison_id UUID REFERENCES ligne_bon_livraison(id),
    article_id UUID NOT NULL REFERENCES article(id),
    lot_id UUID REFERENCES lot(id),
    
    qty_retournee DECIMAL(19, 4) NOT NULL,
    qty_acceptee DECIMAL(19, 4) DEFAULT 0,
    qty_rejetee DECIMAL(19, 4) DEFAULT 0,
    
    etat_marchandise VARCHAR(50), -- 'INTACT', 'ABIME', 'DEFECTUEUX'
    decision VARCHAR(50), -- 'REINTEGRER', 'REBUTER', 'QUARANTAINE'
    emplacement_id UUID REFERENCES emplacement(id),
    
    notes TEXT
);

-- ==============================================================================
-- 12. TRANSFERTS INTER-DePÔTS
-- ==============================================================================

CREATE TABLE statut_transfert (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);
INSERT INTO statut_transfert VALUES 
('DEMANDE', 'Demande'),
('APPROUVE', 'Approuve'),
('EXPEDIE', 'Expedie'),
('EN_TRANSIT', 'En transit'),
('RECEPTIONNE', 'Receptionne'),
('COMPLETE', 'Complete'),
('CLOTURE', 'Clôture'),
('ANNULE', 'Annule');

CREATE TABLE transfert_stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) NOT NULL UNIQUE,
    
    depot_source_id UUID NOT NULL REFERENCES depot(id),
    depot_dest_id UUID NOT NULL REFERENCES depot(id),
    
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_transfert(code),
    
    demandeur_id UUID NOT NULL REFERENCES utilisateur(id),
    approbateur_id UUID REFERENCES utilisateur(id),
    expediteur_id UUID REFERENCES utilisateur(id),
    recepteur_id UUID REFERENCES utilisateur(id),
    
    date_demande TIMESTAMPTZ DEFAULT NOW(),
    date_approbation TIMESTAMPTZ,
    date_expedition TIMESTAMPTZ,
    date_reception TIMESTAMPTZ,
    
    motif TEXT,
    notes TEXT
);

CREATE TABLE ligne_transfert_stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transfert_id UUID NOT NULL REFERENCES transfert_stock(id),
    article_id UUID NOT NULL REFERENCES article(id),
    lot_id UUID REFERENCES lot(id),
    
    emplacement_source_id UUID REFERENCES emplacement(id),
    emplacement_dest_id UUID REFERENCES emplacement(id),
    
    qty_demandee DECIMAL(19, 4) NOT NULL,
    qty_expedie DECIMAL(19, 4) DEFAULT 0,
    qty_recue DECIMAL(19, 4) DEFAULT 0,
    
    unit_cost DECIMAL(19, 4)
);

-- ==============================================================================
-- 13. CONSOMMATION INTERNE & REBUT
-- ==============================================================================

CREATE TABLE statut_demande_sortie (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);
INSERT INTO statut_demande_sortie VALUES 
('BROUILLON', 'Brouillon'),
('SOUMISE', 'Soumise'),
('APPROUVEE', 'Approuvee'),
('REJETEE', 'Rejetee'),
('EXECUTEE', 'Executee');

CREATE TABLE motif_sortie (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL -- 'CONSOMMATION' ou 'REBUT'
);
INSERT INTO motif_sortie VALUES 
('CONSO_INTERNE', 'Consommation interne (fournitures bureau)', 'CONSOMMATION'),
('CONSO_PRODUCTION', 'Consommation production', 'CONSOMMATION'),
('CONSO_DEMO', 'Demonstration / echantillon', 'CONSOMMATION'),
('REBUT_CASSE', 'Produit casse', 'REBUT'),
('REBUT_PERIME', 'Produit perime', 'REBUT'),
('REBUT_OBSOLETE', 'Produit obsolete', 'REBUT'),
('REBUT_QUALITE', 'Non-conformite qualite', 'REBUT'),
('REBUT_VOL', 'Vol / Perte', 'REBUT');

CREATE TABLE demande_sortie_stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL, -- 'CONSOMMATION' ou 'REBUT'
    
    depot_id UUID NOT NULL REFERENCES depot(id),
    motif_code VARCHAR(50) NOT NULL REFERENCES motif_sortie(code),
    
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_demande_sortie(code),
    
    demandeur_id UUID NOT NULL REFERENCES utilisateur(id),
    approbateur_id UUID REFERENCES utilisateur(id),
    executeur_id UUID REFERENCES utilisateur(id),
    
    date_demande TIMESTAMPTZ DEFAULT NOW(),
    date_approbation TIMESTAMPTZ,
    date_execution TIMESTAMPTZ,
    
    justification TEXT NOT NULL,
    commentaire_approbation TEXT,
    
    -- Coût total de la perte (calcule)
    cout_total DECIMAL(19, 2)
);

CREATE TABLE ligne_demande_sortie (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    demande_id UUID NOT NULL REFERENCES demande_sortie_stock(id),
    article_id UUID NOT NULL REFERENCES article(id),
    lot_id UUID REFERENCES lot(id),
    emplacement_id UUID REFERENCES emplacement(id),
    
    qty_demandee DECIMAL(19, 4) NOT NULL,
    qty_executee DECIMAL(19, 4) DEFAULT 0,
    
    unit_cost DECIMAL(19, 4),
    montant DECIMAL(19, 2)
);

-- ==============================================================================
-- 14. AJUSTEMENTS STOCK PONCTUELS
-- ==============================================================================

CREATE TABLE statut_ajustement (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);
INSERT INTO statut_ajustement VALUES 
('BROUILLON', 'Brouillon'),
('SOUMIS', 'Soumis pour validation'),
('APPROUVE_NIVEAU1', 'Approuve Niveau 1'),
('APPROUVE_FINAL', 'Approuve - Validation finale'),
('REJETE', 'Rejete'),
('EXECUTE', 'Execute / Stock ajuste');

CREATE TABLE motif_ajustement (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);
INSERT INTO motif_ajustement VALUES 
('ECART_INVENTAIRE', 'ecart suite a inventaire'),
('PERTE', 'Perte / Introuvable'),
('ERREUR_SAISIE', 'Erreur de saisie'),
('REGULARISATION', 'Regularisation comptable'),
('CASSE', 'Casse non declaree'),
('AUTRE', 'Autre motif');

CREATE TABLE ajustement_stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    numero VARCHAR(50) NOT NULL UNIQUE,
    
    depot_id UUID NOT NULL REFERENCES depot(id),
    article_id UUID NOT NULL REFERENCES article(id),
    lot_id UUID REFERENCES lot(id),
    emplacement_id UUID REFERENCES emplacement(id),
    
    qty_theorique DECIMAL(19, 4) NOT NULL,
    qty_reelle DECIMAL(19, 4) NOT NULL,
    qty_ecart DECIMAL(19, 4) GENERATED ALWAYS AS (qty_reelle - qty_theorique) STORED,
    
    motif_code VARCHAR(50) NOT NULL REFERENCES motif_ajustement(code),
    statut_code VARCHAR(50) NOT NULL REFERENCES statut_ajustement(code),
    
    demandeur_id UUID NOT NULL REFERENCES utilisateur(id),
    approbateur_niveau1_id UUID REFERENCES utilisateur(id),
    approbateur_final_id UUID REFERENCES utilisateur(id),
    
    date_demande TIMESTAMPTZ DEFAULT NOW(),
    date_approbation_niveau1 TIMESTAMPTZ,
    date_approbation_finale TIMESTAMPTZ,
    date_execution TIMESTAMPTZ,
    
    justification TEXT NOT NULL,
    commentaire_approbation TEXT,
    
    unit_cost DECIMAL(19, 4),
    montant_impact DECIMAL(19, 2),
    
    -- Photo de preuve
    photo_url TEXT
);

-- Regle : Le demandeur ne peut PAS être approbateur
CREATE OR REPLACE FUNCTION check_ajustement_separation_taches()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.approbateur_niveau1_id = NEW.demandeur_id THEN
        RAISE EXCEPTION 'Le demandeur ne peut pas approuver son propre ajustement';
    END IF;
    IF NEW.approbateur_final_id = NEW.demandeur_id THEN
        RAISE EXCEPTION 'Le demandeur ne peut pas approuver son propre ajustement';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_ajustement_separation
BEFORE INSERT OR UPDATE ON ajustement_stock
FOR EACH ROW EXECUTE FUNCTION check_ajustement_separation_taches();

-- ==============================================================================
-- ReGLES DE SePARATION DES TÂCHES - MODULE VENTE
-- ==============================================================================

-- Regle : L'encaisseur ne peut pas être le commercial qui a cree la commande
CREATE OR REPLACE FUNCTION check_encaissement_separation_taches()
RETURNS TRIGGER AS $$
DECLARE
    v_commercial_id UUID;
BEGIN
    -- Recuperer le commercial de la commande associee a la facture
    SELECT c.commercial_id INTO v_commercial_id
    FROM facture_client f
    LEFT JOIN commande_client c ON c.id = f.commande_id
    WHERE f.id = NEW.facture_id;
    
    IF v_commercial_id IS NOT NULL AND v_commercial_id = NEW.encaisseur_id THEN
        RAISE EXCEPTION 'Le commercial qui a cree la commande ne peut pas encaisser le paiement';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_encaissement_separation
BEFORE INSERT OR UPDATE ON encaissement_client
FOR EACH ROW EXECUTE FUNCTION check_encaissement_separation_taches();

-- Regle : Le validateur ne peut pas être le createur du retour
CREATE OR REPLACE FUNCTION check_retour_separation_taches()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.approbateur_id = NEW.demandeur_id THEN
        RAISE EXCEPTION 'Le demandeur ne peut pas approuver son propre retour/avoir';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_retour_separation
BEFORE INSERT OR UPDATE ON retour_client
FOR EACH ROW EXECUTE FUNCTION check_retour_separation_taches();