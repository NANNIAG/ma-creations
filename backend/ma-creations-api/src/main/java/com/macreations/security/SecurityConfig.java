package com.macreations.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.macreations.repository.CustomerRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CustomerJwtAuthenticationFilter customerJwtAuthenticationFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customer/auth/request-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customer/auth/verify-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/customer/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/media/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cart/merge").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/wishlist/merge").hasRole("CUSTOMER")
                        .requestMatchers("/api/cart", "/api/cart/**").permitAll()
                        .requestMatchers("/api/wishlist", "/api/wishlist/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/checkout/preview").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/initiate").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/fail").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook/razorpay").permitAll()
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(customerJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CustomerJwtAuthenticationFilter customerJwtAuthenticationFilter(
            CustomerJwtService customerJwtService,
            CustomerRepository customerRepository) {
        return new CustomerJwtAuthenticationFilter(customerJwtService, customerRepository);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            AdminUserDetailsService adminUserDetailsService) {
        return new JwtAuthenticationFilter(jwtService, adminUserDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            AdminUserDetailsService adminUserDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(adminUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
