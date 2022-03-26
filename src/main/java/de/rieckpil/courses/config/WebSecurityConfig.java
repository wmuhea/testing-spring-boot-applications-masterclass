package de.rieckpil.courses.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity(debug = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private final JwtIssuerAuthenticationManagerResolver jwtIssuerAuthenticationManagerResolver;

  public WebSecurityConfig(JwtIssuerAuthenticationManagerResolver jwtIssuerAuthenticationManagerResolver) {
    this.jwtIssuerAuthenticationManagerResolver = jwtIssuerAuthenticationManagerResolver;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    http
      .authorizeRequests(authorize -> authorize
        .mvcMatchers(HttpMethod.GET, "/api/books").permitAll()
        .mvcMatchers(HttpMethod.GET, "/api/books/reviews").permitAll()
        .mvcMatchers("/api/**").authenticated()
      )
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      .and()
      .cors()
      .and()
      .csrf().disable()
      .oauth2ResourceServer(oauth2 ->
        oauth2
          .authenticationManagerResolver(jwtIssuerAuthenticationManagerResolver)
      );
  }
}
