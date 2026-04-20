package infraestructura;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuracion de conexion a Supabase.
 * <p>
 * Se cargan dos valores: URL del proyecto (https://xxxx.supabase.co) y la
 * API key (anon o service_role). Orden de prioridad:
 * <ol>
 *   <li>Variables de entorno SUPABASE_URL / SUPABASE_KEY.</li>
 *   <li>Archivo .env en el working directory (KEY=value por linea).</li>
 *   <li>Archivo Avance/supabase/.env.</li>
 * </ol>
 * Si no se encuentran, {@link #isConfigured()} devuelve false y la app
 * arranca sin persistencia (ver NoOpSensorRepository).
 */
public class SupabaseConfig {

    private final String url;
    private final String key;

    private SupabaseConfig(String url, String key) {
        this.url = url;
        this.key = key;
    }

    public String getUrl() { return url; }
    public String getKey() { return key; }

    public boolean isConfigured() {
        return url != null && !url.isBlank() && key != null && !key.isBlank();
    }

    public static SupabaseConfig load() {
        String url = System.getenv("SUPABASE_URL");
        String key = System.getenv("SUPABASE_KEY");

        if (isBlank(url) || isBlank(key)) {
            String[] candidates = { ".env", "Avance/supabase/.env", "supabase/.env" };
            for (String path : candidates) {
                Path p = Paths.get(path);
                if (Files.isRegularFile(p)) {
                    try {
                        String[] fromFile = readEnvFile(p);
                        if (isBlank(url)) url = fromFile[0];
                        if (isBlank(key)) key = fromFile[1];
                    } catch (IOException e) {
                        System.err.println("  [SUPABASE] No se pudo leer " + p + ": " + e.getMessage());
                    }
                }
            }
        }

        if (!isBlank(url) && url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return new SupabaseConfig(url, key);
    }

    private static String[] readEnvFile(Path path) throws IOException {
        String url = null, key = null;
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int eq = trimmed.indexOf('=');
                if (eq <= 0) continue;
                String k = trimmed.substring(0, eq).trim();
                String v = trimmed.substring(eq + 1).trim();
                if (v.length() >= 2 &&
                        ((v.startsWith("\"") && v.endsWith("\"")) ||
                         (v.startsWith("'") && v.endsWith("'")))) {
                    v = v.substring(1, v.length() - 1);
                }
                if ("SUPABASE_URL".equals(k)) url = v;
                else if ("SUPABASE_KEY".equals(k)) key = v;
            }
        }
        return new String[] { url, key };
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
