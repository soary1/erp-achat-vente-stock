package module.avs.controller.api;

import lombok.RequiredArgsConstructor;
import module.avs.dto.GeoLocationDTO;
import module.avs.model.organisation.Depot;
import module.avs.model.organisation.Site;
import module.avs.model.tiers.Client;
import module.avs.model.tiers.Fournisseur;
import module.avs.repository.organisation.DepotRepository;
import module.avs.repository.organisation.SiteRepository;
import module.avs.repository.tiers.ClientRepository;
import module.avs.repository.tiers.FournisseurRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoLocationApiController {

    private final SiteRepository siteRepository;
    private final DepotRepository depotRepository;
    private final ClientRepository clientRepository;
    private final FournisseurRepository fournisseurRepository;

    /**
     * Récupère toutes les localisations (sites, dépôts, clients, fournisseurs)
     */
    @GetMapping("/all")
    public ResponseEntity<List<GeoLocationDTO>> getAllLocations() {
        List<GeoLocationDTO> locations = new ArrayList<>();
        
        // Sites
        locations.addAll(getSiteLocations());
        
        // Dépôts
        locations.addAll(getDepotLocations());
        
        // Clients
        locations.addAll(getClientLocations());
        
        // Fournisseurs
        locations.addAll(getFournisseurLocations());
        
        return ResponseEntity.ok(locations);
    }

    /**
     * Récupère les localisations des sites
     */
    @GetMapping("/sites")
    public ResponseEntity<List<GeoLocationDTO>> getSites() {
        return ResponseEntity.ok(getSiteLocations());
    }

    /**
     * Récupère les localisations des dépôts
     */
    @GetMapping("/depots")
    public ResponseEntity<List<GeoLocationDTO>> getDepots() {
        return ResponseEntity.ok(getDepotLocations());
    }

    /**
     * Récupère les localisations des dépôts d'un site
     */
    @GetMapping("/sites/{siteId}/depots")
    public ResponseEntity<List<GeoLocationDTO>> getDepotsBySite(@PathVariable UUID siteId) {
        List<GeoLocationDTO> depots = depotRepository.findBySiteIdAndIsActiveTrue(siteId).stream()
            .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
            .map(this::mapDepotToGeoDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(depots);
    }

    /**
     * Récupère les localisations des clients
     */
    @GetMapping("/clients")
    public ResponseEntity<List<GeoLocationDTO>> getClients() {
        return ResponseEntity.ok(getClientLocations());
    }

    /**
     * Récupère les localisations des fournisseurs
     */
    @GetMapping("/fournisseurs")
    public ResponseEntity<List<GeoLocationDTO>> getFournisseurs() {
        return ResponseEntity.ok(getFournisseurLocations());
    }

    /**
     * Récupère un site spécifique
     */
    @GetMapping("/sites/{id}")
    public ResponseEntity<GeoLocationDTO> getSite(@PathVariable UUID id) {
        return siteRepository.findById(id)
            .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
            .map(this::mapSiteToGeoDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère un dépôt spécifique
     */
    @GetMapping("/depots/{id}")
    public ResponseEntity<GeoLocationDTO> getDepot(@PathVariable UUID id) {
        return depotRepository.findById(id)
            .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
            .map(this::mapDepotToGeoDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Méthodes privées pour récupérer les localisations
    
    private List<GeoLocationDTO> getSiteLocations() {
        return siteRepository.findByIsActiveTrue().stream()
            .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
            .map(this::mapSiteToGeoDTO)
            .collect(Collectors.toList());
    }

    private List<GeoLocationDTO> getDepotLocations() {
        return depotRepository.findByIsActiveTrue().stream()
            .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
            .map(this::mapDepotToGeoDTO)
            .collect(Collectors.toList());
    }

    private List<GeoLocationDTO> getClientLocations() {
        return clientRepository.findByIsActiveTrue().stream()
            .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
            .map(this::mapClientToGeoDTO)
            .collect(Collectors.toList());
    }

    private List<GeoLocationDTO> getFournisseurLocations() {
        return fournisseurRepository.findByIsActiveTrue().stream()
            .filter(f -> f.getLatitude() != null && f.getLongitude() != null)
            .map(this::mapFournisseurToGeoDTO)
            .collect(Collectors.toList());
    }

    // Méthodes de mapping

    private GeoLocationDTO mapSiteToGeoDTO(Site site) {
        return GeoLocationDTO.builder()
            .id(site.getId())
            .code(site.getCode())
            .name(site.getName())
            .type("SITE")
            .latitude(site.getLatitude())
            .longitude(site.getLongitude())
            .address(site.getAddress())
            .iconColor("blue")
            .popupContent(String.format("<strong>%s</strong><br/>Site: %s<br/>%s", 
                site.getName(), site.getCode(), site.getAddress() != null ? site.getAddress() : ""))
            .build();
    }

    private GeoLocationDTO mapDepotToGeoDTO(Depot depot) {
        return GeoLocationDTO.builder()
            .id(depot.getId())
            .code(depot.getCode())
            .name(depot.getName())
            .type("DEPOT")
            .latitude(depot.getLatitude())
            .longitude(depot.getLongitude())
            .address(depot.getAddress())
            .iconColor("green")
            .popupContent(String.format("<strong>%s</strong><br/>Dépôt: %s<br/>Site: %s<br/>%s", 
                depot.getName(), depot.getCode(), 
                depot.getSite() != null ? depot.getSite().getName() : "",
                depot.getAddress() != null ? depot.getAddress() : ""))
            .build();
    }

    private GeoLocationDTO mapClientToGeoDTO(Client client) {
        return GeoLocationDTO.builder()
            .id(client.getId())
            .code(client.getCode())
            .name(client.getName())
            .type("CLIENT")
            .latitude(client.getLatitude())
            .longitude(client.getLongitude())
            .address(client.getAdresse())
            .iconColor("orange")
            .popupContent(String.format("<strong>%s</strong><br/>Client: %s<br/>%s<br/>%s", 
                client.getName(), client.getCode(),
                client.getTelephone() != null ? "Tél: " + client.getTelephone() : "",
                client.getAdresse() != null ? client.getAdresse() : ""))
            .build();
    }

    private GeoLocationDTO mapFournisseurToGeoDTO(Fournisseur fournisseur) {
        return GeoLocationDTO.builder()
            .id(fournisseur.getId())
            .code(fournisseur.getCode())
            .name(fournisseur.getName())
            .type("FOURNISSEUR")
            .latitude(fournisseur.getLatitude())
            .longitude(fournisseur.getLongitude())
            .address(fournisseur.getAdresse())
            .iconColor("red")
            .popupContent(String.format("<strong>%s</strong><br/>Fournisseur: %s<br/>%s<br/>%s", 
                fournisseur.getName(), fournisseur.getCode(),
                fournisseur.getTelephone() != null ? "Tél: " + fournisseur.getTelephone() : "",
                fournisseur.getAdresse() != null ? fournisseur.getAdresse() : ""))
            .build();
    }
}
