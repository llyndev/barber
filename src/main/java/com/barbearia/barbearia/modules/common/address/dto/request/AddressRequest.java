package com.barbearia.barbearia.modules.common.address.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve estar no formato 00000-000")
    String cep,

    @NotBlank(message = "Logradouro é obrigatório.")
    @Size(max = 150)
    String logradouro,

    @NotBlank(message = "Número é obrigatório.")
    @Size(max = 20)
    String numero,

    @Size(max = 100)
    String complemento,

    @NotBlank(message = "Bairro é obrigatório.")
    @Size(max = 100)
    String bairro,

    @NotBlank(message = "Cidade é obrigatório.")
    @Size(max = 100)
    String localidade, // cidade

    @NotBlank(message = "UF é obrigatória.")
    @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
            message = "UF inválida")
    String uf
) {}
