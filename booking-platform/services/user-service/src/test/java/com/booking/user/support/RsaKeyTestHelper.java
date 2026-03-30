package com.booking.user.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Generates a fresh in-memory RSA-2048 keypair for every test class that needs one.
 *
 * <p>Using a programmatically generated keypair instead of hardcoded Base64 strings
 * means tests are not broken by key rotation and do not carry private key material in VCS.
 * The generated pair is identical in structure to the production keys.
 */
public final class RsaKeyTestHelper {

    private RsaKeyTestHelper() {}

    /** Generates a 2048-bit RSA keypair. */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA keypair for tests", e);
        }
    }

    /** Returns the PKCS#8 DER-encoded private key as a Base64 string (no newlines). */
    public static String toPrivateKeyBase64(KeyPair kp) {
        return Base64.getEncoder().encodeToString(
                ((RSAPrivateKey) kp.getPrivate()).getEncoded());
    }

    /** Returns the X.509 DER-encoded public key as a Base64 string (no newlines). */
    public static String toPublicKeyBase64(KeyPair kp) {
        return Base64.getEncoder().encodeToString(
                ((RSAPublicKey) kp.getPublic()).getEncoded());
    }
}
