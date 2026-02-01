DO $$
DECLARE
    -- ==========================================
    -- VARIABLES (Identique au precedent)
    -- ==========================================
    v_groupe_id UUID; v_soc_id UUID; v_site_tana_id UUID; v_site_tmm_id UUID;
    v_depot_tanjo_id UUID; v_depot_port_id UUID; v_emp_A1_id UUID; v_emp_QUAR_id UUID;
    v_dept_dir_id UUID; v_dept_fin_id UUID; v_dept_ach_id UUID; v_dept_vte_id UUID;
    v_dept_mag_id UUID; v_dept_it_id UUID;
    v_role_admin_id UUID; v_role_dir_id UUID; v_role_mgr_id UUID; v_role_sup_id UUID; v_role_op_id UUID;
    v_user_dg_id UUID; v_user_daf_id UUID; v_user_ach_mgr_id UUID; v_user_ach_op_id UUID;
    v_user_mag_chef_id UUID; v_user_mag_op_id UUID; v_user_vte_mgr_id UUID; v_user_vte_op_id UUID;
    v_fam_ppn_id UUID; v_fam_hitech_id UUID; v_fam_boisson_id UUID; v_fam_hyg_id UUID;
    v_art_riz_id UUID; v_art_laptop_id UUID; v_art_coca_id UUID;
    v_frs_china_id UUID; v_frs_local_id UUID; v_frs_star_id UUID;
    v_cli_b2b_id UUID; v_cli_retail_id UUID;
    v_da_id UUID; v_cmd_achat_id UUID; v_reception_id UUID; v_lot_riz_id UUID;
    v_facture_frs_id UUID; v_cmd_vente_id UUID; v_bl_id UUID; v_inv_id UUID;

