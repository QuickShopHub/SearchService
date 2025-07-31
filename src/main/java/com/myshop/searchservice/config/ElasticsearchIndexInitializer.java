package com.myshop.searchservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchIndexInitializer {

    private final ElasticsearchClient client;

    public ElasticsearchIndexInitializer(ElasticsearchClient client) {
        this.client = client;
    }

    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        createProductIndexIfNotExists();
    }

    private void createProductIndexIfNotExists() {
        try {
            // Проверяем, существует ли индекс
            boolean exists = client.indices().exists(e -> e.index("ProductForSearch")).value();

            if (!exists) {
                // Создаём индекс
                client.indices().create(c -> c
                        .index("ProductForSearch")
                        .settings(s -> s
                                .numberOfShards("1")
                                .numberOfReplicas("0")
                        )
                        .mappings(m -> m
                                .properties("id", p -> p.keyword(k -> k))
                                .properties("name", p -> p.text(t -> t.analyzer("russian")))
                                .properties("description", p -> p.text(t -> t.analyzer("russian")))
                                .properties("price", p -> p.doubleRange(l -> l))
                                .properties("article", p -> p.text(t -> t.analyzer("russian")))
                                .properties("rating", p -> p.float_(l -> l))
                                .properties("quantitySold", p -> p.long_(l -> l))
                        )
                );
                System.out.println("Индекс 'ProductForSearch' создан");
            } else {
                System.out.println("Индекс 'ProductForSearch' уже существует");
            }
        } catch (Exception e) {
            System.err.println("Ошибка при создании индекса: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}