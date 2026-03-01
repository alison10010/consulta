package com.consulta.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.consulta.service.AuthFailureHandler;
import com.consulta.service.AuthSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    @Autowired SecurityFilter securityFilter;
    
    @Lazy
    @Autowired CustomAuthenticationProvider customAuthenticationProvider;
    
    @Autowired private AuthSuccessHandler authSuccessHandler;

    @Autowired private AuthFailureHandler authFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
        	.cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
            .authorizeHttpRequests(authorize -> authorize

                // Libera acesso aos recursos estáticos e do JSF
                .requestMatchers(
                	    "/javax.faces.resource/**",
                	    "/jakarta.faces.resource/**",
                	    "/resources/**",
                	    "/static/**",
                	    "/css/**",
                	    "/js/**",
                	    "/img/**",
                	    "/bootstrap/**"
                ).permitAll()

                // Libera end-point das rotas
                .requestMatchers("/login", "/error", "/api/**").permitAll()
                
                .requestMatchers("/cadastro-colaborador", "/cadastro", "/recupera-senha").permitAll()
                
                // Libera as pastas publicas
                .requestMatchers("/view/livre/**", "/view/login/**").permitAll()                

                // Libera a rota do JWT
                .requestMatchers(HttpMethod.POST, "/jwt/token").permitAll()
                
                .requestMatchers("/colaborador/**").hasAnyAuthority("ROLE_COLABORADOR")
                
                .requestMatchers("/paciente/**").hasAnyAuthority("ROLE_NORMAL")
                
                .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN")
                

                // Tudo mais requer autenticação
                .anyRequest().authenticated()
            )

            // Configura página de acesso negado (403)
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/")
            )

            // Configura a página de login customizada
            .formLogin(form -> form
                .loginPage("/login")               // URL da página de login (GET)
                .loginProcessingUrl("/login")      // URL que recebe o POST
                .successHandler(authSuccessHandler)
                .failureHandler(authFailureHandler)
                .permitAll()
            )

            // Configura logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )

            .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(customAuthenticationProvider) 
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
    	return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ Seu front (porta 80)
        config.setAllowedOrigins(List.of("*"));

        // Se você também acessa por https, adicione também:
        // config.setAllowedOrigins(List.of("http://...", "https://..."));

        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        // Se você NÃO usa cookie/sessão no fetch, deixe false
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}


