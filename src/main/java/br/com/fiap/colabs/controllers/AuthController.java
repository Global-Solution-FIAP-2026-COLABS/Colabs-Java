package br.com.fiap.colabs.controllers;

import br.com.fiap.colabs.dtos.AccountResponse;
import br.com.fiap.colabs.dtos.LoginRequest;
import br.com.fiap.colabs.dtos.LoginResponse;
import br.com.fiap.colabs.dtos.RegisterRequest;
import br.com.fiap.colabs.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Registro de contas e geração de token JWT")
public class AuthController {

    private final AccountService accountService;

    @PostMapping("/register")
    @Operation(summary = "Cadastro único: cria a conta e o perfil de cidadão ou de ponto de coleta")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário e emitir token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(accountService.login(request));
    }
}