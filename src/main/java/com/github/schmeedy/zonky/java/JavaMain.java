package com.github.schmeedy.zonky.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.client.HttpClients;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class JavaMain {
    private static int CHECK_INTERVAL_MINUTES = 5;

    public static void main(String[] args) {
        Reporter reporter = Reporter.CONSOLE;
        ZonkyClient client = new ZonkyClient(HttpClients.createDefault(), new ObjectMapper());

        Loan mostRecentLoan = client.getMostRecentLoans(0).get(0);
        NewLoanChecker newLoanChecker = new NewLoanChecker(client, mostRecentLoan.getId());

        long checkIntervalMs = CHECK_INTERVAL_MINUTES * 60_000;

        new Timer(false).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<Loan> newLoans = newLoanChecker.getNewLoans();
                reporter.newLoansFetched(newLoans);
            }
        }, checkIntervalMs, checkIntervalMs);

        reporter.timerStarted(mostRecentLoan, checkIntervalMs);
    }
}
