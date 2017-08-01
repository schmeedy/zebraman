package com.github.schmeedy.zonky.java;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ZonkyClient {
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    public ZonkyClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * @param page zero based!
     */
    public List<Loan> getMostRecentLoans(int page) {
        HttpGet get = new HttpGet("https://api.zonky.cz/loans/marketplace");
        get.addHeader("X-Page", String.valueOf(page));
        get.addHeader("X-Order", "-datePublished");
        try {
            HttpResponse response = httpClient.execute(get);
            return readJsonEntity(response, new TypeReference<List<Loan>>() {});
        } catch (IOException e) {
            throw new HttpClientException("Zonky API request failed", e);
        }
    }

    private <E> E readJsonEntity(HttpResponse response, TypeReference<E> ref) {
        try (InputStream in = response.getEntity().getContent()) {
            return objectMapper.readValue(in, ref);
        } catch (IOException e) {
            throw new HttpClientException("Failed to deserialize JSON response.", e);
        }
    }
}
