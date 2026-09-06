package com.barbearia.barbearia.modules.account.dto.response;

import com.barbearia.barbearia.modules.business.model.BusinessRole;

import java.math.BigDecimal;

/* Record para informar em quais barbearias o usuario atua
não carrega os dados do usuário porque ele já é o dono da resposta.
*/
public record MyMembershipResponse(
        Long membershipId,
        Long businessId,
        String businessName,
        String slug,
        BusinessRole role,
        BigDecimal commisionPercentage,
        boolean businessActive
) {
}
