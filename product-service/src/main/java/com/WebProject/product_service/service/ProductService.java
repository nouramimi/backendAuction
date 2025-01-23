package com.WebProject.product_service.service;

import com.WebProject.product_service.dto.ProductRequest;
import com.WebProject.product_service.dto.ProductResponse;
import com.WebProject.product_service.event.ProductCreatedEvent;
import com.WebProject.product_service.model.Product;
import com.WebProject.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;
    private final KafkaTemplate<String,ProductCreatedEvent> kafkaTemplate;

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToProductResponse(product);
    }

    public ProductResponse createProduct(ProductRequest productRequest) {
        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .startingPrice(productRequest.getStartingPrice())
                .currentPrice(productRequest.getStartingPrice()) // Initialize current price to starting price
                .auctionStartTime(productRequest.getAuctionStartTime())
                .auctionEndTime(productRequest.getAuctionEndTime())
                .isActive(true)
                .category(productRequest.getCategory())
                .ownerId(productRequest.getOwnerId())
                .build();
        //productRepository.save(product);
        //log.info("Product {} is saved", product.getId());
        Product savedProduct = productRepository.save(product);
        log.info("Product {} is saved", savedProduct.getId());

        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(product.getId());
        kafkaTemplate.send("notificationTopic", productCreatedEvent);

        return mapToProductResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(this::mapToProductResponse).collect(Collectors.toList());
        //.toList();
    }

    public List<ProductResponse> getProductsByOwner(String ownerId) {
        List<Product> ownerProducts = productRepository.findByOwnerId(ownerId);
        return ownerProducts.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .currentPrice(product.getCurrentPrice())
                .auctionStartTime(product.getAuctionStartTime())
                .auctionEndTime(product.getAuctionEndTime())
                .isActive(product.isActive())
                .category(product.getCategory())
                .ownerId(product.getOwnerId())
                .build();
    }
}