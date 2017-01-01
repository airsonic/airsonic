package org.libresonic.player.security;

import org.libresonic.player.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private CsrfSecurityRequestMatcher csrfSecurityRequestMatcher;
    @Autowired
    LoginFailureLogger loginFailureLogger;

    @Override
    @Bean(name = "authenticationManager")
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }


    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
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
                        "/flash/**", "/script/**", "/sonos/**", "/crossdomain.xml")
                    .permitAll()
                .antMatchers("/personalSettings.view", "/passwordSettings.view",
                        "/playerSettings.view", "/shareSettings.view")
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
                    .defaultSuccessUrl("/index.view")
                    .failureUrl("/login?error=1")
                    .usernameParameter("j_username")
                    .passwordParameter("j_password")
            .and().rememberMe().userDetailsService(securityService).key("libresonic");

    }
}