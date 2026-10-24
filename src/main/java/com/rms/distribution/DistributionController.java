package com.rms.distribution;

import com.rms.distribution.DistributionDtos.AdjustmentRequest;
import com.rms.distribution.DistributionDtos.AdjustmentResult;
import com.rms.distribution.DistributionDtos.ClaimDistribution;
import com.rms.distribution.DistributionDtos.ClaimListItem;
import com.rms.distribution.DistributionDtos.PolicyDistribution;
import com.rms.distribution.DistributionDtos.PolicyListItem;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Policy numbers and claim numbers contain slashes, so they are passed as query parameters. */
@RestController
@RequestMapping("/api/v1")
public class DistributionController {
    private final DistributionService service;
    private final DistributionAdjustmentService adjustments;

    public DistributionController(DistributionService service, DistributionAdjustmentService adjustments) {
        this.service = service;
        this.adjustments = adjustments;
    }

    @GetMapping("/policies")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public List<PolicyListItem> policies(@RequestParam(required = false) String q) {
        return service.searchPolicies(q);
    }

    @GetMapping("/policies/distribution")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public PolicyDistribution policy(@RequestParam String policyNo) {
        return service.policy(policyNo);
    }

    @GetMapping("/claims")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public List<ClaimListItem> claims(@RequestParam(required = false) String q) {
        return service.searchClaims(q);
    }

    @GetMapping("/claims/distribution")
    @PreAuthorize("hasAuthority('TREATY_VIEW')")
    public ClaimDistribution claim(@RequestParam String claimNo) {
        return service.claim(claimNo);
    }

    /** Propose a different reinsurer split for one policy cession layer or one claim recovery (maker-checker). */
    @PostMapping("/distribution/adjustments")
    @PreAuthorize("hasAuthority('DISTRIBUTION_MAKE')")
    public AdjustmentResult propose(@RequestBody AdjustmentRequest request) {
        return adjustments.propose(request);
    }
}
