package com.mybakery.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Stores only supported image formats under server-generated filenames. */
@Service
public class ImageStorageService {
    private static final Path UPLOAD_DIRECTORY = Paths.get("uploads").toAbsolutePath().normalize();
    public String store(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        try {
            Files.createDirectories(UPLOAD_DIRECTORY);
            try (BufferedInputStream input = new BufferedInputStream(imageFile.getInputStream())) {
                String extension = detectImageExtension(input);
                String storedFileName = UUID.randomUUID() + extension;
                Path target = UPLOAD_DIRECTORY.resolve(storedFileName).normalize();
                if (!target.startsWith(UPLOAD_DIRECTORY)) {
                    throw new IllegalArgumentException("Invalid image path.");
                }
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                return "/product-images/" + storedFileName;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store image file.", ex);
        }
    }

    public void delete(String imagePath) {
        if (imagePath == null || !imagePath.startsWith("/product-images/")) {
            return;
        }
        Path target = UPLOAD_DIRECTORY.resolve(imagePath.substring("/product-images/".length())).normalize();
        if (target.startsWith(UPLOAD_DIRECTORY)) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to delete image file.", ex);
            }
        }
    }

    private String detectImageExtension(BufferedInputStream input) throws IOException {
        input.mark(12);
        byte[] header = input.readNBytes(12);
        input.reset();

        if (header.length >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if (header.length >= 8 && header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
            return ".png";
        }
        if (header.length >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F'
                && header[3] == '8' && (header[4] == '7' || header[4] == '9') && header[5] == 'a') {
            return ".gif";
        }
        if (header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return ".webp";
        }
        throw new IllegalArgumentException("Upload a valid JPG, PNG, GIF, or WebP image.");
    }
}
