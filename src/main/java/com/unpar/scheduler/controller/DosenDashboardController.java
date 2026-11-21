package com.unpar.scheduler.controller;

import com.unpar.scheduler.dto.dashboard.DosenDashboardSummaryDTO;
import com.unpar.scheduler.service.DosenDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dosen/dashboard")
public class DosenDashboardController {

    private final DosenDashboardService dosenDashboardService;

    public DosenDashboardController(DosenDashboardService dosenDashboardService) {
        this.dosenDashboardService = dosenDashboardService;
    }

    @GetMapping({"", "/summary"})
    public ResponseEntity<DosenDashboardSummaryDTO> getSummary(
        @RequestParam(name = "dosenId", required = false) String dosenId
    ) {
        DosenDashboardSummaryDTO summary = dosenDashboardService.getDashboardSummary(dosenId);
        return ResponseEntity.ok(summary);
    }
}
