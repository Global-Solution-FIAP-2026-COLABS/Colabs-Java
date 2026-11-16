package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.RecordType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldFailWhenCitizenCpfHasInvalidFormat() {
        CitizenRequest request = new CitizenRequest("João Silva", "123", "(11) 99999-0000", "Rua das Flores, 123");
        Set<ConstraintViolation<CitizenRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("cpf")));
    }

    @Test
    void shouldFailWhenDonationQuantityIsZeroOrDescriptionTooShort() {
        DonationCreateRequest request = new DonationCreateRequest(
                RecordType.DONATION_OFFER, "A", "Curta", 0, 1L, 1L, 1L
        );
        Set<ConstraintViolation<DonationCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.size() >= 3);
    }

    @Test
    void shouldPassWhenHelpRequestWithoutCollectionPointIsValid() {
        DonationCreateRequest request = new DonationCreateRequest(
                RecordType.HELP_REQUEST,
                "Cestas Básicas Urgentes",
                "Família precisando de alimentos não perecíveis esta semana",
                2,
                1L,
                null,
                1L
        );
        Set<ConstraintViolation<DonationCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}