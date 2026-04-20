package infraestructura;

import dominio.SensorData;
import dominio.persistence.ISensorRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Repositorio Supabase basado en la API REST (PostgREST) expuesta en
 * {@code <project>/rest/v1/<tabla>}. No requiere drivers JDBC ni librerias
 * externas: usa {@link java.net.http.HttpClient} (Java 11+).
 * <p>
 * La escritura se hace en un hilo aparte (fire-and-forget) para no bloquear
 * el ciclo de lectura de sensores. Si la red cae, el error se registra pero
 * el sistema sigue operando.
 */
public class SupabaseRestRepository implements ISensorRepository {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final String url;
    private final String key;
    private final HttpClient http;

    public SupabaseRestRepository(SupabaseConfig config) {
        if (!config.isConfigured()) {
            throw new IllegalArgumentException("SupabaseConfig no tiene URL/KEY");
        }
        this.url = config.getUrl();
        this.key = config.getKey();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void saveReading(SensorData data) {
        String body = "{"
                + "\"entry_count\":" + data.getEntryCount() + ","
                + "\"distance_cm\":" + data.getDistanceCm() + ","
                + "\"presence_detected\":" + data.isPresenceDetected() + ","
                + "\"light_intensity\":" + data.getLightIntensity() + ","
                + "\"source\":\"arduino\""
                + "}";
        postAsync("sensor_readings", body, null);
    }

    @Override
    public void saveEvent(String kind, String payloadJson) {
        String safeKind = escape(kind == null ? "" : kind);
        String safePayload = (payloadJson == null || payloadJson.isBlank()) ? "{}" : payloadJson;
        String body = "{"
                + "\"kind\":\"" + safeKind + "\","
                + "\"payload\":" + safePayload
                + "}";
        postAsync("events", body, null);
    }

    @Override
    public void updateConfig(int occupancyThreshold, float distanceThreshold) {
        String body = "{"
                + "\"id\":1,"
                + "\"occupancy_threshold\":" + occupancyThreshold + ","
                + "\"distance_threshold\":" + distanceThreshold + ","
                + "\"updated_at\":\"" + java.time.Instant.now() + "\""
                + "}";
        // Upsert sobre la fila singleton (id=1).
        postAsync("stadium_config", body, "resolution=merge-duplicates");
    }

    /** POST/UPSERT asincrono contra /rest/v1/{table}. */
    private void postAsync(String table, String body, String extraPreferHeader) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url + "/rest/v1/" + table))
                .timeout(TIMEOUT)
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .header("Prefer", extraPreferHeader == null ? "return=minimal"
                                                            : "return=minimal," + extraPreferHeader)
                .POST(HttpRequest.BodyPublishers.ofString(body));

        HttpRequest req = builder.build();

        http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenAccept(resp -> {
                if (resp.statusCode() >= 300) {
                    System.err.println("  [SUPABASE] " + table + " -> HTTP "
                            + resp.statusCode() + ": " + resp.body());
                }
            })
            .exceptionally(err -> {
                System.err.println("  [SUPABASE] Error " + table + ": " + err.getMessage());
                return null;
            });
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
