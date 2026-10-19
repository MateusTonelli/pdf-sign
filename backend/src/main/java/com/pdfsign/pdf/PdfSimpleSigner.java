package com.pdfsign.pdf;

import com.pdfsign.domain.SignatureField;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

@Component
public class PdfSimpleSigner {

    public byte[] applySimpleSignature(
            byte[] pdfBytes,
            List<SignatureField> fields,
            String typedName,
            String signatureImageBase64)
            throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (SignatureField field : fields) {
                int pageIndex = Math.max(0, field.getPage() - 1);
                if (pageIndex >= document.getNumberOfPages()) {
                    continue;
                }
                PDPage page = document.getPage(pageIndex);
                PdfCoordinateUtils.PdfRect rect =
                        PdfCoordinateUtils.toPdfRect(page, field.getX(), field.getY(), field.getWidth(), field.getHeight());

                if (signatureImageBase64 != null && !signatureImageBase64.isBlank()) {
                    byte[] imageBytes = decodeBase64Image(signatureImageBase64);
                    PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, "sig");
                    try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        cs.drawImage(image, rect.x(), rect.y(), rect.width(), rect.height());
                    }
                } else if (typedName != null && !typedName.isBlank()) {
                    PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
                    float fontSize = Math.min(rect.height() * 0.6f, 24f);
                    try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        cs.setNonStrokingColor(Color.DARK_GRAY);
                        cs.beginText();
                        cs.setFont(font, fontSize);
                        cs.newLineAtOffset(rect.x() + 4, rect.y() + (rect.height() - fontSize) / 2);
                        cs.showText(typedName);
                        cs.endText();
                    }
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] decodeBase64Image(String base64) {
        String data = base64;
        int comma = base64.indexOf(',');
        if (comma >= 0) {
            data = base64.substring(comma + 1);
        }
        byte[] raw = Base64.getDecoder().decode(data);
        try {
            ImageIO.read(new ByteArrayInputStream(raw));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid signature image", e);
        }
        return raw;
    }
}
