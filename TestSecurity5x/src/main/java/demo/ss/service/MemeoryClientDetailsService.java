package demo.ss.service;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.integration.IntegrationProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.builders.ClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.builders.ClientDetailsServiceBuilder.ClientBuilder;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Component
public class MemeoryClientDetailsService implements ClientDetailsService {

    static ClientDetails client = null;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {

        if (!clientId.equals("client")) {
            System.out.println("client id not match");
            return null;
        }

        if (client == null) {
            client = newClient();
        }
        return client;
    }

    @SneakyThrows
    private ClientDetails newClient() {
        InMemoryClientDetailsServiceBuilder builder = new InMemoryClientDetailsServiceBuilder();
        builder.withClient("client")
                .secret(passwordEncoder.encode("secret"))
                .redirectUris("http://www.baidu.com")
                .scopes("read:user")
                .authorizedGrantTypes("authorization_code", "refresh_token", "implicit", "password", "client_credentials");
        return builder.build().loadClientByClientId("client");
    }
}
