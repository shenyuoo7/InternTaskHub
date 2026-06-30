package com.internhub.controller;

import com.internhub.dto.DashboardSummary;
import com.internhub.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummary summary(@RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        return dashboardService.summary(currentUserId);
    }
}
