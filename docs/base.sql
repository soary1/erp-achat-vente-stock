CREATE SCHEMA IF NOT EXISTS gestion;

-- =========================
-- 1) ORGANISATION (multi-entités / multi-sites / multi-dépôts)
-- =========================

CREATE TABLE gestion.entite_legale (
  id_entite_legale BIGSERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  nom VARCHAR(200) NOT NULL,
  identifiant_fiscal VARCHAR(80),
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE gestion.site (
  id_site BIGSERIAL PRIMARY KEY,
  id_entite_legale BIGINT NOT NULL REFERENCES gestion.entite_legale(id_entite_legale),
  code VARCHAR(50) NOT NULL,
  nom VARCHAR(200) NOT NULL,
  adresse TEXT,
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (id_entite_legale, code)
);

CREATE TABLE gestion.depot (
  id_depot BIGSERIAL PRIMARY KEY,
  id_site BIGINT NOT NULL REFERENCES gestion.site(id_site),
  code VARCHAR(50) NOT NULL,
  nom VARCHAR(200) NOT NULL,
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (id_site, code)
);

CREATE TABLE gestion.emplacement (
  id_emplacement BIGSERIAL PRIMARY KEY,
  id_depot BIGINT NOT NULL REFERENCES gestion.depot(id_depot),
  code VARCHAR(80) NOT NULL,
  zone VARCHAR(80),
  allee VARCHAR(80),
  rack VARCHAR(80),
  niveau VARCHAR(80),
  bac VARCHAR(80),
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (id_depot, code)
);

CREATE TABLE gestion.departement (
  id_departement BIGSERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  nom VARCHAR(200) NOT NULL
);

-- =========================
-- 2) UTILISATEURS / ROLES (RBAC + ABAC via périmètres)
-- =========================

CREATE TABLE gestion.utilisateur (
  id_utilisateur BIGSERIAL PRIMARY KEY,
  identifiant VARCHAR(80) NOT NULL UNIQUE,
  nom_complet VARCHAR(200) NOT NULL,
  email VARCHAR(200),
  telephone VARCHAR(50),
  id_departement BIGINT REFERENCES gestion.departement(id_departement),
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  bloque BOOLEAN NOT NULL DEFAULT FALSE,
  tentatives INT NOT NULL DEFAULT 0,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  derniere_connexion TIMESTAMPTZ
);

CREATE TABLE gestion.role (
  id_role BIGSERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  libelle VARCHAR(200) NOT NULL
);

CREATE TABLE gestion.utilisateur_role (
  id_utilisateur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur) ON DELETE CASCADE,
  id_role BIGINT NOT NULL REFERENCES gestion.role(id_role) ON DELETE CASCADE,
  PRIMARY KEY (id_utilisateur, id_role)
);

-- ABAC : restrictions par site/dépôt/entité/famille/ plafond montant
CREATE TABLE gestion.perimetre_acces (
  id_perimetre BIGSERIAL PRIMARY KEY,
  id_utilisateur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur) ON DELETE CASCADE,
  id_entite_legale BIGINT REFERENCES gestion.entite_legale(id_entite_legale),
  id_site BIGINT REFERENCES gestion.site(id_site),
  id_depot BIGINT REFERENCES gestion.depot(id_depot),
  code_famille_article VARCHAR(80),
  plafond_montant NUMERIC(18,2),
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE gestion.delegation_acces (
  id_delegation BIGSERIAL PRIMARY KEY,
  id_donneur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  id_receveur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  id_role BIGINT NOT NULL REFERENCES gestion.role(id_role),
  justification TEXT NOT NULL,
  debut TIMESTAMPTZ NOT NULL DEFAULT now(),
  fin TIMESTAMPTZ NOT NULL,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (fin > debut)
);

-- =========================
-- 3) REFERENTIELS
-- =========================

CREATE TABLE gestion.unite (
  id_unite BIGSERIAL PRIMARY KEY,
  code VARCHAR(30) NOT NULL UNIQUE,
  libelle VARCHAR(100) NOT NULL
);

CREATE TABLE gestion.taxe (
  id_taxe BIGSERIAL PRIMARY KEY,
  code VARCHAR(30) NOT NULL UNIQUE,
  libelle VARCHAR(100) NOT NULL,
  taux NUMERIC(7,4) NOT NULL CHECK (taux >= 0)
);

