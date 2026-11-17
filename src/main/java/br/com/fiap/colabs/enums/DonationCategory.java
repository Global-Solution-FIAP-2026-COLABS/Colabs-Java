package br.com.fiap.colabs.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DonationCategory {
    FOOD("Alimentos"),
    CLOTHING("Vestuário"),
    HYGIENE("Higiene e Limpeza"),
    MEDICINE("Medicamentos e Saúde"),
    FURNITURE("Móveis e Eletrodomésticos"),
    OTHER("Outros");

    private final String label;
}