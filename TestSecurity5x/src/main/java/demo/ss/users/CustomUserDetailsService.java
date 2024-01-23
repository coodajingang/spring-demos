package demo.ss.users;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username.startsWith("root")) {
            final CustomUserDetails user = new CustomUserDetails();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(username));
            user.setAuthorities(Arrays.asList(new SimpleGrantedAuthority("ADMIN")));

            if (username.equals("root")) {
                user.setMfaStatus(2);
                user.setMfaSecret("CSE3C7WSQM566JLI");
            } else if (username.equals("root1")) {
                user.setMfaStatus(1);
                user.setMfaSecret("CSE3C7WSQM566JLI");
            } else {
                user.setMfaStatus(0);
            }
            return user;
        }

        log.error("User {} not Found !", username);
        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}
