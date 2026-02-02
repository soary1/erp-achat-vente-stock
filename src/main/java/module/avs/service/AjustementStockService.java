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
public class AjustementStockService {
    
    private final AjustementStockRepository ajustementRepository;
    private final StockRepository stockRepository;
    private final MouvementStockRepository mouvementRepository;
    private final TypeMouvementRepository typeMouvementRepository;
    private final AuditService auditService;
    
    // Seuil pour double validation (peut être paramétré)
    private static final BigDecimal SEUIL_DOUBLE_VALIDATION = new BigDecimal("50000"); // 50 000 MGA
    
    // ============ CONSULTATION ============
    
    public List<AjustementStock> findAllAjustements() {
        return ajustementRepository.findAll();
    }
    
    public Page<AjustementStock> findAllAjustements(Pageable pageable) {
        return ajustementRepository.findAllByOrderByDateDemandeDesc(pageable);
    }
    
    public Optional<AjustementStock> findAjustementById(UUID id) {
        return ajustementRepository.findById(id);
    }
    
    public List<AjustementStock> findAjustementsByStatut(String statut) {
        return ajustementRepository.findByStatutCode(statut);
    }
    
    public List<AjustementStock> findAjustementsEnAttenteDoubleValidation() {
        return ajustementRepository.findEnAttenteDoubleValidation(SEUIL_DOUBLE_VALIDATION);
    }
    
    // ============ NUMÉROTATION ============
    