CREATE TABLE gestion.famille_article (
  id_famille_article BIGSERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  libelle VARCHAR(150) NOT NULL,
  lot_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
  date_peremption_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
  fefo_active BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE gestion.article (
  id_article BIGSERIAL PRIMARY KEY,
  sku VARCHAR(80) NOT NULL UNIQUE,
  libelle VARCHAR(250) NOT NULL,
  id_famille_article BIGINT REFERENCES gestion.famille_article(id_famille_article),
  id_unite BIGINT NOT NULL REFERENCES gestion.unite(id_unite),
  id_taxe BIGINT REFERENCES gestion.taxe(id_taxe),
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  perissable BOOLEAN NOT NULL DEFAULT FALSE,
  methode_valorisation VARCHAR(10) NOT NULL DEFAULT 'CUMP',
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (methode_valorisation IN ('FIFO','CUMP'))
);

CREATE TABLE gestion.fournisseur (
  id_fournisseur BIGSERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  nom VARCHAR(250) NOT NULL,
  identifiant_fiscal VARCHAR(80),
  adresse TEXT,
  telephone VARCHAR(50),
  email VARCHAR(200),
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE gestion.client (
  id_client BIGSERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  nom VARCHAR(250) NOT NULL,
  identifiant_fiscal VARCHAR(80),
  adresse TEXT,
  telephone VARCHAR(50),
  email VARCHAR(200),
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE gestion.liste_tarifaire (
  id_liste_tarifaire BIGSERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  libelle VARCHAR(200) NOT NULL,
  devise VARCHAR(10) NOT NULL DEFAULT 'MGA',
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE gestion.ligne_liste_tarifaire (
  id_ligne BIGSERIAL PRIMARY KEY,
  id_liste_tarifaire BIGINT NOT NULL REFERENCES gestion.liste_tarifaire(id_liste_tarifaire) ON DELETE CASCADE,
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  prix_unitaire NUMERIC(18,4) NOT NULL CHECK (prix_unitaire >= 0),
  date_debut DATE NOT NULL DEFAULT CURRENT_DATE,
  date_fin DATE,
  UNIQUE (id_liste_tarifaire, id_article, date_debut),
  CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

-- =========================
-- 4) JOURNALISATION / TRAÇABILITÉ
-- =========================

CREATE TABLE gestion.journal_audit (
  id_audit BIGSERIAL PRIMARY KEY,
  date_action TIMESTAMPTZ NOT NULL DEFAULT now(),
  id_utilisateur BIGINT REFERENCES gestion.utilisateur(id_utilisateur),
  action VARCHAR(80) NOT NULL,
  entite VARCHAR(80) NOT NULL,
  id_entite BIGINT,
  details JSONB NOT NULL DEFAULT '{}'::jsonb
);

-- =========================
-- 5) ACHATS : DA -> BC -> RECEPTION -> FACTURE FOURNISSEUR -> PAIEMENT
-- =========================

CREATE TABLE gestion.demande_achat (
  id_demande_achat BIGSERIAL PRIMARY KEY,
  numero VARCHAR(80) NOT NULL UNIQUE,
  id_entite_legale BIGINT NOT NULL REFERENCES gestion.entite_legale(id_entite_legale),
  id_site BIGINT NOT NULL REFERENCES gestion.site(id_site),
  id_departement BIGINT REFERENCES gestion.departement(id_departement),
  id_demandeur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
  justification TEXT,
  date_besoin DATE,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  soumise_le TIMESTAMPTZ,
  CHECK (statut IN ('BROUILLON','SOUMISE','APPROUVEE','REJETEE','ANNULEE'))
);

CREATE TABLE gestion.ligne_demande_achat (
  id_ligne BIGSERIAL PRIMARY KEY,
  id_demande_achat BIGINT NOT NULL REFERENCES gestion.demande_achat(id_demande_achat) ON DELETE CASCADE,
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  quantite NUMERIC(18,4) NOT NULL CHECK (quantite > 0),
  id_unite BIGINT NOT NULL REFERENCES gestion.unite(id_unite),
  prix_cible NUMERIC(18,4) CHECK (prix_cible IS NULL OR prix_cible >= 0),
  notes TEXT
);

-- Règles d’approbation paramétrables (seuils)
CREATE TABLE gestion.regle_approbation (
  id_regle BIGSERIAL PRIMARY KEY,
  scope VARCHAR(30) NOT NULL,
  niveau INT NOT NULL,
  montant_min NUMERIC(18,2) NOT NULL DEFAULT 0,
  montant_max NUMERIC(18,2),
  id_role BIGINT NOT NULL REFERENCES gestion.role(id_role),
  id_site BIGINT REFERENCES gestion.site(id_site),
  code_famille_article VARCHAR(80),
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (scope, niveau, id_role, id_site, code_famille_article),
  CHECK (scope IN ('DA','BC','AJUSTEMENT','REMISE','ANNULATION_FACTURE'))
);

CREATE TABLE gestion.etape_approbation (
  id_etape BIGSERIAL PRIMARY KEY,
  scope VARCHAR(30) NOT NULL,
  type_document VARCHAR(30) NOT NULL,
  id_document BIGINT NOT NULL,
  niveau INT NOT NULL,
  id_regle BIGINT REFERENCES gestion.regle_approbation(id_regle),
  id_approbateur BIGINT REFERENCES gestion.utilisateur(id_utilisateur),
  decision VARCHAR(15),
  decide_le TIMESTAMPTZ,
  motif TEXT,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (scope, type_document, id_document, niveau),
  CHECK (scope IN ('DA','BC','AJUSTEMENT','REMISE','ANNULATION_FACTURE')),
  CHECK (decision IN ('APPROUVEE','REJETEE') OR decision IS NULL)
);

CREATE TABLE gestion.bon_commande (
  id_bon_commande BIGSERIAL PRIMARY KEY,
  numero VARCHAR(80) NOT NULL UNIQUE,
  id_entite_legale BIGINT NOT NULL REFERENCES gestion.entite_legale(id_entite_legale),
  id_site BIGINT NOT NULL REFERENCES gestion.site(id_site),
  id_fournisseur BIGINT NOT NULL REFERENCES gestion.fournisseur(id_fournisseur),
  id_acheteur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  id_demande_achat BIGINT REFERENCES gestion.demande_achat(id_demande_achat),
  statut VARCHAR(25) NOT NULL DEFAULT 'BROUILLON',
  date_commande DATE NOT NULL DEFAULT CURRENT_DATE,
  devise VARCHAR(10) NOT NULL DEFAULT 'MGA',
  sous_total NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_taxe NUMERIC(18,2) NOT NULL DEFAULT 0,
  total NUMERIC(18,2) NOT NULL DEFAULT 0,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  soumise_le TIMESTAMPTZ,
  CHECK (statut IN ('BROUILLON','SOUMIS','APPROUVE','REJETE','PARTIELLEMENT_RECU','RECU','ANNULE'))
);

CREATE TABLE gestion.ligne_bon_commande (
  id_ligne BIGSERIAL PRIMARY KEY,
  id_bon_commande BIGINT NOT NULL REFERENCES gestion.bon_commande(id_bon_commande) ON DELETE CASCADE,
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  qte_commandee NUMERIC(18,4) NOT NULL CHECK (qte_commandee > 0),
  qte_recue NUMERIC(18,4) NOT NULL DEFAULT 0 CHECK (qte_recue >= 0),
  id_unite BIGINT NOT NULL REFERENCES gestion.unite(id_unite),
  prix_unitaire NUMERIC(18,4) NOT NULL CHECK (prix_unitaire >= 0),
  id_taxe BIGINT REFERENCES gestion.taxe(id_taxe),
  sous_total_ligne NUMERIC(18,2) NOT NULL DEFAULT 0,
  taxe_ligne NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_ligne NUMERIC(18,2) NOT NULL DEFAULT 0
);

-- =========================
-- 6) LOTS / SERIES
-- =========================

CREATE TABLE gestion.lot (
  id_lot BIGSERIAL PRIMARY KEY,
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  numero_lot VARCHAR(120) NOT NULL,
  numero_serie VARCHAR(120),
  date_fabrication DATE,
  date_peremption DATE,
  statut_qualite VARCHAR(20) NOT NULL DEFAULT 'OK',
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (id_article, numero_lot),
  CHECK (statut_qualite IN ('OK','EN_ATTENTE','REJETE'))
);

-- =========================
-- 7) RÉCEPTION (GRN) + STOCK (mouvements + quantités)
-- =========================

CREATE TABLE gestion.bon_reception (
  id_bon_reception BIGSERIAL PRIMARY KEY,
  numero VARCHAR(80) NOT NULL UNIQUE,
  id_entite_legale BIGINT NOT NULL REFERENCES gestion.entite_legale(id_entite_legale),
  id_site BIGINT NOT NULL REFERENCES gestion.site(id_site),
  id_depot BIGINT NOT NULL REFERENCES gestion.depot(id_depot),
  id_fournisseur BIGINT NOT NULL REFERENCES gestion.fournisseur(id_fournisseur),
  id_bon_commande BIGINT NOT NULL REFERENCES gestion.bon_commande(id_bon_commande),
  id_receptionneur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  recu_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  statut VARCHAR(20) NOT NULL DEFAULT 'POSTE',
  CHECK (statut IN ('BROUILLON','POSTE','ANNULE'))
);

CREATE TABLE gestion.ligne_bon_reception (
  id_ligne BIGSERIAL PRIMARY KEY,
  id_bon_reception BIGINT NOT NULL REFERENCES gestion.bon_reception(id_bon_reception) ON DELETE CASCADE,
  id_ligne_bon_commande BIGINT NOT NULL REFERENCES gestion.ligne_bon_commande(id_ligne),
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  id_emplacement BIGINT REFERENCES gestion.emplacement(id_emplacement),
  id_lot BIGINT REFERENCES gestion.lot(id_lot),
  quantite_recue NUMERIC(18,4) NOT NULL CHECK (quantite_recue > 0),
  cout_unitaire NUMERIC(18,4) NOT NULL DEFAULT 0 CHECK (cout_unitaire >= 0)
);

-- Stock: quantité par dépôt/emplacement/lot
CREATE TABLE gestion.stock (
  id_stock BIGSERIAL PRIMARY KEY,
  id_depot BIGINT NOT NULL REFERENCES gestion.depot(id_depot),
  id_emplacement BIGINT REFERENCES gestion.emplacement(id_emplacement),
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  id_lot BIGINT REFERENCES gestion.lot(id_lot),
  quantite NUMERIC(18,4) NOT NULL DEFAULT 0 CHECK (quantite >= 0),
  quantite_reservee NUMERIC(18,4) NOT NULL DEFAULT 0 CHECK (quantite_reservee >= 0),
  UNIQUE (id_depot, id_emplacement, id_article, id_lot),
  CHECK (quantite_reservee <= quantite)
);

CREATE TABLE gestion.mouvement_stock (
  id_mouvement BIGSERIAL PRIMARY KEY,
  numero VARCHAR(80) NOT NULL UNIQUE,
  type_mouvement VARCHAR(30) NOT NULL,
  reference_document VARCHAR(120),
  id_entite_legale BIGINT NOT NULL REFERENCES gestion.entite_legale(id_entite_legale),
  id_site BIGINT NOT NULL REFERENCES gestion.site(id_site),
  id_depot BIGINT NOT NULL REFERENCES gestion.depot(id_depot),
  id_utilisateur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  date_mouvement TIMESTAMPTZ NOT NULL DEFAULT now(),
  commentaire TEXT,
  CHECK (type_mouvement IN (
    'ENTREE_RECEPTION','ENTREE_RETOUR_CLIENT','ENTREE_AJUSTEMENT','ENTREE_TRANSFERT',
    'SORTIE_LIVRAISON','SORTIE_CONSO_INTERNE','SORTIE_REBUT','SORTIE_AJUSTEMENT','SORTIE_TRANSFERT'
  ))
);

CREATE TABLE gestion.ligne_mouvement_stock (
  id_ligne BIGSERIAL PRIMARY KEY,
  id_mouvement BIGINT NOT NULL REFERENCES gestion.mouvement_stock(id_mouvement) ON DELETE CASCADE,
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  id_emplacement BIGINT REFERENCES gestion.emplacement(id_emplacement),
  id_lot BIGINT REFERENCES gestion.lot(id_lot),
  quantite NUMERIC(18,4) NOT NULL CHECK (quantite > 0),
  cout_unitaire NUMERIC(18,4) NOT NULL DEFAULT 0 CHECK (cout_unitaire >= 0)
);

-- =========================
-- 8) FACTURE FOURNISSEUR + 3-WAY MATCH + PAIEMENT
-- =========================

