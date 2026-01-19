package com.barbearia.barbearia.modules.business.mapper;

import org.springframework.stereotype.Component;

import com.barbearia.barbearia.modules.business.dto.request.ExpensesRequest;
import com.barbearia.barbearia.modules.business.dto.response.ExpensesResponse;
import com.barbearia.barbearia.modules.business.model.Expenses;

@Component
public class ExpensesMapper {
    

    public Expenses toEntity(ExpensesRequest request) {
        if (request == null) return null;

        return Expenses.builder()
            .title(request.title())
            .description(request.description())
            .category(request.category())
            .amount(request.amount())
            .date(request.date())
            .build();
    }

    public ExpensesResponse toResponse(Expenses expenses) {
        if(expenses == null) return null;

        return new ExpensesResponse(
            expenses.getId(),
            expenses.getTitle(),
            expenses.getDescription(),
            expenses.getCategory(),
            expenses.getAmount(),
            expenses.getDate()
        );
    }

}
