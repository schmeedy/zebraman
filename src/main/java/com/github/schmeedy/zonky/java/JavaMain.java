package com.github.schmeedy.zonky.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.client.HttpClients;

import java.time.LocalDateTime;
import java.util.*;

public class JavaMain {
    private static int CHECK_INTERVAL_MINUTES = 1;

    private static List<Loan> allLoansSince(LocalDateTime dt, ZonkyClient client) {
        List<Loan> allPages = new ArrayList<>();
        List<Loan> pageData;
        int page = 0;
        do {
            pageData = client.getMostRecentLoansSince(dt, page);
            allPages.addAll(pageData);
            page++;
        } while (!pageData.isEmpty());
        return allPages;
    }

    public static void main(String[] args) {
        Reporter reporter = Reporter.CONSOLE;
        ZonkyClient client = new ZonkyClient(HttpClients.createDefault(), new ObjectMapper());

        Loan mostRecentLoan = client.getMostRecentLoans(0).get(0);

        long checkIntervalMs = CHECK_INTERVAL_MINUTES * 60_000;

        new Timer(false).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LocalDateTime dt = LocalDateTime.now().minusMinutes(CHECK_INTERVAL_MINUTES);
                List<Loan> loans = allLoansSince(dt, client);
                reporter.newLoansFetched(loans);
            }
        }, checkIntervalMs, checkIntervalMs);

        reporter.timerStarted(mostRecentLoan, checkIntervalMs);
    }
}
