package com.mybakery.controller;

import com.mybakery.model.Product;
import com.mybakery.model.User;
import com.mybakery.service.ImageStorageService;
import com.mybakery.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin-only web controller for managing bakery products.
 * All routes live under {@code /admin/products} — separate from the public storefront.
 */
@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final ImageStorageService imageStorageService;

    public AdminProductController(ProductService productService, ImageStorageService imageStorageService) {
        this.productService = productService;
        this.imageStorageService = imageStorageService;
    }

    /** GET /admin/products — admin product list with edit/delete actions. */
    @GetMapping
    public String listProducts(Model model, Authentication authentication) {
        model.addAttribute("products", productService.findAll());
        
        // Add current user info for MFA settings display
        if (authentication != null && authentication.isAuthenticated()) {
            User user = (User) authentication.getPrincipal();
            model.addAttribute("currentUser", user);
            model.addAttribute("mfaEnabled", user.getMfaEnabled());
        }
        
        return "admin/products/list";
    }

    /** GET /admin/products/new — form to add a new product. */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("isEdit", false);
        return "admin/products/form";
    }

    /** POST /admin/products — save a newly created product. */
    @PostMapping
    public String createProduct(@Valid @ModelAttribute("product") Product product,
                                BindingResult bindingResult,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "admin/products/form";
        }

        String imagePath = imageStorageService.store(imageFile);
        if (imagePath != null) {
            product.setImagePath(imagePath);
        }

        productService.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Product added successfully!");
        return "redirect:/admin/products";
    }

    /** GET /admin/products/{id}/edit — form pre-filled with existing product data. */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findById(id));
        model.addAttribute("isEdit", true);
        return "admin/products/form";
    }

    /** POST /admin/products/{id} — update an existing product. */
    @PostMapping("/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("product") Product product,
                                BindingResult bindingResult,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "admin/products/form";
        }

        Product existing = productService.findById(id);
        String imagePath = imageStorageService.store(imageFile);
        if (imagePath != null) {
            product.setImagePath(imagePath);
        }

        productService.update(id, product);
        if (imagePath != null) {
            imageStorageService.delete(existing.getImagePath());
        }
        redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
        return "redirect:/admin/products";
    }

    /** POST /admin/products/{id}/availability — switches public visibility for a product. */
    @PostMapping("/{id}/availability")
    public String toggleAvailability(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Product product = productService.toggleAvailability(id);
        String status = product.isAvailable() ? "available" : "not available";
        redirectAttributes.addFlashAttribute("successMessage", product.getName() + " is now " + status + ".");
        return "redirect:/admin/products";
    }

    /** POST /admin/products/{id}/delete — remove a product. */
    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Product product = productService.findById(id);
        productService.deleteById(id);
        imageStorageService.delete(product.getImagePath());
        redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully!");
        return "redirect:/admin/products";
    }

}
