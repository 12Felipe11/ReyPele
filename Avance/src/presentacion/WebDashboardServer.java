package presentacion;

import aplicacion.StadiumController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dominio.SensorData;
import dominio.StadiumFacade;
import dominio.actuator.StadiumZone;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Dashboard web en tiempo real.
 * Sirve una pagina HTML que consulta /api/status cada segundo
 * para mostrar los sensores del Arduino de forma bonita y estructurada.
 */
public class WebDashboardServer {

    private final StadiumFacade facade;
    private final StadiumController controller;
    private final int port;
    private HttpServer server;

    public WebDashboardServer(StadiumFacade facade, StadiumController controller, int port) {
        this.facade = facade;
        this.controller = controller;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        final String JS = "application/javascript; charset=utf-8";
        server.createContext("/",               new IndexHandler());
        server.createContext("/style.css",      new StaticFileHandler("/style.css",      "text/css; charset=utf-8"));
        server.createContext("/app.js",         new StaticFileHandler("/app.js",         JS));
        server.createContext("/dashboard.js",   new StaticFileHandler("/dashboard.js",   JS));
        server.createContext("/iluminacion.js", new StaticFileHandler("/iluminacion.js", JS));
        server.createContext("/alarmas.js",     new StaticFileHandler("/alarmas.js",     JS));
        server.createContext("/analytics.js",   new StaticFileHandler("/analytics.js",   JS));
        server.createContext("/audit.js",       new StaticFileHandler("/audit.js",       JS));
        server.createContext("/config.js",      new StaticFileHandler("/config.js",      JS));
        server.createContext("/api/status",     new StatusHandler());
        server.createContext("/api/analytics",  new AnalyticsHandler());
        server.createContext("/api/history",    new HistoryHandler());
        server.createContext("/api/audit",      new AuditHandler());
        server.createContext("/api/debug",      new DebugHandler());
        server.createContext("/api/command",    new CommandHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        String url = "http://localhost:" + port + "/";
        System.out.println("  [WEB] Dashboard disponible en " + url);
        openBrowser(url);
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignored) {}
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            System.out.println("  [WEB] Abra manualmente: " + url);
        }
    }

    // ---------------- Handlers ----------------

    private class IndexHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            if (!"/".equals(path) && !"/index.html".equals(path)) {
                ex.sendResponseHeaders(404, -1);
                return;
            }
            serveClasspathResource(ex, "/index.html", "text/html; charset=utf-8");
        }
    }

    private class StaticFileHandler implements HttpHandler {
        private final String resource;
        private final String contentType;
        StaticFileHandler(String resource, String contentType) {
            this.resource = resource;
            this.contentType = contentType;
        }
        @Override public void handle(HttpExchange ex) throws IOException {
            serveClasspathResource(ex, resource, contentType);
        }
    }

    private void serveClasspathResource(HttpExchange ex, String path, String contentType) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                ex.sendResponseHeaders(404, -1);
                return;
            }
            byte[] body = is.readAllBytes();
            ex.getResponseHeaders().add("Content-Type", contentType);
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String json;
            try {
                SensorData data = facade.readAllSensors();
                List<String> actions = facade.evaluateRules();
                json = buildJson(data, actions);
            } catch (Exception e) {
                json = "{\"error\":\"" + escape(e.getMessage()) + "\"}";
            }
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.getResponseHeaders().add("Cache-Control", "no-store");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }
    }

    private class AnalyticsHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String json = facade.getDashboardAnalytics();
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.getResponseHeaders().add("Cache-Control", "no-store");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }
    }

    private class HistoryHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String query  = ex.getRequestURI().getQuery();
            String type   = "occupancy";
            String period = "day";
            if (query != null) {
                for (String p : query.split("&")) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2) {
                        if ("type".equals(kv[0]))   type   = kv[1];
                        if ("period".equals(kv[0])) period = kv[1];
                    }
                }
            }
            String json = facade.getHistory(type, period);
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.getResponseHeaders().add("Cache-Control", "no-store");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }
    }

    private class DebugHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String json = facade.getDebugInfo();
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.getResponseHeaders().add("Cache-Control", "no-store");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }
    }

    private class AuditHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            String query  = ex.getRequestURI().getQuery();
            String filter = "";
            int    limit  = 500;
            int    offset = 0;
            if (query != null) {
                for (String p : query.split("&")) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2) {
                        switch (kv[0]) {
                            case "filter": filter = kv[1]; break;
                            case "limit":  try { limit  = Integer.parseInt(kv[1]); } catch (NumberFormatException ignored) {} break;
                            case "offset": try { offset = Integer.parseInt(kv[1]); } catch (NumberFormatException ignored) {} break;
                        }
                    }
                }
            }
            String json = facade.getAuditLog(filter, limit, offset);
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.getResponseHeaders().add("Cache-Control", "no-store");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }
    }

    private class CommandHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1);
                return;
            }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
            String cmd = extractCommand(body);
            String result = (cmd == null || cmd.isEmpty())
                    ? "  Comando vacio."
                    : controller.processCommand(cmd);
            if (result == null) result = "  EXIT recibido (ignorado por dashboard).";
            String json = "{\"ok\":true,\"output\":\"" + escape(result) + "\"}";
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(200, out.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(out); }
        }

        private String extractCommand(String body) {
            int i = body.indexOf("\"cmd\"");
            if (i < 0) return body;
            int c = body.indexOf(':', i);
            int q1 = body.indexOf('"', c + 1);
            int q2 = body.indexOf('"', q1 + 1);
            if (q1 < 0 || q2 < 0) return "";
            return body.substring(q1 + 1, q2);
        }
    }

    // ---------------- JSON helpers ----------------

    private String buildJson(SensorData data, List<String> actions) {
        StringBuilder sb = new StringBuilder(512);
        sb.append('{');
        sb.append("\"timestamp\":").append(System.currentTimeMillis()).append(',');
        sb.append("\"distanceCm\":").append(data.getDistanceCm()).append(',');
        sb.append("\"distanceDesc\":\"").append(escape(facade.getDistanceSensor().getDistanceDescription())).append("\",");
        sb.append("\"presence\":").append(data.isPresenceDetected()).append(',');
        sb.append("\"entryCount\":").append(data.getEntryCount()).append(',');
        sb.append("\"occupancyThreshold\":").append(facade.getConfig().getOccupancyThreshold()).append(',');
        sb.append("\"distanceThreshold\":").append(facade.getConfig().getDistanceThreshold()).append(',');
        sb.append("\"lightIntensity\":").append(data.getLightIntensity()).append(',');
        sb.append("\"alarmActive\":").append(facade.getAlarmActuator().isActive()).append(',');
        sb.append("\"mode\":\"").append(escape(facade.getCurrentMode().getModeName())).append("\",");
        sb.append("\"hardwareConnected\":").append(facade.getHardware().isConnected()).append(',');
        sb.append("\"actions\":[");
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escape(actions.get(i))).append('"');
        }
        sb.append("],");
        // Zonas de iluminacion
        sb.append("\"zones\":[");
        boolean firstZone = true;
        for (Map.Entry<String, StadiumZone> e : facade.getZones().entrySet()) {
            if (!firstZone) sb.append(',');
            firstZone = false;
            StadiumZone z = e.getValue();
            sb.append("{\"name\":\"").append(e.getKey()).append("\",");
            sb.append("\"intensity\":").append(z.getIntensity()).append(",");
            sb.append("\"color\":\"").append(z.getColorHex()).append("\",");
            sb.append("\"active\":").append(z.isActive()).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
