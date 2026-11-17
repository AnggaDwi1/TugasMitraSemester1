package com.mycompany.mavenproject1;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Client kasir ke backend sandbox lokal; tidak memegang Server Key. */
final class MidtransClient {
    private static final String BASE_URL = "http://127.0.0.1:8765/api/payments";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    private MidtransClient() { }

    static void ensureReady() throws Exception {
        request(HttpRequest.newBuilder(URI.create("http://127.0.0.1:8765/api/health"))
                .timeout(Duration.ofSeconds(5)).GET().build());
    }

    static String createSnap(String orderId) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("order_id", orderId);
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/snap"))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build();
        JsonObject response = request(request);
        return response.get("redirect_url").getAsString();
    }

    static String checkStatus(String orderId) throws Exception {
        String encoded = java.net.URLEncoder.encode(orderId, StandardCharsets.UTF_8).replace("+", "%20");
        JsonObject response = request(HttpRequest.newBuilder(URI.create(BASE_URL + "/" + encoded + "/status"))
                .timeout(Duration.ofSeconds(20)).GET().build());
        return response.has("order_status") ? response.get("order_status").getAsString() : "pending";
    }

    static void cancelPending(String orderId) {
        try {
            String encoded = java.net.URLEncoder.encode(orderId, StandardCharsets.UTF_8).replace("+", "%20");
            request(HttpRequest.newBuilder(URI.create(BASE_URL + "/" + encoded + "/cancel"))
                    .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.noBody()).build());
        } catch (Exception ignored) { }
    }

    private static JsonObject request(HttpRequest request) throws Exception {
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = body.has("error") ? body.get("error").getAsString() : "Backend Midtrans tidak dapat memproses permintaan.";
            throw new IllegalStateException(message);
        }
        return body;
    }
}
