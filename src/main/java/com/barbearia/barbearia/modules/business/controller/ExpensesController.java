package com.barbearia.barbearia.modules.business.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barbearia.barbearia.modules.business.dto.request.ExpensesRequest;
import com.barbearia.barbearia.modules.business.dto.response.ExpensesResponse;
import com.barbearia.barbearia.modules.business.service.ExpensesService;
import com.barbearia.barbearia.security.UserDetailsImpl;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpensesController {

    private final ExpensesService expensesService;

    @GetMapping
    public ResponseEntity<List<ExpensesResponse>> listExpenses(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return ResponseEntity.ok(expensesService.listExpenses(startDate, endDate));
    }

    @PostMapping
    public ResponseEntity<ExpensesResponse> createExpense(@RequestBody @Valid ExpensesRequest request) {
        return ResponseEntity.ok(expensesService.createExpense(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpensesResponse> updateExpense(
            @PathVariable Long id,
            @RequestBody @Valid ExpensesRequest request) {
        return ResponseEntity.ok(expensesService.updateExpenses(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expensesService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}
