package com.msal.config;

import com.msal.filters.CustomAuthenticationSuccessHandler;
import com.msal.filters.MsalAuthenticationFilter;
import com.msal.service.MsalService;
import com.msal.service.MsalUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@ComponentScan(basePackages = {"com.msal.filters", "com.msal.service"})
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Configuration
    @Order(1)
    public static class AzureAdSecurityConfig extends WebSecurityConfigurerAdapter {

        @Autowired
        private CustomAuthenticationSuccessHandler successHandler;

        @Autowired
        private PasswordEncoder passwordEncoder;
        @Autowired
        private MsalService msalService;

        @Bean
        public MsalAuthenticationFilter msalAuthenticationFilter(MsalService msalService) throws Exception {
            MsalAuthenticationFilter filter = new MsalAuthenticationFilter(authenticationManagerBean(), msalService);
            filter.setAuthenticationSuccessHandler(successHandler);
            return filter;
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            // Configure Azure AD authentication provider
            auth.authenticationProvider(new DaoAuthenticationProvider());
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .requestMatchers()
                    .antMatchers("/auth/microsoft/**", "/login/oauth2/code/**")
                    .and()
                    .authorizeRequests()
                    .antMatchers("/auth/microsoft/**", "/login/oauth2/code/**").permitAll()
                    .and()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .and()
                    .addFilterBefore(msalAuthenticationFilter(msalService), UsernamePasswordAuthenticationFilter.class)
                    .csrf().disable();
        }

        @Override
        @Bean
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }
    }

    @Configuration
    @Order(2)
    public static class FormLoginSecurityConfig extends WebSecurityConfigurerAdapter {

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Bean
        public InMemoryUserDetailsManager userDetailsService() {
            UserDetails admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .roles("ADMIN")
                    .build();

            UserDetails user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .roles("USER")
                    .build();

            return new InMemoryUserDetailsManager(admin, user);
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
            DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
            provider.setPasswordEncoder(passwordEncoder);
            provider.setUserDetailsService(userDetailsService());
            return provider;
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.authenticationProvider(authenticationProvider());
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .authorizeRequests()
                    .antMatchers("/resources/**", "/auth/login", "/auth/microsoft/**", "/login/oauth2/code/**", "/").permitAll()
                    .anyRequest().authenticated()
                    .and()
                    .formLogin()
                    .loginPage("/auth/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/index")
                    .failureUrl("/auth/login?error=true")
                    .permitAll()
                    .and()
                    .logout()
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/auth/login?logout=true")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
                    .and()
                    .csrf().disable();
        }
    }
}