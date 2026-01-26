-- ==============================================================================
-- DATA SEED - ERP MADAGASCAR "MADA-DISTRIBUTION"
-- Conformité : Cahier des Charges (PDF) + Structure Hiérarchique
-- ==============================================================================

DO $$
DECLARE
    -- ==========================================
    -- VARIABLES DE RECUPERATION DES IDs (UUID)
    -- ==========================================
    
    -- Organisation
    v_groupe_id UUID;
    v_soc_id UUID;
    v_site_tana_id UUID;
    v_site_tmm_id UUID;
    v_depot_tanjo_id UUID;
    v_depot_port_id UUID;
    v_emp_A1_id UUID;
    v_emp_QUAR_id UUID;

    -- Départements
    v_dept_dir_id UUID;
    v_dept_fin_id UUID;
    v_dept_ach_id UUID;
    v_dept_vte_id UUID;
    v_dept_mag_id UUID;

    -- Rôles Hiérarchiques
    v_role_op_id UUID;
    v_role_sup_id UUID;
    v_role_mgr_id UUID;
    v_role_dir_id UUID;

    -- Utilisateurs (Acteurs du PDF)
    v_user_dg_id UUID;       -- Direction
    v_user_daf_id UUID;      -- Finance Manager
    v_user_compta_id UUID;   -- Finance Op
    v_user_ach_mgr_id UUID;  -- Achat Manager
    v_user_ach_op_id UUID;   -- Achat Op (Demandeur)
    v_user_mag_chef_id UUID; -- Magasin Superviseur
    v_user_mag_op_id UUID;   -- Magasin Op
    v_user_vte_mgr_id UUID;  -- Vente Manager
    v_user_vte_op_id UUID;   -- Vente Op

    -- Articles & Familles
    v_fam_ppn_id UUID;
    v_fam_hitech_id UUID;
    v_art_riz_id UUID;
    v_art_laptop_id UUID;

    -- Tiers
    v_frs_china_id UUID;
    v_frs_local_id UUID;
    v_cli_b2b_id UUID;
    v_cli_retail_id UUID;

    -- Flux Transactionnels (Pour lier les tables)
    v_da_id UUID;
    v_cmd_achat_id UUID;
    v_reception_id UUID;
    v_lot_riz_id UUID;
    v_lot_pc_id UUID;
    v_facture_frs_id UUID;
    v_cmd_vente_id UUID;
    v_bl_id UUID;

