package com.myshop.searchservice.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.myshop.searchservice.DTO.ProductForSearch;
import com.myshop.searchservice.DTO.SearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchService {

    private final  ElasticsearchClient  elasticsearchClient;

    private String sessionSeed = String.valueOf(System.currentTimeMillis());


    public SearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public Page<ProductForSearch> search(SearchRequest searchRequest) {

        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize());

        if (searchRequest.getQuery() == null || searchRequest.getQuery().isEmpty()) {
            return getAllProductsRandomOrder(pageable);
        }

        BoolQuery.Builder bool = getBuilder(searchRequest);



        if(bool == null) {
            return Page.empty(pageable);
        }

        try {
            SearchResponse<ProductForSearch> response = elasticsearchClient.search(s -> s
                            .index("products")
                            .query(bool.build()._toQuery())
                            .from((int) pageable.getOffset())
                            .size(pageable.getPageSize())
                            .trackTotalHits(t -> t.enabled(true)),
                    ProductForSearch.class
            );
            log.info("Поиск окончен");

            // Извлекаем список найденных объектов
            List<ProductForSearch> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());


            long totalHits = response.hits().total().value();



            // Возвращаем Page
            return new PageImpl<>(results, pageable, totalHits);

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

//        RangeQuery.Builder range = new RangeQuery.Builder().field("price");
//        range.gte(JsonData.of(searchRequest.getMinPrice()));
//        range.lte(JsonData.of(searchRequest.getMaxPrice()));
//
//        bool.filter(range.build()._toQuery());



        return bool;
    }


    private Page<ProductForSearch> getAllProductsRandomOrder(Pageable pageable) {
        try {
            SearchResponse<ProductForSearch> response = elasticsearchClient.search(s -> s
                            .index("products")
                            .query(q -> q
                                    .functionScore(fs -> fs
                                            .query(q2 -> q2.matchAll(m -> m))
                                            .functions(f -> f
                                                    .randomScore(rs -> rs
                                                            .seed(sessionSeed)
                                                            .field("_seq_no")
                                                    )
                                            )
                                            .boostMode(FunctionBoostMode.Replace)
                                    )
                            )
                            .from((int) pageable.getOffset())
                            .size(pageable.getPageSize())
                            .trackTotalHits(t -> t.enabled(true)),
                    ProductForSearch.class
            );

            List<ProductForSearch> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            long totalHits = response.hits().total().value();

            return new PageImpl<>(results, pageable, totalHits);

        } catch (IOException e) {
            log.error("Ошибка при получении всех товаров");
            throw new RuntimeException("Ошибка при получении товаров из Elasticsearch", e);
        }
    }


    @Scheduled(fixedRate = 3600000) // 3600000 мс = 1 час
    public void updateSessionSeed() {
        this.sessionSeed = String.valueOf(System.currentTimeMillis());
        log.info("updateSessionSeed");
    }
}
