package com.rms.allocation;

import com.rms.allocation.AllocationPlanService.SimulationOutput;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/allocation")
@PreAuthorize("hasAuthority('TREATY_VIEW')")
public class AllocationController {
    public record SimulateRequest(Long coverId, LocalDate date, BigDecimal sumInsured, BigDecimal premium, Long riskGradeId) { }

    private final AllocationPlanService service;

    public AllocationController(AllocationPlanService service) {
        this.service = service;
    }

    /** Ordered execution chain (treaty, sequence, method, basis, residual action) that applies to a cover on a date. */
    @GetMapping("/plan")
    public List<PlanStep> plan(@RequestParam Long coverId, @RequestParam LocalDate date) {
        return service.plan(coverId, date);
    }

    /** Runs the chain on a sample risk: what each step takes, what is retained, what goes facultative. */
    @PostMapping("/simulate")
    public SimulationOutput simulate(@RequestBody SimulateRequest r) {
        return service.simulate(r.coverId(), r.date(), r.sumInsured(), r.premium(), r.riskGradeId());
    }
}
