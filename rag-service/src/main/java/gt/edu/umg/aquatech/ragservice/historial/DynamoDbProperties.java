package gt.edu.umg.aquatech.ragservice.historial;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dynamodb")
public record DynamoDbProperties(
        String endpoint,
        String tableName,
        String region) {
}