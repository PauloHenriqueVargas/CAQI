package br.com.caqi.financeiro.core.security;

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
 * RBAC declarativo do caq-financeiro-svc:
 *   LEITOR  → GETs (consulta de receitas, despesas, execução Fundeb)
 *   GESTOR  → POST receitas/despesas (lançamentos do dia-a-dia)
 *   ADMIN   → PUT/DELETE em receitas/despesas e gestão de fonte_recurso (estrutural)
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
        return new InMemoryUserDetailsManager(
                User.withUsername(props.leitor().username())
                        .password(encoder.encode(props.leitor().password()))
                        .roles("LEITOR").build(),
                User.withUsername(props.gestor().username())
                        .password(encoder.encode(props.gestor().password()))
                        .roles("LEITOR", "GESTOR").build(),
                User.withUsername(props.admin().username())
                        .password(encoder.encode(props.admin().password()))
                        .roles("LEITOR", "GESTOR", "ADMIN").build()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**",
                                "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Transparência pública (LAI / LRF art. 48-A) — sem auth
                        .requestMatchers(HttpMethod.GET, "/api/public/transparencia/**").permitAll()

                        // Estrutural: ADMIN (catálogos + contratos têm impacto auditorial)
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/financeiro/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/financeiro/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/financeiro/fontes-recurso/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/financeiro/contratos").hasRole("ADMIN")

                        // Operacional: GESTOR
                        .requestMatchers(HttpMethod.POST,   "/api/v1/financeiro/receitas/**").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/financeiro/despesas/**").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/financeiro/fornecedores/**").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/financeiro/contratos/*/medicoes/**").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/tributario/**").hasRole("GESTOR")

                        // Consulta: LEITOR
                        .requestMatchers(HttpMethod.GET,    "/api/v1/financeiro/**").hasRole("LEITOR")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/fundeb/**").hasRole("LEITOR")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/tributario/**").hasRole("LEITOR")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/siope/**").hasRole("LEITOR")

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
