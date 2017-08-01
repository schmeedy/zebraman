package com.github.schmeedy.zonky.java;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

public class ZonkyClient {
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    public ZonkyClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * @param page zero based
     */
    public List<Loan> getMostRecentLoans(int page) {
        return getMostRecentLoansSince(null, page);
    }

    /**
     * @param dt nullable
     * @param page zero based
     */
    public List<Loan> getMostRecentLoansSince(LocalDateTime dt, int page) {
        try {
            String filter = dt == null ? "" : "?datePublished__gt=" + dt;
            HttpGet get = new HttpGet(new URI("https://api.zonky.cz/loans/marketplace" + filter));
            get.addHeader("X-Page", String.valueOf(page));
            get.addHeader("X-Order", "-datePublished");
            HttpResponse response = httpClient.execute(get);
            return readJsonEntity(response, new TypeReference<List<Loan>>() {});
        } catch (IOException | URISyntaxException e) {
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
