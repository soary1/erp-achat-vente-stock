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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SortieStockService {
    
    private final DemandeSortieStockRepository demandeRepository;
    private final LigneDemandeSortieRepository ligneDemandeRepository;
    private final StockRepository stockRepository;
    private final MouvementStockRepository mouvementRepository;
    private final TypeMouvementRepository typeMouvementRepository;
    private final AuditService auditService;
    
    // ============ CONSULTATION ============
    
    public List<DemandeSortieStock> findAllDemandes() {
        return demandeRepository.findAll();
    }
    
    public Page<DemandeSortieStock> findAllDemandes(Pageable pageable) {
        return demandeRepository.findAllByOrderByDateDemandeDesc(pageable);
    }
    
    public Page<DemandeSortieStock> findDemandesByType(String type, Pageable pageable) {
        return demandeRepository.findByTypeOrderByDateDemandeDesc(type, pageable);
    }
    
    public Optional<DemandeSortieStock> findDemandeById(UUID id) {
        return demandeRepository.findById(id);
    }
    
    public List<DemandeSortieStock> findDemandesByStatut(String statut) {
        return demandeRepository.findByStatutCode(statut);
    }
    
    public List<DemandeSortieStock> findDemandesConsommation() {
        return demandeRepository.findByType("CONSOMMATION");
    }
    
    public List<DemandeSortieStock> findDemandesRebut() {
        return demandeRepository.findByType("REBUT");
    }
    
    // ============ NUMÉROTATION ============
    
    public synchronized String generateDemandeNumero(String type) {
        String prefix = ("CONSOMMATION".equals(type) ? "CONSO-" : "REBUT-") 
            + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = demandeRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%03d", nextNum);
            if (demandeRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    // ============ WORKFLOW ============
    
    public DemandeSortieStock createDemande(DemandeSortieStock demande, Utilisateur demandeur) {
        demande.setNumero(generateDemandeNumero(demande.getType()));
        demande.setStatutCode("BROUILLON");
        demande.setDemandeur(demandeur);
        demande.setDateDemande(OffsetDateTime.now());
        
        // Calculer les coûts
        for (LigneDemandeSortie ligne : demande.getLignes()) {
            ligne.calculerMontant();
        }
        demande.calculerCoutTotal();
        
        DemandeSortieStock saved = demandeRepository.save(demande);
        auditService.logAction("DEMANDE_SORTIE_STOCK", saved.getId(), "CREATION", demandeur, null);
        return saved;
    }
    
    public DemandeSortieStock soumettreDemande(UUID demandeId, Utilisateur demandeur) {
        DemandeSortieStock demande = demandeRepository.findById(demandeId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        if (!"BROUILLON".equals(demande.getStatutCode())) {
            throw new RuntimeException("Seules les demandes en brouillon peuvent être soumises");
        }
        
        demande.setStatutCode("SOUMISE");
        DemandeSortieStock saved = demandeRepository.save(demande);
        auditService.logWorkflow("DEMANDE_SORTIE_STOCK", demandeId, "BROUILLON", "SOUMISE", demandeur, "SOUMISSION", null);
        return saved;
    }
    
    public DemandeSortieStock approuverDemande(UUID demandeId, Utilisateur approbateur, String commentaire) {
        DemandeSortieStock demande = demandeRepository.findById(demandeId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        if (!"SOUMISE".equals(demande.getStatutCode())) {
            throw new RuntimeException("Seules les demandes soumises peuvent être approuvées");
        }
        
        // Vérification : l'approbateur ne peut pas être le demandeur (règle de séparation des tâches)
        if (demande.getDemandeur().getId().equals(approbateur.getId())) {
            throw new RuntimeException("Le demandeur ne peut pas approuver sa propre demande");
        }
        
        demande.setStatutCode("APPROUVEE");
        demande.setApprobateur(approbateur);
        demande.setDateApprobation(OffsetDateTime.now());
        demande.setCommentaireApprobation(commentaire);
        
        DemandeSortieStock saved = demandeRepository.save(demande);
        auditService.logWorkflow("DEMANDE_SORTIE_STOCK", demandeId, "SOUMISE", "APPROUVEE", approbateur, "APPROBATION", commentaire);
        return saved;
    }
    
    public DemandeSortieStock rejeterDemande(UUID demandeId, Utilisateur approbateur, String motif) {
        DemandeSortieStock demande = demandeRepository.findById(demandeId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        demande.setStatutCode("REJETEE");
        demande.setApprobateur(approbateur);
        demande.setDateApprobation(OffsetDateTime.now());
        demande.setCommentaireApprobation(motif);
        
        DemandeSortieStock saved = demandeRepository.save(demande);
        auditService.logWorkflow("DEMANDE_SORTIE_STOCK", demandeId, demande.getStatutCode(), "REJETEE", approbateur, "REJET", motif);
        return saved;
    }
    
    public DemandeSortieStock executerDemande(UUID demandeId, Utilisateur executeur) {
        DemandeSortieStock demande = demandeRepository.findById(demandeId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        if (!"APPROUVEE".equals(demande.getStatutCode())) {
            throw new RuntimeException("Seules les demandes approuvées peuvent être exécutées");
        }
        
        // Déterminer le type de mouvement
        String typeMouvementCode = "CONSOMMATION".equals(demande.getType()) ? "CONSOMMATION" : "REBUT";
        TypeMouvement typeMouvement = typeMouvementRepository.findById(typeMouvementCode)
            .orElseThrow(() -> new RuntimeException("Type mouvement " + typeMouvementCode + " non trouvé"));
        
        // Exécuter chaque ligne
        for (LigneDemandeSortie ligne : demande.getLignes()) {
            // Vérifier le stock
            Stock stock = stockRepository.findByDepotIdAndArticleIdAndLotId(
                demande.getDepot().getId(),
                ligne.getArticle().getId(),
                ligne.getLot() != null ? ligne.getLot().getId() : null
            ).orElseThrow(() -> new RuntimeException("Stock non trouvé pour " + ligne.getArticle().getLabel()));
            
            if (stock.getQtyDisponible().compareTo(ligne.getQtyDemandee()) < 0) {
                throw new RuntimeException("Stock insuffisant pour " + ligne.getArticle().getLabel() + 
                    " (disponible: " + stock.getQtyDisponible() + ", demandé: " + ligne.getQtyDemandee() + ")");
            }
            
            // Décrémenter le stock
            stock.setQtyReel(stock.getQtyReel().subtract(ligne.getQtyDemandee()));
            stockRepository.save(stock);
            
            // Créer le mouvement
            creerMouvement(typeMouvement, demande, ligne, executeur);
            
            // Marquer la ligne comme exécutée
            ligne.setQtyExecutee(ligne.getQtyDemandee());
            ligneDemandeRepository.save(ligne);
        }
        
        demande.setStatutCode("EXECUTEE");
        demande.setExecuteur(executeur);
        demande.setDateExecution(OffsetDateTime.now());
        
        DemandeSortieStock saved = demandeRepository.save(demande);
        auditService.logWorkflow("DEMANDE_SORTIE_STOCK", demandeId, "APPROUVEE", "EXECUTEE", executeur, "EXECUTION", null);
        return saved;
    }
    
    // ============ HELPER METHODS ============
    
    private void creerMouvement(TypeMouvement typeMouvement, DemandeSortieStock demande,
                                LigneDemandeSortie ligne, Utilisateur user) {
        String numeroMvt = "MVT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = mouvementRepository.findMaxNumero(numeroMvt);
        numeroMvt += String.format("%05d", (maxNum != null ? maxNum : 0) + 1);
        
        MouvementStock mouvement = MouvementStock.builder()
            .numero(numeroMvt)
            .typeMouvement(typeMouvement)
            .referenceDoc(demande.getNumero())
            .article(ligne.getArticle())
            .lot(ligne.getLot())
            .depotSource(demande.getDepot())
            .emplacementSource(ligne.getEmplacement())
            .qty(ligne.getQtyDemandee())
            .unitCost(ligne.getUnitCost())
            .utilisateur(user)
            .createdAt(OffsetDateTime.now())
            .build();
        
        mouvementRepository.save(mouvement);
    }
    
    // ============ STATISTIQUES ============
    
    public Long countByStatut(String statut) {
        return demandeRepository.countByStatut(statut);
    }
    
    public Long countConsommationByStatut(String statut) {
        return demandeRepository.countByTypeAndStatut("CONSOMMATION", statut);
    }
    
    public Long countRebutByStatut(String statut) {
        return demandeRepository.countByTypeAndStatut("REBUT", statut);
    }
}
