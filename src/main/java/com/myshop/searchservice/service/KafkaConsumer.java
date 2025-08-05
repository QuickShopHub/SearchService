package com.myshop.searchservice.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;

import com.myshop.searchservice.DTO.DeleteDTO;
import com.myshop.searchservice.DTO.ProductForSearch;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.UUID;


@Slf4j
@Service
public class KafkaConsumer {

    public final ElasticsearchClient elasticsearchClient;

    public KafkaConsumer(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @KafkaListener(topics = "updateElastic", containerFactory = "kafkaListenerContainerFactoryUpdate")
    public void addNewProduct(ProductForSearch productForSearch) {
        try {
            log.info("Received message: {}", productForSearch);

            try {
                // Выполняем индексацию
                IndexResponse response = elasticsearchClient.index(builder -> builder
                        .index("products")                    // имя индекса
                        .id(productForSearch.getId().toString())         // используем ID из объекта (или можно не указывать — тогда ES сгенерирует)
                        .document(productForSearch)           // сам объект
                        .refresh(Refresh.True)                // делаем документ сразу доступным для поиска
                );
                log.info("Запись создана");

            } catch (Exception e) {
                throw new RuntimeException("Ошибка при сохранении продукта в Elasticsearch", e);
            }

        } catch (Exception e) {
            log.error("Error processing message: {}", productForSearch, e);
            throw e; // Для повторной обработки
        }
    }


    @KafkaListener(topics = "deleteElastic", containerFactory = "kafkaListenerContainerFactoryDelete")
    public void deleteProduct(DeleteDTO idsDTO) {
        List<UUID> ids = idsDTO.getIds();
        try {
            log.info("Received message: {}", ids);

            if (ids == null) {
                log.warn("No product IDs to delete");
                return;
            }

            try {
                BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();


                log.info("ids: {}", ids);
                ids.forEach(id -> {
                    bulkBuilder.operations(op -> op
                            .delete(del -> del
                                    .index("products")
                                    .id(id.toString())
                            )
                    );
                });


                BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());


                if (response.errors()) {
                    log.error("Some deletions failed:");
                    response.items().forEach(item -> {
                        if (item.error() != null) {
                            log.error("Failed to delete document with id: {}, error: {}",
                                    item.id(), item.error().reason());
                        }
                    });
                    throw new RuntimeException("Some documents failed to delete from Elasticsearch");
                }

                log.info("Successfully deleted {} products", ids.size());
            }
            catch (Exception e) {
                throw new RuntimeException("Ошибка при удалении из Elasticsearch", e);
            }


        }
        catch (Exception e) {
            log.error("Error processing message: {}", ids, e);
            throw e; // Для повторной обработки
        }
    }
}
