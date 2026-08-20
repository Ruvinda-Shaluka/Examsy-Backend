package lk.ijse.examsybackend.config;

import lk.ijse.examsybackend.security.RateLimitFilter;
import lk.ijse.examsybackend.util.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitFilter rateLimitFilter;

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomOAuth2AuthorizationRequestResolver customAuthorizationRequestResolver;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                /*
                Tells spring security to use the CORS configuration
                we defined in the corsConfigurationSource() method below.
                Using Customizer.withDefaults() automatically wires it up safely!
                */
                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth
                        // EXPLICITLY ALLOW ALL 'OPTIONS' PREFLIGHT REQUESTS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Keep your public login/signup endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // EXPLICITLY ALLOW OAUTH2 ENDPOINTS
                        // Without these, Spring will block the Google redirect and throw a 403!
                        .requestMatchers("/oauth2/**", "/login/oauth2/code/**").permitAll()

                        // Lock down everything else
                        .anyRequest().authenticated()
                )

                // CRITICAL CHANGE: removed the strict STATELESS session policy here.
                // .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Why? OAuth2 login requires a temporary, tiny session to store a CSRF "state" parameter
                // between the time the user leaves for Google and the time they come back.
                // Your JwtAuthFilter will still protect your actual API endpoints flawlessly.

                // ADD THE OAUTH2 LOGIN CONFIGURATION
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authEndpoint -> authEndpoint
                                // Wire up the custom resolver to catch the "?role=" parameter from React
                                .authorizationRequestResolver(customAuthorizationRequestResolver)
                        )
                        // Wire up the success handler to generate the JWT and redirect back to React
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )

                // Call the method directly here to break the circular dependency!
                .authenticationProvider(authenticateProvider())

                // 1. FIRST: Run the JWT filter to extract the token and log the user in
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // 2. THEN: Run the Rate Limiter (anchored after the standard auth filter)
                // Now it knows exactly who the user is!
                .addFilterAfter(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // DEFINE THE CORS RULES FOR THE ENTIRE APP
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow your React frontend
        configuration.setAllowedOrigins(List.of("http://localhost:5173","http://localhost:5174" ));

        // Allow these HTTP methods (including OPTIONS!)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow these headers (Authorization is the critical one here!)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        // Allow cookies/credentials
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticateProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }
}