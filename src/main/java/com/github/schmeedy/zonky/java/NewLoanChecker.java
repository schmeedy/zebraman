package com.github.schmeedy.zonky.java;

import java.util.ArrayList;
import java.util.List;

public class NewLoanChecker {
    private ZonkyClient zonkyClient;
    private int lastSeenLoanId;

    public NewLoanChecker(ZonkyClient zonkyClient, int lastSeenLoanId) {
        this.zonkyClient = zonkyClient;
        this.lastSeenLoanId = lastSeenLoanId;
    }

    /**
     * Returns new Loans since the last time this method was invoked on given checker object (or, in case of
     * first invocation, newer than the reference Loan given in constructor)
     */
    public List<Loan> getNewLoans() {
        return getNewLoans(0);
    }

    private List<Loan> getNewLoans(int page) {
        List<Loan> pageData = zonkyClient.getMostRecentLoans(page);
        List<Loan> newLoans = CollectionUtils.takeWhile(pageData, (Loan l) -> l.getId() != lastSeenLoanId);
        if (pageData.isEmpty()) {
            return pageData;
        } else if (newLoans.size() == pageData.size()) {
            List<Loan> result = new ArrayList<>(newLoans);
            result.addAll(getNewLoans(page + 1));
            return result;
        } else  {
            if (!newLoans.isEmpty()) {
                lastSeenLoanId = newLoans.get(newLoans.size() - 1).getId();
            }
            return newLoans;
        }
    }
}