CREATE TABLE gestion.facture_fournisseur (
  id_facture_fournisseur BIGSERIAL PRIMARY KEY,
  numero_interne VARCHAR(80) NOT NULL UNIQUE,
  id_entite_legale BIGINT NOT NULL REFERENCES gestion.entite_legale(id_entite_legale),
  id_site BIGINT NOT NULL REFERENCES gestion.site(id_site),
  id_fournisseur BIGINT NOT NULL REFERENCES gestion.fournisseur(id_fournisseur),
  numero_facture_fournisseur VARCHAR(120) NOT NULL,
  date_facture DATE NOT NULL,
  devise VARCHAR(10) NOT NULL DEFAULT 'MGA',
  statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
  sous_total NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_taxe NUMERIC(18,2) NOT NULL DEFAULT 0,
  total NUMERIC(18,2) NOT NULL DEFAULT 0,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (id_fournisseur, numero_facture_fournisseur),
  CHECK (statut IN ('BROUILLON','RAPPROCHEMENT','APPROUVEE','REJETEE','PAYEE','ANNULEE'))
);

CREATE TABLE gestion.rapprochement_achat (
  id_rapprochement BIGSERIAL PRIMARY KEY,
  id_facture_fournisseur BIGINT NOT NULL REFERENCES gestion.facture_fournisseur(id_facture_fournisseur) ON DELETE CASCADE,
  id_bon_commande BIGINT NOT NULL REFERENCES gestion.bon_commande(id_bon_commande),
  id_bon_reception BIGINT NOT NULL REFERENCES gestion.bon_reception(id_bon_reception),
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (id_facture_fournisseur, id_bon_commande, id_bon_reception)
);

