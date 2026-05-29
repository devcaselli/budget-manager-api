package br.com.casellisoftware.budgetmanager.configs.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtKeyConfiguration} private-key permission validation.
 *
 * <p>Key-strength validation ({@code validateKeyStrength}) is covered implicitly by the
 * full integration test suite which boots the app with real keys. Tested here: the
 * permission-check path, which requires only a file on disk — no actual RSA key content.</p>
 */
@DisabledOnOs(OS.WINDOWS) // POSIX permission checks not supported on Windows
class JwtKeyConfigurationValidationTest {

    @TempDir
    Path tempDir;

    private JwtKeyConfiguration configWithPrivateKeyPath(String path) {
        JwtKeyConfiguration config = new JwtKeyConfiguration();
        ReflectionTestUtils.setField(config, "privateKeyPath", path);
        ReflectionTestUtils.setField(config, "publicKeyPath", path); // unused in permission check
        return config;
    }

    /** Invokes the private validateFilePermissions method via reflection. */
    private void invokeValidateFilePermissions(JwtKeyConfiguration config, String path) {
        try {
            var method = JwtKeyConfiguration.class.getDeclaredMethod("validateFilePermissions", String.class);
            method.setAccessible(true);
            method.invoke(config, path);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof IOException ioe) throw new RuntimeException(ioe);
            throw new RuntimeException(cause);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void privateKey_ownerReadOnly_passes() throws IOException {
        Path keyFile = tempDir.resolve("private.pem");
        Files.writeString(keyFile, "dummy");
        Files.setPosixFilePermissions(keyFile, Set.of(PosixFilePermission.OWNER_READ));

        JwtKeyConfiguration config = configWithPrivateKeyPath(keyFile.toString());
        assertThatNoException().isThrownBy(() -> invokeValidateFilePermissions(config, keyFile.toString()));
    }

    @Test
    void privateKey_ownerReadWrite_passes() throws IOException {
        Path keyFile = tempDir.resolve("private.pem");
        Files.writeString(keyFile, "dummy");
        Files.setPosixFilePermissions(keyFile, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));

        JwtKeyConfiguration config = configWithPrivateKeyPath(keyFile.toString());
        assertThatNoException().isThrownBy(() -> invokeValidateFilePermissions(config, keyFile.toString()));
    }

    @Test
    void privateKey_groupReadable_throwsIllegalState() throws IOException {
        Path keyFile = tempDir.resolve("private.pem");
        Files.writeString(keyFile, "dummy");
        Files.setPosixFilePermissions(keyFile, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.GROUP_READ));

        JwtKeyConfiguration config = configWithPrivateKeyPath(keyFile.toString());
        assertThatThrownBy(() -> invokeValidateFilePermissions(config, keyFile.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insecure permissions")
                .hasMessageContaining(keyFile.toString())
                .hasMessageContaining("chmod 600");
    }

    @Test
    void privateKey_othersReadable_throwsIllegalState() throws IOException {
        Path keyFile = tempDir.resolve("private.pem");
        Files.writeString(keyFile, "dummy");
        Files.setPosixFilePermissions(keyFile, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OTHERS_READ));

        JwtKeyConfiguration config = configWithPrivateKeyPath(keyFile.toString());
        assertThatThrownBy(() -> invokeValidateFilePermissions(config, keyFile.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insecure permissions");
    }

    @Test
    void privateKey_worldWritable_throwsIllegalState() throws IOException {
        Path keyFile = tempDir.resolve("private.pem");
        Files.writeString(keyFile, "dummy");
        Files.setPosixFilePermissions(keyFile, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OTHERS_WRITE));

        JwtKeyConfiguration config = configWithPrivateKeyPath(keyFile.toString());
        assertThatThrownBy(() -> invokeValidateFilePermissions(config, keyFile.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insecure permissions");
    }
}
