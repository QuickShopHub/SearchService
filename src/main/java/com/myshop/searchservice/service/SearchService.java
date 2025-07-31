package com.myshop.searchservice.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.myshop.searchservice.DTO.ProductForSearch;
import com.myshop.searchservice.DTO.SearchRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    public final  ElasticsearchClient  elasticsearchClient;


    public SearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public List<ProductForSearch> search(SearchRequest searchRequest) {

        BoolQuery.Builder bool = new BoolQuery.Builder();

        // Поиск по имени и описанию
        if (searchRequest.getQuery() != null && !searchRequest.getQuery().isBlank()) {
            bool.should(sh -> sh.match(m -> m.field("name").query(searchRequest.getQuery()).boost(2.0f)));
            bool.should(sh -> sh.match(m -> m.field("description").query(searchRequest.getQuery())));
        }

        // Фильтр по цене
//        if (searchRequest.getMinPrice() != null || searchRequest.getMaxPrice() != null) {
//            RangeQuery.Builder range = new RangeQuery.Builder().field("price");
//            if (searchRequest.getMinPrice() != null) range.gte(searchRequest.getMinPrice());
//            if (searchRequest.getMaxPrice() != null) range.lte(searchRequest.getMaxPrice());
//            bool.filter(range.build()._toQuery());
//        }
        //и так далее


        try {
            SearchResponse<ProductForSearch> response = elasticsearchClient.search(s -> s
                            .index("products")
                            .query(bool.build()._toQuery())
                            .from(searchRequest.getPage() * searchRequest.getSize())
                            .size(searchRequest.getSize()),
                    ProductForSearch.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при поиске в Elasticsearch", e);
        }
    }

}
