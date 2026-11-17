package com.mycompany.mavenproject1;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Backend sandbox Midtrans lokal untuk development.
 * Jangan gunakan server lokal ini untuk transaksi produksi.
 */
public final class MidtransServer {
    private static final Gson JSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10)).build();
    private static final String SNAP_URL = "https://app.sandbox.midtrans.com/snap/v1/transactions";
    private static final String STATUS_URL = "https://api.sandbox.midtrans.com/v2/";

    private MidtransServer() { }

    public static void main(String[] args) throws IOException {
        String key = System.getenv("MIDTRANS_SERVER_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Atur environment variable MIDTRANS_SERVER_KEY dengan Sandbox Server Key.");
        }
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8765), 0);
        server.createContext("/api/payments", exchange -> handle(exchange, key));
        server.createContext("/api/health", exchange -> health(exchange));
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "midtrans-pending-reconciliation");
            thread.setDaemon(true);
            return thread;
        }).scheduleWithFixedDelay(() -> reconcilePending(key), 10, 10, TimeUnit.SECONDS);
        System.out.println("Backend Midtrans SANDBOX aktif di http://127.0.0.1:8765");
    }

    private static void reconcilePending(String key) {
        List<String> orderIds = new ArrayList<>();
        List<String> orphanOrderIds = new ArrayList<>();
        try (Connection c = Koneksi.get(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT order_id FROM midtrans_payment WHERE transaction_status='pending'")) {
            while (rs.next()) orderIds.add(rs.getString(1));
        } catch (SQLException ex) {
            System.err.println("Pemeriksaan transaksi Midtrans tertunda: " + ex.getMessage());
            return;
        }
        try (Connection c = Koneksi.get(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT t.kode_transaksi FROM transaksi t WHERE t.status='pending' "
                     + "AND t.tanggal < NOW() - INTERVAL 2 MINUTE "
                     + "AND NOT EXISTS (SELECT 1 FROM midtrans_payment mp WHERE mp.id_transaksi=t.id_transaksi)")) {
            while (rs.next()) orphanOrderIds.add(rs.getString(1));
        } catch (SQLException ex) {
            System.err.println("Gagal memeriksa order pending tanpa token: " + ex.getMessage());
        }
        for (String orderId : orphanOrderIds) {
            try { cancelPending(orderId); }
            catch (SQLException ex) { System.err.println("Gagal melepas stok order " + orderId + ": " + ex.getMessage()); }
        }
        for (String orderId : orderIds) {
            try { getStatus(orderId, key); }
            catch (Exception ex) { System.err.println("Gagal memperbarui status " + orderId + ": " + safeMessage(ex)); }
        }
    }

    private static void handle(HttpExchange exchange, String key) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) && "/api/payments/snap".equals(path)) {
                JsonObject body = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject result = createSnap(body.get("order_id").getAsString(), key);
                send(exchange, 200, result);
                return;
            }
            String prefix = "/api/payments/";
            String suffix = "/status";
            String cancelSuffix = "/cancel";
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) && path.startsWith(prefix) && path.endsWith(cancelSuffix)) {
                String orderId = java.net.URLDecoder.decode(path.substring(prefix.length(), path.length() - cancelSuffix.length()), StandardCharsets.UTF_8);
                cancelPending(orderId);
                send(exchange, 200, new JsonObject());
                return;
            }
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && path.startsWith(prefix) && path.endsWith(suffix)) {
                String orderId = java.net.URLDecoder.decode(path.substring(prefix.length(), path.length() - suffix.length()), StandardCharsets.UTF_8);
                send(exchange, 200, getStatus(orderId, key));
                return;
            }
            send(exchange, 404, error("Endpoint tidak ditemukan."));
        } catch (IllegalArgumentException ex) {
            send(exchange, 400, error(ex.getMessage()));
        } catch (Exception ex) {
            send(exchange, 500, error("Gagal memproses Midtrans: " + safeMessage(ex)));
        } finally {
            exchange.close();
        }
    }

    private static void health(HttpExchange exchange) throws IOException {
        try (Connection c = Koneksi.get(); Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM midtrans_payment")) { rs.next(); }
            try (ResultSet rs = s.executeQuery("SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pembayaran' AND COLUMN_NAME='metode'")) {
                if (!rs.next() || !rs.getString(1).contains("midtrans")) {
                    throw new SQLException("Metode Midtrans belum ditambahkan ke database.");
                }
            }
            JsonObject result = new JsonObject();
            result.addProperty("ok", true);
            send(exchange, 200, result);
        } catch (Exception ex) {
            send(exchange, 503, error("Database belum siap. Jalankan database/migrations/004_midtrans.sql dulu."));
        } finally {
            exchange.close();
        }
    }

    private static JsonObject createSnap(String orderId, String key) throws Exception {
        if (orderId == null || orderId.isBlank() || orderId.length() > 50) throw new IllegalArgumentException("Kode transaksi tidak valid.");
        long amount;
        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT total_harga, status FROM transaksi WHERE kode_transaksi=?")) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Transaksi tidak ditemukan.");
                if (!"pending".equalsIgnoreCase(rs.getString("status"))) throw new IllegalArgumentException("Transaksi bukan berstatus pending.");
                amount = Math.round(rs.getDouble("total_harga"));
            }
        }

        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT snap_token, redirect_url FROM midtrans_payment WHERE order_id=?")) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString("snap_token") != null) {
                    JsonObject saved = new JsonObject();
                    saved.addProperty("token", rs.getString("snap_token"));
                    saved.addProperty("redirect_url", rs.getString("redirect_url"));
                    return saved;
                }
            }
        }

        JsonObject details = new JsonObject();
        details.addProperty("order_id", orderId);
        details.addProperty("gross_amount", amount);
        JsonObject payload = new JsonObject();
        payload.add("transaction_details", details);
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create(SNAP_URL))
                .timeout(java.time.Duration.ofSeconds(25))
                .header("Authorization", authorization(key))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(payload))).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) throw new IllegalStateException("Midtrans menolak pembuatan pembayaran (HTTP " + response.statusCode() + ").");
        JsonObject midtrans = JsonParser.parseString(response.body()).getAsJsonObject();
        String token = midtrans.get("token").getAsString();
        String redirect = midtrans.get("redirect_url").getAsString();
        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO midtrans_payment (id_transaksi, order_id, snap_token, redirect_url, transaction_status) "
              + "SELECT id_transaksi, ?, ?, ?, 'pending' FROM transaksi WHERE kode_transaksi=? "
              + "ON DUPLICATE KEY UPDATE snap_token=VALUES(snap_token), redirect_url=VALUES(redirect_url)")) {
            ps.setString(1, orderId);
            ps.setString(2, token);
            ps.setString(3, redirect);
            ps.setString(4, orderId);
            ps.executeUpdate();
        }
        JsonObject result = new JsonObject();
        result.addProperty("token", token);
        result.addProperty("redirect_url", redirect);
        return result;
    }

    private static JsonObject getStatus(String orderId, String key) throws Exception {
        String url = STATUS_URL + URLEncoder.encode(orderId, StandardCharsets.UTF_8) + "/status";
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(15)).header("Authorization", authorization(key))
                .header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            JsonObject pending = new JsonObject();
            pending.addProperty("transaction_status", "pending");
            pending.addProperty("message", "Pembayaran belum dipilih atau belum tercatat oleh Midtrans.");
            return pending;
        }
        if (response.statusCode() != 200) throw new IllegalStateException("Gagal mengambil status pembayaran (HTTP " + response.statusCode() + ").");
        JsonObject status = JsonParser.parseString(response.body()).getAsJsonObject();
        String state = value(status, "transaction_status", "pending");
        String paymentType = value(status, "payment_type", null);
        String fraud = value(status, "fraud_status", null);
        updateLocalStatus(orderId, state, paymentType, fraud);
        JsonObject result = new JsonObject();
        result.addProperty("transaction_status", state);
        try (Connection c = Koneksi.get(); PreparedStatement ps = c.prepareStatement("SELECT status FROM transaksi WHERE kode_transaksi=?")) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                result.addProperty("order_status", rs.next() ? rs.getString(1) : "pending");
            }
        }
        if (paymentType != null) result.addProperty("payment_type", paymentType);
        if (fraud != null) result.addProperty("fraud_status", fraud);
        return result;
    }

    private static void updateLocalStatus(String orderId, String state, String paymentType, String fraud) throws SQLException {
        boolean paid = "settlement".equalsIgnoreCase(state)
                || ("capture".equalsIgnoreCase(state) && "accept".equalsIgnoreCase(fraud));
        boolean failed = "deny".equalsIgnoreCase(state) || "expire".equalsIgnoreCase(state) || "cancel".equalsIgnoreCase(state);
        try (Connection c = Koneksi.get()) {
            c.setAutoCommit(false);
            try {
                int idTrx;
                String trxStatus;
                double total;
                try (PreparedStatement ps = c.prepareStatement("SELECT id_transaksi, status, total_harga FROM transaksi WHERE kode_transaksi=? FOR UPDATE")) {
                    ps.setString(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Transaksi lokal tidak ditemukan.");
                        idTrx = rs.getInt("id_transaksi");
                        trxStatus = rs.getString("status");
                        total = rs.getDouble("total_harga");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE midtrans_payment SET transaction_status=?, payment_type=?, fraud_status=? WHERE order_id=?")) {
                    ps.setString(1, state);
                    ps.setString(2, paymentType);
                    ps.setString(3, fraud);
                    ps.setString(4, orderId);
                    ps.executeUpdate();
                }
                if (paid && "pending".equalsIgnoreCase(trxStatus)) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO pembayaran (id_transaksi, metode, jumlah_bayar, kembalian) VALUES (?, 'midtrans', ?, 0)")) {
                        ps.setInt(1, idTrx);
                        ps.setDouble(2, total);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = c.prepareStatement("UPDATE transaksi SET status='selesai' WHERE id_transaksi=? AND status='pending'")) {
                        ps.setInt(1, idTrx);
                        ps.executeUpdate();
                    }
                } else if (failed && "pending".equalsIgnoreCase(trxStatus)) {
                    restoreReservedStock(c, idTrx);
                    try (PreparedStatement ps = c.prepareStatement("UPDATE transaksi SET status='batal' WHERE id_transaksi=? AND status='pending'")) {
                        ps.setInt(1, idTrx);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private static void cancelPending(String orderId) throws SQLException {
        try (Connection c = Koneksi.get()) {
            c.setAutoCommit(false);
            try {
                int idTrx;
                try (PreparedStatement ps = c.prepareStatement("SELECT id_transaksi FROM transaksi WHERE kode_transaksi=? AND status='pending' FOR UPDATE")) {
                    ps.setString(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { c.commit(); return; }
                        idTrx = rs.getInt(1);
                    }
                }
                restoreReservedStock(c, idTrx);
                try (PreparedStatement ps = c.prepareStatement("UPDATE transaksi SET status='batal' WHERE id_transaksi=? AND status='pending'")) {
                    ps.setInt(1, idTrx);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE midtrans_payment SET transaction_status='cancel' WHERE order_id=?")) {
                    ps.setString(1, orderId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private static void restoreReservedStock(Connection c, int idTrx) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id_menu, jumlah FROM transaksi_detail WHERE id_transaksi=?")) {
            ps.setInt(1, idTrx);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try (PreparedStatement stock = c.prepareStatement("UPDATE menu SET stok=stok+? WHERE id_menu=?")) {
                        stock.setInt(1, rs.getInt("jumlah"));
                        stock.setInt(2, rs.getInt("id_menu"));
                        stock.executeUpdate();
                    }
                }
            }
        }
    }

    private static String authorization(String key) {
        return "Basic " + Base64.getEncoder().encodeToString((key + ":").getBytes(StandardCharsets.UTF_8));
    }

    private static String value(JsonObject object, String property, String fallback) {
        return object.has(property) && !object.get(property).isJsonNull() ? object.get(property).getAsString() : fallback;
    }

    private static JsonObject error(String message) {
        JsonObject result = new JsonObject();
        result.addProperty("error", message == null ? "Permintaan tidak valid." : message);
        return result;
    }

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null ? ex.getClass().getSimpleName() : message;
    }

    private static void send(HttpExchange exchange, int code, JsonObject body) throws IOException {
        byte[] data = JSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(code, data.length);
        exchange.getResponseBody().write(data);
    }
}
