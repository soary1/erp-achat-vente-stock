package module.avs.config;

import lombok.RequiredArgsConstructor;
import module.avs.service.UtilisateurService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final UtilisateurService utilisateurService;
    
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> utilisateurService.findByUsername(username)
            .map(utilisateur -> {
                // Récupère les rôles de l'utilisateur
                String[] roles = utilisateur.getRoles().stream()
                    .map(r -> r.getCode())
                    .toArray(String[]::new);
                if (roles.length == 0) {
                    roles = new String[]{"USER"};
                }
                return org.springframework.security.core.userdetails.User.builder()
                    .username(utilisateur.getUsername())
                    .password(utilisateur.getPasswordHash())
                    .roles(roles)
                    .disabled(utilisateur.getIsActive() == null || !utilisateur.getIsActive())
                    .build();
            })
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + username));
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Resources statiques
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // Pages d'authentification
                .requestMatchers("/login", "/logout").permitAll()
                // API endpoints (pour Leaflet et autres)
                .requestMatchers("/api/**").authenticated()
                // Cartographie
                .requestMatchers("/carte/**").authenticated()
                // Admin uniquement
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Achats
                .requestMatchers("/achats/demandes/**").hasAnyRole("ACHETEUR", "MANAGER_ACHATS", "ADMIN")
                .requestMatchers("/achats/commandes/**").hasAnyRole("ACHETEUR", "MANAGER_ACHATS", "ADMIN")
                .requestMatchers("/achats/a-approuver/**").hasAnyRole("MANAGER_ACHATS", "ADMIN")
                // Ventes
                .requestMatchers("/ventes/**").hasAnyRole("COMMERCIAL", "MANAGER_VENTES", "ADMIN")
                // Stock
                .requestMatchers("/stock/**").hasAnyRole("MAGASINIER", "MANAGER_STOCK", "ADMIN")
                // Inventaires
                .requestMatchers("/inventaires/**").hasAnyRole("MAGASINIER", "MANAGER_STOCK", "CONTROLEUR", "ADMIN")
                // Finance
                .requestMatchers("/finance/**").hasAnyRole("COMPTABLE", "DAF", "ADMIN")
                // Référentiels
                .requestMatchers("/referentiels/**").hasAnyRole("ADMIN", "MANAGER_ACHATS", "MANAGER_VENTES", "DAF")
                // Dashboard
                .requestMatchers("/", "/dashboard/**").authenticated()
                // Tout le reste
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("erp-avs-secret-key")
                .tokenValiditySeconds(86400)
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );
        
        return http.build();
    }
}