CREATE TABLE gestion.paiement_fournisseur (
  id_paiement BIGSERIAL PRIMARY KEY,
  id_facture_fournisseur BIGINT NOT NULL REFERENCES gestion.facture_fournisseur(id_facture_fournisseur) ON DELETE RESTRICT,
  id_payeur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  paye_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  montant NUMERIC(18,2) NOT NULL CHECK (montant > 0),
  mode_paiement VARCHAR(30) NOT NULL,
  reference VARCHAR(120),
  CHECK (mode_paiement IN ('ESPECES','VIREMENT','CHEQUE','MOBILE_MONEY','CARTE'))
);

-- =========================
-- 9) VENTES : DEVIS -> COMMANDE -> LIVRAISON -> FACTURE -> ENCAISSEMENT
-- =========================

CREATE TABLE gestion.devis_client (
  id_devis BIGSERIAL PRIMARY KEY,
  numero VARCHAR(80) NOT NULL UNIQUE,
  id_entite_legale BIGINT NOT NULL REFERENCES gestion.entite_legale(id_entite_legale),
  id_site BIGINT NOT NULL REFERENCES gestion.site(id_site),
  id_client BIGINT NOT NULL REFERENCES gestion.client(id_client),
  id_commercial BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  id_liste_tarifaire BIGINT REFERENCES gestion.liste_tarifaire(id_liste_tarifaire),
  statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
  devise VARCHAR(10) NOT NULL DEFAULT 'MGA',
  taux_remise NUMERIC(7,4) NOT NULL DEFAULT 0 CHECK (taux_remise >= 0),
  montant_remise NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (montant_remise >= 0),
  sous_total NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_taxe NUMERIC(18,2) NOT NULL DEFAULT 0,
  total NUMERIC(18,2) NOT NULL DEFAULT 0,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (statut IN ('BROUILLON','SOUMIS','APPROUVE','REJETE','EXPIRE','ANNULE'))
);

