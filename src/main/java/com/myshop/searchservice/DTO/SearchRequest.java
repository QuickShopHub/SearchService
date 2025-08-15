package com.myshop.searchservice.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequest {

    private String query;

    //фильтры
    private int page;
    private int size;
    private double maxPrice = Double.MAX_VALUE;
    private double minPrice = Double.MIN_VALUE;
}
