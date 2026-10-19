package com.pdfsign.web.dto;

import com.pdfsign.domain.SignatureType;
import com.pdfsign.domain.SignerStatus;
import java.util.List;

public record SignSessionDto(
        String documentTitle,
        SignatureType signatureType,
        String signerName,
        String signerEmail,
        SignerStatus status,
        List<SignatureFieldDto> fields,
        boolean canSignWithCertificate,
        String certificateHint) {
}
