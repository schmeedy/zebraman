package com.github.schmeedy.zonky.java;

import java.time.LocalDateTime;

public interface Reporter {
    void timerStarted(Loan lastLoan, long checkIntervalMs);
    void newLoansFetched(Iterable<Loan> loans);

    Reporter CONSOLE = new Reporter() {
        @Override
        public void timerStarted(Loan lastLoan, long checkIntervalMs) {
            println("last loan in the marketplace: " + lastLoan);
            println("checking for new loans every " + (checkIntervalMs / 1000) + " seconds");
        }

        @Override
        public void newLoansFetched(Iterable<Loan> loans) {
            if (!loans.iterator().hasNext()) {
                println(LocalDateTime.now() + " - no new loans");
            } else {
                println();
                println(LocalDateTime.now() + " - new loan(s):");
                loans.forEach((Loan l) -> println("  " + l));
                println();
            }
        }

        private void println(String msg) {
            System.out.println(msg);
        }

        private void println() {
            System.out.println();
        }
    };
}
