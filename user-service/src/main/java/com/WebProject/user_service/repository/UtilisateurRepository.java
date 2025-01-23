package com.WebProject.user_service.repository;

import com.WebProject.user_service.model.Utilisateur;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface UtilisateurRepository extends MongoRepository<Utilisateur, String> {
    Utilisateur findByEmail(String email);
}
