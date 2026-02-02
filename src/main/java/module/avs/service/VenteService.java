package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.*;
import module.avs.model.tiers.Client;
import module.avs.model.vente.*;
import module.avs.repository.stock.*;
import module.avs.repository.vente.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map;

@Service
@Transactional
public class VenteService {
    
    private final DevisClientRepository devisClientRepository;
    private final LigneDevisClientRepository ligneDevisClientRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final LigneCommandeClientRepository ligneCommandeClientRepository;
    private final ReservationStockRepository reservationStockRepository;
    private final BonLivraisonRepository bonLivraisonRepository;
    private final LigneBonLivraisonRepository ligneBonLivraisonRepository;
    private final RetourClientRepository retourClientRepository;
    private final LigneRetourClientRepository ligneRetourClientRepository;
    private final StockRepository stockRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final TypeMouvementRepository typeMouvementRepository;
    private final LotRepository lotRepository;
    private final AuditService auditService;
    private final UtilisateurService utilisateurService;
    private final FinanceService financeService;
    private final module.avs.repository.article.ArticleRepository articleRepository;
    private final module.avs.repository.tiers.ClientRepository clientRepository;
    private final module.avs.repository.organisation.SiteRepository siteRepository;
    
    public VenteService(
            DevisClientRepository devisClientRepository,
            LigneDevisClientRepository ligneDevisClientRepository,
            CommandeClientRepository commandeClientRepository,
            LigneCommandeClientRepository ligneCommandeClientRepository,
            ReservationStockRepository reservationStockRepository,
            BonLivraisonRepository bonLivraisonRepository,
            LigneBonLivraisonRepository ligneBonLivraisonRepository,
            RetourClientRepository retourClientRepository,
            LigneRetourClientRepository ligneRetourClientRepository,
            StockRepository stockRepository,
            MouvementStockRepository mouvementStockRepository,
            TypeMouvementRepository typeMouvementRepository,
            LotRepository lotRepository,
            AuditService auditService,
            UtilisateurService utilisateurService,
            @Lazy FinanceService financeService,
            module.avs.repository.article.ArticleRepository articleRepository,
            module.avs.repository.tiers.ClientRepository clientRepository,
            module.avs.repository.organisation.SiteRepository siteRepository) {
        this.devisClientRepository = devisClientRepository;
        this.ligneDevisClientRepository = ligneDevisClientRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.ligneCommandeClientRepository = ligneCommandeClientRepository;
        this.reservationStockRepository = reservationStockRepository;
        this.bonLivraisonRepository = bonLivraisonRepository;
        this.ligneBonLivraisonRepository = ligneBonLivraisonRepository;
        this.retourClientRepository = retourClientRepository;
        this.ligneRetourClientRepository = ligneRetourClientRepository;
        this.stockRepository = stockRepository;
        this.mouvementStockRepository = mouvementStockRepository;
        this.typeMouvementRepository = typeMouvementRepository;
        this.lotRepository = lotRepository;
        this.auditService = auditService;
        this.utilisateurService = utilisateurService;
        this.financeService = financeService;
        this.articleRepository = articleRepository;
        this.clientRepository = clientRepository;
        this.siteRepository = siteRepository;
    }
    
    // ============ DEVIS ============
    
    public List<DevisClient> findAllDevis() {
        return devisClientRepository.findAll();
    }
    
