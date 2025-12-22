package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MoviesStore {
    private final AtomicLong idSeq = new AtomicLong(1);
    private final Map<Long, Movie> movies = new HashMap<>();

    public Movie add(String title, int year) {
        long id = idSeq.getAndIncrement();
        Movie movie = new Movie(id, title, year);
        movies.put(id, movie);
        return movie;
    }

    public List<Movie> getAll() {
        List<Movie> list = new ArrayList<>(movies.values());
        list.sort(Comparator.comparingLong(Movie::getId));
        return list;
    }

    public List<Movie> getByYear(int year) {
        List<Movie> list = new ArrayList<>();
        for (Movie m : movies.values()) {
            if (m.getYear() == year) list.add(m);
        }
        list.sort(Comparator.comparingLong(Movie::getId));
        return list;
    }

    public Movie getById(long id) {
        return movies.get(id);
    }

    public boolean delete(long id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        idSeq.set(1);
    }
}
