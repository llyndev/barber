package com.barbearia.barbearia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "address")
public class Address {

    @Column(name = "add_cep", nullable = false)
    private String cep;

    @Column(name = "add_logradouro", nullable = false)
    private String logradouro;

    @Column(name = "add_numero", nullable = false)
    private String numero;

    @Column(name = "add_complemento")
    private String complemento;

    @Column(name = "add_bairro", nullable = false)
    private String bairro;

    @Column(name = "add_localidade", nullable = false)
    private String localidade;

    @Column(name = "add_uf", nullable = false)
    private String uf;
}
