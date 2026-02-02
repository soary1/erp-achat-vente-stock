package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.organisation.Societe;
import module.avs.model.security.*;
import module.avs.repository.organisation.SocieteRepository;
import module.avs.repository.security.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UtilisateurService {
    
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final DepartementRepository departementRepository;
    private final PerimetreAccesRepository perimetreAccesRepository;
    private final DelegationAccesRepository delegationAccesRepository;
    private final RegleApprobationRepository regleApprobationRepository;
    private final JournalAuditRepository journalAuditRepository;
    private final SocieteRepository societeRepository;
    private final PasswordEncoder passwordEncoder;
    
    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll();
    }
    
    public List<Utilisateur> findAllActive() {
        return utilisateurRepository.findByIsActiveTrue();
    }
    
    public Optional<Utilisateur> findById(UUID id) {
        return utilisateurRepository.findById(id);
    }
    
    public Optional<Utilisateur> findByUsername(String username) {
        return utilisateurRepository.findByUsername(username);
    }
    
    public Utilisateur save(Utilisateur utilisateur) {
        return utilisateurRepository.save(utilisateur);
    }
    
    public void delete(UUID id) {
        utilisateurRepository.findById(id).ifPresent(u -> {
            u.setIsActive(false);
            utilisateurRepository.save(u);
        });
    }
    
    // Vérification des rôles
    public boolean hasRole(Utilisateur user, String roleCode) {
        return user.getRoles().stream()
            .anyMatch(r -> r.getCode().equals(roleCode));
    }
    
    // Vérification avec délégation
    public boolean hasRoleWithDelegation(UUID userId, String roleCode) {
        Utilisateur user = utilisateurRepository.findById(userId).orElse(null);
        if (user == null) return false;
        
        // Vérifier les rôles propres
        if (hasRole(user, roleCode)) return true;
        
        // Vérifier les délégations actives
        List<DelegationAcces> delegations = delegationAccesRepository
            .findActiveDelegationsForUser(userId, OffsetDateTime.now());
        
        return delegations.stream()
            .anyMatch(d -> d.getRole().getCode().equals(roleCode));
    }
    
    // Vérification du périmètre d'accès (ABAC)
    public boolean canAccessSite(UUID userId, UUID siteId) {
        List<PerimetreAcces> perimetres = perimetreAccesRepository
            .findByUtilisateurIdAndActiveTrue(userId);
        
        return perimetres.stream()
            .anyMatch(p -> p.getSite() == null || p.getSite().getId().equals(siteId));
    }
    
    public boolean canAccessDepot(UUID userId, UUID depotId) {
        List<PerimetreAcces> perimetres = perimetreAccesRepository
            .findByUtilisateurIdAndActiveTrue(userId);
        
        return perimetres.stream()
            .anyMatch(p -> p.getDepot() == null || p.getDepot().getId().equals(depotId));
    }
    
    // Vérification du montant maximum d'approbation
    public boolean canApproveAmount(UUID userId, BigDecimal amount) {
        List<PerimetreAcces> perimetres = perimetreAccesRepository
            .findByUtilisateurIdAndActiveTrue(userId);
        
        return perimetres.stream()
            .anyMatch(p -> p.getMaxAmountApproval() == null || 
                          p.getMaxAmountApproval().compareTo(amount) >= 0);
    }
    
    // Récupérer les règles d'approbation applicables
    public List<RegleApprobation> getApplicableApprovalRules(String docType, UUID societeId, BigDecimal montant) {
        return regleApprobationRepository.findApplicableRules(docType, societeId, montant);
    }
    
    // Vérification de séparation des tâches
    public boolean canApproveDocument(UUID approverId, UUID creatorId) {
        if (approverId.equals(creatorId)) {
            // Allow if the approver has ADMIN role
            Utilisateur user = findById(approverId).orElse(null);
            if (user != null && user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()))) {
                return true;
            }
            return false;
        }
        return true;
    }
    
    // Gestion des rôles
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }
    
    public Optional<Role> findRoleByCode(String code) {
        return roleRepository.findByCode(code);
    }
    
    // Gestion des départements
    public List<Departement> findAllDepartements() {
        return departementRepository.findAll();
    }
    
    public Optional<Departement> findDepartementByCode(String code) {
        return departementRepository.findByCode(code);
    }
    
    public Departement saveDepartement(Departement departement) {
        return departementRepository.save(departement);
    }
    
    // Pagination pour les utilisateurs
    public Page<Utilisateur> findAllUtilisateurs(Pageable pageable) {
        return utilisateurRepository.findAll(pageable);
    }
    
    // Save role
    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }
    
    // Journal d'audit
    public Page<JournalAudit> getAuditLogs(Pageable pageable) {
        return journalAuditRepository.findAll(pageable);
    }
    
    // Règles d'approbation
    public List<RegleApprobation> findAllReglesApprobation() {
        return regleApprobationRepository.findAll();
    }
    
    public RegleApprobation saveRegleApprobation(RegleApprobation regle) {
        return regleApprobationRepository.save(regle);
    }
    
    // Récupérer l'utilisateur courant
    public Utilisateur getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            return findByUsername(username).orElse(null);
        }
        return null;
    }
    
    // Récupérer la société de l'utilisateur courant
    public Societe getCurrentUserSociete() {
        Utilisateur currentUser = getCurrentUser();
        if (currentUser != null) {
            // Récupérer la société depuis le périmètre d'accès
            List<PerimetreAcces> perimetres = perimetreAccesRepository
                .findByUtilisateurIdAndActiveTrue(currentUser.getId());
            
            if (!perimetres.isEmpty() && perimetres.get(0).getSociete() != null) {
                return perimetres.get(0).getSociete();
            }
            
            // Sinon, récupérer la première société disponible (fallback)
            return societeRepository.findAll().stream().findFirst().orElse(null);
        }
        
        // Fallback : retourner la première société si pas d'utilisateur connecté
        return societeRepository.findAll().stream().findFirst().orElse(null);
    }
}
