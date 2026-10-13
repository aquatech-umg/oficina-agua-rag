package gt.edu.umg.aquatech.ragservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import gt.edu.umg.aquatech.ragservice.api.model.Fuente;
import gt.edu.umg.aquatech.ragservice.historial.DynamoDbProperties;
import gt.edu.umg.aquatech.ragservice.historial.HistorialDynamoDb;
import gt.edu.umg.aquatech.ragservice.llm.LlmProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;

@EnabledIfEnvironmentVariable(named = "DYNAMODB_IT", matches = "true")
class HistorialDynamoDbIntegracionTests {

    @Test
    void guardaYConsultaConversacionCompletaEnOrden() throws InterruptedException {

        try (DynamoDbClient cliente = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8010"))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("local", "local")))
                .build()) {

            DynamoDbProperties properties =
                    new DynamoDbProperties(
                            "http://localhost:8010",
                            "chatbot-historial",
                            "us-east-1");

            LlmProperties llmProperties =
                    new LlmProperties(
                            "http://localhost:11434",
                            "qwen3:8b",
                            "bge-m3",
                            1024);

            HistorialDynamoDb historial =
                    new HistorialDynamoDb(
                            cliente,
                            properties,
                            llmProperties);

            UUID conversacionId = UUID.randomUUID();

            Fuente fuente = new Fuente();
            fuente.setTitulo("Prueba AQ-115");
            fuente.setFragmento("Fragmento de prueba para DynamoDB.");

            historial.guardar(
                    conversacionId,
                    "Primera pregunta AQ-115",
                    "Primera respuesta AQ-115",
                    List.of(fuente));

            Thread.sleep(10);

            historial.guardar(
                    conversacionId,
                    "Segunda pregunta AQ-115",
                    "Segunda respuesta AQ-115",
                    List.of(fuente));

            var items = historial.consultar(conversacionId);

            assertEquals(2, items.size());

            assertEquals(
                    "Primera pregunta AQ-115",
                    items.get(0).get("pregunta").s());

            assertEquals(
                    "Segunda pregunta AQ-115",
                    items.get(1).get("pregunta").s());

            assertEquals(
                    "Primera respuesta AQ-115",
                    items.get(0).get("respuesta").s());

            assertEquals(
                    "Segunda respuesta AQ-115",
                    items.get(1).get("respuesta").s());

            for (var item : items) {
                assertEquals(
                        conversacionId.toString(),
                        item.get("conversacionId").s());

                cliente.deleteItem(
                        DeleteItemRequest.builder()
                                .tableName("chatbot-historial")
                                .key(Map.of(
                                        "conversacionId",
                                        AttributeValue.builder()
                                                .s(conversacionId.toString())
                                                .build(),
                                        "orden",
                                        AttributeValue.builder()
                                                .s(item.get("orden").s())
                                                .build()))
                                .build());
            }
        }
    }
}