package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.DonationCategory;
import br.com.fiap.colabs.enums.RecordType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
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
    void shouldFailWhenDonationItemsListIsEmptyOrItemQuantityIsZero() {
        DonationCreateRequest emptyItems = new DonationCreateRequest(
                RecordType.DONATION_OFFER, "A", "Curta", 1L, 1L, List.of()
        );
        Set<ConstraintViolation<DonationCreateRequest>> violations = validator.validate(emptyItems);
        assertTrue(violations.size() >= 3);

        DonationCreateRequest invalidItem = new DonationCreateRequest(
                RecordType.DONATION_OFFER,
                "Kit Higiene",
                "Itens diversos para doação comunitária",
                1L,
                1L,
                List.of(new DonationItemRequest("Sabonete", 0, DonationCategory.HYGIENE))
        );
        Set<ConstraintViolation<DonationCreateRequest>> itemViolations = validator.validate(invalidItem);
        assertFalse(itemViolations.isEmpty());
    }

    @Test
    void shouldPassWhenHelpRequestWithMultipleItemsIsValid() {
        DonationCreateRequest request = new DonationCreateRequest(
                RecordType.HELP_REQUEST,
                "Apoio Familiar Semanal",
                "Família precisando de alimentos e itens de higiene esta semana",
                1L,
                null,
                List.of(
                        new DonationItemRequest("Arroz 5kg", 2, DonationCategory.FOOD),
                        new DonationItemRequest("Sabonete", 6, DonationCategory.HYGIENE)
                )
        );
        Set<ConstraintViolation<DonationCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}