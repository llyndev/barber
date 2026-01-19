package com.barbearia.barbearia.modules.business.service;

import java.time.LocalDate;
import java.util.List;

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
    private final BusinessService businessService;

    @Transactional(readOnly = true)
    public List<ExpensesResponse> listExpenses(String slug, LocalDate start, LocalDate end, AppUser user) {
        Business business = businessService.validateOwnerOrManagerBySlug(slug, user.getId());

        List<Expenses> expenses;
        
        if (start != null && end != null) {
            expenses = expensesRepository.findAllByBusinessIdAndDateBetweenOrderByDateDesc(business.getId(), start, end);
        } else {
            expenses = expensesRepository.findAllByBusinessIdOrderByDateDesc(business.getId());
        }

        return expenses.stream()
                .map(expensesMapper::toResponse)
                .toList();
    }

    @Transactional
    public ExpensesResponse createExpense(String slug, ExpensesRequest request, AppUser user) {
        Business business = businessService.validateOwnerOrManagerBySlug(slug, user.getId());

        Expenses expenses = expensesMapper.toEntity(request);
        expenses.setBusiness(business);

        return expensesMapper.toResponse(expensesRepository.save(expenses));
    }

    @Transactional
    public ExpensesResponse updateExpenses(String slug, Long id, ExpensesRequest request, AppUser user) {
        Business business = businessService.validateOwnerOrManagerBySlug(slug, user.getId());

        Expenses expense = expensesRepository.findByIdAndBusinessId(id, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada"));
        
        expense.setTitle(request.title());
        expense.setDescription(request.description());
        expense.setCategory(request.category());
        expense.setAmount(request.amount());
        expense.setDate(request.date());

        return expensesMapper.toResponse(expensesRepository.save(expense));
    
    }

    @Transactional
    public void deleteExpense(String slug, Long id, AppUser user) {
        Business business = businessService.validateOwnerOrManagerBySlug(slug, user.getId());

        Expenses expense = expensesRepository.findByIdAndBusinessId(id, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada"));

        expensesRepository.delete(expense);
    }
}
