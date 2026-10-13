package gt.edu.umg.aquatech.ragservice.historial;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import gt.edu.umg.aquatech.ragservice.api.model.Fuente;
import gt.edu.umg.aquatech.ragservice.llm.LlmProperties;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

@Component
public class HistorialDynamoDb implements HistorialConversaciones {

    private static final DateTimeFormatter FORMATO_FECHA =
            new DateTimeFormatterBuilder()
                    .appendInstant(3)
                    .toFormatter();

    private final DynamoDbClient dynamoDb;
    private final DynamoDbProperties properties;
    private final LlmProperties llmProperties;

    public HistorialDynamoDb(
            DynamoDbClient dynamoDb,
            DynamoDbProperties properties,
            LlmProperties llmProperties) {

        this.dynamoDb = dynamoDb;
        this.properties = properties;
        this.llmProperties = llmProperties;
    }

    @Override
    public void guardar(
            UUID conversacionId,
            String pregunta,
            String respuesta,
            List<Fuente> fuentes) {

        String creadoEn = formatearFecha(Instant.now());
        UUID intercambioId = UUID.randomUUID();

        String orden = creadoEn + "#" + intercambioId;

        Map<String, AttributeValue> item = new HashMap<>();

        item.put(
                "conversacionId",
                AttributeValue.builder()
                        .s(conversacionId.toString())
                        .build());

        item.put(
                "orden",
                AttributeValue.builder()
                        .s(orden)
                        .build());

        item.put(
                "creadoEn",
                AttributeValue.builder()
                        .s(creadoEn)
                        .build());

        item.put(
                "intercambioId",
                AttributeValue.builder()
                        .s(intercambioId.toString())
                        .build());

        item.put(
                "pregunta",
                AttributeValue.builder()
                        .s(pregunta)
                        .build());

        item.put(
                "respuesta",
                AttributeValue.builder()
                        .s(respuesta)
                        .build());

        item.put(
                "fuentes",
                AttributeValue.builder()
                        .l(convertirFuentes(fuentes))
                        .build());

        item.put(
                "modelo",
                AttributeValue.builder()
                        .s(llmProperties.model())
                        .build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(properties.tableName())
                .item(item)
                .build();

        dynamoDb.putItem(request);
    }

    public List<Map<String, AttributeValue>> consultar(UUID conversacionId) {

        Map<String, AttributeValue> valores = Map.of(
                ":conversacionId",
                AttributeValue.builder()
                        .s(conversacionId.toString())
                        .build());

        QueryRequest request = QueryRequest.builder()
                .tableName(properties.tableName())
                .keyConditionExpression("conversacionId = :conversacionId")
                .expressionAttributeValues(valores)
                .scanIndexForward(true)
                .build();

        return dynamoDb.query(request).items();
    }

    public static String formatearFecha(Instant instant) {
        return FORMATO_FECHA.format(instant);
    }

    private List<AttributeValue> convertirFuentes(List<Fuente> fuentes) {

        if (fuentes == null || fuentes.isEmpty()) {
            return List.of();
        }

        List<AttributeValue> resultado = new ArrayList<>();

        for (Fuente fuente : fuentes) {

            Map<String, AttributeValue> mapa = new HashMap<>();

            if (fuente.getTitulo() != null) {
                mapa.put(
                        "titulo",
                        AttributeValue.builder()
                                .s(fuente.getTitulo())
                                .build());
            }

            if (fuente.getFragmento() != null) {
                mapa.put(
                        "fragmento",
                        AttributeValue.builder()
                                .s(fuente.getFragmento())
                                .build());
            }

            resultado.add(
                    AttributeValue.builder()
                            .m(mapa)
                            .build());
        }

        return resultado;
    }
}