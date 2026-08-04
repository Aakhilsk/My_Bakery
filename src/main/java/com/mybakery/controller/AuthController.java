package com.mybakery.controller;

import com.mybakery.model.User;
import com.mybakery.repository.UserRepository;
import com.mybakery.service.TotpService;
import org.apache.commons.codec.binary.Base32;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.mybakery.config.MfaEnforcementFilter;

/**
 * Authentication controller handling login, MFA setup, and verification.
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    public AuthController(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         TotpService totpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login";
    }

    /**
     * Show account settings page (username and password changes).
     */
    @GetMapping("/account-settings")
    public String showAccountSettings(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        model.addAttribute("currentUser", user);
        return "auth/account-settings";
    }

    /**
     * Change username.
     */
    @PostMapping("/change-username")
    public String changeUsername(@RequestParam String newUsername,
                                @RequestParam String passwordForUsername,
                                Authentication authentication,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        User dbUser = userRepository.findById(user.getId()).orElseThrow();

        // Validate current password
        if (!passwordEncoder.matches(passwordForUsername, dbUser.getPassword())) {
            redirectAttributes.addFlashAttribute("usernameError", "Password is incorrect.");
            return "redirect:/auth/account-settings";
        }

        // Validate new username format
        if (newUsername.length() < 3) {
            redirectAttributes.addFlashAttribute("usernameError", "Username must be at least 3 characters long.");
            return "redirect:/auth/account-settings";
        }

        if (!newUsername.matches("^[a-zA-Z0-9_]+$")) {
            redirectAttributes.addFlashAttribute("usernameError", "Username can only contain letters, numbers, and underscores.");
            return "redirect:/auth/account-settings";
        }

        // Check if username is already taken
        if (!newUsername.equals(dbUser.getUsername()) && userRepository.existsByUsername(newUsername)) {
            redirectAttributes.addFlashAttribute("usernameError", "Username is already taken. Please choose a different one.");
            return "redirect:/auth/account-settings";
        }

        // Update username
        dbUser.setUsername(newUsername);
        userRepository.save(dbUser);

        redirectAttributes.addFlashAttribute("usernameSuccess", "true");
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/auth/login?usernameSuccess";
    }

    /**
     * Show password change form.
     */
    @GetMapping("/mfa/setup")
    public String showMfaSetup(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        User dbUser = userRepository.findById(user.getId()).orElseThrow();

        if (dbUser.getMfaEnabled()) {
            model.addAttribute("message", "MFA is already enabled for your account.");
            return "auth/mfa-setup";
        }

        // Generate new secret
        String secret = generateNewTotpSecret();
        String qrUri = totpService.generateQRCodeUri(secret, user.getUsername(), "MyBakery Admin");
        
        try {
            String qrCodeDataUrl = totpService.generateQRCodeDataUrl(qrUri);
            model.addAttribute("qrCodeDataUrl", qrCodeDataUrl);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to generate QR code. Please try again.");
            return "auth/mfa-setup";
        }
        
        model.addAttribute("secret", secret);
        model.addAttribute("username", user.getUsername());

        return "auth/mfa-setup";
    }

    /**
     * Verify and enable MFA.
     */
    @PostMapping("/mfa/enable")
    public String enableMfa(@RequestParam String secret,
                           @RequestParam String code,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        User dbUser = userRepository.findById(user.getId()).orElseThrow();

        // Verify the code
        if (!totpService.verifyCode(secret, code)) {
            redirectAttributes.addFlashAttribute("error", "Invalid verification code. Please try again.");
            redirectAttributes.addAttribute("secret", secret);
            return "redirect:/auth/mfa/setup";
        }

        // Enable MFA and save secret
        dbUser.setMfaSecret(secret);
        dbUser.setMfaEnabled(true);
        userRepository.save(dbUser);
        // The setup code proves possession of the authenticator for this session.
        // The next sign-in will always require the separate verification screen.

        redirectAttributes.addFlashAttribute("success", "MFA has been successfully enabled!");
        return "redirect:/admin/products";
    }

    /** Displays the required second step after a password login for MFA-enabled admins. */
    @GetMapping("/mfa/verify")
    public String showMfaVerification(Authentication authentication) {
        currentUser(authentication);
        return "auth/mfa-verify";
    }

    /** Completes password-plus-TOTP sign-in and unlocks the current session. */
    @PostMapping("/mfa/verify")
    public String verifyMfaLogin(@RequestParam String code,
                                 Authentication authentication,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        User dbUser = userRepository.findById(user.getId()).orElseThrow();
        if (!Boolean.TRUE.equals(dbUser.getMfaEnabled()) || !totpService.verifyCode(dbUser.getMfaSecret(), code)) {
            redirectAttributes.addFlashAttribute("error", "Invalid verification code. Please try again.");
            return "redirect:/auth/mfa/verify";
        }
        request.getSession().setAttribute(MfaEnforcementFilter.MFA_VERIFIED_SESSION_KEY, true);
        return "redirect:/admin/products";
    }

    /**
     * Disable MFA (requires current password for security).
     */
    @PostMapping("/mfa/disable")
    public String disableMfa(@RequestParam String password,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        User dbUser = userRepository.findById(user.getId()).orElseThrow();

        // Verify password
        if (!passwordEncoder.matches(password, dbUser.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Invalid password.");
            return "redirect:/admin/products";
        }

        // Disable MFA
        dbUser.setMfaSecret(null);
        dbUser.setMfaEnabled(false);
        userRepository.save(dbUser);

        redirectAttributes.addFlashAttribute("success", "MFA has been disabled.");
        return "redirect:/admin/products";
    }

    /**
     * Show password change form (redirects to account settings).
     */
    @GetMapping("/change-password")
    public String showChangePasswordForm(Authentication authentication) {
        currentUser(authentication);
        return "redirect:/auth/account-settings";
    }

    /**
     * Process password change.
     */
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Authentication authentication,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        User dbUser = userRepository.findById(user.getId()).orElseThrow();

        // Validate current password
        if (!passwordEncoder.matches(currentPassword, dbUser.getPassword())) {
            redirectAttributes.addFlashAttribute("passwordError", "Current password is incorrect.");
            return "redirect:/auth/account-settings";
        }

        // Validate new passwords match
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", "New passwords do not match.");
            return "redirect:/auth/account-settings";
        }

        // Validate password length
        if (newPassword.length() < 12) {
            redirectAttributes.addFlashAttribute("passwordError", "New password must be at least 12 characters long.");
            return "redirect:/auth/account-settings";
        }

        // Validate new password is different from current
        if (passwordEncoder.matches(newPassword, dbUser.getPassword())) {
            redirectAttributes.addFlashAttribute("passwordError", "New password must be different from your current password.");
            return "redirect:/auth/account-settings";
        }

        // Update password
        dbUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(dbUser);

        redirectAttributes.addFlashAttribute("passwordSuccess", "true");
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/auth/login?passwordSuccess";
    }

    /**
     * Generate a new TOTP secret key.
     */
    private String generateNewTotpSecret() {
        // Generate a random 20-byte array and Base32 encode it
        byte[] bytes = new byte[20];
        java.security.SecureRandom random = new java.security.SecureRandom();
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication is required.");
        }
        return user;
    }
}