    public Page<DevisClient> findAllDevis(Pageable pageable) {
        return devisClientRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public Optional<DevisClient> findDevisById(UUID id) {
        return devisClientRepository.findById(id);
    }
    
    public synchronized String generateDevisNumero() {
        String prefix = "DEV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = devisClientRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%03d", nextNum);
            if (devisClientRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    public DevisClient createDevis(DevisClient devis, Utilisateur createur) {
        // Charger les entités référencées si nécessaire
        if (devis.getClient() != null && devis.getClient().getId() != null) {
            devis.setClient(clientRepository.findById(devis.getClient().getId())
                .orElseThrow(() -> new RuntimeException("Client non trouvé")));
        }
        
        if (devis.getSite() != null && devis.getSite().getId() != null) {
            devis.setSite(siteRepository.findById(devis.getSite().getId())
                .orElseThrow(() -> new RuntimeException("Site non trouvé")));
        }
        
        // Charger les articles dans les lignes
        if (devis.getLignes() != null) {
            for (LigneDevisClient ligne : devis.getLignes()) {
                if (ligne.getArticle() != null && ligne.getArticle().getId() != null) {
                    ligne.setArticle(articleRepository.findById(ligne.getArticle().getId())
                        .orElseThrow(() -> new RuntimeException("Article non trouvé: " + ligne.getArticle().getId())));
                }
                ligne.setDevis(devis);
            }
        }
        
        devis.setCommercial(createur);
        devis.setNumero(generateDevisNumero());
        devis.setStatutCode("BROUILLON");
        devis.recalculerTotaux();
        DevisClient saved = devisClientRepository.save(devis);
        
        auditService.logAction("DEVIS_CLIENT", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    public DevisClient saveDevis(DevisClient devis) {
        devis.recalculerTotaux();
        return devisClientRepository.save(devis);
    }
    
    public DevisClient validerDevis(UUID devisId, Utilisateur acteur) {
        DevisClient devis = devisClientRepository.findById(devisId)
            .orElseThrow(() -> new RuntimeException("Devis non trouvé"));
        
        devis.setStatutCode("VALIDE");
        DevisClient saved = devisClientRepository.save(devis);
        
        auditService.logWorkflow("DEVIS_CLIENT", devisId, "BROUILLON", "VALIDE", acteur, "VALIDATION", null);
        return saved;
    }
    
    // ============ COMMANDES CLIENT ============
    
    public List<CommandeClient> findAllCommandesClient() {
        return commandeClientRepository.findAll();
    }
    
    public Page<CommandeClient> findAllCommandesClient(Pageable pageable) {
        return commandeClientRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public Optional<CommandeClient> findCommandeClientById(UUID id) {
        return commandeClientRepository.findById(id);
    }
    
    public List<CommandeClient> findCommandesByStatut(String statut) {
        return commandeClientRepository.findByStatutCode(statut);
    }
    
    public synchronized String generateCommandeClientNumero() {
        String prefix = "CMD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = commandeClientRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%03d", nextNum);
            if (commandeClientRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    public CommandeClient createCommandeClient(CommandeClient commande, Utilisateur createur) {
        commande.setNumero(generateCommandeClientNumero());
        commande.setStatutCode("BROUILLON");
        commande.setCommercial(createur);
        commande.recalculerTotaux();
        CommandeClient saved = commandeClientRepository.save(commande);
        
        auditService.logAction("COMMANDE_CLIENT", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    public CommandeClient createCommandeFromDevis(UUID devisId, Utilisateur createur) {
        DevisClient devis = devisClientRepository.findById(devisId)
            .orElseThrow(() -> new RuntimeException("Devis non trouvé"));
        
        CommandeClient commande = CommandeClient.builder()
            .devis(devis)
            .client(devis.getClient())
            .site(devis.getSite())
            .commercial(createur)
            .statutCode("BROUILLON")
            .totalHT(devis.getTotalHT())
            .totalTTC(devis.getTotalTTC())
            .build();
        
        commande.setNumero(generateCommandeClientNumero());
        CommandeClient saved = commandeClientRepository.save(commande);
        
        // Copier les lignes du devis
        for (LigneDevisClient ligneDevis : devis.getLignes()) {
            LigneCommandeClient ligneCmd = LigneCommandeClient.builder()
                .commande(saved)
                .article(ligneDevis.getArticle())
                .qtyOrdered(ligneDevis.getQty())
                .priceUnit(ligneDevis.getPriceUnit())
                .remisePct(ligneDevis.getRemisePct())
                .build();
            ligneCommandeClientRepository.save(ligneCmd);
        }
        
        devis.setStatutCode("TRANSFORME");
        devisClientRepository.save(devis);
        
        auditService.logAction("COMMANDE_CLIENT", saved.getId(), "CREATION_DEPUIS_DEVIS", createur, null);
        return saved;
    }
    
    public CommandeClient saveCommandeClient(CommandeClient commande) {
        commande.recalculerTotaux();
        return commandeClientRepository.save(commande);
    }
    
    public CommandeClient confirmerCommande(UUID commandeId, Utilisateur acteur) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        // Réserver le stock pour chaque ligne
        for (LigneCommandeClient ligne : commande.getLignes()) {
            reserverStockPourLigne(ligne);
        }
        
        commande.setStatutCode("CONFIRMEE");
        CommandeClient saved = commandeClientRepository.save(commande);
        
        auditService.logWorkflow("COMMANDE_CLIENT", commandeId, "BROUILLON", "CONFIRMEE", acteur, "CONFIRMATION", null);
        return saved;
    }
    
    private void reserverStockPourLigne(LigneCommandeClient ligne) {
        // Récupérer la méthode de valorisation de l'article
        String methodeValorisation = ligne.getArticle().getFamille().getMethodeValorisation().getCode();
        
        // Obtenir les stocks disponibles selon la méthode de valorisation
        List<Stock> stocksDispo = getStocksDisponiblesParMethode(ligne.getArticle().getId(), methodeValorisation);
        BigDecimal qtyAReserver = ligne.getQtyOrdered();
        
        for (Stock stock : stocksDispo) {
            if (qtyAReserver.compareTo(BigDecimal.ZERO) <= 0) break;
            
            BigDecimal disponible = stock.getQtyDisponible();
            BigDecimal aReserver = disponible.min(qtyAReserver);
            
            if (aReserver.compareTo(BigDecimal.ZERO) > 0) {
                // Créer la réservation
                ReservationStock reservation = ReservationStock.builder()
                    .ligneCommande(ligne)
                    .article(ligne.getArticle())
                    .depot(stock.getDepot())
                    .lot(stock.getLot())
                    .qtyReservee(aReserver)
                    .build();
                reservationStockRepository.save(reservation);
                
                // Mettre à jour le stock réservé
                stock.setQtyReserve(stock.getQtyReserve().add(aReserver));
                stockRepository.save(stock);
                
                qtyAReserver = qtyAReserver.subtract(aReserver);
            }
        }
        
        if (qtyAReserver.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Stock insuffisant pour l'article " + ligne.getArticle().getSku());
        }
    }
    
    private List<Stock> getStocksDisponiblesParMethode(UUID articleId, String methodeValorisation) {
        List<Stock> stocks;
        
        switch (methodeValorisation) {
            case "FIFO":
                // FIFO : Premier Entré, Premier Sorti (par date de fabrication ascendante)
                stocks = stockRepository.findByArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationAsc(
                    articleId, BigDecimal.ZERO);
                break;
                
            case "LIFO":
                // LIFO : Dernier Entré, Premier Sorti (par date de fabrication descendante)
                stocks = stockRepository.findByArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationDesc(
                    articleId, BigDecimal.ZERO);
                break;
                
            case "CUMP":
            default:
                // CUMP : Coût Unitaire Moyen Pondéré (pas d'ordre spécifique, ou par ID)
                stocks = stockRepository.findByArticleIdAndQtyReelGreaterThan(
                    articleId, BigDecimal.ZERO);
                break;
        }
        
        // Filtrer uniquement les stocks avec quantité disponible
        return stocks.stream()
            .filter(s -> s.getQtyDisponible().compareTo(BigDecimal.ZERO) > 0)
            .toList();
    }
    
    public CommandeClient preparerCommande(UUID commandeId, Utilisateur acteur) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        commande.setStatutCode("PREPARATION");
        CommandeClient saved = commandeClientRepository.save(commande);
        
        auditService.logWorkflow("COMMANDE_CLIENT", commandeId, "CONFIRMEE", "PREPARATION", acteur, "PREPARATION", null);
        return saved;
    }
    
    public List<ReservationStock> getReservationsParMethode(UUID commandeId) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        // Grouper les réservations par article pour appliquer la méthode de valorisation
        Map<UUID, List<ReservationStock>> reservationsParArticle = new HashMap<>();
        
        for (LigneCommandeClient ligne : commande.getLignes()) {
            List<ReservationStock> ligneReservations = reservationStockRepository.findByLigneCommandeId(ligne.getId());
            
            // Récupérer la méthode de valorisation de l'article
            String methodeValorisation = ligne.getArticle().getFamille().getMethodeValorisation().getCode();
            
            // Trier les réservations selon la méthode de valorisation
            ligneReservations = trierReservationsParMethode(ligneReservations, methodeValorisation);
            
            reservationsParArticle.put(ligne.getArticle().getId(), ligneReservations);
        }
        
        // Aplatir la map en liste tout en conservant l'ordre
        List<ReservationStock> allReservations = new ArrayList<>();
        for (LigneCommandeClient ligne : commande.getLignes()) {
            List<ReservationStock> reservations = reservationsParArticle.get(ligne.getArticle().getId());
            if (reservations != null) {
                allReservations.addAll(reservations);
            }
        }
        
        return allReservations;
    }
    
    private List<ReservationStock> trierReservationsParMethode(List<ReservationStock> reservations, String methodeValorisation) {
        return switch (methodeValorisation) {
            case "FIFO" -> {
                // FIFO : Trier par date de fabrication ascendante (plus ancien d'abord)
                reservations.sort((r1, r2) -> {
                    // Trier d'abord par dépôt pour regrouper
                    int depotCompare = compareDepot(r1, r2);
                    if (depotCompare != 0) return depotCompare;
                    
                    // Puis par date de fabrication
                    return compareDateFabrication(r1, r2, true);
                });
                yield reservations;
            }
            case "LIFO" -> {
                // LIFO : Trier par date de fabrication descendante (plus récent d'abord)
                reservations.sort((r1, r2) -> {
                    // Trier d'abord par dépôt pour regrouper
                    int depotCompare = compareDepot(r1, r2);
                    if (depotCompare != 0) return depotCompare;
                    
                    // Puis par date de fabrication inversée
                    return compareDateFabrication(r1, r2, false);
                });
                yield reservations;
            }
            case "CUMP" -> {
                // CUMP : Trier par dépôt et lot, pas de préférence de date
                reservations.sort((r1, r2) -> {
                    int depotCompare = compareDepot(r1, r2);
                    if (depotCompare != 0) return depotCompare;
                    
                    // Trier par numéro de lot pour cohérence
                    return compareLot(r1, r2);
                });
                yield reservations;
            }
            default -> reservations;
        };
    }
    
    private int compareDepot(ReservationStock r1, ReservationStock r2) {
        if (r1.getDepot() == null && r2.getDepot() == null) return 0;
        if (r1.getDepot() == null) return 1;
        if (r2.getDepot() == null) return -1;
        return r1.getDepot().getCode().compareTo(r2.getDepot().getCode());
    }
    
    private int compareDateFabrication(ReservationStock r1, ReservationStock r2, boolean ascending) {
        if (r1.getLot() == null && r2.getLot() == null) return 0;
        if (r1.getLot() == null) return 1;
        if (r2.getLot() == null) return -1;
        if (r1.getLot().getDateFabrication() == null && r2.getLot().getDateFabrication() == null) return 0;
        if (r1.getLot().getDateFabrication() == null) return 1;
        if (r2.getLot().getDateFabrication() == null) return -1;
        
        int result = r1.getLot().getDateFabrication().compareTo(r2.getLot().getDateFabrication());
        return ascending ? result : -result;
    }
    
    private int compareLot(ReservationStock r1, ReservationStock r2) {
        if (r1.getLot() == null && r2.getLot() == null) return 0;
        if (r1.getLot() == null) return 1;
        if (r2.getLot() == null) return -1;
        return r1.getLot().getNumeroLot().compareTo(r2.getLot().getNumeroLot());
    }
    
    public CommandeClient validerPicking(UUID commandeId, Utilisateur acteur) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        commande.setStatutCode("PRETE");
        CommandeClient saved = commandeClientRepository.save(commande);
        
        auditService.logWorkflow("COMMANDE_CLIENT", commandeId, "PREPARATION", "PRETE", acteur, "PICKING_VALIDE", null);
        return saved;
    }
    
    // ============ LIVRAISONS ============
    
    public List<BonLivraison> findAllLivraisons() {
        return bonLivraisonRepository.findAll();
    }
    
    public Page<BonLivraison> findAllLivraisons(Pageable pageable) {
        return bonLivraisonRepository.findAllByOrderByDateExpeditionDesc(pageable);
    }
    
    public Optional<BonLivraison> findLivraisonById(UUID id) {
        return bonLivraisonRepository.findById(id);
    }
    
    public synchronized String generateLivraisonNumero() {
        String prefix = "BL-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = bonLivraisonRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%03d", nextNum);
            if (bonLivraisonRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    public BonLivraison createLivraison(UUID commandeId, Utilisateur createur) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        BonLivraison bl = BonLivraison.builder()
            .numero(generateLivraisonNumero())
            .commande(commande)
            .statutCode("BROUILLON")
            .dateExpedition(OffsetDateTime.now())
            .preparateur(createur)
            .datePreparation(OffsetDateTime.now())
            .build();
        
        BonLivraison saved = bonLivraisonRepository.save(bl);
        
        // Créer les lignes de livraison basées sur les réservations de stock
        for (LigneCommandeClient ligneCmd : commande.getLignes()) {
            List<ReservationStock> reservations = reservationStockRepository.findByLigneCommandeId(ligneCmd.getId());
            
            for (ReservationStock reservation : reservations) {
                LigneBonLivraison ligneLivraison = LigneBonLivraison.builder()
                    .bonLivraison(saved)
                    .article(reservation.getArticle())
                    .lot(reservation.getLot())
                    .depot(reservation.getDepot())
                    .qtyCommandee(ligneCmd.getQtyOrdered())
                    .qtyLivree(reservation.getQtyReservee())
                    .qtyPreparee(reservation.getQtyReservee())
                    .build();
                ligneBonLivraisonRepository.save(ligneLivraison);
                saved.addLigne(ligneLivraison);
            }
        }
        
        auditService.logAction("BON_LIVRAISON", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    public BonLivraison validerLivraison(UUID livraisonId, Utilisateur acteur) {
        BonLivraison livraison = bonLivraisonRepository.findById(livraisonId)
            .orElseThrow(() -> new RuntimeException("Livraison non trouvée"));
        
        TypeMouvement typeMouvement = typeMouvementRepository.findById("EXPEDITION")
            .orElseThrow(() -> new RuntimeException("Type mouvement non trouvé"));
        
        // CORRECTION COMPLÈTE : Décrémenter le stock directement depuis les lignes de livraison
        // Chaque ligne de livraison correspond déjà à UNE réservation spécifique (1 lot)
        // Il ne faut PAS rechercher toutes les réservations pour chaque ligne !
        
        for (LigneBonLivraison ligne : livraison.getLignes()) {
            BigDecimal qtyALivrer = ligne.getQtyLivree();
            
            if (qtyALivrer.compareTo(BigDecimal.ZERO) <= 0) continue;
            
            // Mettre à jour le stock correspondant à cette ligne (1 ligne = 1 lot spécifique)
            Optional<Stock> stockOpt = stockRepository.findByDepotIdAndArticleIdAndLotId(
                ligne.getDepot().getId(),
                ligne.getArticle().getId(),
                ligne.getLot() != null ? ligne.getLot().getId() : null
            );
            
            if (stockOpt.isPresent()) {
                Stock stock = stockOpt.get();
                
                // Déduire la quantité livrée du stock réel et réservé
                stock.setQtyReel(stock.getQtyReel().subtract(qtyALivrer));
                stock.setQtyReserve(stock.getQtyReserve().subtract(qtyALivrer));
                stockRepository.save(stock);
                
                // Générer le numéro de mouvement
                String numeroMvt = "MVT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
                Integer maxNum = mouvementStockRepository.findMaxNumero(numeroMvt);
                numeroMvt += String.format("%05d", (maxNum != null ? maxNum : 0) + 1);
                
                // Créer le mouvement de sortie
                MouvementStock mouvement = MouvementStock.builder()
                    .numero(numeroMvt)
                    .typeMouvement(typeMouvement)
                    .referenceDoc(livraison.getNumero())
                    .article(ligne.getArticle())
                    .lot(ligne.getLot())
                    .depotSource(stock.getDepot())
                    .emplacementSource(stock.getEmplacement())
                    .qty(qtyALivrer)
                    .utilisateur(acteur)
                    .createdAt(OffsetDateTime.now())
                    .build();
                mouvementStockRepository.save(mouvement);
            }
        }
        
        // Mettre à jour les quantités livrées sur les lignes de commande (par article)
        Map<UUID, BigDecimal> qteLivreeParArticle = new HashMap<>();
        for (LigneBonLivraison ligne : livraison.getLignes()) {
            UUID articleId = ligne.getArticle().getId();
            qteLivreeParArticle.merge(articleId, ligne.getQtyLivree(), BigDecimal::add);
        }
        
        for (LigneCommandeClient ligneCmd : livraison.getCommande().getLignes()) {
            UUID articleId = ligneCmd.getArticle().getId();
            if (qteLivreeParArticle.containsKey(articleId)) {
                ligneCmd.setQtyDelivered(ligneCmd.getQtyDelivered().add(qteLivreeParArticle.get(articleId)));
                ligneCommandeClientRepository.save(ligneCmd);
                
                // CORRECTION : Supprimer les réservations après livraison pour éviter double traitement
                List<ReservationStock> reservations = reservationStockRepository.findByLigneCommandeId(ligneCmd.getId());
                reservationStockRepository.deleteAll(reservations);
            }
        }
        
        livraison.setStatutCode("VALIDE");
        livraison.setValidateur(acteur);
        livraison.setDateValidation(OffsetDateTime.now());
        BonLivraison saved = bonLivraisonRepository.save(livraison);
        
        // Mettre à jour le statut de la commande
        updateStatutCommandeApresLivraison(livraison.getCommande().getId());
        
        // CRÉATION AUTOMATIQUE DE LA FACTURE CLIENT
        try {
            financeService.createFactureFromBonLivraison(livraisonId, acteur);
        } catch (Exception e) {
            // Log error but don't fail the transaction
            auditService.logAction("FACTURE_CLIENT", null, "CREATION_AUTO_FAILED", acteur, 
                Map.of("bonLivraisonId", livraisonId.toString(), "error", e.getMessage()));
        }
        
        auditService.logWorkflow("BON_LIVRAISON", livraisonId, "BROUILLON", "VALIDE", acteur, "VALIDATION", null);
        return saved;
    }
    
    private void updateStatutCommandeApresLivraison(UUID commandeId) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        boolean toutLivre = commande.getLignes().stream()
            .allMatch(l -> l.getQtyRestante().compareTo(BigDecimal.ZERO) <= 0);
        
        if (toutLivre) {
            commande.setStatutCode("EXPEDIEE");
            commandeClientRepository.save(commande);
        }
    }
    
    // ============ VALIDATION REMISES ============
    
    public boolean validerRemise(BigDecimal remisePct, BigDecimal plafondRemise, UUID validateurId) {
        if (remisePct.compareTo(plafondRemise) <= 0) {
            return true;
        }
        // Vérifier si le validateur a le droit d'accorder cette remise
        return utilisateurService.hasRoleWithDelegation(validateurId, "MANAGER");
    }
    
    // ============ RETOURS CLIENT (SAV) ============
    
    public List<RetourClient> findAllRetours() {
        return retourClientRepository.findAll();
    }
    
    public Page<RetourClient> findAllRetours(Pageable pageable) {
        return retourClientRepository.findAllByOrderByDateDemandeDesc(pageable);
    }
    
    public Optional<RetourClient> findRetourById(UUID id) {
        return retourClientRepository.findById(id);
    }
    
    public List<RetourClient> findRetoursByStatut(String statut) {
        return retourClientRepository.findByStatutCode(statut);
    }
    
    public List<RetourClient> findRetoursByClient(UUID clientId) {
        return retourClientRepository.findByClientId(clientId);
    }
    
    public synchronized String generateRetourNumero() {
        String prefix = "RET-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = retourClientRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%03d", nextNum);
            if (retourClientRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    public RetourClient createRetour(RetourClient retour, Utilisateur demandeur) {
        retour.setNumero(generateRetourNumero());
        retour.setStatutCode("DEMANDE");
        retour.setDemandeur(demandeur);
        retour.setDateDemande(OffsetDateTime.now());
        
        RetourClient saved = retourClientRepository.save(retour);
        auditService.logAction("RETOUR_CLIENT", saved.getId(), "CREATION", demandeur, null);
        return saved;
    }
    
    public RetourClient approuverRetour(UUID retourId, Utilisateur approbateur) {
        RetourClient retour = retourClientRepository.findById(retourId)
            .orElseThrow(() -> new RuntimeException("Retour non trouvé"));
        
        if (!"DEMANDE".equals(retour.getStatutCode())) {
            throw new RuntimeException("Seuls les retours en statut DEMANDE peuvent être approuvés");
        }
        
        retour.setStatutCode("APPROUVE");
        retour.setApprobateur(approbateur);
        retour.setDateApprobation(OffsetDateTime.now());
        
        RetourClient saved = retourClientRepository.save(retour);
        auditService.logWorkflow("RETOUR_CLIENT", retourId, "DEMANDE", "APPROUVE", approbateur, "APPROBATION", null);
        return saved;
    }
    
    public RetourClient refuserRetour(UUID retourId, Utilisateur approbateur, String motif) {
        RetourClient retour = retourClientRepository.findById(retourId)
            .orElseThrow(() -> new RuntimeException("Retour non trouvé"));
        
        retour.setStatutCode("REFUSE");
        retour.setApprobateur(approbateur);
        retour.setDateApprobation(OffsetDateTime.now());
        retour.setNotes(motif);
        
        RetourClient saved = retourClientRepository.save(retour);
        auditService.logWorkflow("RETOUR_CLIENT", retourId, retour.getStatutCode(), "REFUSE", approbateur, "REFUS", motif);
        return saved;
    }
    
    public RetourClient receptionnerRetour(UUID retourId, Utilisateur recepteur) {
        RetourClient retour = retourClientRepository.findById(retourId)
            .orElseThrow(() -> new RuntimeException("Retour non trouvé"));
        
        if (!"APPROUVE".equals(retour.getStatutCode())) {
            throw new RuntimeException("Seuls les retours approuvés peuvent être réceptionnés");
        }
        
        retour.setStatutCode("RECEPTIONNE");
        retour.setDateReception(OffsetDateTime.now());
        
        RetourClient saved = retourClientRepository.save(retour);
        auditService.logWorkflow("RETOUR_CLIENT", retourId, "APPROUVE", "RECEPTIONNE", recepteur, "RECEPTION", null);
        return saved;
    }
    
    public RetourClient controlerRetour(UUID retourId, Utilisateur controleur) {
        RetourClient retour = retourClientRepository.findById(retourId)
            .orElseThrow(() -> new RuntimeException("Retour non trouvé"));
        
        retour.setStatutCode("CONTROLE");
        RetourClient saved = retourClientRepository.save(retour);
        auditService.logWorkflow("RETOUR_CLIENT", retourId, retour.getStatutCode(), "CONTROLE", controleur, "CONTROLE_QUALITE", null);
        return saved;
    }
    
    public RetourClient traiterRetour(UUID retourId, Utilisateur responsable) {
        RetourClient retour = retourClientRepository.findById(retourId)
            .orElseThrow(() -> new RuntimeException("Retour non trouvé"));
        
        if (!"CONTROLE".equals(retour.getStatutCode()) && !"RECEPTIONNE".equals(retour.getStatutCode())) {
            throw new RuntimeException("Le retour doit être réceptionné ou en contrôle pour être traité");
        }
        
        TypeMouvement typeMouvement = typeMouvementRepository.findById("RETOUR_CLIENT")
            .orElseThrow(() -> new RuntimeException("Type mouvement RETOUR_CLIENT non trouvé"));
        
        // Traiter chaque ligne selon la décision
        for (LigneRetourClient ligne : retour.getLignes()) {
            if ("REINTEGRER".equals(ligne.getDecision()) && ligne.getQtyAcceptee().compareTo(BigDecimal.ZERO) > 0) {
                // Réintégrer au stock
                Stock stock = findOrCreateStock(
                    retour.getDepotRetour(),
                    ligne.getEmplacement(),
                    ligne.getArticle(),
                    ligne.getLot()
                );
                
                stock.setQtyReel(stock.getQtyReel().add(ligne.getQtyAcceptee()));
                stockRepository.save(stock);
                
                // Créer mouvement de retour
                creerMouvementRetour(typeMouvement, retour, ligne, responsable);
                
            } else if ("QUARANTAINE".equals(ligne.getDecision())) {
                // Mettre en quarantaine
                if (ligne.getLot() != null) {
                    ligne.getLot().setStatutQualiteCode("QUARANTAINE");
                    lotRepository.save(ligne.getLot());
                }
            }
            // REBUTER : ne rien faire, la marchandise ne rentre pas en stock
        }
        
        retour.setStatutCode("INTEGRE");
        RetourClient saved = retourClientRepository.save(retour);
        auditService.logWorkflow("RETOUR_CLIENT", retourId, retour.getStatutCode(), "INTEGRE", responsable, "TRAITEMENT", null);
        return saved;
    }
    
    private Stock findOrCreateStock(module.avs.model.organisation.Depot depot,
                                   module.avs.model.organisation.Emplacement emplacement,
                                   module.avs.model.article.Article article,
                                   Lot lot) {
        Optional<Stock> existingStock = emplacement != null
            ? stockRepository.findByDepotIdAndEmplacementIdAndArticleIdAndLotId(
                depot.getId(), emplacement.getId(), article.getId(), lot != null ? lot.getId() : null)
            : stockRepository.findByDepotIdAndArticleIdAndLotId(
                depot.getId(), article.getId(), lot != null ? lot.getId() : null);
        
        return existingStock.orElseGet(() -> {
            Stock newStock = Stock.builder()
                .depot(depot)
                .emplacement(emplacement)
                .article(article)
                .lot(lot)
                .qtyReel(BigDecimal.ZERO)
                .qtyReserve(BigDecimal.ZERO)
                .build();
            return stockRepository.save(newStock);
        });
    }
    
    private void creerMouvementRetour(TypeMouvement typeMouvement, RetourClient retour,
                                      LigneRetourClient ligne, Utilisateur user) {
        String numeroMvt = "MVT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = mouvementStockRepository.findMaxNumero(numeroMvt);
        numeroMvt += String.format("%05d", (maxNum != null ? maxNum : 0) + 1);
        
        MouvementStock mouvement = MouvementStock.builder()
            .numero(numeroMvt)
            .typeMouvement(typeMouvement)
            .referenceDoc(retour.getNumero())
            .article(ligne.getArticle())
            .lot(ligne.getLot())
            .depotDest(retour.getDepotRetour())
            .emplacementDest(ligne.getEmplacement())
            .qty(ligne.getQtyAcceptee())
            .utilisateur(user)
            .createdAt(OffsetDateTime.now())
            .build();
        
        mouvementStockRepository.save(mouvement);
    }
    
    public Optional<BonLivraison> findBonLivraisonById(UUID id) {
        return bonLivraisonRepository.findById(id);
    }
}
