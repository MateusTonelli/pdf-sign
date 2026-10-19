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
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
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
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream ksIn = new ByteArrayInputStream(pkcs12Bytes)) {
                keyStore.load(ksIn, password);
            }

            String alias = findKeyAlias(keyStore);
            if (alias == null) {
                throw new IOException("No private key entry found in PKCS#12");
            }

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password);
            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (privateKey == null || chain == null || chain.length == 0) {
                throw new IOException("PKCS#12 does not contain a signing certificate");
            }

            String displayName = signerName != null && !signerName.isBlank() ? signerName : alias;
            byte[] withAppearance = stampVisualAppearance(pdfBytes, fields, displayName);

            try (PDDocument document = Loader.loadPDF(withAppearance);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    SignatureOptions options = new SignatureOptions()) {

                PDSignature signature = new PDSignature();
                signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
                signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
                signature.setName(displayName);
                signature.setReason("Document signed via PDF Sign MVP");
                signature.setLocation("PDF Sign");
                signature.setSignDate(Calendar.getInstance());

                SignatureInterface signatureInterface =
                        content -> CmsPdfSigner.signDetached(content, privateKey, chain);

                options.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
                document.addSignature(signature, signatureInterface, options);
                document.saveIncremental(out);
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Certificate signing failed: " + e.getMessage(), e);
        }
    }

    private static byte[] stampVisualAppearance(byte[] pdfBytes, List<SignatureField> fields, String label)
            throws IOException {
        if (fields == null || fields.isEmpty()) {
            return pdfBytes;
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (SignatureField field : fields) {
                int pageIndex = Math.max(0, field.getPage() - 1);
                if (pageIndex >= document.getNumberOfPages()) {
                    continue;
                }
                PDPage page = document.getPage(pageIndex);
                PdfCoordinateUtils.PdfRect rect =
                        PdfCoordinateUtils.toPdfRect(
                                page, field.getX(), field.getY(), field.getWidth(), field.getHeight());
                byte[] appearance = renderAppearance(label, (int) rect.width(), (int) rect.height());
                PDImageXObject image = PDImageXObject.createFromByteArray(document, appearance, "cert-sig");
                try (PDPageContentStream cs =
                        new PDPageContentStream(
                                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.drawImage(image, rect.x(), rect.y(), rect.width(), rect.height());
                }
            }
            document.save(out);
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
        g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, Math.min(18, Math.max(10, h - 8))));
        g.drawString("Assinado: " + label, 8, h / 2 + 6);
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
