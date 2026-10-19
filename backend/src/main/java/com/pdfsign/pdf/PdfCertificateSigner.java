package com.pdfsign.pdf;

import com.pdfsign.domain.SignatureField;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.springframework.stereotype.Component;

@Component
public class PdfCertificateSigner {

    public byte[] applyCertificateSignature(
            byte[] pdfBytes,
            List<SignatureField> fields,
            byte[] pkcs12Bytes,
            char[] password,
            String signerName)
            throws IOException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream ksIn = new ByteArrayInputStream(pkcs12Bytes)) {
            keyStore.load(ksIn, password);
        } catch (Exception e) {
            throw new IOException("Could not unlock PKCS#12 with the provided password", e);
        }

        String alias = findKeyAlias(keyStore);
        if (alias == null) {
            throw new IOException("No private key entry found in PKCS#12");
        }

        PrivateKey privateKey;
        Certificate[] chain;
        try {
            privateKey = (PrivateKey) keyStore.getKey(alias, password);
            chain = keyStore.getCertificateChain(alias);
        } catch (Exception e) {
            throw new IOException("Could not read private key from PKCS#12", e);
        }

        if (privateKey == null || chain == null || chain.length == 0) {
            throw new IOException("PKCS#12 does not contain a signing certificate");
        }

        SignatureField primary = fields.isEmpty() ? null : fields.get(0);
        try (PDDocument document = Loader.loadPDF(pdfBytes);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(signerName != null ? signerName : alias);
            signature.setReason("Document signed via PDF Sign MVP");
            signature.setLocation("PDF Sign");
            Calendar cal = Calendar.getInstance();
            signature.setSignDate(cal);

            SignatureInterface signatureInterface = content -> CmsPdfSigner.signDetached(content, privateKey, chain);

            SignatureOptions options = new SignatureOptions();
            options.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);

            if (primary != null) {
                int pageIndex = Math.max(0, primary.getPage() - 1);
                if (pageIndex < document.getNumberOfPages()) {
                    var page = document.getPage(pageIndex);
                    PdfCoordinateUtils.PdfRect rect =
                            PdfCoordinateUtils.toPdfRect(page, primary.getX(), primary.getY(), primary.getWidth(), primary.getHeight());
                    byte[] appearance = renderAppearance(signerName != null ? signerName : alias, (int) rect.width(), (int) rect.height());
                    try (InputStream appearanceStream = new ByteArrayInputStream(appearance)) {
                        PDVisibleSignDesigner designer = new PDVisibleSignDesigner(document, appearanceStream, primary.getPage());
                        designer.xAxis((int) rect.x()).yAxis((int) rect.y()).zoom(0).adjustForRotation();
                        PDVisibleSigProperties properties = new PDVisibleSigProperties();
                        properties.signerName(signature.getName())
                                .signerLocation(signature.getLocation())
                                .signatureReason(signature.getReason())
                                .preferredSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2)
                                .page(primary.getPage())
                                .visualSignEnabled(true)
                                .setPdVisibleSignature(designer);
                        properties.buildSignature();
                        signature.setName(properties.getSignerName());
                        signature.setLocation(properties.getSignerLocation());
                        signature.setReason(properties.getSignatureReason());
                        options.setVisualSignature(properties);
                        options.setPage(pageIndex);
                    }
                }
            }

            document.addSignature(signature, signatureInterface, options);
            document.saveIncremental(out);
            options.close();
            return out.toByteArray();
        }
    }

    private static byte[] renderAppearance(String label, int width, int height) throws IOException {
        int w = Math.max(width, 120);
        int h = Math.max(height, 40);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(255, 255, 255, 220));
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, Math.min(18, h - 8)));
        g.drawString("Signed: " + label, 8, h / 2 + 6);
        g.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return png.toByteArray();
    }

    private static String findKeyAlias(KeyStore keyStore) throws Exception {
        var aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                return alias;
            }
        }
        return null;
    }
}
