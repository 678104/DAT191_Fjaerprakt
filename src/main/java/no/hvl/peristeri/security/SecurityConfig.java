package no.hvl.peristeri.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.hvl.peristeri.feature.bruker.Rolle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import java.io.IOException;

@Configuration
@EnableWebSecurity(debug = false)
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(Customizer.withDefaults())
		    .authorizeHttpRequests(auth -> auth
				    .requestMatchers("/admin/**").hasRole(Rolle.ADMIN.name())
				    .requestMatchers("/dommer/**").hasAnyRole(Rolle.DOMMER.name(), Rolle.ADMIN.name())
				    .requestMatchers("/bruker/**").authenticated()
				    .requestMatchers("/paamelding/**").authenticated()
				    .anyRequest().permitAll()
		    ).formLogin(form -> form
				    .loginPage("/login")
				    .successHandler(roleBasedSuccessHandler())
				    .loginProcessingUrl("/login")
				    .permitAll()

		    ).logout(logout -> logout
				    .logoutUrl("/logout")
				    .logoutSuccessUrl("/login?logout")
				    .clearAuthentication(true)
				    .invalidateHttpSession(true)
				    .deleteCookies("JSESSIONID")
				    .permitAll()
		    ).exceptionHandling(exeption -> exeption
				    .accessDeniedHandler((request, response, accessDeniedException) -> {
					    // Create a flash attribute
					    FlashMap flashMap = new FlashMap();
					    flashMap.put("errorMessage", "Du har ikke tilgang til denne siden: " + request.getRequestURI());

					    // Get the FlashMapManager and store the flash attributes
					    FlashMapManager flashMapManager = new SessionFlashMapManager();
					    flashMapManager.saveOutputFlashMap(flashMap, request, response);

					    response.sendRedirect("/");
				    })
		    );
		return http.build();
	}

	@Bean
	public AuthenticationSuccessHandler roleBasedSuccessHandler() {
		return new SimpleUrlAuthenticationSuccessHandler() {
			@Override
			public void onAuthenticationSuccess(HttpServletRequest request,
			                                    HttpServletResponse response,
			                                    Authentication authentication) throws IOException, ServletException {
				var roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
				if (roles.contains("ROLE_ADMIN")) {
					setDefaultTargetUrl("/admin");
				} else if (roles.contains("ROLE_DOMMER")) {
					setDefaultTargetUrl("/dommer");
				} else {
					setDefaultTargetUrl("/bruker");
				}
				super.onAuthenticationSuccess(request, response, authentication);
			}
		};
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

}
