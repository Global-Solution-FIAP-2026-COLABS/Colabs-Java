package br.com.fiap.colabs.security;

import br.com.fiap.colabs.dtos.LoginRequest;
import br.com.fiap.colabs.dtos.LoginResponse;
import br.com.fiap.colabs.dtos.RegisterRequest;
import br.com.fiap.colabs.models.Account;
import br.com.fiap.colabs.repositories.AccountRepository;
import br.com.fiap.colabs.services.AccountService;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AccountService accountService;

    public AuthService(
            AccountRepository accountRepository,
            @Lazy AuthenticationManager authenticationManager,
            TokenService tokenService,
            AccountService accountService
    ) {
        this.accountRepository = accountRepository;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.accountService = accountService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(username.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado: " + username));
        return new AccountUserDetails(account);
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        AccountUserDetails userDetails = (AccountUserDetails) authentication.getPrincipal();
        String token = tokenService.generateToken(userDetails);

        return new LoginResponse(
                token,
                "Bearer",
                userDetails.getUsername(),
                userDetails.getRole().name(),
                userDetails.getCitizenId(),
                userDetails.getCollectionPointId()
        );
    }


    public LoginResponse register(RegisterRequest request) {
        accountService.register(request);
        return login(new LoginRequest(request.email(), request.password()));
    }
}