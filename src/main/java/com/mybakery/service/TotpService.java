package com.mybakery.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.apache.commons.codec.binary.Base32;
import org.jboss.aerogear.security.otp.Totp;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.security.SecureRandom;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service for TOTP (Time-based One-Time Password) generation and verification.
 * Uses Google Authenticator compatible algorithm.
 */
@Service
public class TotpService {

    /**
     * Verify the provided OTP code against the secret.
     *
     * @param secret Base32-encoded secret
     * @param code The 6-digit code to verify
     * @return true if the code is valid
     */
    public boolean verifyCode(String secret, String code) {
        try {
            Totp totp = new Totp(secret);
            return totp.verify(code);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate a new TOTP secret (Base32 encoded).
     * This creates a random Base32-encoded secret suitable for Google Authenticator.
     */
    public String generateNewSecret() {
        byte[] bytes = new byte[20];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }

    /**
     * Generate QR code provisioning URI for Google Authenticator.
     *
     * @param secret The TOTP secret
     * @param username The username
     * @param issuer The app name (issuer)
     * @return The provisioning URI
     */
    public String generateQRCodeUri(String secret, String username, String issuer) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                URLEncoder.encode(issuer, StandardCharsets.UTF_8),
                URLEncoder.encode(username, StandardCharsets.UTF_8),
                secret,
                URLEncoder.encode(issuer, StandardCharsets.UTF_8)
        );
    }

    /**
     * Generate QR code image as Data URL.
     *
     * @param uri The provisioning URI
     * @return Base64-encoded data URL for the QR code
     */
    public String generateQRCodeDataUrl(String uri) throws WriterException {
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(uri, BarcodeFormat.QR_CODE, 200, 200);

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", stream);
            byte[] imageData = stream.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            return "data:image/png;base64," + base64Image;
        } catch (Exception e) {
            throw new WriterException("Failed to generate QR code image: " + e.getMessage());
        }
    }
}

