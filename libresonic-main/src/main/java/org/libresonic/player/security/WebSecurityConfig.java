package org.libresonic.player.security;

import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private CsrfSecurityRequestMatcher csrfSecurityRequestMatcher;

    @Autowired
    LoginFailureLogger loginFailureLogger;

    @Autowired
    SettingsService settingsService;

    @Autowired
    LibresonicUserDetailsContextMapper libresonicUserDetailsContextMapper;

    @Override
    @Bean(name = "authenticationManager")
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        if (settingsService.isLdapEnabled()) {
            auth.ldapAuthentication()
                    .contextSource()
                        .managerDn(settingsService.getLdapManagerDn())
                        .managerPassword(settingsService.getLdapManagerPassword())
                        .url(settingsService.getLdapUrl())
                    .and()
                    .userSearchFilter(settingsService.getLdapSearchFilter())
                    .userDetailsContextMapper(libresonicUserDetailsContextMapper);
        }
        auth.userDetailsService(securityService);
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {

        RESTRequestParameterProcessingFilter restAuthenticationFilter = new RESTRequestParameterProcessingFilter();
        restAuthenticationFilter.setAuthenticationManager((AuthenticationManager) getApplicationContext().getBean("authenticationManager"));
        restAuthenticationFilter.setSecurityService(securityService);
        restAuthenticationFilter.setLoginFailureLogger(loginFailureLogger);
        http = http.addFilterBefore(restAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http
            .csrf()
                .requireCsrfProtectionMatcher(csrfSecurityRequestMatcher)
            .and().headers()
                .frameOptions()
                .sameOrigin()
            .and().authorizeRequests()
                .antMatchers("recover.view", "accessDenied.view",
                        "coverArt.view", "/hls/**", "/stream/**", "/ws/**",
                        "/share/**", "/style/**", "/icons/**",
                        "/flash/**", "/script/**", "/sonos/**", "/crossdomain.xml", "/login")
                    .permitAll()
                .antMatchers("/personalSettings.view", "/passwordSettings.view",
                        "/playerSettings.view", "/shareSettings.view","/passwordSettings.view")
                    .hasRole("SETTINGS")
                .antMatchers("/generalSettings.view","/advancedSettings.view","/userSettings.view",
                        "/musicFolderSettings.view","/networkSettings.view")
                    .hasRole("ADMIN")
                .antMatchers("/deletePlaylist.view","/savePlaylist.view")
                    .hasRole("PLAYLIST")
                .antMatchers("/download.view")
                    .hasRole("DOWNLOAD")
                .antMatchers("/upload.view")
                    .hasRole("UPLOAD")
                .antMatchers("/createShare.view")
                    .hasRole("SHARE")
                .antMatchers("/changeCoverArt.view","/editTags.view")
                    .hasRole("COVERART")
                .antMatchers("/setMusicFileInfo.view")
                    .hasRole("COMMENT")
                .antMatchers("/podcastReceiverAdmin.view")
                    .hasRole("PODCAST")
                .antMatchers("/**")
                    .hasRole("USER")
                .anyRequest().authenticated()
            .and().formLogin()
                .loginPage("/login")
                    .permitAll()
                    .defaultSuccessUrl("/index.view", true)
                    .failureUrl("/login?error=1")
                    .usernameParameter("j_username")
                    .passwordParameter("j_password")
            // see http://docs.spring.io/spring-security/site/docs/3.2.4.RELEASE/reference/htmlsingle/#csrf-logout
            .and().logout().logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET")).logoutSuccessUrl("/login?logout")
            .and().rememberMe().key("libresonic");
    }
}