package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.*;
import module.avs.repository.stock.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class InventaireService {
    
    private final InventaireRepository inventaireRepository;
    private final LigneInventaireRepository ligneInventaireRepository;
    private final SaisieInventaireRepository saisieInventaireRepository;
    private final StockRepository stockRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final TypeMouvementRepository typeMouvementRepository;
    private final AuditService auditService;
    private final UtilisateurService utilisateurService;
    
    // ============ GESTION DES INVENTAIRES ============
    
    public List<Inventaire> findAllInventaires() {
        return inventaireRepository.findAll();
    }
    
    public Page<Inventaire> findAllInventaires(Pageable pageable) {
        return inventaireRepository.findAllByOrderByDateDebutDesc(pageable);
    }
    
    public Optional<Inventaire> findInventaireById(UUID id) {
        return inventaireRepository.findById(id);
    }
    
    public List<Inventaire> findInventairesByStatut(String statut) {
        return inventaireRepository.findByStatutCode(statut);
    }
    
    public synchronized String generateInventaireNumero(String type) {
        String prefix = "INV-" + type.substring(0, 1) + "-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = inventaireRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%03d", nextNum);
            if (inventaireRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    public Inventaire createInventaire(Inventaire inventaire, Utilisateur createur) {
        inventaire.setNumero(generateInventaireNumero(inventaire.getTypeCode()));
        inventaire.setStatutCode("PLANIFIE");
        inventaire.setCreePar(createur);
        
        // Récupérer le site à partir du dépôt
        if (inventaire.getDepot() != null && inventaire.getSite() == null) {
            inventaire.setSite(inventaire.getDepot().getSite());
        }
        
        Inventaire saved = inventaireRepository.save(inventaire);
        
        // Générer les lignes d'inventaire à partir du stock théorique
        List<Stock> stocks = inventaire.getDepot() != null 
            ? stockRepository.findByDepotId(inventaire.getDepot().getId())
            : new ArrayList<>();
        
        for (Stock stock : stocks) {
            if (stock.getQtyReel().compareTo(BigDecimal.ZERO) > 0) {
                LigneInventaire ligne = LigneInventaire.builder()
                    .inventaire(saved)
                    .article(stock.getArticle())
                    .emplacement(stock.getEmplacement())
                    .lot(stock.getLot())
                    .qtyTheorique(stock.getQtyReel())
                    .estTraitee(false)
                    .estValidee(false)
                    .build();
                ligneInventaireRepository.save(ligne);
            }
        }
        
        auditService.logAction("INVENTAIRE", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    public Inventaire demarrerInventaire(UUID inventaireId, Utilisateur acteur) {
        Inventaire inventaire = inventaireRepository.findById(inventaireId)
            .orElseThrow(() -> new RuntimeException("Inventaire non trouvé"));
        
        inventaire.setStatutCode("EN_COURS");
        inventaire.setDateDebut(OffsetDateTime.now());
        Inventaire saved = inventaireRepository.save(inventaire);
        
        auditService.logWorkflow("INVENTAIRE", inventaireId, "PLANIFIE", "EN_COURS", acteur, "DEMARRAGE", null);
        return saved;
    }
    
    public Inventaire passerEnAnalyse(UUID inventaireId, Utilisateur acteur) {
        Inventaire inventaire = inventaireRepository.findById(inventaireId)
            .orElseThrow(() -> new RuntimeException("Inventaire non trouvé"));
        
        // Vérifier que toutes les lignes ont été comptées
        long lignesNonTraitees = inventaire.getLignes().stream()
            .filter(ligne -> !Boolean.TRUE.equals(ligne.getEstTraitee()))
            .count();
        
        if (lignesNonTraitees > 0) {
            throw new RuntimeException("Impossible de passer en analyse : " + lignesNonTraitees + " ligne(s) non comptée(s)");
        }
        
        // Vérifier que toutes les lignes ont été validées
        long lignesNonValidees = inventaire.getLignes().stream()
            .filter(ligne -> !Boolean.TRUE.equals(ligne.getEstValidee()))
            .count();
        
        if (lignesNonValidees > 0) {
            throw new RuntimeException("Impossible de passer en analyse : " + lignesNonValidees + " ligne(s) non validée(s)");
        }
        
        inventaire.setStatutCode("ANALYSE");
        Inventaire saved = inventaireRepository.save(inventaire);
        
        auditService.logWorkflow("INVENTAIRE", inventaireId, "EN_COURS", "ANALYSE", acteur, "ANALYSE", null);
        return saved;
    }
    
    // ============ SAISIE DES COMPTAGES ============
    
    public SaisieInventaire saisirComptage(UUID ligneId, Utilisateur operateur, BigDecimal qtyComptee, 
                                           Utilisateur superviseur, int tour) {
        LigneInventaire ligne = ligneInventaireRepository.findById(ligneId)
            .orElseThrow(() -> new RuntimeException("Ligne inventaire non trouvée"));
        
        // Créer la saisie
        SaisieInventaire saisie = SaisieInventaire.builder()
            .ligneInventaire(ligne)
            .operateur(operateur)
            .superviseur(superviseur)
            .qtyComptee(qtyComptee)
            .tourComptage(tour)
            .dateSaisie(OffsetDateTime.now())
            .estRetenue(true) // Retenue automatiquement
            .build();
        
        SaisieInventaire savedSaisie = saisieInventaireRepository.save(saisie);
        
        // Mettre à jour la ligne avec la quantité comptée
        ligne.setQtyReelleRetenue(qtyComptee);
        ligne.setEstTraitee(true);
        ligneInventaireRepository.save(ligne);
        
        return savedSaisie;
    }
    
    public LigneInventaire retenirSaisie(UUID saisieId, Utilisateur validateur) {
        SaisieInventaire saisie = saisieInventaireRepository.findById(saisieId)
            .orElseThrow(() -> new RuntimeException("Saisie non trouvée"));
        
        saisie.setEstRetenue(true);
        saisieInventaireRepository.save(saisie);
        
        LigneInventaire ligne = saisie.getLigneInventaire();
        ligne.setQtyReelleRetenue(saisie.getQtyComptee());
        ligne.setEstTraitee(true);
        
        return ligneInventaireRepository.save(ligne);
    }
    
    // ============ VALIDATION DES ÉCARTS ============
    
    public LigneInventaire validerEcart(UUID ligneId, Utilisateur validateur, String notes) {
        LigneInventaire ligne = ligneInventaireRepository.findById(ligneId)
            .orElseThrow(() -> new RuntimeException("Ligne inventaire non trouvée"));
        
        // Vérification séparation des tâches (sauf pour les admins)
        boolean estAdmin = validateur.getRoles().stream()
            .anyMatch(role -> "ADMIN".equals(role.getCode()));
        
        if (!estAdmin) {
            List<SaisieInventaire> saisies = saisieInventaireRepository.findByLigneInventaireIdOrderByTourComptage(ligneId);
            boolean operateurEstValidateur = saisies.stream()
                .anyMatch(s -> s.getOperateur().getId().equals(validateur.getId()));
            
            if (operateurEstValidateur) {
                throw new RuntimeException("L'opérateur ne peut pas valider son propre comptage");
            }
        }
        
        ligne.setEstValidee(true);
        ligne.setArbitragePar(validateur);
        ligne.setDateArbitrage(OffsetDateTime.now());
        ligne.setNotesArbitrage(notes);
        
        return ligneInventaireRepository.save(ligne);
    }
    
    // ============ CLÔTURE ET AJUSTEMENTS ============
    
    public Inventaire cloturerInventaire(UUID inventaireId, Utilisateur validateur) {
        Inventaire inventaire = inventaireRepository.findById(inventaireId)
            .orElseThrow(() -> new RuntimeException("Inventaire non trouvé"));
        
        // Vérifier que toutes les lignes sont traitées et validées
        List<LigneInventaire> lignesNonTraitees = ligneInventaireRepository.findNonTraitees(inventaireId);
        if (!lignesNonTraitees.isEmpty()) {
            throw new RuntimeException("Toutes les lignes doivent être traitées avant clôture");
        }
        
        // Appliquer les ajustements de stock
        TypeMouvement typeAjustement = typeMouvementRepository.findById("AJUSTEMENT")
            .orElseThrow(() -> new RuntimeException("Type mouvement non trouvé"));
        
        for (LigneInventaire ligne : inventaire.getLignes()) {
            if (ligne.getEstValidee() && ligne.getQtyReelleRetenue() != null) {
                BigDecimal ecart = ligne.getEcartFinal();
                
                if (ecart.compareTo(BigDecimal.ZERO) != 0) {
                    // Créer le mouvement d'ajustement
                    MouvementStock mouvement = MouvementStock.builder()
                        .typeMouvement(typeAjustement)
                        .referenceDoc(inventaire.getNumero())
                        .article(ligne.getArticle())
                        .lot(ligne.getLot())
                        .qty(ecart.abs())
                        .utilisateur(validateur)
                        .createdAt(OffsetDateTime.now())
                        .build();
                    
                    if (ecart.compareTo(BigDecimal.ZERO) > 0) {
                        mouvement.setDepotDest(inventaire.getDepot());
                        mouvement.setEmplacementDest(ligne.getEmplacement());
                    } else {
                        mouvement.setDepotSource(inventaire.getDepot());
                        mouvement.setEmplacementSource(ligne.getEmplacement());
                    }
                    
                    mouvementStockRepository.save(mouvement);
                    
                    // Mettre à jour le stock
                    Optional<Stock> stockOpt = stockRepository.findByDepotIdAndEmplacementIdAndArticleIdAndLotId(
                        inventaire.getDepot().getId(),
                        ligne.getEmplacement() != null ? ligne.getEmplacement().getId() : null,
                        ligne.getArticle().getId(),
                        ligne.getLot() != null ? ligne.getLot().getId() : null
                    );
                    
                    if (stockOpt.isPresent()) {
                        Stock stock = stockOpt.get();
                        stock.setQtyReel(ligne.getQtyReelleRetenue());
                        stockRepository.save(stock);
                    }
                }
            }
        }
        
        inventaire.setStatutCode("VALIDE");
        inventaire.setDateCloture(OffsetDateTime.now());
        inventaire.setValidePar(validateur);
        Inventaire saved = inventaireRepository.save(inventaire);
        
        auditService.logWorkflow("INVENTAIRE", inventaireId, "ANALYSE", "VALIDE", validateur, "CLOTURE", null);
        return saved;
    }
    
    // ============ STATISTIQUES ============
    
    public BigDecimal getTotalEcart(UUID inventaireId) {
        BigDecimal total = ligneInventaireRepository.getTotalEcart(inventaireId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public Map<String, Object> getInventaireStats(UUID inventaireId) {
        Inventaire inventaire = inventaireRepository.findById(inventaireId)
            .orElseThrow(() -> new RuntimeException("Inventaire non trouvé"));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLignes", inventaire.getLignes().size());
        stats.put("lignesTraitees", inventaire.getLignes().stream().filter(l -> Boolean.TRUE.equals(l.getEstTraitee())).count());
        stats.put("lignesValidees", inventaire.getLignes().stream().filter(l -> Boolean.TRUE.equals(l.getEstValidee())).count());
        stats.put("totalEcart", getTotalEcart(inventaireId));
        
        return stats;
    }
}
