package demo.ss.config;

import demo.ss.filter.MFAFilter;
import demo.ss.handler.AiopsLogoutSuccessHandler;
import demo.ss.service.MemeoryClientDetailsService;
import demo.ss.users.CustomUserDetailsService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;

@Order(90)
@Configuration
public class WebSecurityConfigurer extends WebSecurityConfigurerAdapter {

    @Autowired
    private AiopsLogoutSuccessHandler aiopsLogoutSuccessHandler;
    @Autowired
    private MemeoryClientDetailsService memoryClientDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MFAFilter newMfaFilter() {
        return new MFAFilter(memoryClientDetailsService , userDetailsService(), passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //http.addFilterBefore(new TenantContextHolderFilter(), ChannelProcessingFilter.class);
        //http.authorizeRequests().antMatchers("/token/**", "/actuator/**").permitAll()
                http
                        .addFilterBefore(newMfaFilter(), ChannelProcessingFilter.class)
                        .authorizeRequests().anyRequest().authenticated()
                .and().logout().logoutSuccessHandler(aiopsLogoutSuccessHandler)
                .and().formLogin()
                .and().csrf().disable();
    }


    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailsService(passwordEncoder());
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService());
    }

//    @Override
//    public void configure(WebSecurity web) {
//        web.ignoring().antMatchers("/favicon.ico", "/css/**", "/error", "/hello");
//    }


    @SneakyThrows
    @Bean
    @Override // 将内部的 authenticationManagerBean 暴漏出来 为password模式使用
    public AuthenticationManager authenticationManagerBean() {
        return super.authenticationManagerBean();
    }

}
