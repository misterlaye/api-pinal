package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.api.WorkerHubQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/worker-hub")
@org.springframework.security.access.prepost.PreAuthorize("@authz.isWorkerOrOwner()")
public class WorkerHubController {

    private final WorkerHubQueries workerHubQueries;
    private final TenantContext tenantContext;

    public WorkerHubController(WorkerHubQueries workerHubQueries, TenantContext tenantContext) {
        this.workerHubQueries = workerHubQueries;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/home")
    public WorkerHubQueries.WorkerHomeData getHomeData(@RequestParam(required = false) UUID exploitationId) {
        return workerHubQueries.getWorkerHomeData(
                tenantContext.currentTenantId(),
                tenantContext.requireExploitation(exploitationId)
        );
    }

    @GetMapping("/animals")
    public List<WorkerHubQueries.WorkerAnimalData> getAnimals(@RequestParam(required = false) UUID exploitationId) {
        return workerHubQueries.getWorkerAnimals(
                tenantContext.currentTenantId(),
                tenantContext.requireExploitation(exploitationId)
        );
    }

    @GetMapping("/animals/{animalId}")
    public WorkerHubQueries.WorkerAnimalDetailData getAnimalDetail(@PathVariable UUID animalId) {
        return workerHubQueries.getWorkerAnimalDetail(
                tenantContext.currentTenantId(),
                tenantContext.currentExploitationId(),
                animalId
        );
    }
}
