package com.myshop.searchservice.controller;


import com.myshop.searchservice.DTO.ProductForSearch;

import com.myshop.searchservice.DTO.SearchRequest;
import com.myshop.searchservice.service.SearchService;
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
    public List<ProductForSearch> searchProducts(@RequestBody SearchRequest query) {
        return searchService.search(query);
    }

}
