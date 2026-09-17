package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.domain.Ration;
import org.springframework.data.domain.Page;

import java.util.List;

public record RationsPageResponse(
        List<RationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static RationsPageResponse from(Page<Ration> page) {
        return new RationsPageResponse(
                page.getContent()
                        .stream()
                        .map(RationResponse::from)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}