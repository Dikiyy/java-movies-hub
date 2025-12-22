package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static MoviesServer server;
    private static MoviesStore store;

    private HttpClient client;
    private Gson gson;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        gson = new Gson();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));

        Type listType = new ListOfMoviesTypeToken().getType();
        List<Movie> movies = gson.fromJson(resp.body(), listType);
        assertEquals(0, movies.size());
    }

    @Test
    void postMovies_valid_createsMovie() throws Exception {
        String json = "{\"title\":\"Matrix\",\"year\":1999}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));

        Movie created = gson.fromJson(resp.body(), Movie.class);
        assertTrue(created.getId() > 0);
        assertEquals("Matrix", created.getTitle());
        assertEquals(1999, created.getYear());
    }

    @Test
    void getMovies_afterPost_returnsListWithMovie() throws Exception {
        // POST
        String json = "{\"title\":\"Matrix\",\"year\":1999}";
        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        client.send(post, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // GET
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();
        HttpResponse<String> resp =
                client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        Type listType = new ListOfMoviesTypeToken().getType();
        List<Movie> movies = gson.fromJson(resp.body(), listType);
        assertEquals(1, movies.size());
        assertEquals("Matrix", movies.get(0).getTitle());
    }

    @Test
    void postMovies_emptyTitle_returns422() throws Exception {
        int okYear = 2000;
        String json = "{\"title\":\"\",\"year\":" + okYear + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());

        ErrorResponse er = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", er.getError());
        assertNotNull(er.getDetails());
        assertFalse(er.getDetails().isEmpty());
    }

    @Test
    void postMovies_tooLongTitle_returns422() throws Exception {
        String longTitle = "a".repeat(101);
        int okYear = 2000;
        String json = "{\"title\":\"" + longTitle + "\",\"year\":" + okYear + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMovies_invalidYear_returns422() throws Exception {
        int tooSmall = 1800;
        String json = "{\"title\":\"Ok\",\"year\":" + tooSmall + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());

        int tooBig = Year.now().getValue() + 2;
        String json2 = "{\"title\":\"Ok\",\"year\":" + tooBig + "}";

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json2, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp2 =
                client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp2.statusCode());
    }

    @Test
    void postMovies_wrongContentType_returns415() throws Exception {
        String json = "{\"title\":\"Matrix\",\"year\":1999}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode());
    }

    @Test
    void postMovies_invalidJson_returns400() throws Exception {
        String bad = "{ title: }";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bad, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
    }

    @Test
    void getMovieById_found_returns200() throws Exception {
        // POST
        String json = "{\"title\":\"Matrix\",\"year\":1999}";
        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> postResp =
                client.send(post, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Movie created = gson.fromJson(postResp.body(), Movie.class);

        // GET /movies/{id}
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + created.getId()))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        Movie found = gson.fromJson(resp.body(), Movie.class);
        assertEquals(created.getId(), found.getId());
    }

    @Test
    void getMovieById_notFound_returns404() throws Exception {
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/999"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
    }

    @Test
    void getMovieById_notNumber_returns400() throws Exception {
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
    }

    @Test
    void deleteMovieById_existing_returns204() throws Exception {
        // POST
        String json = "{\"title\":\"Matrix\",\"year\":1999}";
        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> postResp =
                client.send(post, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Movie created = gson.fromJson(postResp.body(), Movie.class);

        // DELETE
        HttpRequest del = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + created.getId()))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(del, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode());
    }

    @Test
    void deleteMovieById_notFound_returns404() throws Exception {
        HttpRequest del = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(del, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
    }

    @Test
    void deleteMovieById_notNumber_returns400() throws Exception {
        HttpRequest del = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(del, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
    }

    @Test
    void getMovies_filterByYear_returnsMovies() throws Exception {
        // Добавим 2 фильма разных лет
        HttpRequest post1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"A\",\"year\":2000}", StandardCharsets.UTF_8))
                .build();
        client.send(post1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest post2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"B\",\"year\":2001}", StandardCharsets.UTF_8))
                .build();
        client.send(post2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // GET фильтр
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies?year=2000"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        Type listType = new ListOfMoviesTypeToken().getType();
        List<Movie> movies = gson.fromJson(resp.body(), listType);
        assertEquals(1, movies.size());
        assertEquals(2000, movies.get(0).getYear());
    }

    @Test
    void getMovies_filterByYear_noMatches_returnsEmptyList() throws Exception {
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies?year=1990"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        Type listType = new ListOfMoviesTypeToken().getType();
        List<Movie> movies = gson.fromJson(resp.body(), listType);
        assertEquals(0, movies.size());
    }

    @Test
    void getMovies_filterByYear_notNumber_returns400() throws Exception {
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
    }
}
