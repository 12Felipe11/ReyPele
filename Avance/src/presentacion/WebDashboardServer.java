package presentacion;

import aplicacion.StadiumController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dominio.SensorData;
import dominio.StadiumFacade;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
        server.createContext("/",           new IndexHandler());
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/command", new CommandHandler());
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
            if (!"/".equals(ex.getRequestURI().getPath())) {
                ex.sendResponseHeaders(404, -1);
                return;
            }
            byte[] body = DashboardHtml.PAGE.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
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
