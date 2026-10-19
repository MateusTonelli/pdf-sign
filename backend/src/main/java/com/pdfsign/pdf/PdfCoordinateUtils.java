package com.pdfsign.pdf;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public final class PdfCoordinateUtils {

    private PdfCoordinateUtils() {
    }

    public record PdfRect(float x, float y, float width, float height) {
    }

    /** Normalized origin top-left (0-1) to PDF user space (origin bottom-left). */
    public static PdfRect toPdfRect(PDPage page, double nx, double ny, double nw, double nh) {
        PDRectangle mediaBox = page.getMediaBox();
        float pageWidth = mediaBox.getWidth();
        float pageHeight = mediaBox.getHeight();
        float w = (float) (nw * pageWidth);
        float h = (float) (nh * pageHeight);
        float x = (float) (nx * pageWidth);
        float y = pageHeight - (float) (ny * pageHeight) - h;
        return new PdfRect(x, y, w, h);
    }
}
