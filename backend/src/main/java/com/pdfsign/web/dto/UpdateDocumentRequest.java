package com.pdfsign.web.dto;

import com.pdfsign.domain.SignatureType;

public record UpdateDocumentRequest(String title, SignatureType signatureType) {
}
