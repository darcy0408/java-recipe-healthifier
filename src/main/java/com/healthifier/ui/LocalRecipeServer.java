package com.healthifier.ui;

import com.healthifier.application.ConversionRequest;
import com.healthifier.application.ConversionService;
import com.healthifier.domain.ConversionResult;
import com.healthifier.domain.ConvertInput;
import com.healthifier.domain.RuleCompliance;
import com.healthifier.domain.RuleId;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LocalRecipeServer implements AutoCloseable {
    private static final int MAX_FORM_BYTES = 100_000;
    private final ConversionService conversionService;
    private final HttpServer server;
    private final ExecutorService executor;
    private final String csrfToken;

    public LocalRecipeServer(ConversionService conversionService, int port) throws IOException {
        if (port < 0 || port > 65_535) throw new IllegalArgumentException("port must be between 0 and 65535");
        this.conversionService = Objects.requireNonNull(conversionService, "conversionService");
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.server.setExecutor(executor);
        byte[] token = new byte[24];
        new SecureRandom().nextBytes(token);
        this.csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        server.createContext("/", this::handleHome);
        server.createContext("/convert", this::handleConvert);
    }

    public void start() { server.start(); }

    public int port() { return server.getAddress().getPort(); }

    public List<String> accessUrls() {
        List<String> urls = new ArrayList<>();
        urls.add("http://localhost:" + port());
        try {
            NetworkInterface.networkInterfaces()
                .filter(network -> {
                    try { return network.isUp() && !network.isLoopback() && !network.isVirtual(); }
                    catch (IOException ignored) { return false; }
                })
                .flatMap(NetworkInterface::inetAddresses)
                .filter(Inet4Address.class::isInstance)
                .map(address -> "http://" + address.getHostAddress() + ":" + port())
                .sorted()
                .forEach(urls::add);
        } catch (IOException ignored) {
            // Localhost remains usable if network interface discovery is unavailable.
        }
        return List.copyOf(urls);
    }

    public void awaitShutdown() throws InterruptedException {
        new CountDownLatch(1).await();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void handleHome(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, page("Method not allowed", error("Use GET for this page.")));
            return;
        }
        send(exchange, 200, page("Recipe Healthifier", form("", Set.of(), "", null)));
    }

    private void handleConvert(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, page("Method not allowed", error("Use the recipe form to convert a recipe.")));
            return;
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
            send(exchange, 415, page("Unsupported request", error("The form must use URL-encoded data.")));
            return;
        }
        byte[] requestBytes = exchange.getRequestBody().readNBytes(MAX_FORM_BYTES + 1);
        if (requestBytes.length > MAX_FORM_BYTES) {
            send(exchange, 413, page("Recipe too large", error("Recipe text must be smaller than 100 KB.")));
            return;
        }

        Map<String, List<String>> fields = parseForm(new String(requestBytes, StandardCharsets.UTF_8));
        String recipe = first(fields, "recipe").orElse("");
        String avoid = first(fields, "avoid").orElse("");
        Set<RuleId> selectedRules;
        try {
            selectedRules = parseRules(fields.getOrDefault("rule", List.of()));
        } catch (IllegalArgumentException exception) {
            send(exchange, 400, page("Invalid rule", form(recipe, Set.of(), avoid, exception.getMessage())));
            return;
        }
        if (!csrfToken.equals(first(fields, "csrf").orElse(null))) {
            send(exchange, 403, page("Request rejected", error("The form expired. Reload the page and try again.")));
            return;
        }
        if (recipe.isBlank() || (selectedRules.isEmpty() && avoid.isBlank())) {
            send(exchange, 400, page("Check the form", form(recipe, selectedRules, avoid,
                "Enter a recipe and select at least one health goal or custom avoidance.")));
            return;
        }
        try {
            ConvertInput input = new ConvertInput.Text(recipe, "local-web", false);
            ConversionRequest request = new ConversionRequest(input, selectedRules,
                avoid.isBlank() ? Optional.empty() : Optional.of(avoid));
            ConversionResult result = conversionService.convert(request);
            send(exchange, 200, page("Converted recipe", form(recipe, selectedRules, avoid, null)
                + renderResult(result)));
        } catch (IllegalArgumentException exception) {
            send(exchange, 400, page("Unable to convert recipe",
                form(recipe, selectedRules, avoid, exception.getMessage())));
        } catch (RuntimeException exception) {
            send(exchange, 500, page("Conversion failed",
                error("The recipe could not be converted. Check the terminal for details.")));
            exception.printStackTrace(System.err);
        }
    }

    private String form(String recipe, Set<RuleId> selected, String avoid, String message) {
        StringBuilder html = new StringBuilder();
        if (message != null) html.append(error(message));
        html.append("<form action=\"/convert\" method=\"post\">\n")
            .append("<input type=\"hidden\" name=\"csrf\" value=\"")
            .append(escape(csrfToken)).append("\">\n")
            .append("<div class=\"field\"><label for=\"recipe\">Recipe text <strong>(required)</strong></label>")
            .append("<p id=\"recipe-help\" class=\"hint\">Include a title, Ingredients section, and Instructions section.</p>")
            .append("<textarea id=\"recipe\" name=\"recipe\" rows=\"14\" maxlength=\"90000\" aria-describedby=\"recipe-help\" required>")
            .append(escape(recipe)).append("</textarea></div>\n")
            .append("<fieldset><legend>Health goals</legend><div class=\"goal-grid\">");
        for (RuleId rule : RuleId.values()) {
            html.append("<label class=\"goal\" for=\"rule-").append(rule.name()).append("\">")
                .append("<input type=\"checkbox\" id=\"rule-").append(rule.name())
                .append("\" name=\"rule\" value=\"").append(rule.name()).append("\"")
                .append(selected.contains(rule) ? " checked" : "")
                .append("><span>").append(label(rule)).append("</span></label>");
        }
        html.append("</div></fieldset>\n")
            .append("<div class=\"field\"><label for=\"avoid\">Custom ingredient to avoid <span class=\"optional\">(optional)</span></label>")
            .append("<input id=\"avoid\" name=\"avoid\" type=\"text\" maxlength=\"100\" value=\"")
            .append(escape(avoid)).append("\" autocomplete=\"off\"></div>")
            .append("<button type=\"submit\">Healthify this recipe</button></form>");
        return html.toString();
    }

    private static String renderResult(ConversionResult result) {
        StringBuilder html = new StringBuilder("<section class=\"result\" aria-labelledby=\"result-title\">")
            .append("<p class=\"eyebrow\">Converted recipe</p><h2 id=\"result-title\">")
            .append(escape(result.title())).append("</h2><p><strong>Servings:</strong> ")
            .append(escape(result.servings())).append("</p><h3>Ingredients</h3><ul>");
        result.ingredients().forEach(ingredient -> {
            html.append("<li>").append(escape(ingredient.text()));
            if (!ingredient.appliedSwaps().isEmpty()) {
                html.append("<small>Changed: ");
                for (int index = 0; index < ingredient.appliedSwaps().size(); index++) {
                    if (index > 0) html.append("; ");
                    var swap = ingredient.appliedSwaps().get(index);
                    html.append(escape(swap.from())).append(" → ").append(escape(swap.to()));
                }
                html.append("</small>");
            }
            html.append("</li>");
        });
        html.append("</ul><h3>Instructions</h3><ol>");
        result.steps().forEach(step -> html.append("<li>").append(escape(step.text())).append("</li>"));
        html.append("</ol><h3>Compliance</h3><ul class=\"compliance\">");
        result.ruleCompliance().forEach((rule, status) -> html.append("<li><span>")
            .append(escape(rule.replace('_', ' '))).append("</span><strong class=\"status ")
            .append(statusClass(status)).append("\">").append(escape(status.name().replace('_', ' ')))
            .append("</strong></li>"));
        html.append("</ul>");
        if (!result.unfixable().isEmpty()) {
            html.append("<div class=\"attention\"><h3>Needs attention</h3><ul>");
            result.unfixable().forEach(item -> html.append("<li>").append(escape(item)).append("</li>"));
            html.append("</ul></div>");
        }
        return html.append("<p class=\"disclaimer\">Compliance is based on recognized ingredient terms and is not medical or nutritional certification.</p></section>").toString();
    }

    private static String page(String title, String content) {
        return """
            <!doctype html>
            <html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>{{TITLE}} · Recipe Healthifier</title>
            <style>
            :root{color-scheme:light;--ink:#17332b;--muted:#52655f;--paper:#fffdf7;--panel:#fff;--line:#c8d4cf;--brand:#176b4d;--brand-dark:#0d4d37;--warm:#f4b942;--danger:#a7342d;--partial:#8a5a00}*{box-sizing:border-box}body{margin:0;background:linear-gradient(135deg,#edf6ef,#fff7e3);color:var(--ink);font-family:system-ui,-apple-system,"Segoe UI",sans-serif;line-height:1.5;accent-color:var(--brand)}main{width:min(100% - 2rem,70rem);margin:0 auto;padding-block:2rem 5rem}.hero{margin-block-end:1.5rem}.eyebrow{text-transform:uppercase;letter-spacing:.12em;font-weight:800;color:var(--brand);font-size:.78rem}h1{font-size:clamp(2rem,7vw,4.5rem);line-height:1;margin:.25rem 0}.lede{color:var(--muted);font-size:1.1rem;max-width:48rem}.card,.result{background:var(--panel);border:1px solid var(--line);border-radius:1rem;padding:clamp(1rem,4vw,2rem);box-shadow:0 1rem 3rem rgb(18 61 46/.1)}.result{margin-block-start:1.5rem}.field,fieldset{margin-block-end:1.5rem}label,legend{font-weight:750}label:not(.goal){display:block;margin-block-end:.35rem}.hint,.optional,.disclaimer{color:var(--muted);font-size:.9rem}.hint{margin:.15rem 0 .5rem}textarea,input[type=text]{display:block;width:100%;border:1px solid #899b94;border-radius:.6rem;padding:.8rem;font:inherit;font-size:1rem;background:#fff;color:var(--ink)}textarea{resize:vertical;min-height:14rem}fieldset{border:0;padding:0}legend{margin-block-end:.6rem}.goal-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(13rem,1fr));gap:.6rem}.goal{display:flex;align-items:center;gap:.65rem;min-height:48px;padding:.65rem .8rem;border:1px solid var(--line);border-radius:.6rem;background:var(--paper);cursor:pointer}.goal input{width:1.2rem;height:1.2rem;flex:none}button{min-height:50px;border:0;border-radius:.65rem;padding:.8rem 1.2rem;background:var(--brand);color:#fff;font:inherit;font-weight:800;cursor:pointer}button:hover{background:var(--brand-dark)}:focus-visible{outline:3px solid var(--warm);outline-offset:3px}.error,.attention{border-inline-start:.35rem solid var(--danger);background:#fff0ee;padding:.8rem 1rem;border-radius:.35rem;margin-block-end:1rem}.attention{margin-block-start:1rem}.result li{margin-block:.45rem}.result small{display:block;color:var(--muted)}.compliance{list-style:none;padding:0}.compliance li{display:flex;justify-content:space-between;gap:1rem;border-block-end:1px solid var(--line);padding-block:.55rem}.status{font-size:.78rem;padding:.2rem .55rem;border-radius:999px}.ok{color:#075b3d;background:#d9f6e8}.partial{color:#704900;background:#fff0c2}.no{color:#8b2520;background:#ffe0dd}@media(max-width:38rem){main{width:min(100% - 1rem,70rem);padding-block-start:1rem}.card,.result{border-radius:.7rem}.compliance li{align-items:center}}@media(prefers-reduced-motion:no-preference){button{transition:background-color .15s ease}}
            </style></head><body><main><header class="hero"><p class="eyebrow">Local · explainable · Java 26</p><h1>Recipe Healthifier</h1><p class="lede">Paste a recipe, choose your goals, and review every suggested ingredient change. Your recipe stays on this computer.</p></header><div class="card">{{CONTENT}}</div></main></body></html>
            """.replace("{{TITLE}}", escape(title)).replace("{{CONTENT}}", content);
    }

    private static String error(String message) {
        return "<div class=\"error\" role=\"alert\"><strong>Something needs attention.</strong><br>"
            + escape(message) + "</div>";
    }

    private static String statusClass(RuleCompliance status) {
        return switch (status) {
            case COMPLIANT -> "ok";
            case PARTIAL -> "partial";
            case NOT_POSSIBLE -> "no";
        };
    }

    private static String label(RuleId rule) {
        String words = rule.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static Set<RuleId> parseRules(List<String> values) {
        EnumSet<RuleId> rules = EnumSet.noneOf(RuleId.class);
        values.forEach(value -> rules.add(RuleId.valueOf(value)));
        return Set.copyOf(rules);
    }

    private static Map<String, List<String>> parseForm(String body) {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        if (body.isEmpty()) return fields;
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String name = decode(parts[0]);
            String value = parts.length == 2 ? decode(parts[1]) : "";
            fields.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }
        return fields;
    }

    private static Optional<String> first(Map<String, List<String>> fields, String name) {
        return fields.getOrDefault(name, List.of()).stream().findFirst();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/html; charset=utf-8");
        headers.set("Content-Security-Policy",
            "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
