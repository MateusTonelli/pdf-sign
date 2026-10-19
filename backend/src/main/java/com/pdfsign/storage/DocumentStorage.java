package com.pdfsign.storage;

import com.pdfsign.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentStorage {

    private final Path root;

    public DocumentStorage(AppProperties appProperties) throws IOException {
        this.root = Path.of(appProperties.getStoragePath()).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public StoredFile store(MultipartFile file, UUID tenantId, UUID documentId) throws IOException {
        String extension = extractExtension(file.getOriginalFilename());
        Path relative = Path.of(tenantId.toString(), documentId + extension);
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Invalid storage path");
        }
        Files.createDirectories(target.getParent());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = file.getInputStream();
                DigestInputStream din = new DigestInputStream(in, digest)) {
            Files.copy(din, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            if (e instanceof IOException io) {
                throw io;
            }
            throw new IOException("Failed to store file", e);
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        return new StoredFile(relative.toString().replace('\\', '/'), hash);
    }

    public Path resolvePath(String storagePath) {
        Path resolved = root.resolve(storagePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage path");
        }
        return resolved;
    }

    public byte[] readBytes(String storagePath) throws IOException {
        return Files.readAllBytes(resolvePath(storagePath));
    }

    public void writeBytes(String storagePath, byte[] data) throws IOException {
        Path path = resolvePath(storagePath);
        Files.createDirectories(path.getParent());
        Files.write(path, data);
    }

    private static String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".pdf";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    public record StoredFile(String storagePath, String sha256) {
    }
}
