package br.com.caqi.engine.core.security;

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
 * MVP — HTTP Basic Auth com user store em memória.
 *
 * Roles:
 *   LEITOR  → GETs (consulta)
 *   GESTOR  → LEITOR + POSTs operacionais (cálculos, novas vigências de custo)
 *   ADMIN   → GESTOR + estrutural (criar/atualizar insumos, parametros)
 *
 * Endpoints públicos (permitAll): /api/v1/health, /actuator/health/**, swagger.
 *
 * Em produção: substituir por Gov.br OAuth2 (NextAuth no web BFF emite JWT
 * trust validado aqui) — ver Fase 9 do roadmap.
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
        log.info("Auth bootstrap — usuários: leitor={} gestor={} admin={}",
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
                        // Transparência pública (LAI / LRF art. 48-A) — sem auth
                        .requestMatchers(HttpMethod.GET, "/api/public/transparencia/**").permitAll()

                        // ── RBAC declarativo (single source of truth) ──
                        // Estrutural: criar/atualizar insumo, parametros — ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/caqi/insumos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/caqi/insumos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/caqi/parametros/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/caqi/parametros/**").hasRole("ADMIN")

                        // Operacional: cálculos, simulações e novas vigências de custo — GESTOR
                        .requestMatchers(HttpMethod.POST, "/api/v1/caqi/calculos").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/caqi/simulacoes").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/caqi/simulacoes/presets/*").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/caqi/insumos/*/custos").hasRole("GESTOR")

                        // MFA: cada usuário gerencia o próprio (basta estar autenticado)
                        .requestMatchers("/api/v1/auth/mfa/**").authenticated()

                        // Consulta: GETs no domínio CAQ — LEITOR
                        .requestMatchers(HttpMethod.GET, "/api/v1/caqi/**").hasRole("LEITOR")

                        // Demais endpoints autenticados pedem qualquer role válida
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
