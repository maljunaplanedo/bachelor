package ru.dbhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;

import static org.springframework.http.HttpStatus.*;

class RestUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private record UsernameAndPassword(
        @NotNull String username,
        @NotNull String password
    ) {
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;

    private static AuthenticationCredentialsNotFoundException newBadJSONFormatException() {
        return new AuthenticationCredentialsNotFoundException("Bad JSON format");
    }

    @Override
    public Authentication attemptAuthentication(
        HttpServletRequest request, HttpServletResponse response
    ) throws AuthenticationException {
        UsernameAndPassword usernameAndPassword;
        try {
            usernameAndPassword = objectMapper.readValue(request.getReader(), UsernameAndPassword.class);
        } catch (IOException exception) {
            throw newBadJSONFormatException();
        }

        if (!validator.validate(usernameAndPassword).isEmpty()) {
            throw newBadJSONFormatException();
        }

        var token = UsernamePasswordAuthenticationToken.unauthenticated(
            usernameAndPassword.username(), usernameAndPassword.password()
        );
        return getAuthenticationManager().authenticate(token);
    }
}

class RestUsernamePasswordAuthenticationFilterConfigurer<H extends HttpSecurityBuilder<H>> extends
    AbstractAuthenticationFilterConfigurer<
        H, RestUsernamePasswordAuthenticationFilterConfigurer<H>, RestUsernamePasswordAuthenticationFilter
    > {
    public RestUsernamePasswordAuthenticationFilterConfigurer() {
        super(new RestUsernamePasswordAuthenticationFilter(), null);
    }

    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return new AntPathRequestMatcher(loginProcessingUrl, "POST");
    }
}

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final String ADMIN_ROLE = "ADMIN";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
        @Value("${ru.dbhub.admin-username}") String adminUsername,
        @Value("${ru.dbhub.admin-password}") String adminPassword,
        PasswordEncoder passwordEncoder
    ) {
        UserDetails userDetails = User.builder()
            .username(adminUsername)
            .passwordEncoder(passwordEncoder::encode)
            .password(adminPassword)
            .authorities("ROLE_" + ADMIN_ROLE)
            .build();

        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .requestCache(RequestCacheConfigurer::disable)
            .csrf(CsrfConfigurer::disable)
            .authorizeHttpRequests(
                httpRequests -> httpRequests
                    .requestMatchers("/api/admin/**").hasRole(ADMIN_ROLE)
                    .anyRequest().permitAll()
            )
            .exceptionHandling(
                exceptionHandling -> exceptionHandling
                    .accessDeniedHandler((req, res, ex) -> res.setStatus(FORBIDDEN.value()))
                    .authenticationEntryPoint((req, res, ex) -> res.setStatus(UNAUTHORIZED.value()))
            )
            .with(new RestUsernamePasswordAuthenticationFilterConfigurer<>(),
                login -> login
                    .loginProcessingUrl("/api/login")
                    .successHandler((req, res, auth) -> res.setStatus(OK.value()))
                    .failureHandler(
                        (req, res, ex) -> {
                            HttpStatus status = FORBIDDEN;
                            if (ex instanceof AuthenticationCredentialsNotFoundException) {
                                status = BAD_REQUEST;
                            }
                            res.setStatus(status.value());
                        }
                    )
            )
            .logout(
                logout -> logout
                    .logoutUrl("/api/logout")
                    .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
            )
            .build();
    }
}