    public synchronized String generateAjustementNumero() {
        String prefix = "ADJ-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = ajustementRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%03d", nextNum);
            if (ajustementRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    // ============ WORKFLOW ============
    
    public AjustementStock createAjustement(AjustementStock ajustement, Utilisateur demandeur) {
        // Récupérer le stock actuel
        Stock stock = stockRepository.findByDepotIdAndArticleIdAndLotId(
            ajustement.getDepot().getId(),
            ajustement.getArticle().getId(),
            ajustement.getLot() != null ? ajustement.getLot().getId() : null
        ).orElseThrow(() -> new RuntimeException("Stock non trouvé"));
        
        ajustement.setNumero(generateAjustementNumero());
        ajustement.setStatutCode("BROUILLON");
        ajustement.setDemandeur(demandeur);
        ajustement.setDateDemande(OffsetDateTime.now());
        ajustement.setQtyTheorique(stock.getQtyReel());
        ajustement.calculerMontantImpact();
        
        AjustementStock saved = ajustementRepository.save(ajustement);
        auditService.logAction("AJUSTEMENT_STOCK", saved.getId(), "CREATION", demandeur, null);
        return saved;
    }
    
    public AjustementStock soumettreAjustement(UUID ajustementId, Utilisateur demandeur) {
        AjustementStock ajustement = ajustementRepository.findById(ajustementId)
            .orElseThrow(() -> new RuntimeException("Ajustement non trouvé"));
        
        if (!"BROUILLON".equals(ajustement.getStatutCode())) {
            throw new RuntimeException("Seuls les ajustements en brouillon peuvent être soumis");
        }
        
        ajustement.setStatutCode("SOUMIS");
        AjustementStock saved = ajustementRepository.save(ajustement);
        auditService.logWorkflow("AJUSTEMENT_STOCK", ajustementId, "BROUILLON", "SOUMIS", demandeur, "SOUMISSION", null);
        return saved;
    }
    
    public AjustementStock approuverNiveau1(UUID ajustementId, Utilisateur approbateur, String commentaire) {
        AjustementStock ajustement = ajustementRepository.findById(ajustementId)
            .orElseThrow(() -> new RuntimeException("Ajustement non trouvé"));
        
        if (!"SOUMIS".equals(ajustement.getStatutCode())) {
            throw new RuntimeException("Seuls les ajustements soumis peuvent être approuvés");
        }
        
        // Vérification : l'approbateur ne peut pas être le demandeur (règle de séparation des tâches)
        // Cette vérification est aussi faite au niveau base de données (trigger)
        if (ajustement.getDemandeur().getId().equals(approbateur.getId())) {
            throw new RuntimeException("Le demandeur ne peut pas approuver son propre ajustement");
        }
        
        ajustement.setApprobateurNiveau1(approbateur);
        ajustement.setDateApprobationNiveau1(OffsetDateTime.now());
        ajustement.setCommentaireApprobation(commentaire);
        
        // Vérifier si double validation nécessaire
        BigDecimal montantAbsolu = ajustement.getMontantImpact() != null 
            ? ajustement.getMontantImpact().abs() 
            : BigDecimal.ZERO;
        
        if (montantAbsolu.compareTo(SEUIL_DOUBLE_VALIDATION) > 0) {
            // Nécessite une 2ème approbation
            ajustement.setStatutCode("APPROUVE_NIVEAU1");
            AjustementStock saved = ajustementRepository.save(ajustement);
            auditService.logWorkflow("AJUSTEMENT_STOCK", ajustementId, "SOUMIS", "APPROUVE_NIVEAU1", approbateur, "APPROBATION_N1", commentaire);
            return saved;
        } else {
            // Validation simple suffit
            ajustement.setStatutCode("APPROUVE_FINAL");
            ajustement.setApprobateurFinal(approbateur);
            ajustement.setDateApprobationFinale(OffsetDateTime.now());
            AjustementStock saved = ajustementRepository.save(ajustement);
            auditService.logWorkflow("AJUSTEMENT_STOCK", ajustementId, "SOUMIS", "APPROUVE_FINAL", approbateur, "APPROBATION", commentaire);
            return saved;
        }
    }
    
    public AjustementStock approuverFinal(UUID ajustementId, Utilisateur approbateurFinal, String commentaire) {
        AjustementStock ajustement = ajustementRepository.findById(ajustementId)
            .orElseThrow(() -> new RuntimeException("Ajustement non trouvé"));
        
        if (!"APPROUVE_NIVEAU1".equals(ajustement.getStatutCode())) {
            throw new RuntimeException("Seuls les ajustements approuvés niveau 1 nécessitent une approbation finale");
        }
        
        // Vérification : l'approbateur final ne peut pas être le demandeur
        if (ajustement.getDemandeur().getId().equals(approbateurFinal.getId())) {
            throw new RuntimeException("Le demandeur ne peut pas approuver son propre ajustement");
        }
        
        ajustement.setStatutCode("APPROUVE_FINAL");
        ajustement.setApprobateurFinal(approbateurFinal);
        ajustement.setDateApprobationFinale(OffsetDateTime.now());
        if (commentaire != null) {
            ajustement.setCommentaireApprobation(
                (ajustement.getCommentaireApprobation() != null ? ajustement.getCommentaireApprobation() + "\n" : "") + 
                "Niveau 2: " + commentaire
            );
        }
        
        AjustementStock saved = ajustementRepository.save(ajustement);
        auditService.logWorkflow("AJUSTEMENT_STOCK", ajustementId, "APPROUVE_NIVEAU1", "APPROUVE_FINAL", approbateurFinal, "APPROBATION_N2", commentaire);
        return saved;
    }
    
    public AjustementStock rejeterAjustement(UUID ajustementId, Utilisateur approbateur, String motif) {
        AjustementStock ajustement = ajustementRepository.findById(ajustementId)
            .orElseThrow(() -> new RuntimeException("Ajustement non trouvé"));
        
        ajustement.setStatutCode("REJETE");
        ajustement.setCommentaireApprobation(motif);
        
        AjustementStock saved = ajustementRepository.save(ajustement);
        auditService.logWorkflow("AJUSTEMENT_STOCK", ajustementId, ajustement.getStatutCode(), "REJETE", approbateur, "REJET", motif);
        return saved;
    }
    
    public AjustementStock executerAjustement(UUID ajustementId, Utilisateur executeur) {
        AjustementStock ajustement = ajustementRepository.findById(ajustementId)
            .orElseThrow(() -> new RuntimeException("Ajustement non trouvé"));
        
        if (!"APPROUVE_FINAL".equals(ajustement.getStatutCode())) {
            throw new RuntimeException("Seuls les ajustements approuvés peuvent être exécutés");
        }
        
        // Récupérer le stock
        Stock stock = stockRepository.findByDepotIdAndArticleIdAndLotId(
            ajustement.getDepot().getId(),
            ajustement.getArticle().getId(),
            ajustement.getLot() != null ? ajustement.getLot().getId() : null
        ).orElseThrow(() -> new RuntimeException("Stock non trouvé"));
        
        // Calculer l'écart (qtyEcart est calculé automatiquement par la base mais on le récupère ici)
        BigDecimal ecart = ajustement.getQtyReelle().subtract(ajustement.getQtyTheorique());
        
        // Déterminer le type de mouvement
        String typeMouvementCode;
        if (ecart.compareTo(BigDecimal.ZERO) > 0) {
            typeMouvementCode = "AJUSTEMENT_POS";
        } else if (ecart.compareTo(BigDecimal.ZERO) < 0) {
            typeMouvementCode = "AJUSTEMENT_NEG";
        } else {
            // Pas d'écart, pas besoin de créer de mouvement
            ajustement.setStatutCode("EXECUTE");
            ajustement.setDateExecution(OffsetDateTime.now());
            AjustementStock saved = ajustementRepository.save(ajustement);
            auditService.logWorkflow("AJUSTEMENT_STOCK", ajustementId, "APPROUVE_FINAL", "EXECUTE", executeur, "EXECUTION", "Aucun écart");
            return saved;
        }
        
        TypeMouvement typeMouvement = typeMouvementRepository.findById(typeMouvementCode)
            .orElseThrow(() -> new RuntimeException("Type mouvement " + typeMouvementCode + " non trouvé"));
        
        // Mettre à jour le stock
        stock.setQtyReel(ajustement.getQtyReelle());
        stockRepository.save(stock);
        
        // Créer le mouvement
        creerMouvement(typeMouvement, ajustement, ecart, executeur);
        
        ajustement.setStatutCode("EXECUTE");
        ajustement.setDateExecution(OffsetDateTime.now());
        
        AjustementStock saved = ajustementRepository.save(ajustement);
        auditService.logWorkflow("AJUSTEMENT_STOCK", ajustementId, "APPROUVE_FINAL", "EXECUTE", executeur, "EXECUTION", null);
        return saved;
    }
    
    // ============ HELPER METHODS ============
    
    private void creerMouvement(TypeMouvement typeMouvement, AjustementStock ajustement,
                                BigDecimal ecart, Utilisateur user) {
        String numeroMvt = "MVT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = mouvementRepository.findMaxNumero(numeroMvt);
        numeroMvt += String.format("%05d", (maxNum != null ? maxNum : 0) + 1);
        
        MouvementStock.MouvementStockBuilder builder = MouvementStock.builder()
            .numero(numeroMvt)
            .typeMouvement(typeMouvement)
            .referenceDoc(ajustement.getNumero())
            .article(ajustement.getArticle())
            .lot(ajustement.getLot())
            .qty(ecart.abs())
            .unitCost(ajustement.getUnitCost())
            .utilisateur(user)
            .createdAt(OffsetDateTime.now());
        
        if (ecart.compareTo(BigDecimal.ZERO) > 0) {
            // Ajustement positif : entrée
            builder.depotDest(ajustement.getDepot())
                   .emplacementDest(ajustement.getEmplacement());
        } else {
            // Ajustement négatif : sortie
            builder.depotSource(ajustement.getDepot())
                   .emplacementSource(ajustement.getEmplacement());
        }
        
        mouvementRepository.save(builder.build());
    }
    
    // ============ STATISTIQUES ============
    
    public Long countByStatut(String statut) {
        return ajustementRepository.countByStatut(statut);
    }
    
    public BigDecimal getSeuilDoubleValidation() {
        return SEUIL_DOUBLE_VALIDATION;
    }
}
