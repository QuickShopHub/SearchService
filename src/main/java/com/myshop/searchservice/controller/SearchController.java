package com.myshop.searchservice.controller;



import org.springframework.data.domain.Page;
import com.myshop.searchservice.DTO.ProductForSearch;

import com.myshop.searchservice.DTO.SearchRequest;
import com.myshop.searchservice.service.SearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api")
public class SearchController {


    private final SearchService searchService;


    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping(path = "/query")
    public Page<ProductForSearch> searchProducts(
            @RequestBody SearchRequest query,
            @RequestParam(defaultValue = "0", name = "page", required = false) int page,
            @RequestParam(defaultValue = "1", name = "size", required = false) int size) {
        return searchService.search(query, PageRequest.of(page, size));
    }

}
