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
            @RequestHeader("X-Business-Slug") String slug,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(expensesService.listExpenses(slug, startDate, endDate, userDetails.user()));
    }

    @PostMapping
    public ResponseEntity<ExpensesResponse> createExpense(
            @RequestHeader("X-Business-Slug") String slug,
            @RequestBody @Valid ExpensesRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        return ResponseEntity.ok(expensesService.createExpense(slug, request, userDetails.user()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpensesResponse> updateExpense(
            @RequestHeader("X-Business-Slug") String slug,
            @PathVariable Long id,
            @RequestBody @Valid ExpensesRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        return ResponseEntity.ok(expensesService.updateExpenses(slug, id, request, userDetails.user()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(
            @RequestHeader("X-Business-Slug") String slug,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        expensesService.deleteExpense(slug, id, userDetails.user());
        return ResponseEntity.noContent().build();
    }
}
