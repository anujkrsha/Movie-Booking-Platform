package com.booking.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds {@code jwt.*} properties from each service's {@code application.yml}.
 *
 * <h3>RS256 mode (recommended — user-service and any token-verifying service)</h3>
 * <pre>
 * jwt:
 *   public-key-base64:  &lt;base64-encoded DER X.509 RSA public key&gt;
 *   private-key-base64: &lt;base64-encoded DER PKCS#8 RSA private key&gt;  # only on user-service
 *   expiration: 900000          # 15 min access token
 *   refresh-expiration: 604800000  # 7 days
 * </pre>
 *
 * <p>Generate a 2048-bit keypair:
 * <pre>
 *   openssl genrsa -out rsa_private.pem 2048
 *   openssl pkcs8 -topk8 -inform PEM -outform DER -in rsa_private.pem -nocrypt | base64
 *   openssl rsa -in rsa_private.pem -pubout -outform DER | base64
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Base64-encoded DER PKCS#8 RSA private key.
     * Required only on user-service (token issuer). Leave blank on all other services.
     */
    private String privateKeyBase64;

    /**
     * Base64-encoded DER X.509 RSA public key.
     * Required on every service that validates JWTs (i.e. all services).
     */
    private String publicKeyBase64;

    /** Access-token TTL in milliseconds. Default: 15 minutes. */
    private long expiration = 900_000L;

    /** Refresh-token TTL in milliseconds. Default: 7 days. */
    private long refreshExpiration = 604_800_000L;
}
