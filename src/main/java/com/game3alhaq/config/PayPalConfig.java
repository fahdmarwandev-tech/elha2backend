package com.game3alhaq.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class PayPalConfig {

    @Value("${paypal.client-id:}")
    private String clientId;

    @Value("${paypal.client-secret:}")
    private String clientSecret;

    @Value("${paypal.mode:sandbox}")
    private String mode = "sandbox";

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && !"mock".equalsIgnoreCase(clientId);
    }

    public String getBaseUrl() {
        if ("live".equalsIgnoreCase(mode)) {
            return "https://api-m.paypal.com";
        }
        return "https://api-m.sandbox.paypal.com";
    }

    public String getCheckoutUrl(String token) {
        if ("live".equalsIgnoreCase(mode)) {
            return "https://www.paypal.com/checkoutnow?token=" + token;
        }
        return "https://www.sandbox.paypal.com/checkoutnow?token=" + token;
    }
}
