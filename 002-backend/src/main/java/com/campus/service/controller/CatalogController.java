package com.campus.service.controller;

import com.campus.service.common.ApiResult;
import com.campus.service.entity.ServiceCategory;
import com.campus.service.entity.ServiceItem;
import com.campus.service.entity.Venue;
import com.campus.service.service.CatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public ApiResult<List<ServiceCategory>> categories() {
        return ApiResult.ok(catalogService.categories());
    }

    @GetMapping("/items")
    public ApiResult<List<ServiceItem>> items(@RequestParam(required = false) Long categoryId) {
        return ApiResult.ok(catalogService.items(categoryId));
    }

    @GetMapping("/venues")
    public ApiResult<List<Venue>> venues() {
        return ApiResult.ok(catalogService.venues());
    }
}
