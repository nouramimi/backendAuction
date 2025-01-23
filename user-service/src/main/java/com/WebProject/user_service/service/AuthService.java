package com.WebProject.user_service.service;

import com.WebProject.user_service.model.Utilisateur;
import com.WebProject.user_service.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.WebProject.user_service.dto.*;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;

    public LoginResponse login(LoginRequest loginRequest) {
        Utilisateur user = utilisateurRepository.findByEmail(loginRequest.getEmail());

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setToken(UUID.randomUUID().toString());
        return response;
    }
    public Utilisateur register(Utilisateur utilisateur) {
        if (utilisateurRepository.findByEmail(utilisateur.getEmail())!=null) {
            throw new IllegalArgumentException("Email already in use");
        }

        return utilisateurRepository.save(utilisateur);
    }

}