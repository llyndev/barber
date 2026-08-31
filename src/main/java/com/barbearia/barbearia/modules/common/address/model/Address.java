package com.barbearia.barbearia.modules.common.address.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Address {

    @Column(name = "add_cep", nullable = false, length = 8)
    @Pattern(regexp = "\\d{8}", message = "CEP deve conter 8 dígitos")
    private String cep;

    @Column(name = "add_logradouro", nullable = false)
    private String logradouro;

    @Column(name = "add_numero", nullable = false, length = 20)
    private String numero;

    @Column(name = "add_complemento")
    private String complemento;

    @Column(name = "add_bairro", nullable = false)
    private String bairro;

    @Column(name = "add_localidade", nullable = false)
    private String localidade;

    @Column(name = "add_uf", nullable = false)
    @Pattern(regexp = "[A-Z]{2}", message = "UF deve ter 2 letras maiúsculas")
    private String uf;

    public static Address of(String cep, String logradouro, String numero,
                             String complemento, String bairro,
                             String localidade, String uf) {
        return new Address(
                cep.replaceAll("\\D", ""),        // normaliza na construção
                logradouro.trim(),
                numero.trim(),
                complemento == null ? null : complemento.trim(),
                bairro.trim(),
                localidade.trim(),
                uf.trim().toUpperCase(Locale.ROOT)
        );
    }
}
