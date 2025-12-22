package ru.practicum.moviehub.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath(); // /movies or /movies/{id}
        String query = ex.getRequestURI().getQuery(); // year=...

        // Разбор пути
        String[] parts = path.split("/");
        // parts: ["", "movies"] или ["", "movies", "{id}"]

        if (parts.length == 2) {
            // /movies
            if (method.equalsIgnoreCase("GET")) {
                handleGetAll(ex, query);
                return;
            }
            if (method.equalsIgnoreCase("POST")) {
                handlePost(ex);
                return;
            }

            sendJson(ex, 405, new ErrorResponse("Method Not Allowed"));
            return;
        }

        if (parts.length == 3) {
            String idPart = parts[2];

            if (method.equalsIgnoreCase("GET")) {
                handleGetById(ex, idPart);
                return;
            }
            if (method.equalsIgnoreCase("DELETE")) {
                handleDelete(ex, idPart);
                return;
            }

            sendJson(ex, 405, new ErrorResponse("Method Not Allowed"));
            return;
        }

        // всё остальное
        sendJson(ex, 404, new ErrorResponse("Not Found"));
    }

    private void handleGetAll(HttpExchange ex, String query) throws IOException {
        if (query == null || query.isBlank()) {
            sendJson(ex, 200, store.getAll());
            return;
        }

        // ожидаем только year=YYYY
        String[] kv = query.split("=", 2);
        if (kv.length != 2 || !kv[0].equals("year")) {
            sendJson(ex, 400, new ErrorResponse("Некорректный параметр запроса — 'year'."));
            return;
        }

        int year;
        try {
            year = Integer.parseInt(kv[1]);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, new ErrorResponse("Некорректный параметр запроса — 'year'."));
            return;
        }

        sendJson(ex, 200, store.getByYear(year));
    }

    private void handlePost(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            sendStatus(ex, 415);
            return;
        }

        String bodyText;
        try {
            bodyText = readBody(ex);
        } catch (Exception e) {
            sendJson(ex, 400, new ErrorResponse("Некорректный JSON."));
            return;
        }

        JsonObject obj;
        try {
            JsonElement el = JsonParser.parseString(bodyText);
            if (!el.isJsonObject()) {
                sendJson(ex, 400, new ErrorResponse("Некорректный JSON."));
                return;
            }
            obj = el.getAsJsonObject();
        } catch (Exception e) {
            sendJson(ex, 400, new ErrorResponse("Некорректный JSON."));
            return;
        }

        String title = null;
        Integer year = null;

        try {
            if (obj.has("title") && !obj.get("title").isJsonNull()) {
                title = obj.get("title").getAsString();
            }
        } catch (Exception ignored) {
        }

        try {
            if (obj.has("year") && !obj.get("year").isJsonNull()) {
                year = obj.get("year").getAsInt();
            }
        } catch (Exception ignored) {
        }

        List<String> details = validate(title, year);
        if (!details.isEmpty()) {
            sendJson(ex, 422, new ErrorResponse("Ошибка валидации", details));
            return;
        }

        Movie created = store.add(title.trim(), year);
        sendJson(ex, 201, created);
    }

    private List<String> validate(String title, Integer year) {
        List<String> details = new ArrayList<>();

        if (title == null || title.trim().isEmpty()) {
            details.add("название не должно быть пустым");
        } else if (title.trim().length() > 100) {
            details.add("название не должно быть длиннее 100 символов");
        }

        int minYear = 1888;
        int maxYear = Year.now().getValue() + 1;

        if (year == null) {
            details.add("год должен быть между " + minYear + " и " + maxYear);
        } else if (year < minYear || year > maxYear) {
            details.add("год должен быть между " + minYear + " и " + maxYear);
        }

        return details;
    }

    private void handleGetById(HttpExchange ex, String idPart) throws IOException {
        long id;
        try {
            id = Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, new ErrorResponse("Некорректный ID."));
            return;
        }

        Movie movie = store.getById(id);
        if (movie == null) {
            sendJson(ex, 404, new ErrorResponse("Фильм не найден."));
            return;
        }

        sendJson(ex, 200, movie);
    }

    private void handleDelete(HttpExchange ex, String idPart) throws IOException {
        long id;
        try {
            id = Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, new ErrorResponse("Некорректный ID."));
            return;
        }

        boolean deleted = store.delete(id);
        if (!deleted) {
            sendJson(ex, 404, new ErrorResponse("Фильм не найден."));
            return;
        }

        sendNoContent(ex);
    }
}