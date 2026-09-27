package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.application.AlimentService;
import com.dairy.apipinal.nutrition.application.CreateAliment;
import com.dairy.apipinal.nutrition.domain.Aliment;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aliments")
public class AlimentController {

    private final AlimentService alimentService;

    public AlimentController(AlimentService alimentService) {
        this.alimentService = alimentService;
    }

    @PostMapping
    public ResponseEntity<AlimentResponse> create(
            @Valid @RequestBody CreateAlimentRequest request
    ) {
        CreateAliment command = new CreateAliment(
                request.code(),
                request.nom(),
                request.categorie(),
                request.unite()
        );

        Aliment aliment = alimentService.create(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AlimentResponse.from(aliment));
    }

    @GetMapping
    public ResponseEntity<List<AlimentResponse>> getAll() {
        List<AlimentResponse> response = alimentService.getAll()
                .stream()
                .map(AlimentResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
