package com.WebProject.user_service.controller;

import com.WebProject.user_service.dto.LoginRequest;
import com.WebProject.user_service.dto.LoginResponse;
import com.WebProject.user_service.model.Utilisateur;
import com.WebProject.user_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/utilisateurs/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Utilisateur> register(@RequestBody Utilisateur utilisateur) {
        Utilisateur createdUser = authService.register(utilisateur);
        return ResponseEntity.ok(createdUser);
    }
}
