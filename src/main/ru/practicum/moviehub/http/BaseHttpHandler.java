package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {

    protected static final Gson gson = new Gson();

    protected void sendJson(HttpExchange ex, int statusCode, Object body) throws IOException {
        byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    protected void sendStatus(HttpExchange ex, int statusCode) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(statusCode, -1);
        ex.close();
    }

    protected String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
