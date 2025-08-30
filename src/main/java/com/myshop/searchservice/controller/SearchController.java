package com.myshop.searchservice.controller;



import org.springframework.data.domain.Page;
import com.myshop.searchservice.DTO.ProductForSearch;

import com.myshop.searchservice.DTO.SearchRequest;
import com.myshop.searchservice.service.SearchService;

import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping(path = "/api")
@CrossOrigin(origins = {"http://26.94.50.207:4200", "http://localhost:4200", "http://localhost:80", "http://localhost", "http://185.161.64.35"})
public class SearchController {


    private final SearchService searchService;


    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }


    @PostMapping(path = "/query")
    public ResponseEntity<PagedModel<EntityModel<ProductForSearch>>> searchProducts(@RequestBody SearchRequest query,
                                                                                    PagedResourcesAssembler<ProductForSearch> pagedResourcesAssembler) {
        Page<ProductForSearch> page = searchService.search(query);
        PagedModel<EntityModel<ProductForSearch>> pagedModel = pagedResourcesAssembler.toModel(page);
        return ResponseEntity.ok(pagedModel);
    }

}