CREATE TABLE gestion.ligne_devis_client (
  id_ligne BIGSERIAL PRIMARY KEY,
  id_devis BIGINT NOT NULL REFERENCES gestion.devis_client(id_devis) ON DELETE CASCADE,
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  quantite NUMERIC(18,4) NOT NULL CHECK (quantite > 0),
  id_unite BIGINT NOT NULL REFERENCES gestion.unite(id_unite),
  prix_unitaire NUMERIC(18,4) NOT NULL CHECK (prix_unitaire >= 0),
  sous_total_ligne NUMERIC(18,2) NOT NULL DEFAULT 0
);

CREATE TABLE gestion.commande_client (
  id_commande_client BIGSERIAL PRIMARY KEY,
  numero VARCHAR(80) NOT NULL UNIQUE,
  id_entite_legale BIGINT NOT NULL REFERENCES gestion.entite_legale(id_entite_legale),
  id_site BIGINT NOT NULL REFERENCES gestion.site(id_site),
  id_client BIGINT NOT NULL REFERENCES gestion.client(id_client),
  id_commercial BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  id_devis BIGINT REFERENCES gestion.devis_client(id_devis),
  statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
  devise VARCHAR(10) NOT NULL DEFAULT 'MGA',
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (statut IN ('BROUILLON','CONFIRMEE','EN_PREPARATION','LIVREE_PARTIELLEMENT','LIVREE','ANNULEE'))
);

