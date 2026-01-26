package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.repository.achat.*;
import module.avs.repository.finance.*;
import module.avs.repository.stock.*;
import module.avs.repository.vente.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    
    private final CommandeAchatRepository commandeAchatRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final FactureFournisseurRepository factureFournisseurRepository;
    private final FactureClientRepository factureClientRepository;
    private final StockRepository stockRepository;
    private final InventaireRepository inventaireRepository;
    private final LotRepository lotRepository;
    private final BonReceptionRepository bonReceptionRepository;
    private final BonLivraisonRepository bonLivraisonRepository;
    
    // ============ KPIs DIRECTION GÉNÉRALE ============
    
    public Map<String, Object> getKPIsDirection() {
        Map<String, Object> kpis = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        LocalDate startMonth = today.withDayOfMonth(1);
        LocalDate startYear = today.withDayOfYear(1);
        
        // Chiffre d'affaires du mois
        BigDecimal caMois = factureClientRepository.sumByPeriod(startMonth, today);
        kpis.put("caMois", caMois != null ? caMois : BigDecimal.ZERO);
        
        // Chiffre d'affaires de l'année
        BigDecimal caAnnee = factureClientRepository.sumByPeriod(startYear, today);
        kpis.put("caAnnee", caAnnee != null ? caAnnee : BigDecimal.ZERO);
        
        // Créances clients
        BigDecimal creancesClients = factureClientRepository.getTotalOutstandingAmount();
        kpis.put("creancesClients", creancesClients != null ? creancesClients : BigDecimal.ZERO);
        
        // Dettes fournisseurs
        BigDecimal dettesFournisseurs = factureFournisseurRepository.getTotalOutstandingAmount();
        kpis.put("dettesFournisseurs", dettesFournisseurs != null ? dettesFournisseurs : BigDecimal.ZERO);
        
        // Commandes en cours
        kpis.put("commandesClientEnCours", commandeClientRepository.countByStatut("CONFIRMEE"));
        kpis.put("commandesAchatEnCours", commandeAchatRepository.countByStatut("ENVOYEE"));
        
        return kpis;
    }
    
    // ============ KPIs ACHATS ============
    
    public Map<String, Object> getKPIsAchats() {
        Map<String, Object> kpis = new HashMap<>();
        
        // Commandes par statut
        kpis.put("commandesBrouillon", commandeAchatRepository.countByStatut("BROUILLON"));
        kpis.put("commandesValidees", commandeAchatRepository.countByStatut("VALIDEE"));
        kpis.put("commandesEnvoyees", commandeAchatRepository.countByStatut("ENVOYEE"));
        kpis.put("commandesPartielles", commandeAchatRepository.countByStatut("PARTIEL"));
        kpis.put("commandesCloturees", commandeAchatRepository.countByStatut("CLOTUREE"));
        
        // Réceptions en attente
        kpis.put("receptionsEnAttente", bonReceptionRepository.findByStatutCode("BROUILLON").size());
        
        // Factures bloquées (3-way mismatch)
        kpis.put("facturesBloquees", factureFournisseurRepository.findByStatutCode("BROUILLON").size());
        
        return kpis;
    }
    
    // ============ KPIs VENTES ============
    
    public Map<String, Object> getKPIsVentes() {
        Map<String, Object> kpis = new HashMap<>();
        
        // Commandes par statut
        kpis.put("commandesBrouillon", commandeClientRepository.countByStatut("BROUILLON"));
        kpis.put("commandesConfirmees", commandeClientRepository.countByStatut("CONFIRMEE"));
        kpis.put("commandesPreparation", commandeClientRepository.countByStatut("PREPARATION"));
        kpis.put("commandesExpediees", commandeClientRepository.countByStatut("EXPEDIEE"));
        
        // Livraisons du jour
        kpis.put("livraisonsDuJour", bonLivraisonRepository.findAll().stream()
            .filter(bl -> bl.getDateExpedition().toLocalDate().equals(LocalDate.now()))
            .count());
        
        // Factures impayées
        kpis.put("facturesImpayees", factureClientRepository.findByStatutCode("A_PAYER").size());
        
        return kpis;
    }
    
    // ============ KPIs STOCK ============
    
    public Map<String, Object> getKPIsStock() {
        Map<String, Object> kpis = new HashMap<>();
        
        // Nombre d'articles en stock
        kpis.put("articlesEnStock", stockRepository.count());
        
        // Articles à faible stock (moins de 10 unités)
        long articlesFaibleStock = stockRepository.findAll().stream()
            .filter(s -> s.getQtyReel().compareTo(new BigDecimal("10")) < 0)
            .count();
        kpis.put("articlesFaibleStock", articlesFaibleStock);
        
        // Articles réservés
        long articlesReserves = stockRepository.findAll().stream()
            .filter(s -> s.getQtyReserve().compareTo(BigDecimal.ZERO) > 0)
            .count();
        kpis.put("articlesReserves", articlesReserves);
        
        // Lots périmés
        kpis.put("lotsPerimes", lotRepository.findExpiredLots(LocalDate.now()).size());
        
        // Lots expirant bientôt (30 jours)
        kpis.put("lotsExpirantBientot", lotRepository.findLotsExpiringSoon(LocalDate.now(), LocalDate.now().plusDays(30)).size());
        
        // Inventaires en cours
        kpis.put("inventairesEnCours", inventaireRepository.findByStatutCode("EN_COURS").size());
        
        return kpis;
    }
    
    // ============ KPIs FINANCE ============
    
    public Map<String, Object> getKPIsFinance() {
        Map<String, Object> kpis = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        LocalDate startMonth = today.withDayOfMonth(1);
        
        // Encaissements du mois
        BigDecimal encaissementsMois = BigDecimal.ZERO; // À calculer depuis les encaissements
        kpis.put("encaissementsMois", encaissementsMois);
        
        // Paiements du mois
        BigDecimal paiementsMois = BigDecimal.ZERO; // À calculer depuis les paiements
        kpis.put("paiementsMois", paiementsMois);
        
        // Factures en retard
        kpis.put("facturesClientRetard", factureClientRepository.findOverdueFactures(today).size());
        kpis.put("facturesFournisseurRetard", factureFournisseurRepository.findOverdueFactures(today).size());
        
        // Créances totales
        BigDecimal creances = factureClientRepository.getTotalOutstandingAmount();
        kpis.put("creancesTotales", creances != null ? creances : BigDecimal.ZERO);
        
        // Dettes totales
        BigDecimal dettes = factureFournisseurRepository.getTotalOutstandingAmount();
        kpis.put("dettesTotales", dettes != null ? dettes : BigDecimal.ZERO);
        
        return kpis;
    }
    
    // ============ DONNÉES POUR GRAPHIQUES ============
    
    public List<Map<String, Object>> getEvolutionCA(int nbMois) {
        List<Map<String, Object>> evolution = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = nbMois - 1; i >= 0; i--) {
            LocalDate mois = today.minusMonths(i);
            LocalDate debut = mois.withDayOfMonth(1);
            LocalDate fin = mois.withDayOfMonth(mois.lengthOfMonth());
            
            BigDecimal ca = factureClientRepository.sumByPeriod(debut, fin);
            
            Map<String, Object> point = new HashMap<>();
            point.put("mois", mois.getMonth().toString());
            point.put("ca", ca != null ? ca : BigDecimal.ZERO);
            evolution.add(point);
        }
        
        return evolution;
    }
    
    public Map<String, Long> getRepartitionStatutsCommandes() {
        Map<String, Long> repartition = new HashMap<>();
        repartition.put("BROUILLON", commandeClientRepository.countByStatut("BROUILLON"));
        repartition.put("CONFIRMEE", commandeClientRepository.countByStatut("CONFIRMEE"));
        repartition.put("PREPARATION", commandeClientRepository.countByStatut("PREPARATION"));
        repartition.put("EXPEDIEE", commandeClientRepository.countByStatut("EXPEDIEE"));
        return repartition;
    }
}
