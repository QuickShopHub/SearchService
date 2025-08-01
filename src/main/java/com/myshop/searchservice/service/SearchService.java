package com.myshop.searchservice.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.myshop.searchservice.DTO.ProductForSearch;
import com.myshop.searchservice.DTO.SearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchService {

    public final  ElasticsearchClient  elasticsearchClient;


    public SearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public List<ProductForSearch> search(SearchRequest searchRequest) {

        BoolQuery.Builder bool = getBuilder(searchRequest);

        if(bool == null) {
            return List.of();
        }

        try {
            SearchResponse<ProductForSearch> response = elasticsearchClient.search(s -> s
                            .index("products")
                            .query(bool.build()._toQuery())
                            .from(searchRequest.getPage() * searchRequest.getSize())
                            .size(searchRequest.getSize()),
                    ProductForSearch.class
            );
            log.info("Поиск окончен");
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Ошибка при поиске");
            throw new RuntimeException("Ошибка при поиске в Elasticsearch", e);
        }
    }


    private static BoolQuery.Builder getBuilder(SearchRequest searchRequest) {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        // Поиск по имени, описанию и артикулу
        if (searchRequest.getQuery() != null && !searchRequest.getQuery().isBlank()) {
            String query = searchRequest.getQuery().trim();

            // Проверяем, похоже ли на артикул: цифры, буквы, тире, подчёркивания, без пробелов
            if (query.matches("^[0-9]+$")) {
                // Считаем, что это артикул — ищем точное совпадение
                bool.must(m -> m.term(t -> t
                        .field("article")
                        .value(query)
                ));
            } else {
                // Обычный текст — используем fuzzy-поиск по name и description
                bool.should(sh -> sh.match(m -> m
                        .field("name")
                        .query(query)
                        .fuzziness("AUTO")
                        .prefixLength(1)
                        .boost(2.0f)
                ));
                bool.should(sh -> sh.match(m -> m
                        .field("description")
                        .query(query)
                        .fuzziness("AUTO")
                        .prefixLength(1)
                ));
            }
        }
        else{
            return null;
        }

        // Фильтр по цене
//        if (searchRequest.getMinPrice() != null || searchRequest.getMaxPrice() != null) {
//            RangeQuery.Builder range = new RangeQuery.Builder().field("price");
//            if (searchRequest.getMinPrice() != null) range.gte(searchRequest.getMinPrice());
//            if (searchRequest.getMaxPrice() != null) range.lte(searchRequest.getMaxPrice());
//            bool.filter(range.build()._toQuery());
//        }
        //и так далее


        return bool;
    }



    public ProductForSearch addNewProductForSearch(ProductForSearch productForSearch) {

        try {
            // Выполняем индексацию
            IndexResponse response = elasticsearchClient.index(builder -> builder
                    .index("products")                    // имя индекса
                    .id(productForSearch.getId().toString())         // используем ID из объекта (или можно не указывать — тогда ES сгенерирует)
                    .document(productForSearch)           // сам объект
                    .refresh(Refresh.True)                // делаем документ сразу доступным для поиска
            );
            log.info("Запись создана");
            return productForSearch; // возвращаем объект с ID

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сохранении продукта в Elasticsearch", e);
        }

    }

}