BEGIN

    -- ==========================================================================
    -- 1. REFERENTIELS GLOBAUX (Standards ISO & Malgaches)
    -- ==========================================================================
    
    -- Devises
    -- MGA est déjà inséré par le schéma, on s'assure que USD/EUR sont là
    INSERT INTO devise (code, label, symbol) VALUES ('CNY', 'Yuan Chinois', '¥') ON CONFLICT DO NOTHING;

    -- Taxes (TVA 20% standard Mada)
    -- Déjà inséré dans le schéma, on ajoute une retenue à la source si besoin
    INSERT INTO type_taxe (code, label, rate) VALUES ('AIRSI_5', 'AIRSI 5%', 0.05) ON CONFLICT DO NOTHING;

    -- Modes de Paiement (Spécificité Mada : Mobile Money)
    INSERT INTO mode_paiement (code, label) VALUES ('MVOLA', 'MVola (Mobile)') ON CONFLICT DO NOTHING;
    INSERT INTO mode_paiement (code, label) VALUES ('ORANGE_MONEY', 'Orange Money') ON CONFLICT DO NOTHING;

    -- ==========================================================================
    -- 2. ORGANISATION (Multi-site / Multi-dépôt - PDF §3)
    -- ==========================================================================

    -- Groupe
    INSERT INTO groupe_societe (code, name) 
    VALUES ('GRP_MADA', 'Groupe Mada-Distribution') 
    RETURNING id INTO v_groupe_id;

    -- Société Légale
    INSERT INTO societe (groupe_id, code, name, tax_id, pays_code, devise_code)
    VALUES (v_groupe_id, 'MADA_DIS', 'Mada Distribution SA', 'NIF 3000123456 STAT 51100', 'MG', 'MGA')
    RETURNING id INTO v_soc_id;

    -- Site 1 : Siège (Administratif)
    INSERT INTO site (societe_id, code, name, address, geo_location)
    VALUES (v_soc_id, 'SITE_ANDRA', 'Siège Andraharo', 'Zone Galaxy, Antananarivo 101', ST_SetSRID(ST_MakePoint(47.51, -18.88), 4326))
    RETURNING id INTO v_site_tana_id;

    -- Site 2 : Logistique (Port de Tamatave)
    INSERT INTO site (societe_id, code, name, address, geo_location)
    VALUES (v_soc_id, 'SITE_TMM', 'Hub Logistique Toamasina', 'Bd Joffre, Toamasina', ST_SetSRID(ST_MakePoint(49.40, -18.15), 4326))
    RETURNING id INTO v_site_tmm_id;

    -- Dépôts
    INSERT INTO depot (site_id, code, name) 
    VALUES (v_site_tana_id, 'DEP_TANJO', 'Entrepôt Central Tanjombato') 
    RETURNING id INTO v_depot_tanjo_id;

    INSERT INTO depot (site_id, code, name) 
    VALUES (v_site_tmm_id, 'DEP_PORT', 'Entrepôt Douane Port') 
    RETURNING id INTO v_depot_port_id;

    -- Emplacements (Gestion fine stock - PDF §5.1)
    INSERT INTO emplacement (depot_id, code, aisle, rack, shelf) VALUES (v_depot_tanjo_id, 'A-01-01', 'A', '01', '01') RETURNING id INTO v_emp_A1_id;
    INSERT INTO emplacement (depot_id, code, aisle, rack, shelf) VALUES (v_depot_tanjo_id, 'QUARANTAINE', 'Z', '99', '99') RETURNING id INTO v_emp_QUAR_id;

    -- ==========================================================================
    -- 3. STRUCTURE RH & SECURITE (Matrice Rôles - PDF §4)
    -- ==========================================================================

    -- 3.1 Départements
    INSERT INTO departement (code, name) VALUES ('DIR', 'Direction Générale') RETURNING id INTO v_dept_dir_id;
    INSERT INTO departement (code, name) VALUES ('FIN', 'Finance & Compta') RETURNING id INTO v_dept_fin_id;
    INSERT INTO departement (code, name) VALUES ('ACH', 'Achats & Appro') RETURNING id INTO v_dept_ach_id;
    INSERT INTO departement (code, name) VALUES ('VTE', 'Ventes & Commerce') RETURNING id INTO v_dept_vte_id;
    INSERT INTO departement (code, name) VALUES ('MAG', 'Logistique & Magasin') RETURNING id INTO v_dept_mag_id;

    -- 3.2 Hiérarchie (Roles)
    INSERT INTO role (code, label) VALUES ('DIRECTEUR', 'Directeur (C-Level)') RETURNING id INTO v_role_dir_id;
    INSERT INTO role (code, label) VALUES ('MANAGER', 'Responsable de Service') RETURNING id INTO v_role_mgr_id;
    INSERT INTO role (code, label) VALUES ('SUPERVISEUR', 'Chef d''équipe / Superviseur') RETURNING id INTO v_role_sup_id;
    INSERT INTO role (code, label) VALUES ('OPERATEUR', 'Opérateur / Agent') RETURNING id INTO v_role_op_id;

    -- 3.3 Utilisateurs (Casting Réaliste)
    
    -- DIRECTION : Andry (DG) - Valide tout
    INSERT INTO utilisateur (username, email, password_hash, departement_id) 
    VALUES ('andry.dg', 'dg@madadis.mg', 'hash1', v_dept_dir_id) RETURNING id INTO v_user_dg_id;
    INSERT INTO utilisateur_role VALUES (v_user_dg_id, v_role_dir_id);

    -- FINANCE : Bakoly (DAF) et Mamy (Comptable)
    INSERT INTO utilisateur (username, email, password_hash, departement_id) 
    VALUES ('bakoly.daf', 'daf@madadis.mg', 'hash2', v_dept_fin_id) RETURNING id INTO v_user_daf_id;
    INSERT INTO utilisateur_role VALUES (v_user_daf_id, v_role_mgr_id);

    INSERT INTO utilisateur (username, email, password_hash, departement_id) 
    VALUES ('mamy.compta', 'compta@madadis.mg', 'hash3', v_dept_fin_id) RETURNING id INTO v_user_compta_id;
    INSERT INTO utilisateur_role VALUES (v_user_compta_id, v_role_op_id);

    -- ACHATS : Faly (Mgr) et Sitraka (Op)
    INSERT INTO utilisateur (username, email, password_hash, departement_id) 
    VALUES ('faly.ach', 'achat.mgr@madadis.mg', 'hash4', v_dept_ach_id) RETURNING id INTO v_user_ach_mgr_id;
    INSERT INTO utilisateur_role VALUES (v_user_ach_mgr_id, v_role_mgr_id);

    INSERT INTO utilisateur (username, email, password_hash, departement_id) 
    VALUES ('sitraka.ach', 'achat.op@madadis.mg', 'hash5', v_dept_ach_id) RETURNING id INTO v_user_ach_op_id;
    INSERT INTO utilisateur_role VALUES (v_user_ach_op_id, v_role_op_id);

    -- MAGASIN : Tiana (Chef Mag) et Koto (Manutention)
    INSERT INTO utilisateur (username, email, password_hash, departement_id) 
    VALUES ('tiana.stock', 'stock.chef@madadis.mg', 'hash6', v_dept_mag_id) RETURNING id INTO v_user_mag_chef_id;
    INSERT INTO utilisateur_role VALUES (v_user_mag_chef_id, v_role_sup_id);

    INSERT INTO utilisateur (username, email, password_hash, departement_id) 
    VALUES ('koto.stock', 'magasinier@madadis.mg', 'hash7', v_dept_mag_id) RETURNING id INTO v_user_mag_op_id;
    INSERT INTO utilisateur_role VALUES (v_user_mag_op_id, v_role_op_id);

    -- VENTES : Soa (Mgr) et Rivo (Commercial)
    INSERT INTO utilisateur (username, email, password_hash, departement_id) 
    VALUES ('soa.vte', 'sales.mgr@madadis.mg', 'hash8', v_dept_vte_id) RETURNING id INTO v_user_vte_mgr_id;
    INSERT INTO utilisateur_role VALUES (v_user_vte_mgr_id, v_role_mgr_id);

    INSERT INTO utilisateur (username, email, password_hash, departement_id) 
    VALUES ('rivo.vte', 'commercial@madadis.mg', 'hash9', v_dept_vte_id) RETURNING id INTO v_user_vte_op_id;
    INSERT INTO utilisateur_role VALUES (v_user_vte_op_id, v_role_op_id);

    -- Règles d'approbation (ABAC - PDF §4.1)
    -- Le Manager Achats valide jusqu'à 50M Ar, au delà c'est le DG
    INSERT INTO regle_approbation (societe_id, document_type, min_amount, max_amount, role_id, level_index)
    VALUES (v_soc_id, 'DEMANDE_ACHAT', 0, 50000000, v_role_mgr_id, 1);

    INSERT INTO regle_approbation (societe_id, document_type, min_amount, max_amount, role_id, level_index)
    VALUES (v_soc_id, 'DEMANDE_ACHAT', 50000000, 9999999999, v_role_dir_id, 2);


    -- ==========================================================================
    -- 4. PRODUITS & TIERS (Le catalogue)
    -- ==========================================================================

    -- Familles (CUMP pour PPN, FIFO pour Tech - PDF §5.2)
    INSERT INTO famille_article (code, name, methode_valorisation_code, is_lot_obligatoire, is_peremption_obligatoire)
    VALUES ('PPN', 'Produits Première Nécessité', 'CUMP', TRUE, TRUE) RETURNING id INTO v_fam_ppn_id;

    INSERT INTO famille_article (code, name, methode_valorisation_code, is_lot_obligatoire)
    VALUES ('HITECH', 'Informatique & Technologie', 'FIFO', TRUE) RETURNING id INTO v_fam_hitech_id;

    -- Articles
    -- Article 1 : Riz (Gestion par lot + péremption)
    INSERT INTO article (societe_id, famille_id, unite_code, taxe_vente_code, taxe_achat_code, sku, label, weight)
    VALUES (v_soc_id, v_fam_ppn_id, 'KG', 'EXO', 'EXO', 'RIZ-LUX-50', 'Riz Luxury 50kg', 50.0)
    RETURNING id INTO v_art_riz_id;

    -- Article 2 : Laptop (Gestion par numéro de série)
    INSERT INTO article (societe_id, famille_id, unite_code, taxe_vente_code, taxe_achat_code, sku, label, weight)
    VALUES (v_soc_id, v_fam_hitech_id, 'PCE', 'TVA_20', 'TVA_20', 'HP-PROBOOK', 'HP Probook 450 G9', 2.5)
    RETURNING id INTO v_art_laptop_id;

    -- Fournisseurs
    INSERT INTO fournisseur (code, name, tax_id, devise_code) 
    VALUES ('FRS_CHINA', 'Shenzhen Tech Export', 'CN-8899', 'USD') RETURNING id INTO v_frs_china_id;

    INSERT INTO fournisseur (code, name, tax_id, devise_code) 
    VALUES ('FRS_LOCAL', 'Tiko Agri', 'NIF 111222', 'MGA') RETURNING id INTO v_frs_local_id;

    -- Clients
    INSERT INTO client (code, name, tax_id, devise_code)
    VALUES ('CLI_JUMBO', 'Jumbo Score', 'NIF 999888', 'MGA') RETURNING id INTO v_cli_b2b_id;

    INSERT INTO client (code, name, tax_id, devise_code)
    VALUES ('CLI_SHOP', 'Supermaki', 'NIF 777666', 'MGA') RETURNING id INTO v_cli_retail_id;


    -- ==========================================================================
    -- 5. FLUX ACHAT - SCENARIO COMPLET (PDF §2)
    -- Opérateur -> Validation Manager -> Commande -> Réception -> Facture
    -- ==========================================================================

    -- 5.1 Demande d'achat (Créée par Sitraka - Opérateur)
    INSERT INTO demande_achat (numero, demandeur_id, site_id, statut_code)
    VALUES ('DA-2401-001', v_user_ach_op_id, v_site_tana_id, 'APPROUVEE') -- Supposons déjà approuvée
    RETURNING id INTO v_da_id;

    -- Historique Workflow (Traçabilité - PDF §4.1)
    INSERT INTO historique_workflow (document_type, document_id, etape_nouvelle, acteur_id, action, commentaire)
    VALUES 
    ('DEMANDE_ACHAT', v_da_id, 'SOUMISE', v_user_ach_op_id, 'SOUMISSION', 'Besoin urgent pour stock riz'),
    ('DEMANDE_ACHAT', v_da_id, 'APPROUVEE', v_user_ach_mgr_id, 'APPROBATION', 'Budget validé OK');

    -- 5.2 Commande d'Achat (Générée par Faly - Manager)
    INSERT INTO commande_achat (numero, demande_achat_id, fournisseur_id, site_id, acheteur_id, devise_code, total_ht, total_ttc, statut_code)
    VALUES ('BC-2401-088', v_da_id, v_frs_local_id, v_site_tana_id, v_user_ach_mgr_id, 'MGA', 10000000, 10000000, 'ENVOYEE')
    RETURNING id INTO v_cmd_achat_id;

    INSERT INTO ligne_commande_achat (commande_id, article_id, qty_ordered, unit_price, taxe_code)
    VALUES (v_cmd_achat_id, v_art_riz_id, 100, 100000, 'EXO'); -- 100 sacs à 100.000 Ar

    -- 5.3 Réception (Effectuée par Tiana - Chef Magasin)
    -- Création du Lot entrant
    INSERT INTO lot (article_id, numero_lot, date_fabrication, date_peremption, statut_qualite_code)
    VALUES (v_art_riz_id, 'LOT-RIZ-DEC23', '2023-12-01', '2025-12-01', 'CONFORME')
    RETURNING id INTO v_lot_riz_id;

    -- Bon de Réception
    INSERT INTO bon_reception (numero, commande_achat_id, site_id, depot_id, statut_code)
    VALUES ('BR-2401-088', v_cmd_achat_id, v_site_tana_id, v_depot_tanjo_id, 'VALIDE')
    RETURNING id INTO v_reception_id;

    INSERT INTO ligne_bon_reception (bon_reception_id, article_id, lot_id, emplacement_id, qty_received)
    VALUES (v_reception_id, v_art_riz_id, v_lot_riz_id, v_emp_A1_id, 100);

    -- Mouvement de Stock (Trace complète - PDF §5.1)
    INSERT INTO mouvement_stock (type_mouvement_code, reference_doc, article_id, lot_id, depot_dest_id, emplacement_dest_id, qty, unit_cost, utilisateur_id)
    VALUES ('RECEPTION', 'BR-2401-088', v_art_riz_id, v_lot_riz_id, v_depot_tanjo_id, v_emp_A1_id, 100, 100000, v_user_mag_chef_id);

    -- Incrément Stock Réel
    INSERT INTO stock (depot_id, emplacement_id, article_id, lot_id, qty_reel)
    VALUES (v_depot_tanjo_id, v_emp_A1_id, v_art_riz_id, v_lot_riz_id, 100);

    -- 5.4 Facturation Fournisseur (Saisie par Mamy - Compta)
    INSERT INTO facture_fournisseur (ref_interne, ref_fournisseur, fournisseur_id, commande_achat_id, montant_ht, montant_ttc, devise_code, statut_code, date_facture)
    VALUES ('FAC-AGRI-001', 'INV-12345', v_frs_local_id, v_cmd_achat_id, 10000000, 10000000, 'MGA', 'A_PAYER', CURRENT_DATE)
    RETURNING id INTO v_facture_frs_id;

    -- Rapprochement (3-way match - PDF §4.3)
    INSERT INTO rapprochement_achat (facture_id, reception_id, montant_rapproche)
    VALUES (v_facture_frs_id, v_reception_id, 10000000);


    -- ==========================================================================
    -- 6. FLUX VENTE - SCENARIO B2B
    -- Devis -> Commande -> Réservation -> Livraison
    -- ==========================================================================

    -- 6.1 Commande Client (Rivo - Commercial)
    INSERT INTO commande_client (numero, client_id, site_id, statut_code)
    VALUES ('CMD-CLI-500', v_cli_b2b_id, v_site_tana_id, 'CONFIRMEE')
    RETURNING id INTO v_cmd_vente_id;

    INSERT INTO ligne_commande_client (commande_id, article_id, qty_ordered, price_unit)
    VALUES (v_cmd_vente_id, v_art_riz_id, 10, 120000); -- Marge de 20%

    -- 6.2 Réservation de Stock (Automatique)
    INSERT INTO reservation_stock (ligne_commande_id, article_id, depot_id, lot_id, qty_reservee)
    VALUES (
        (SELECT id FROM ligne_commande_client WHERE commande_id = v_cmd_vente_id LIMIT 1),
        v_art_riz_id, v_depot_tanjo_id, v_lot_riz_id, 10
    );

    -- Mise à jour table stock (Qty Reserved)
    UPDATE stock SET qty_reserve = qty_reserve + 10 
    WHERE depot_id = v_depot_tanjo_id AND lot_id = v_lot_riz_id;

    -- 6.3 Livraison (BL) - (Koto - Opérateur Magasin)
    INSERT INTO bon_livraison (numero, commande_id, date_expedition)
    VALUES ('BL-CLI-500', v_cmd_vente_id, NOW())
    RETURNING id INTO v_bl_id;

    INSERT INTO ligne_bon_livraison (livraison_id, article_id, lot_id, qty_livree)
    VALUES (v_bl_id, v_art_riz_id, v_lot_riz_id, 10);

    -- Mouvement de sortie
    INSERT INTO mouvement_stock (type_mouvement_code, reference_doc, article_id, lot_id, depot_source_id, emplacement_source_id, qty, utilisateur_id)
    VALUES ('EXPEDITION', 'BL-CLI-500', v_art_riz_id, v_lot_riz_id, v_depot_tanjo_id, v_emp_A1_id, -10, v_user_mag_op_id);

    -- Décrément Stock (On enlève 10 du réel et 10 du réservé)
    UPDATE stock SET qty_reel = qty_reel - 10, qty_reserve = qty_reserve - 10
    WHERE depot_id = v_depot_tanjo_id AND lot_id = v_lot_riz_id;

    -- ==========================================================================
    -- 7. AUDIT & INVENTAIRE (Cas de fraude/erreur - PDF §6)
    -- ==========================================================================

    -- Scénario : Inventaire inopiné demandé par le DAF
    -- Stock théorique restant : 90. Stock compté : 88.
    
    DECLARE v_inv_id UUID;
    BEGIN
        INSERT INTO inventaire (numero, site_id, depot_id, type_code, statut_code, cree_par)
        VALUES ('INV-SPOT-001', v_site_tana_id, v_depot_tanjo_id, 'SPOT', 'ANALYSE', v_user_daf_id)
        RETURNING id INTO v_inv_id;

        INSERT INTO ligne_inventaire (inventaire_id, article_id, emplacement_id, lot_id, qty_theorique, qty_reelle_retenue, arbitrage_par, notes_arbitrage)
        VALUES (v_inv_id, v_art_riz_id, v_emp_A1_id, v_lot_riz_id, 90, 88, v_user_dg_id, 'Ecart inexpliqué - 2 sacs manquants. Enquête ouverte.');
        
        -- Trace de la saisie par l'opérateur Koto
        INSERT INTO saisie_inventaire (ligne_inventaire_id, operateur_id, qty_comptee)
        VALUES (
            (SELECT id FROM ligne_inventaire WHERE inventaire_id = v_inv_id LIMIT 1),
            v_user_mag_op_id,
            88
        );
    END;

END $$;