package com.barbearia.barbearia.tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class TenantInfo {
    private String tenantId;
    private String schema;
}
