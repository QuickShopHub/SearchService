package com.myshop.searchservice.controller;



import org.springframework.data.domain.Page;
import com.myshop.searchservice.DTO.ProductForSearch;

import com.myshop.searchservice.DTO.SearchRequest;
import com.myshop.searchservice.service.SearchService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.bind.annotation.*;


@EnableWebSecurity
@RestController
@RequestMapping(path = "/api")
public class SearchController {


    private final SearchService searchService;


    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping(path = "/query")
    public Page<ProductForSearch> searchProducts(@RequestBody SearchRequest query) {
        return searchService.search(query);
    }

}
