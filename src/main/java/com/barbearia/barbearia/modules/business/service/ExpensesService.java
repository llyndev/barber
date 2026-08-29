package com.barbearia.barbearia.modules.business.service;

import java.time.LocalDate;
import java.util.List;

import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.tenant.BusinessContext;
import com.barbearia.barbearia.tenant.BusinessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.business.dto.request.ExpensesRequest;
import com.barbearia.barbearia.modules.business.dto.response.ExpensesResponse;
import com.barbearia.barbearia.modules.business.mapper.ExpensesMapper;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.Expenses;
import com.barbearia.barbearia.modules.business.repository.ExpensesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpensesService {

    private final ExpensesRepository expensesRepository;
    private final ExpensesMapper expensesMapper;
    private final BusinessGuard businessGuard;
    private final BusinessRepository businessRepository;

    @Transactional(readOnly = true)
    public List<ExpensesResponse> listExpenses(LocalDate start, LocalDate end) {
        businessGuard.requireOwnerOrManager();

        Long business = BusinessContext.requireBusinessId();

        List<Expenses> expenses;
        if (start != null && end != null) {
            expenses = expensesRepository.findAllByBusinessIdAndDateBetweenOrderByDateDesc(business, start, end);
        } else {
            expenses = expensesRepository.findAllByBusinessIdOrderByDateDesc(business);
        }

        return expenses.stream()
                .map(expensesMapper::toResponse)
                .toList();
    }

    @Transactional
    public ExpensesResponse createExpense(ExpensesRequest request) {
        Long businessId = BusinessContext.requireBusinessId();

        businessGuard.requireOwnerOrManager();

        Business businessRef = businessRepository.getReferenceById(businessId);

        Expenses expenses = expensesMapper.toEntity(request);
        expenses.setBusiness(businessRef);

        return expensesMapper.toResponse(expensesRepository.save(expenses));
    }

    @Transactional
    public ExpensesResponse updateExpenses(Long id, ExpensesRequest request) {
        businessGuard.requireOwnerOrManager();

        Long businessId = BusinessContext.requireBusinessId();

        Business business = businessRepository.getReferenceById(businessId);

        Expenses expense = expensesRepository.findByIdAndBusinessId(id, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found."));
        
        expense.setTitle(request.title());
        expense.setDescription(request.description());
        expense.setCategory(request.category());
        expense.setAmount(request.amount());
        expense.setDate(request.date());

        return expensesMapper.toResponse(expensesRepository.save(expense));
    
    }

    @Transactional
    public void deleteExpense(Long id) {
        businessGuard.requireOwnerOrManager();

        Long businessId = BusinessContext.requireBusinessId();

        Business business = businessRepository.getReferenceById(businessId);

        Expenses expense = expensesRepository.findByIdAndBusinessId(id, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found."));

        expensesRepository.delete(expense);
    }
}