CREATE TABLE gestion.ligne_commande_client (
  id_ligne BIGSERIAL PRIMARY KEY,
  id_commande_client BIGINT NOT NULL REFERENCES gestion.commande_client(id_commande_client) ON DELETE CASCADE,
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  quantite NUMERIC(18,4) NOT NULL CHECK (quantite > 0),
  id_unite BIGINT NOT NULL REFERENCES gestion.unite(id_unite),
  prix_unitaire NUMERIC(18,4) NOT NULL CHECK (prix_unitaire >= 0)
);

-- Réservation stock (par ligne commande)
CREATE TABLE gestion.reservation_stock (
  id_reservation BIGSERIAL PRIMARY KEY,
  id_commande_client BIGINT NOT NULL REFERENCES gestion.commande_client(id_commande_client) ON DELETE CASCADE,
  id_ligne_commande BIGINT NOT NULL REFERENCES gestion.ligne_commande_client(id_ligne),
  id_depot BIGINT NOT NULL REFERENCES gestion.depot(id_depot),
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  id_lot BIGINT REFERENCES gestion.lot(id_lot),
  quantite_reservee NUMERIC(18,4) NOT NULL CHECK (quantite_reservee > 0),
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE gestion.bon_livraison (
  id_bon_livraison BIGSERIAL PRIMARY KEY,
  numero VARCHAR(80) NOT NULL UNIQUE,
  id_entite_legale BIGINT NOT NULL REFERENCES gestion.entite_legale(id_entite_legale),
  id_site BIGINT NOT NULL REFERENCES gestion.site(id_site),
  id_depot BIGINT NOT NULL REFERENCES gestion.depot(id_depot),
  id_commande_client BIGINT NOT NULL REFERENCES gestion.commande_client(id_commande_client),
  id_preparateur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  livre_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  statut VARCHAR(20) NOT NULL DEFAULT 'POSTE',
  CHECK (statut IN ('BROUILLON','POSTE','ANNULE'))
);

CREATE TABLE gestion.ligne_bon_livraison (
  id_ligne BIGSERIAL PRIMARY KEY,
  id_bon_livraison BIGINT NOT NULL REFERENCES gestion.bon_livraison(id_bon_livraison) ON DELETE CASCADE,
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  id_emplacement BIGINT REFERENCES gestion.emplacement(id_emplacement),
  id_lot BIGINT REFERENCES gestion.lot(id_lot),
  quantite_livree NUMERIC(18,4) NOT NULL CHECK (quantite_livree > 0)
);

CREATE TABLE gestion.facture_client (
  id_facture_client BIGSERIAL PRIMARY KEY,
  numero VARCHAR(80) NOT NULL UNIQUE,
  id_entite_legale BIGINT NOT NULL REFERENCES gestion.entite_legale(id_entite_legale),
  id_site BIGINT NOT NULL REFERENCES gestion.site(id_site),
  id_client BIGINT NOT NULL REFERENCES gestion.client(id_client),
  id_commande_client BIGINT REFERENCES gestion.commande_client(id_commande_client),
  date_facture DATE NOT NULL DEFAULT CURRENT_DATE,
  devise VARCHAR(10) NOT NULL DEFAULT 'MGA',
  statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
  sous_total NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_taxe NUMERIC(18,2) NOT NULL DEFAULT 0,
  total NUMERIC(18,2) NOT NULL DEFAULT 0,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (statut IN ('BROUILLON','VALIDEE','ANNULEE','PAYEE'))
);

CREATE TABLE gestion.encaissement_client (
  id_encaissement BIGSERIAL PRIMARY KEY,
  id_facture_client BIGINT NOT NULL REFERENCES gestion.facture_client(id_facture_client) ON DELETE RESTRICT,
  id_encaisseur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  encaisse_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  montant NUMERIC(18,2) NOT NULL CHECK (montant > 0),
  mode_paiement VARCHAR(30) NOT NULL,
  reference VARCHAR(120),
  CHECK (mode_paiement IN ('ESPECES','VIREMENT','CHEQUE','MOBILE_MONEY','CARTE'))
);

-- =========================
-- 10) INVENTAIRES + AJUSTEMENTS CONTROLES
-- =========================

