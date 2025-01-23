package com.WebProject.product_service.controller;

import com.WebProject.product_service.dto.ProductRequest;
import com.WebProject.product_service.dto.ProductResponse;
import com.WebProject.product_service.model.Product;
import com.WebProject.product_service.repository.ProductRepository;
import com.WebProject.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest productRequest) {
        ProductResponse createdProduct = productService.createProduct(productRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<ProductResponse>> getProductsByOwner(@PathVariable String ownerId) {
        List<ProductResponse> products = productService.getProductsByOwner(ownerId);
        return ResponseEntity.ok(products);
    }

    // Keep your existing endpoints and add the service call
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }
}


