package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.achat.CommandeAchat;
import module.avs.model.article.*;
import module.avs.model.organisation.*;
import module.avs.model.stock.BonReception;
import module.avs.model.stock.TypeMouvement;
import module.avs.model.tiers.*;
import module.avs.model.referentiel.*;
import module.avs.repository.achat.CommandeAchatRepository;
import module.avs.repository.article.*;
import module.avs.repository.organisation.*;
import module.avs.repository.stock.BonReceptionRepository;
import module.avs.repository.stock.TypeMouvementRepository;
import module.avs.repository.tiers.*;
import module.avs.repository.referentiel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ReferentielService {
    
    // Repositories
    private final DeviseRepository deviseRepository;
    private final PaysRepository paysRepository;
    private final UniteMesureRepository uniteMesureRepository;
    private final TypeTaxeRepository typeTaxeRepository;
    private final MethodeValorisationRepository methodeValorisationRepository;
    private final ModePaiementRepository modePaiementRepository;
    private final GroupeSocieteRepository groupeSocieteRepository;
    private final SocieteRepository societeRepository;
    private final SiteRepository siteRepository;
    private final DepotRepository depotRepository;
    private final EmplacementRepository emplacementRepository;
    private final FamilleArticleRepository familleArticleRepository;
    private final ArticleRepository articleRepository;
    private final ClientRepository clientRepository;
    private final FournisseurRepository fournisseurRepository;
    private final CommandeAchatRepository commandeAchatRepository;
    private final BonReceptionRepository bonReceptionRepository;
    private final TypeMouvementRepository typeMouvementRepository;
    private final MotifRetourRepository motifRetourRepository;
    private final MotifSortieRepository motifSortieRepository;
    private final MotifAjustementRepository motifAjustementRepository;
    
    // ============ DEVISES ============
    public List<Devise> findAllDevises() {
        return deviseRepository.findAll();
    }
    
    public Optional<Devise> findDeviseById(String code) {
        return deviseRepository.findById(code);
    }
    
    public Optional<Devise> findDeviseByCode(String code) {
        return deviseRepository.findById(code);
    }
    
    public Devise saveDevise(Devise devise) {
        return deviseRepository.save(devise);
    }
    
    public void deleteDevise(String code) {
        deviseRepository.deleteById(code);
    }
    
    // ============ PAYS ============
    public List<Pays> findAllPays() {
        return paysRepository.findAll();
    }
    
    public Optional<Pays> findPaysById(String code) {
        return paysRepository.findById(code);
    }
    
    public Pays savePays(Pays pays) {
        return paysRepository.save(pays);
    }
    
    // ============ UNITES ============
    public List<UniteMesure> findAllUnitesMesure() {
        return uniteMesureRepository.findAll();
    }
    
    public List<UniteMesure> findAllUnites() {
        return uniteMesureRepository.findAll();
    }
    
    public Optional<UniteMesure> findUniteByCode(String code) {
        return uniteMesureRepository.findById(code);
    }
    
    public UniteMesure saveUniteMesure(UniteMesure unite) {
        return uniteMesureRepository.save(unite);
    }
    
    public UniteMesure saveUnite(UniteMesure unite) {
        return uniteMesureRepository.save(unite);
    }
    
    // ============ TAXES ============
    public List<TypeTaxe> findAllTypesTaxe() {
        return typeTaxeRepository.findAll();
    }
    
    public List<TypeTaxe> findAllTaxes() {
        return typeTaxeRepository.findAll();
    }
    
    public Optional<TypeTaxe> findTaxeByCode(String code) {
        return typeTaxeRepository.findById(code);
    }
    
    public TypeTaxe saveTypeTaxe(TypeTaxe taxe) {
        return typeTaxeRepository.save(taxe);
    }
    
    public TypeTaxe saveTaxe(TypeTaxe taxe) {
        return typeTaxeRepository.save(taxe);
    }
    
    // ============ METHODES VALORISATION ============
    public List<MethodeValorisation> findAllMethodesValorisation() {
        return methodeValorisationRepository.findAll();
    }
    
    // ============ MODES PAIEMENT ============
    public List<ModePaiement> findAllModesPaiement() {
        return modePaiementRepository.findAll();
    }
    
    public Optional<ModePaiement> findModePaiementByCode(String code) {
        return modePaiementRepository.findById(code);
    }
    
    // ============ ORGANISATION ============
    public List<GroupeSociete> findAllGroupes() {
        return groupeSocieteRepository.findAll();
    }
    
    public List<Societe> findAllSocietes() {
        return societeRepository.findAll();
    }
    
    public Optional<Societe> findSocieteById(UUID id) {
        return societeRepository.findById(id);
    }
    
    public Societe saveSociete(Societe societe) {
        return societeRepository.save(societe);
    }
    
    public List<Site> findAllSites() {
        return siteRepository.findByIsActiveTrue();
    }
    
    public List<Site> findSitesBySociete(UUID societeId) {
        return siteRepository.findBySocieteIdAndIsActiveTrue(societeId);
    }
    
    public Optional<Site> findSiteById(UUID id) {
        return siteRepository.findById(id);
    }
    
    public Site saveSite(Site site) {
        return siteRepository.save(site);
    }
    
    public List<Depot> findAllDepots() {
        return depotRepository.findByIsActiveTrue();
    }
    
    public List<Depot> findDepotsBySite(UUID siteId) {
        return depotRepository.findBySiteIdAndIsActiveTrue(siteId);
    }
    
    public Optional<Depot> findDepotById(UUID id) {
        return depotRepository.findByIdWithSite(id);
    }
    
    public Depot saveDepot(Depot depot) {
        return depotRepository.save(depot);
    }
    
    public List<Emplacement> findEmplacementsByDepot(UUID depotId) {
        return emplacementRepository.findByDepotId(depotId);
    }
    
    public List<Emplacement> findAllEmplacements() {
        return emplacementRepository.findAllWithDepot();
    }
    
    // Pour la sérialisation JSON (formulaires)
    public List<java.util.Map<String, String>> findAllEmplacementsForForm() {
        return emplacementRepository.findAllWithDepot().stream()
            .map(e -> java.util.Map.of(
                "id", e.getId().toString(),
                "code", e.getCode(),
                "depotId", e.getDepot().getId().toString()
            ))
            .collect(java.util.stream.Collectors.toList());
    }
    
    public Optional<Emplacement> findEmplacementById(UUID id) {
        return emplacementRepository.findById(id);
    }
    
    public Emplacement saveEmplacement(Emplacement emplacement) {
        return emplacementRepository.save(emplacement);
    }
    
    // ============ FAMILLES ARTICLES ============
    public List<FamilleArticle> findAllFamilles() {
        return familleArticleRepository.findAll();
    }
    
    public Optional<FamilleArticle> findFamilleById(UUID id) {
        return familleArticleRepository.findById(id);
    }
    
    public FamilleArticle saveFamille(FamilleArticle famille) {
        return familleArticleRepository.save(famille);
    }
    
    // ============ ARTICLES ============
    public List<Article> findAllArticles() {
        return articleRepository.findByIsActiveTrue();
    }
    
    public Page<Article> searchArticles(String search, Pageable pageable) {
        return articleRepository.searchArticles(search, pageable);
    }
    
    public Optional<Article> findArticleById(UUID id) {
        return articleRepository.findById(id);
    }
    
    public Optional<Article> findArticleBySku(String sku) {
        return articleRepository.findBySku(sku);
    }
    
    public Article saveArticle(Article article) {
        return articleRepository.save(article);
    }
    
    public void deleteArticle(UUID id) {
        articleRepository.findById(id).ifPresent(a -> {
            a.setIsActive(false);
            articleRepository.save(a);
        });
    }
    
    // ============ CLIENTS ============
    public List<Client> findAllClients() {
        return clientRepository.findByIsActiveTrue();
    }
    
    public Page<Client> searchClients(String search, Pageable pageable) {
        return clientRepository.searchClients(search, pageable);
    }
    
    public Optional<Client> findClientById(UUID id) {
        return clientRepository.findById(id);
    }
    
    public Client saveClient(Client client) {
        return clientRepository.save(client);
    }
    
    public void deleteClient(UUID id) {
        clientRepository.findById(id).ifPresent(c -> {
            c.setIsActive(false);
            clientRepository.save(c);
        });
    }
    
    // ============ FOURNISSEURS ============
    public List<Fournisseur> findAllFournisseurs() {
        return fournisseurRepository.findByIsActiveTrue();
    }
    
    public Page<Fournisseur> searchFournisseurs(String search, Pageable pageable) {
        return fournisseurRepository.searchFournisseurs(search, pageable);
    }
    
    public Optional<Fournisseur> findFournisseurById(UUID id) {
        return fournisseurRepository.findById(id);
    }
    
    public Fournisseur saveFournisseur(Fournisseur fournisseur) {
        return fournisseurRepository.save(fournisseur);
    }
    
    public void deleteFournisseur(UUID id) {
        fournisseurRepository.findById(id).ifPresent(f -> {
            f.setIsActive(false);
            fournisseurRepository.save(f);
        });
    }
    
    // ============ COMMANDES ACHAT EN ATTENTE ============
    public List<CommandeAchat> findCommandesAchatEnAttente() {
        return commandeAchatRepository.findByStatutCode("ENVOYEE");
    }
    
    // ============ RECEPTIONS NON RAPPROCHEES ============
    public List<BonReception> findReceptionsNonRapprochees() {
        return bonReceptionRepository.findByStatutCode("VALIDE");
    }
    
    // ============ MODES PAIEMENT ============
    public ModePaiement saveModePaiement(ModePaiement mode) {
        return modePaiementRepository.save(mode);
    }
    
    // ============ TYPES MOUVEMENT STOCK ============
    public List<TypeMouvement> findAllTypesMouvement() {
        return typeMouvementRepository.findAll();
    }
    
    // ============ MOTIFS ============
    public List<MotifRetour> findAllMotifs(String type) {
        // Pour compatibilité, "retour" renvoie les motifs de retour
        if ("retour".equalsIgnoreCase(type)) {
            return motifRetourRepository.findAll();
        }
        return motifRetourRepository.findAll(); // par défaut
    }
    
    public List<MotifSortie> findMotifsByType(String type) {
        return motifSortieRepository.findByType(type.toUpperCase());
    }
    
    public List<MotifAjustement> findAllMotifsAjustement() {
        return motifAjustementRepository.findAll();
    }
}
