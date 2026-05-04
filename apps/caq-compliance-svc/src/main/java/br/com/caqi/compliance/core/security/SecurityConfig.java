package br.com.caqi.compliance.core.security;

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
 * caq-compliance-svc — RBAC:
 *   LEITOR  → GETs (notificações, log auditoria, verificar integridade)
 *   GESTOR  → POST avaliar; PATCH status de notificação (resolver/ignorar)
 *   ADMIN   → DELETE em notificações (raro)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
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

                        .requestMatchers(HttpMethod.DELETE, "/api/v1/compliance/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/compliance/notificacoes/**").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/compliance/avaliar").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/compliance/**").hasRole("LEITOR")

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
