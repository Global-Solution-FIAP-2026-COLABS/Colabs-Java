package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.AccountType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static CitizenRequest validCitizen() {
        return new CitizenRequest("Maria Silva", "529.982.247-25", "11987654321", "Centro");
    }

    private static CollectionPointRequest validPoint() {
        return new CollectionPointRequest("Ponto Solidário Centro", "11.222.333/0001-81",
                "Rua das Flores, 100", "Centro", "1133334444", true);
    }

    @Test
    void donationRequest_rejectsBlankTitleAndZeroQuantity() {
        DonationCreateRequest request = new DonationCreateRequest("", "curta", 0, null, null, null);

        Set<ConstraintViolation<DonationCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("citizenId")));
    }

    @Test
    void donationRequest_acceptsValidData() {
        DonationCreateRequest request = new DonationCreateRequest(
                "Cestas básicas", "20 cestas básicas para famílias afetadas", 20, 1L, 1L, 1L);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void citizenRequest_rejectsInvalidCpf() {
        CitizenRequest request = new CitizenRequest("Maria Silva", "111.111.111-11", "11987654321", "Centro");

        Set<ConstraintViolation<CitizenRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("cpf")));
    }

    @Test
    void loginRequest_rejectsInvalidEmail() {
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(new LoginRequest("nao-e-email", "123"));

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void registerRequest_acceptsValidCitizenSignup() {
        RegisterRequest request = new RegisterRequest("maria@email.com", "senha123", AccountType.CITIZEN, validCitizen(), null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void registerRequest_acceptsValidCollectionPointSignup() {
        RegisterRequest request = new RegisterRequest("ponto@email.com", "senha123", AccountType.COLLECTION_POINT, null, validPoint());

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void registerRequest_validatesNestedCitizenData() {
        CitizenRequest invalidCitizen = new CitizenRequest("Maria Silva", "111.111.111-11", "11987654321", "Centro");
        RegisterRequest request = new RegisterRequest("maria@email.com", "senha123", AccountType.CITIZEN, invalidCitizen, null);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("citizen.cpf")));
    }

    @Test
    void registerRequest_requiresTypeAndCredentials() {
        RegisterRequest request = new RegisterRequest("", "123", null, null, null);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("type")));
    }
}