package com.dairy.apipinal.finance.infrastructure.web;

public record TransactionResponse(
        String id,
        String type,
        String category,
        double amount,
        String date,
        String status,
        String label
) {}
