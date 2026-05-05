package br.com.caqi.escolar.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Mesmo modelo do engine-svc: HTTP Basic + 3 roles (LEITOR/GESTOR/ADMIN).
 *
 * Censo Escolar:
 *  POST  /api/v1/escolar/censo/import       — ADMIN (operação estrutural)
 *  POST  /api/v1/escolar/censo/dry-run      — GESTOR (validação prévia)
 *  GET   /api/v1/escolar/censo/importacoes  — LEITOR
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@Slf4j
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(SecurityProperties props, PasswordEncoder encoder) {
        var leitor = User.withUsername(props.leitor().username())
                .password(encoder.encode(props.leitor().password()))
                .roles("LEITOR")
                .build();
        var gestor = User.withUsername(props.gestor().username())
                .password(encoder.encode(props.gestor().password()))
                .roles("LEITOR", "GESTOR")
                .build();
        var admin = User.withUsername(props.admin().username())
                .password(encoder.encode(props.admin().password()))
                .roles("LEITOR", "GESTOR", "ADMIN")
                .build();
        log.info("Auth bootstrap (escolar) — leitor={} gestor={} admin={}",
                props.leitor().username(), props.gestor().username(), props.admin().username());
        return new InMemoryUserDetailsManager(leitor, gestor, admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Públicos ──
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**",
                                         "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                         "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── RBAC: Censo Escolar ──
                        .requestMatchers(HttpMethod.POST, "/api/v1/escolar/censo/import").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/escolar/censo/import-matriculas").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/escolar/censo/dry-run").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/escolar/censo/dry-run-matriculas").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/escolar/censo/**").hasRole("LEITOR")

                        // Demais endpoints autenticados
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