CREATE TABLE gestion.inventaire (
  id_inventaire BIGSERIAL PRIMARY KEY,
  numero VARCHAR(80) NOT NULL UNIQUE,
  id_entite_legale BIGINT NOT NULL REFERENCES gestion.entite_legale(id_entite_legale),
  id_site BIGINT NOT NULL REFERENCES gestion.site(id_site),
  id_depot BIGINT NOT NULL REFERENCES gestion.depot(id_depot),
  type_inventaire VARCHAR(20) NOT NULL,
  statut VARCHAR(20) NOT NULL DEFAULT 'PLANIFIE',
  lance_le TIMESTAMPTZ,
  cloture_le TIMESTAMPTZ,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (type_inventaire IN ('TOURNANT','ANNUEL')),
  CHECK (statut IN ('PLANIFIE','EN_COURS','CLOTURE','ANNULE'))
);

CREATE TABLE gestion.ligne_inventaire (
  id_ligne BIGSERIAL PRIMARY KEY,
  id_inventaire BIGINT NOT NULL REFERENCES gestion.inventaire(id_inventaire) ON DELETE CASCADE,
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  id_lot BIGINT REFERENCES gestion.lot(id_lot),
  id_emplacement BIGINT REFERENCES gestion.emplacement(id_emplacement),
  qte_theorique NUMERIC(18,4) NOT NULL DEFAULT 0 CHECK (qte_theorique >= 0),
  qte_physique NUMERIC(18,4) CHECK (qte_physique IS NULL OR qte_physique >= 0),
  ecart NUMERIC(18,4) NOT NULL DEFAULT 0
);

CREATE TABLE gestion.ajustement_stock (
  id_ajustement BIGSERIAL PRIMARY KEY,
  numero VARCHAR(80) NOT NULL UNIQUE,
  id_inventaire BIGINT REFERENCES gestion.inventaire(id_inventaire),
  id_depot BIGINT NOT NULL REFERENCES gestion.depot(id_depot),
  id_createur BIGINT NOT NULL REFERENCES gestion.utilisateur(id_utilisateur),
  statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
  justification TEXT NOT NULL,
  cree_le TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (statut IN ('BROUILLON','SOUMIS','APPROUVE','REJETE','POSTE','ANNULE'))
);

CREATE TABLE gestion.ligne_ajustement_stock (
  id_ligne BIGSERIAL PRIMARY KEY,
  id_ajustement BIGINT NOT NULL REFERENCES gestion.ajustement_stock(id_ajustement) ON DELETE CASCADE,
  id_article BIGINT NOT NULL REFERENCES gestion.article(id_article),
  id_emplacement BIGINT REFERENCES gestion.emplacement(id_emplacement),
  id_lot BIGINT REFERENCES gestion.lot(id_lot),
  quantite_delta NUMERIC(18,4) NOT NULL CHECK (quantite_delta <> 0),
  cout_unitaire NUMERIC(18,4) NOT NULL DEFAULT 0 CHECK (cout_unitaire >= 0)
);

-- =========================
-- Index utiles (volumétrie élevée)
-- =========================
CREATE INDEX IF NOT EXISTS idx_stock_article ON gestion.stock(id_article);
CREATE INDEX IF NOT EXISTS idx_mvt_date ON gestion.mouvement_stock(date_mouvement);
CREATE INDEX IF NOT EXISTS idx_pr_site ON gestion.demande_achat(id_site);
CREATE INDEX IF NOT EXISTS idx_po_site ON gestion.bon_commande(id_site);
CREATE INDEX IF NOT EXISTS idx_so_site ON gestion.commande_client(id_site);
