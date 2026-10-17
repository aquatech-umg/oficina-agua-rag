package gt.edu.umg.aquatech.ragservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import gt.edu.umg.aquatech.ragservice.api.model.Fuente;
import gt.edu.umg.aquatech.ragservice.historial.DynamoDbProperties;
import gt.edu.umg.aquatech.ragservice.historial.HistorialDynamoDb;
import gt.edu.umg.aquatech.ragservice.llm.LlmProperties;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

class HistorialDynamoDbTests {

    private final DynamoDbClient dynamoDb = mock(DynamoDbClient.class);

    private final DynamoDbProperties properties =
            new DynamoDbProperties(
                    "http://localhost:8010",
                    "chatbot-historial",
                    "us-east-1");

    private final LlmProperties llmProperties =
            new LlmProperties(
                    "http://localhost:11434",
                    "qwen3:8b",
                    "bge-m3",
                    1024);

    private final HistorialDynamoDb historial =
            new HistorialDynamoDb(
                    dynamoDb,
                    properties,
                    llmProperties);

    @Test
    void creadoEnSiempreTieneTresDecimales() {

        assertEquals(
                "2026-10-12T20:31:05.000Z",
                HistorialDynamoDb.formatearFecha(
                        Instant.parse("2026-10-12T20:31:05Z")));

        assertEquals(
                "2026-10-12T20:31:05.120Z",
                HistorialDynamoDb.formatearFecha(
                        Instant.parse("2026-10-12T20:31:05.120Z")));
    }

    @Test
    void guardaIntercambioConClavesYContenidoEsperados() {

        Fuente fuente = new Fuente();
        fuente.setTitulo("Cálculo de consumo");
        fuente.setFragmento(
                "El consumo corresponde a la diferencia entre lecturas.");

        UUID conversacionId =
                UUID.fromString(
                        "7b2b8f5a-08a5-4c0e-9f63-2d7c92e72095");

        historial.guardar(
                conversacionId,
                "¿Cómo se calcula el consumo?",
                "Se calcula usando la diferencia entre lecturas.",
                List.of(fuente));

        ArgumentCaptor<PutItemRequest> captor =
                ArgumentCaptor.forClass(PutItemRequest.class);

        verify(dynamoDb).putItem(captor.capture());

        PutItemRequest request = captor.getValue();
        Map<String, AttributeValue> item = request.item();

        assertEquals(
                "chatbot-historial",
                request.tableName());

        assertEquals(
                conversacionId.toString(),
                item.get("conversacionId").s());

        assertEquals(
                "¿Cómo se calcula el consumo?",
                item.get("pregunta").s());

        assertEquals(
                "Se calcula usando la diferencia entre lecturas.",
                item.get("respuesta").s());

        assertEquals(
                "qwen3:8b",
                item.get("modelo").s());

        assertTrue(
                item.get("orden").s()
                        .startsWith(item.get("creadoEn").s() + "#"));

        assertTrue(
                item.get("creadoEn").s()
                        .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));

        assertEquals(
                1,
                item.get("fuentes").l().size());
    }

    @Test
    void consultaConversacionEnOrdenAscendente() {

        when(dynamoDb.query(any(QueryRequest.class)))
                .thenReturn(
                        QueryResponse.builder()
                                .items(List.of())
                                .build());

        UUID conversacionId = UUID.randomUUID();

        historial.consultar(conversacionId);

        ArgumentCaptor<QueryRequest> captor =
                ArgumentCaptor.forClass(QueryRequest.class);

        verify(dynamoDb).query(captor.capture());

        QueryRequest request = captor.getValue();

        assertEquals(
                "chatbot-historial",
                request.tableName());

        assertEquals(
                "conversacionId = :conversacionId",
                request.keyConditionExpression());

        assertTrue(request.scanIndexForward());
    }
}