BEGIN
    -- 1. REFERENTIELS (Nettoyage préalable pour éviter doublons si relancé)
    -- Note: On utilise ON CONFLICT DO NOTHING pour les clés primaires connues

    INSERT INTO devise (code, label, symbol) VALUES
    ('CNY', 'Yuan Chinois', 'CNY') -- MGA, EUR, USD existent déjà dans init
    ON CONFLICT (code) DO NOTHING;

    INSERT INTO pays (code, label) VALUES
    ('US', 'Etats-Unis'),
    ('CN', 'Chine') -- MG et FR existent déjà
    ON CONFLICT (code) DO NOTHING;

    INSERT INTO unite_mesure (code, label) VALUES
    ('BOX', 'Carton'),
    ('PAL', 'Palette') -- PCE, KG, L, H existent déjà
    ON CONFLICT (code) DO NOTHING;
    
    -- Note: AIRSI_5 n'existe pas dans le script init, ajoutons-le
    INSERT INTO type_taxe (code, label, rate) VALUES
    ('AIRSI_5', 'AIRSI 5%', 0.05)
    ON CONFLICT (code) DO NOTHING;

    -- Ajout modes de paiement manquants
    INSERT INTO mode_paiement (code, label) VALUES
    ('ORANGE_MONEY', 'Orange Money'),
    ('LCR_30', 'Lettre de Change 30j')
    ON CONFLICT (code) DO NOTHING;

    -- Note: Types mouvement sont codés en dur dans le init, on s'assure qu'ils y sont tous
    -- Pas besoin d'insert ici car init a déjà les bons codes

    -- 2. ORGANISATION
    INSERT INTO groupe_societe (code, name) VALUES ('GRP_MADA', 'Groupe Malagasy Distribution') RETURNING id INTO v_groupe_id;
    
    INSERT INTO societe (groupe_id, code, name, tax_id, pays_code, devise_code) 
    VALUES (v_groupe_id, 'MADA_DIS', 'Mada Distribution SA', 'NIF 3000123456 STAT 51100', 'MG', 'MGA') 
    RETURNING id INTO v_soc_id;

    INSERT INTO site (societe_id, code, name, address, latitude, longitude) 
    VALUES (v_soc_id, 'SITE_ANDRA', 'Siege Andraharo', 'Zone Galaxy, Antananarivo 101', -18.88, 47.51) 
    RETURNING id INTO v_site_tana_id; 
    
    INSERT INTO site (societe_id, code, name, address, latitude, longitude) 
    VALUES (v_soc_id, 'SITE_TMM', 'Hub Logistique Toamasina', 'Bd Joffre, Toamasina', -18.15, 49.40) 
    RETURNING id INTO v_site_tmm_id;

    INSERT INTO depot (site_id, code, name) 
    VALUES (v_site_tana_id, 'DEP_TANJO', 'Entrepot Central Tanjombato') 
    RETURNING id INTO v_depot_tanjo_id;
    
    INSERT INTO depot (site_id, code, name) 
    VALUES (v_site_tmm_id, 'DEP_PORT', 'Entrepot Douane Port') 
    RETURNING id INTO v_depot_port_id;

    INSERT INTO emplacement (depot_id, code, aisle, rack, shelf) VALUES (v_depot_tanjo_id, 'A-01-01', 'A', '01', '01') RETURNING id INTO v_emp_A1_id;
    INSERT INTO emplacement (depot_id, code, aisle, rack, shelf) VALUES (v_depot_tanjo_id, 'A-01-02', 'A', '01', '02');
    INSERT INTO emplacement (depot_id, code, aisle, rack, shelf) VALUES (v_depot_tanjo_id, 'QUARANTAINE', 'Z', '99', '99') RETURNING id INTO v_emp_QUAR_id;

    -- 3. RH
    INSERT INTO departement (code, name) VALUES ('DIR', 'Direction Generale') RETURNING id INTO v_dept_dir_id;
    INSERT INTO departement (code, name) VALUES ('FIN', 'Finance & Compta') RETURNING id INTO v_dept_fin_id;
    INSERT INTO departement (code, name) VALUES ('ACH', 'Achats & Appro') RETURNING id INTO v_dept_ach_id;
    INSERT INTO departement (code, name) VALUES ('VTE', 'Ventes & Commerce') RETURNING id INTO v_dept_vte_id;
    INSERT INTO departement (code, name) VALUES ('MAG', 'Logistique & Magasin') RETURNING id INTO v_dept_mag_id;
    INSERT INTO departement (code, name) VALUES ('IT', 'Informatique') RETURNING id INTO v_dept_it_id;

    INSERT INTO role (code, label) VALUES ('ADMIN', 'Administrateur Systeme') RETURNING id INTO v_role_admin_id;
    INSERT INTO role (code, label) VALUES ('DIRECTEUR', 'Directeur (C-Level)') RETURNING id INTO v_role_dir_id;
    INSERT INTO role (code, label) VALUES ('MANAGER', 'Responsable de Service') RETURNING id INTO v_role_mgr_id;
    INSERT INTO role (code, label) VALUES ('SUPERVISEUR', 'Chef d''equipe') RETURNING id INTO v_role_sup_id;
    INSERT INTO role (code, label) VALUES ('OPERATEUR', 'Operateur / Agent') RETURNING id INTO v_role_op_id;

    INSERT INTO utilisateur (username, email, password_hash, departement_id) VALUES ('admin', 'admin@madadis.mg', 'admin', v_dept_it_id);
    INSERT INTO utilisateur_role (utilisateur_id, role_id) VALUES ((SELECT id FROM utilisateur WHERE username='admin'), v_role_admin_id);

    -- AJOUT : Périmètres d'accès pour tous les utilisateurs (expérience réaliste)
    -- Admin : Accès total
    INSERT INTO perimetre_acces (utilisateur_id, societe_id, site_id, depot_id, max_amount_approval, active) 
    VALUES ((SELECT id FROM utilisateur WHERE username='admin'), v_soc_id, NULL, NULL, 999999999, TRUE);  -- Admin : accès total

    INSERT INTO utilisateur (username, email, password_hash, departement_id) VALUES ('andry.dg', 'dg@madadis.mg', 'hash1', v_dept_dir_id) RETURNING id INTO v_user_dg_id;
    INSERT INTO utilisateur_role VALUES (v_user_dg_id, v_role_dir_id);

    INSERT INTO utilisateur (username, email, password_hash, departement_id) VALUES ('bakoly.daf', 'daf@madadis.mg', 'hash2', v_dept_fin_id) RETURNING id INTO v_user_daf_id;
    INSERT INTO utilisateur_role VALUES (v_user_daf_id, v_role_mgr_id);

    INSERT INTO utilisateur (username, email, password_hash, departement_id) VALUES ('faly.ach', 'achat.mgr@madadis.mg', 'hash4', v_dept_ach_id) RETURNING id INTO v_user_ach_mgr_id;
    INSERT INTO utilisateur_role VALUES (v_user_ach_mgr_id, v_role_mgr_id);

    INSERT INTO utilisateur (username, email, password_hash, departement_id) VALUES ('sitraka.ach', 'achat.op@madadis.mg', 'hash5', v_dept_ach_id) RETURNING id INTO v_user_ach_op_id;
    INSERT INTO utilisateur_role VALUES (v_user_ach_op_id, v_role_op_id);

    INSERT INTO utilisateur (username, email, password_hash, departement_id) VALUES ('tiana.stock', 'stock.chef@madadis.mg', 'hash6', v_dept_mag_id) RETURNING id INTO v_user_mag_chef_id;
    INSERT INTO utilisateur_role VALUES (v_user_mag_chef_id, v_role_sup_id);

    INSERT INTO utilisateur (username, email, password_hash, departement_id) VALUES ('koto.stock', 'magasinier@madadis.mg', 'hash7', v_dept_mag_id) RETURNING id INTO v_user_mag_op_id;
    INSERT INTO utilisateur_role VALUES (v_user_mag_op_id, v_role_op_id);

    INSERT INTO utilisateur (username, email, password_hash, departement_id) VALUES ('soa.vte', 'sales.mgr@madadis.mg', 'hash8', v_dept_vte_id) RETURNING id INTO v_user_vte_mgr_id;
    INSERT INTO utilisateur_role VALUES (v_user_vte_mgr_id, v_role_mgr_id);

    INSERT INTO utilisateur (username, email, password_hash, departement_id) VALUES ('rivo.vte', 'commercial@madadis.mg', 'hash9', v_dept_vte_id) RETURNING id INTO v_user_vte_op_id;
    INSERT INTO utilisateur_role VALUES (v_user_vte_op_id, v_role_op_id);

    -- AJOUT : Périmètres d'accès pour tous les utilisateurs (expérience réaliste)
    -- Admin : Accès total
    INSERT INTO perimetre_acces (utilisateur_id, societe_id, site_id, depot_id, max_amount_approval, active) 
    VALUES (v_user_dg_id, v_soc_id, NULL, NULL, 999999999, TRUE);  -- DG : accès total

    INSERT INTO perimetre_acces (utilisateur_id, societe_id, site_id, depot_id, max_amount_approval, active) 
    VALUES (v_user_daf_id, v_soc_id, NULL, NULL, 50000000, TRUE);  -- DAF : accès total, approbation limitée

    INSERT INTO perimetre_acces (utilisateur_id, societe_id, site_id, depot_id, max_amount_approval, active) 
    VALUES (v_user_ach_mgr_id, v_soc_id, v_site_tana_id, NULL, 50000000, TRUE);  -- Manager achats : site Tana

    INSERT INTO perimetre_acces (utilisateur_id, societe_id, site_id, depot_id, max_amount_approval, active) 
    VALUES (v_user_ach_op_id, v_soc_id, v_site_tana_id, NULL, 0, TRUE);  -- Op achats : site Tana, pas d'approbation

    INSERT INTO perimetre_acces (utilisateur_id, societe_id, site_id, depot_id, max_amount_approval, active) 
    VALUES (v_user_mag_chef_id, v_soc_id, v_site_tana_id, v_depot_tanjo_id, 0, TRUE);  -- Chef stock : dépôt Tanjo

    INSERT INTO perimetre_acces (utilisateur_id, societe_id, site_id, depot_id, max_amount_approval, active) 
    VALUES (v_user_mag_op_id, v_soc_id, v_site_tana_id, v_depot_tanjo_id, 0, TRUE);  -- Magasinier : dépôt Tanjo

    INSERT INTO perimetre_acces (utilisateur_id, societe_id, site_id, depot_id, max_amount_approval, active) 
    VALUES (v_user_vte_mgr_id, v_soc_id, v_site_tana_id, NULL, 0, TRUE);  -- Manager ventes : site Tana

    INSERT INTO perimetre_acces (utilisateur_id, societe_id, site_id, depot_id, max_amount_approval, active) 
    VALUES (v_user_vte_op_id, v_soc_id, v_site_tana_id, NULL, 0, TRUE);  -- Commercial : site Tana

    -- Note : Admin a déjà un périmètre ajouté plus tôt, mais on le complète si nécessaire

    INSERT INTO regle_approbation (societe_id, document_type, min_amount, max_amount, role_id, level_index) VALUES (v_soc_id, 'DEMANDE_ACHAT', 0, 50000000, v_role_mgr_id, 1);
    INSERT INTO regle_approbation (societe_id, document_type, min_amount, max_amount, role_id, level_index) VALUES (v_soc_id, 'DEMANDE_ACHAT', 50000000, 9999999999, v_role_dir_id, 2);

    -- 4. PRODUITS & TIERS
    INSERT INTO famille_article (code, name, methode_valorisation_code, is_lot_obligatoire) VALUES ('PPN', 'Produits Premiere Necessite', 'CUMP', TRUE) RETURNING id INTO v_fam_ppn_id;
    INSERT INTO famille_article (code, name, methode_valorisation_code, is_lot_obligatoire) VALUES ('HITECH', 'Informatique & Technologie', 'FIFO', TRUE) RETURNING id INTO v_fam_hitech_id;
    INSERT INTO famille_article (code, name, methode_valorisation_code) VALUES ('BOISSON', 'Boissons & Liquides', 'FIFO') RETURNING id INTO v_fam_boisson_id;
    INSERT INTO famille_article (code, name, methode_valorisation_code) VALUES ('HYGIENE', 'Hygiene & Beaute', 'CUMP') RETURNING id INTO v_fam_hyg_id;

    -- Note: les colonnes prix_achat/vente n'existent pas dans votre table 'article' (vérifié dans init-data.sql)
    -- Elles sont gérées via 'ligne_liste_tarifaire'. J'adapte l'insert.
    INSERT INTO article (societe_id, famille_id, unite_code, taxe_vente_code, taxe_achat_code, sku, label, weight) 
    VALUES (v_soc_id, v_fam_ppn_id, 'KG', 'EXO', 'EXO', 'RIZ-LUX-50', 'Riz Luxury 50kg', 50.0) 
    RETURNING id INTO v_art_riz_id;

    INSERT INTO article (societe_id, famille_id, unite_code, taxe_vente_code, taxe_achat_code, sku, label, weight) 
    VALUES (v_soc_id, v_fam_hitech_id, 'PCE', 'TVA_20', 'TVA_20', 'HP-PROBOOK', 'HP Probook 450 G9', 2.5) 
    RETURNING id INTO v_art_laptop_id;
    
    INSERT INTO article (societe_id, famille_id, unite_code, taxe_vente_code, taxe_achat_code, sku, label) 
    VALUES (v_soc_id, v_fam_boisson_id, 'PCE', 'TVA_20', 'TVA_20', 'COCA-15L', 'Coca-Cola 1.5L') 
    RETURNING id INTO v_art_coca_id;

    -- Pas de colonne delai_paiement dans table fournisseur selon init-data.sql fourni, j'enlève
    INSERT INTO fournisseur (code, name, tax_id, devise_code) VALUES ('FRS_CHINA', 'Shenzhen Tech Export', 'CN-8899', 'USD') RETURNING id INTO v_frs_china_id;
    INSERT INTO fournisseur (code, name, tax_id, devise_code) VALUES ('FRS_TIKO', 'Tiko Agri', 'NIF 111222', 'MGA') RETURNING id INTO v_frs_local_id;
    INSERT INTO fournisseur (code, name, tax_id, devise_code) VALUES ('FRS_STAR', 'STAR Madagascar', 'NIF 777111', 'MGA') RETURNING id INTO v_frs_star_id;

    -- Pas de colonne credit_limit dans table client selon init-data.sql fourni, j'enlève
    INSERT INTO client (code, name, tax_id, devise_code) VALUES ('CLI_JUMBO', 'Jumbo Score', 'NIF 999888', 'MGA') RETURNING id INTO v_cli_b2b_id;
    INSERT INTO client (code, name, tax_id, devise_code) VALUES ('CLI_SHOP', 'Supermaki', 'NIF 777666', 'MGA') RETURNING id INTO v_cli_retail_id;

    -- 5. FLUX
    -- Pas de colonne date_demande dans demande_achat (c'est created_at)
    INSERT INTO demande_achat (numero, demandeur_id, site_id, statut_code) 
    VALUES ('DA-2401-001', v_user_ach_op_id, v_site_tana_id, 'APPROUVEE') 
    RETURNING id INTO v_da_id;
    
    -- Pas de colonne qty_requested mais c'est une table de liaison, le init-data.sql ne montrait pas la structure de "ligne_demande_achat"
    -- Je suppose sa création. Si erreur ici, c'est que la table ligne_demande_achat manque dans le init.
    -- Correction : Le init-data.sql ne contient pas CREATE TABLE ligne_demande_achat !
    -- Je vais donc commenter cette partie pour éviter le crash, ou créer la table à la volée est risqué.
    -- On va simuler que la commande est directe.
    
    -- Workflow
    INSERT INTO historique_workflow (document_type, document_id, etape_nouvelle, acteur_id, action, commentaire) VALUES 
    ('DEMANDE_ACHAT', v_da_id, 'SOUMISE', v_user_ach_op_id, 'SOUMISSION', 'Reappro stock urgence'),
    ('DEMANDE_ACHAT', v_da_id, 'APPROUVEE', v_user_ach_mgr_id, 'APPROBATION', 'OK Budget');

    INSERT INTO commande_achat (numero, demande_achat_id, fournisseur_id, site_id, acheteur_id, devise_code, total_ht, total_ttc, statut_code, date_commande) 
    VALUES ('BC-2401-088', v_da_id, v_frs_local_id, v_site_tana_id, v_user_ach_mgr_id, 'MGA', 9500000, 9500000, 'ENVOYEE', CURRENT_DATE - INTERVAL '8 days') 
    RETURNING id INTO v_cmd_achat_id;
    
    INSERT INTO ligne_commande_achat (commande_id, article_id, qty_ordered, unit_price) 
    VALUES (v_cmd_achat_id, v_art_riz_id, 100, 95000);

    INSERT INTO lot (article_id, numero_lot, date_fabrication, date_peremption, statut_qualite_code) 
    VALUES (v_art_riz_id, 'LOT-RIZ-DEC23', '2023-12-01', '2025-12-01', 'CONFORME') 
    RETURNING id INTO v_lot_riz_id;

    INSERT INTO bon_reception (numero, commande_achat_id, site_id, depot_id, statut_code, date_reception) 
    VALUES ('BR-2401-088', v_cmd_achat_id, v_site_tana_id, v_depot_tanjo_id, 'VALIDE', CURRENT_DATE - INTERVAL '5 days') 
    RETURNING id INTO v_reception_id;
    
    INSERT INTO ligne_bon_reception (bon_reception_id, article_id, lot_id, emplacement_id, qty_received) 
    VALUES (v_reception_id, v_art_riz_id, v_lot_riz_id, v_emp_A1_id, 100);
    
    INSERT INTO mouvement_stock (numero, type_mouvement_code, reference_doc, article_id, lot_id, depot_dest_id, emplacement_dest_id, qty, unit_cost, utilisateur_id) 
    VALUES ('MVT-2401-00001', 'RECEPTION', 'BR-2401-088', v_art_riz_id, v_lot_riz_id, v_depot_tanjo_id, v_emp_A1_id, 100, 95000, v_user_mag_chef_id);
    
    -- Stock : Attention, la colonne 'cump' n'existe pas dans le CREATE TABLE stock du init-data.sql fourni
    -- (uniquement qty_reel, qty_reserve, version). Je l'enlève.
    INSERT INTO stock (depot_id, emplacement_id, article_id, lot_id, qty_reel) 
    VALUES (v_depot_tanjo_id, v_emp_A1_id, v_art_riz_id, v_lot_riz_id, 100);

    INSERT INTO facture_fournisseur (ref_interne, ref_fournisseur, fournisseur_id, commande_achat_id, montant_ht, montant_ttc, devise_code, statut_code, date_facture) 
    VALUES ('FAC-AGRI-001', 'INV-12345', v_frs_local_id, v_cmd_achat_id, 9500000, 9500000, 'MGA', 'A_PAYER', CURRENT_DATE - INTERVAL '4 days') 
    RETURNING id INTO v_facture_frs_id;
    
    INSERT INTO rapprochement_achat (facture_id, reception_id, montant_rapproche) 
    VALUES (v_facture_frs_id, v_reception_id, 9500000);

    -- 6. VENTE
    -- Pas de date_commande dans commande_client (created_at utilisé par defaut)
    INSERT INTO commande_client (numero, client_id, site_id, statut_code) 
    VALUES ('CMD-CLI-500', v_cli_b2b_id, v_site_tana_id, 'CONFIRMEE') 
    RETURNING id INTO v_cmd_vente_id;
    
    INSERT INTO ligne_commande_client (commande_id, article_id, qty_ordered, price_unit) 
    VALUES (v_cmd_vente_id, v_art_riz_id, 10, 120000);

    INSERT INTO reservation_stock (ligne_commande_id, article_id, depot_id, lot_id, qty_reservee) 
    VALUES ((SELECT id FROM ligne_commande_client WHERE commande_id = v_cmd_vente_id LIMIT 1), v_art_riz_id, v_depot_tanjo_id, v_lot_riz_id, 10);
    
    UPDATE stock SET qty_reserve = qty_reserve + 10 WHERE depot_id = v_depot_tanjo_id AND lot_id = v_lot_riz_id;

    INSERT INTO bon_livraison (numero, commande_id, date_expedition) 
    VALUES ('BL-CLI-500', v_cmd_vente_id, CURRENT_DATE) 
    RETURNING id INTO v_bl_id;
    
    INSERT INTO ligne_bon_livraison (livraison_id, article_id, lot_id, qty_livree) 
    VALUES (v_bl_id, v_art_riz_id, v_lot_riz_id, 10);
    
    INSERT INTO mouvement_stock (numero, type_mouvement_code, reference_doc, article_id, lot_id, depot_source_id, emplacement_source_id, qty, utilisateur_id) 
    VALUES ('MVT-2401-00002', 'EXPEDITION', 'BL-CLI-500', v_art_riz_id, v_lot_riz_id, v_depot_tanjo_id, v_emp_A1_id, 10, v_user_mag_op_id);
    
    UPDATE stock SET qty_reel = qty_reel - 10, qty_reserve = qty_reserve - 10 WHERE depot_id = v_depot_tanjo_id AND lot_id = v_lot_riz_id;

    -- 7. AUDIT
    INSERT INTO inventaire (numero, site_id, depot_id, type_code, statut_code, cree_par, date_planification) 
    VALUES ('INV-SPOT-001', v_site_tana_id, v_depot_tanjo_id, 'SPOT', 'ANALYSE', v_user_daf_id, CURRENT_DATE) 
    RETURNING id INTO v_inv_id;
    
    INSERT INTO ligne_inventaire (inventaire_id, article_id, emplacement_id, lot_id, qty_theorique, qty_reelle_retenue, arbitrage_par, notes_arbitrage) 
    VALUES (v_inv_id, v_art_riz_id, v_emp_A1_id, v_lot_riz_id, 90, 88, v_user_dg_id, 'Ecart 2 sacs - Enquete vol potentiel'); 
    
    INSERT INTO saisie_inventaire (ligne_inventaire_id, operateur_id, qty_comptee) 
    VALUES ((SELECT id FROM ligne_inventaire WHERE inventaire_id = v_inv_id LIMIT 1), v_user_mag_op_id, 88);
    
    INSERT INTO journal_audit (entity_name, entity_id, action, utilisateur_id, changes) 
    VALUES ('STOCK', v_lot_riz_id, 'ECART_INVENTAIRE', v_user_daf_id, '{"message": "Detection ecart de -2 unites sur Riz Luxury"}');

    -- ===========================================================================
    -- 8. DONNEES WORKFLOW STOCK (pour tests complets)
    -- ===========================================================================
    
    -- 8.1 Nouveau dépôt avec coordonnées GPS (Antsirabe) + emplacement
    INSERT INTO depot (site_id, code, name, latitude, longitude, is_active) 
    VALUES (v_site_tana_id, 'DEP_ANTSIRABE', 'Entrepot Antsirabe', -19.8659, 47.0333, TRUE)
    RETURNING id INTO v_depot_port_id;  -- On réutilise cette variable
    
    -- Emplacements dans le nouveau dépôt
    INSERT INTO emplacement (depot_id, code, aisle, rack, shelf) 
    VALUES (v_depot_port_id, 'B-01-01', 'B', '01', '01');
    
    -- 8.2 Ajouter des coordonnées GPS aux dépôts existants (pour la carte)
    UPDATE depot SET latitude = -18.9137, longitude = 47.5361 WHERE code = 'DEP_TANJO';
    UPDATE depot SET latitude = -18.1443, longitude = 49.4028 WHERE code = 'DEP_PORT';
    
    -- 8.3 Nouvelle commande achat ENVOYEE (prête pour réception)
    -- Totaux corrigés : Riz (200×95k=19M) + Laptop (50×1.2M=60M) + Coca (100×4.5k=0.45M) = 79,450,000 HT
    -- TTC avec TVA 20% sur Laptop et Coca : 19M + 72M + 0.54M = 91,540,000
    INSERT INTO commande_achat (numero, fournisseur_id, site_id, acheteur_id, devise_code, total_ht, total_ttc, statut_code, date_commande) 
    VALUES ('BC-2601-001', v_frs_star_id, v_site_tana_id, v_user_ach_mgr_id, 'MGA', 79450000, 91540000, 'ENVOYEE', CURRENT_DATE - INTERVAL '3 days') 
    RETURNING id INTO v_cmd_achat_id;
    
    -- Lignes de la nouvelle commande (plusieurs articles)
    INSERT INTO ligne_commande_achat (commande_id, article_id, qty_ordered, unit_price) 
    VALUES (v_cmd_achat_id, v_art_riz_id, 200, 95000);
    
    INSERT INTO ligne_commande_achat (commande_id, article_id, qty_ordered, unit_price) 
    VALUES (v_cmd_achat_id, v_art_laptop_id, 50, 1200000);
    
    INSERT INTO ligne_commande_achat (commande_id, article_id, qty_ordered, unit_price) 
    VALUES (v_cmd_achat_id, v_art_coca_id, 100, 4500);
    
    -- 8.4 Deuxième commande achat ENVOYEE (alternative)
    INSERT INTO commande_achat (numero, fournisseur_id, site_id, acheteur_id, devise_code, total_ht, total_ttc, statut_code, date_commande) 
    VALUES ('BC-2601-002', v_frs_china_id, v_site_tana_id, v_user_ach_mgr_id, 'USD', 15000, 18000, 'ENVOYEE', CURRENT_DATE - INTERVAL '1 day');
    
    INSERT INTO ligne_commande_achat (commande_id, article_id, qty_ordered, unit_price) 
    VALUES ((SELECT id FROM commande_achat WHERE numero = 'BC-2601-002'), v_art_laptop_id, 100, 150);

END $$;
