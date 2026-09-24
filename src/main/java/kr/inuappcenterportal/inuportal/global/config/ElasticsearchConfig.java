package kr.inuappcenterportal.inuportal.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.time.Duration;

@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "kr.inuappcenterportal.inuportal.domain.search.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${ELASTICSEARCH_URIS:${SPRING_ELASTICSEARCH_URIS:${spring.elasticsearch.uris:http://localhost:9200}}}")
    private String elasticsearchUri;

    @Override
    public ClientConfiguration clientConfiguration() {
        String cleanUri = elasticsearchUri.replace("http://", "").replace("https://", "");
        log.info("[ElasticsearchConfig] Connecting to Elasticsearch at: '{}' (raw config: '{}')", cleanUri, elasticsearchUri);
        return ClientConfiguration.builder()
                .connectedTo(cleanUri)
                .withConnectTimeout(Duration.ofSeconds(5))
                .withSocketTimeout(Duration.ofSeconds(10))
                .build();
    }
}
