package com.pdfsign.service;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CertificateParsingService {

    public ParsedCertificate parseAndValidate(MultipartFile file, char[] password) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Certificate file is empty");
        }
        try (InputStream in = file.getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(in, password);
            String alias = null;
            var aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String a = aliases.nextElement();
                if (keyStore.isKeyEntry(a)) {
                    alias = a;
                    break;
                }
            }
            if (alias == null) {
                throw new IllegalArgumentException("PKCS#12 does not contain a private key");
            }
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            if (cert == null) {
                throw new IllegalArgumentException("No certificate found in PKCS#12");
            }
            cert.checkValidity();
            String cn = cert.getSubjectX500Principal().getName();
            Instant validFrom = cert.getNotBefore().toInstant();
            Instant validTo = cert.getNotAfter().toInstant();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            String thumbprint = HexFormat.of().formatHex(digest.digest(cert.getEncoded()));
            byte[] pkcs12Bytes = file.getBytes();
            return new ParsedCertificate(pkcs12Bytes, cn, validFrom, validTo, thumbprint);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid PKCS#12 or password: " + e.getMessage(), e);
        }
    }

    public record ParsedCertificate(
            byte[] pkcs12Bytes, String cn, Instant validFrom, Instant validTo, String thumbprint) {
    }
}
