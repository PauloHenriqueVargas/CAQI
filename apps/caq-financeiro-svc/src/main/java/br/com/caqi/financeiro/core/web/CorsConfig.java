package br.com.caqi.financeiro.core.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS aberto APENAS para /api/public/** — qualquer origem pode consumir
 * dados de transparência (LAI). Endpoints autenticados continuam restritos
 * às origens definidas em CORS_ORIGINS (não tratado aqui — Spring Security
 * filter aplica antes).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter publicCorsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Accept", "Content-Type", "Origin"));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/public/**", cfg);
        return new CorsFilter(source);
    }
}
