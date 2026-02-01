package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.achat.CommandeAchat;
import module.avs.model.stock.*;
import module.avs.model.tiers.Client;
import module.avs.model.tiers.Fournisseur;
import module.avs.model.vente.BonLivraison;
import module.avs.model.vente.CommandeClient;
import module.avs.repository.stock.*;
import module.avs.repository.achat.CommandeAchatRepository;
import module.avs.repository.vente.BonLivraisonRepository;
import module.avs.repository.vente.CommandeClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service dédié à la traçabilité des lots.
 * Permet de suivre un lot depuis son fournisseur d'origine jusqu'aux clients finaux.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TracabiliteService {
    
    private final LotRepository lotRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final BonReceptionRepository bonReceptionRepository;
    private final BonLivraisonRepository bonLivraisonRepository;
    private final CommandeAchatRepository commandeAchatRepository;
    private final CommandeClientRepository commandeClientRepository;
    
    /**
     * Récupère l'historique complet des mouvements d'un lot
     */
    public List<MouvementStock> getHistoriqueMouvements(UUID lotId) {
        return mouvementStockRepository.findByLotIdOrderByCreatedAtAsc(lotId);
    }
    
    /**
     * Récupère le fournisseur d'origine d'un lot (via la réception)
     */
    public Optional<Fournisseur> getFournisseurOrigine(UUID lotId) {
        List<BonReception> receptions = bonReceptionRepository.findByLotId(lotId);
        if (!receptions.isEmpty()) {
            BonReception premiereReception = receptions.get(0);
            if (premiereReception.getCommandeAchat() != null) {
                return Optional.ofNullable(premiereReception.getCommandeAchat().getFournisseur());
            }
        }
        return Optional.empty();
    }
    
    /**
     * Récupère les clients ayant reçu ce lot (via les livraisons)
     */
    public List<Client> getClientsDestinataires(UUID lotId) {
        List<BonLivraison> livraisons = bonLivraisonRepository.findByLotId(lotId);
        Set<Client> clients = new LinkedHashSet<>();
        
        for (BonLivraison bl : livraisons) {
            if (bl.getCommande() != null && bl.getCommande().getClient() != null) {
                clients.add(bl.getCommande().getClient());
            }
        }
        
        return new ArrayList<>(clients);
    }
    
    /**
     * DTO pour la traçabilité complète
     */
    public TracabiliteComplete getTracabiliteComplete(UUID lotId) {
        Lot lot = lotRepository.findById(lotId)
            .orElseThrow(() -> new RuntimeException("Lot non trouvé"));
        
        TracabiliteComplete tracabilite = new TracabiliteComplete();
        tracabilite.setLot(lot);
        tracabilite.setMouvements(getHistoriqueMouvements(lotId));
        tracabilite.setFournisseurOrigine(getFournisseurOrigine(lotId).orElse(null));
        tracabilite.setClientsDestinataires(getClientsDestinataires(lotId));
        
        // Calculer les quantités
        tracabilite.calculerQuantites();
        
        return tracabilite;
    }
    
    /**
     * Classe interne pour regrouper toutes les infos de traçabilité
     */
    @lombok.Data
    public static class TracabiliteComplete {
        private Lot lot;
        private List<MouvementStock> mouvements;
        private Fournisseur fournisseurOrigine;
        private List<Client> clientsDestinataires;
        
        private java.math.BigDecimal qtyRecue = java.math.BigDecimal.ZERO;
        private java.math.BigDecimal qtyExpediee = java.math.BigDecimal.ZERO;
        private java.math.BigDecimal qtyEnStock = java.math.BigDecimal.ZERO;
        
        public void calculerQuantites() {
            for (MouvementStock m : mouvements) {
                if ("RECEPTION".equals(m.getTypeMouvement().getCode())) {
                    qtyRecue = qtyRecue.add(m.getQty());
                } else if ("EXPEDITION".equals(m.getTypeMouvement().getCode())) {
                    qtyExpediee = qtyExpediee.add(m.getQty());
                }
            }
            qtyEnStock = qtyRecue.subtract(qtyExpediee);
        }
    }
}
