package com.myshop.searchservice.DTO;




import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductForSearch {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String article;
    private BigDecimal rating;
    private long quantitySold;
    private String url;
    private long countComments;
}
