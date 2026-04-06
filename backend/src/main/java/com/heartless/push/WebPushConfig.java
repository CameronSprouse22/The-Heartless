package com.heartless.push;

import nl.martijndwars.webpush.PushService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

@Configuration
public class WebPushConfig {

    private static final Logger log = LogManager.getLogger(WebPushConfig.class);

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Value("${push.vapid.public-key:}")
    private String configuredPublicKey;

    @Value("${push.vapid.private-key:}")
    private String configuredPrivateKey;

    @Value("${push.vapid.subject:mailto:admin@heartless.game}")
    private String subject;

    private String resolvedPublicKey;
    private String resolvedPrivateKey;

    @PostConstruct
    public void resolveKeys() throws Exception {
        if (configuredPublicKey != null && !configuredPublicKey.isBlank()
                && configuredPrivateKey != null && !configuredPrivateKey.isBlank()) {
            resolvedPublicKey = configuredPublicKey;
            resolvedPrivateKey = configuredPrivateKey;
        } else {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC", "BC");
            gen.initialize(new ECGenParameterSpec("prime256v1"));
            KeyPair kp = gen.generateKeyPair();
            resolvedPublicKey = encodePublicKey((ECPublicKey) kp.getPublic());
            resolvedPrivateKey = encodePrivateKey((ECPrivateKey) kp.getPrivate());

            log.warn("VAPID keys not configured — auto-generated for this session.");
            log.warn("Add these to application.properties to persist across restarts:");
            log.warn("push.vapid.public-key={}", resolvedPublicKey);
            log.warn("push.vapid.private-key={}", resolvedPrivateKey);
        }
    }

    @Bean
    public PushService pushService() throws GeneralSecurityException {
        PushService service = new PushService();
        service.setPublicKey(resolvedPublicKey);
        service.setPrivateKey(resolvedPrivateKey);
        service.setSubject(subject);
        return service;
    }

    @Bean("vapidPublicKey")
    public String vapidPublicKey() {
        return resolvedPublicKey;
    }

    // ---- encoding helpers ----

    private static String encodePublicKey(ECPublicKey key) {
        org.bouncycastle.jce.interfaces.ECPublicKey bcKey =
                (org.bouncycastle.jce.interfaces.ECPublicKey) key;
        byte[] encoded = bcKey.getQ().getEncoded(false); // uncompressed: 04 || x || y
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encoded);
    }

    private static String encodePrivateKey(ECPrivateKey key) {
        org.bouncycastle.jce.interfaces.ECPrivateKey bcKey =
                (org.bouncycastle.jce.interfaces.ECPrivateKey) key;
        byte[] d = bigIntToBytes(bcKey.getD(), 32);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
    }

    private static byte[] bigIntToBytes(BigInteger value, int length) {
        byte[] raw = value.toByteArray();
        if (raw.length == length) return raw;
        byte[] out = new byte[length];
        if (raw.length > length) {
            System.arraycopy(raw, raw.length - length, out, 0, length);
        } else {
            System.arraycopy(raw, 0, out, length - raw.length, raw.length);
        }
        return out;
    }
}
