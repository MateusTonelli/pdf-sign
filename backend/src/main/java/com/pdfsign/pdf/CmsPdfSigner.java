package com.pdfsign.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

public final class CmsPdfSigner {

    private CmsPdfSigner() {
    }

    public static byte[] signDetached(InputStream content, PrivateKey privateKey, Certificate[] chain) throws IOException {
        try {
            byte[] data = content.readAllBytes();
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            X509Certificate cert = (X509Certificate) chain[0];
            String signatureAlgorithm = resolveSignatureAlgorithm(privateKey, cert);
            ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(privateKey);
            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
                            .build(signer, cert));
            generator.addCertificates(new JcaCertStore(Arrays.asList(chain)));
            CMSSignedData signedData = generator.generate(new CMSProcessableByteArray(data), false);
            return signedData.getEncoded();
        } catch (Exception e) {
            throw new IOException("CMS signing failed: " + e.getMessage(), e);
        }
    }

    private static String resolveSignatureAlgorithm(PrivateKey privateKey, X509Certificate cert) {
        String keyAlg = privateKey.getAlgorithm();
        if ("EC".equalsIgnoreCase(keyAlg) || "ECDSA".equalsIgnoreCase(keyAlg)) {
            return "SHA256withECDSA";
        }
        String sigAlg = cert.getSigAlgName();
        if (sigAlg != null && sigAlg.toUpperCase().contains("RSA")) {
            return "SHA256withRSA";
        }
        return "SHA256withRSA";
    }
}
