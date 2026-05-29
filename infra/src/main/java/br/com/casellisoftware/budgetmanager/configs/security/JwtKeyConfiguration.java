package br.com.casellisoftware.budgetmanager.configs.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;

@Configuration
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class JwtKeyConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfiguration.class);

    /** Minimum RSA key size accepted. NIST SP 800-57 recommends 3072+ from 2030 onward. */
    private static final int MIN_RSA_KEY_BITS = 2048;

    @Value("${app.jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${app.jwt.public-key-path}")
    private String publicKeyPath;

    /**
     * Validates key file security before the application accepts traffic:
     * <ol>
     *   <li>Private key file must not be group- or world-readable/writable (max 0600).</li>
     *   <li>RSA modulus must be at least {@value MIN_RSA_KEY_BITS} bits.</li>
     * </ol>
     *
     * <p>Runs after all beans are constructed so {@link #jwtPublicKey()} is available
     * for the strength check. Skipped on non-POSIX file systems (e.g. Windows dev).</p>
     */
    @PostConstruct
    void validateKeyFiles() throws IOException {
        validateFilePermissions(privateKeyPath);
        validateKeyStrength();
    }

    private void validateFilePermissions(String path) {
        try {
            Path file = Path.of(path);
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
            boolean groupReadable  = perms.contains(PosixFilePermission.GROUP_READ);
            boolean groupWritable  = perms.contains(PosixFilePermission.GROUP_WRITE);
            boolean othersReadable = perms.contains(PosixFilePermission.OTHERS_READ);
            boolean othersWritable = perms.contains(PosixFilePermission.OTHERS_WRITE);

            if (groupReadable || groupWritable || othersReadable || othersWritable) {
                throw new IllegalStateException(
                        "RSA private key file has insecure permissions: " + path +
                        ". Expected at most 0600 (owner read/write only). " +
                        "Fix with: chmod 600 " + path);
            }
            log.debug("RSA private key file permissions OK: {}", path);
        } catch (UnsupportedOperationException e) {
            // Non-POSIX filesystem (Windows) — skip permission check
            log.warn("Skipping RSA key file permission check: POSIX not supported on this OS");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read RSA private key file permissions: " + path, e);
        }
    }

    private void validateKeyStrength() throws IOException {
        RSAPublicKey publicKey;
        try (var inputStream = new FileSystemResource(publicKeyPath).getInputStream()) {
            publicKey = RsaKeyConverters.x509().convert(inputStream);
        }
        int bits = publicKey.getModulus().bitLength();
        if (bits < MIN_RSA_KEY_BITS) {
            throw new IllegalStateException(
                    "RSA key is too weak: " + bits + " bits. " +
                    "Minimum required: " + MIN_RSA_KEY_BITS + " bits. " +
                    "Regenerate with: openssl genrsa -out ~/bm-private.pem 3072");
        }
        log.info("RSA key strength OK: {} bits", bits);
    }

    @Bean
    public RSAPrivateKey jwtPrivateKey() throws IOException {
        try (var inputStream = new FileSystemResource(privateKeyPath).getInputStream()) {
            return RsaKeyConverters.pkcs8().convert(inputStream);
        }
    }

    @Bean
    public RSAPublicKey jwtPublicKey() throws IOException {
        try (var inputStream = new FileSystemResource(publicKeyPath).getInputStream()) {
            return RsaKeyConverters.x509().convert(inputStream);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAPrivateKey jwtPrivateKey, RSAPublicKey jwtPublicKey) {
        RSAKey rsaKey = new RSAKey.Builder(jwtPublicKey).privateKey(jwtPrivateKey).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }
}
