package com.WebProject.product_service.repository;

import com.WebProject.product_service.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository <Product, String> {
    List<Product> findByOwnerId(String ownerId);
}
