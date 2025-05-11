package org.example;

import com.google.gson.Gson;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;

/**
 * A generic class to parse JSON files into Java objects using Gson.
 *
 * @param <T> the type of the objects to be parsed
 */
public class JsonFileParser<T> {
    private final String fileName;
    private final Type listType;
    private final Gson gson = new Gson();

    public JsonFileParser(String fileName, Type listType) {
        this.fileName = fileName;
        this.listType = listType;
    }

    public List<T> parse() throws IOException {
        try (Reader reader = new FileReader(fileName)) {
            return gson.fromJson(reader, listType);
        }
    }
}
