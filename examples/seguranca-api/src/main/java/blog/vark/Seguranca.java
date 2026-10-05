package blog.vark;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class Seguranca {
    @Bean @Order(1)
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .csrf(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/conta").hasAuthority("SCOPE_conta:ler")
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean @Order(2)
    SecurityFilterChain navegador(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
                .requestMatchers("/login", "/error").permitAll()
                .requestMatchers("/web/**").authenticated()
                .anyRequest().denyAll())
            .formLogin(Customizer.withDefaults())
            .logout(Customizer.withDefaults());
        // CSRF permanece habilitado nessa cadeia.
        return http.build();
    }

    static OAuth2TokenValidator<Jwt> validacao(String issuer, String audience) {
        return new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            new JwtClaimValidator<java.time.Instant>("exp", exp -> exp != null),
            new JwtClaimValidator<String>("sub", sub -> sub != null && !sub.isBlank()),
            new JwtClaimValidator<java.util.List<String>>("aud", aud -> aud != null && aud.contains(audience)));
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.issuer}") String issuer,
                          @Value("${app.jwk-set-uri}") String jwks,
                          @Value("${app.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        decoder.setJwtValidator(validacao(issuer, audience));
        return decoder;
    }

    @Bean
    UserDetailsService usuarios(@Value("${demo.password}") String senha) {
        if (senha.isBlank()) throw new IllegalArgumentException("Defina DEMO_PASSWORD para o exemplo local");
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new InMemoryUserDetailsManager(User.withUsername("neto")
            .password(encoder.encode(senha)).roles("USER").build());
    }
}
