package com.github.schmeedy.zonky.java;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NewLoanCheckerTest {

    private static int ITEMS_PER_PAGE = 5;

    private Loan createLoan(int id) {
        return new Loan(id, "Loan " + id, "/loan/" + id);
    }

    private List<Loan> loans = IntStream.range(0, 30)
            .mapToObj(this::createLoan)
            .collect(Collectors.toList());

    private NewLoanChecker createChecker(int lastSeenLoanId) {
        ZonkyClient client = mock(ZonkyClient.class);
        when(client.getMostRecentLoans(anyInt())).thenAnswer((InvocationOnMock invocation) -> {
            int page = invocation.getArgument(0);
            int initIdx = Math.min(ITEMS_PER_PAGE * page, loans.size());
            if (initIdx > loans.size()) {
                return Collections.emptyList();
            } else {
                return loans.subList(initIdx, Math.min(initIdx + ITEMS_PER_PAGE, loans.size()));
            }
        });

        return new NewLoanChecker(client, lastSeenLoanId);
    }

    @Test
    public void shouldReturnNoNewLoansIfLastOneWasSeen() {
        assertEquals(Collections.emptyList(), createChecker(0).getNewLoans());
    }

    @Test
    public void shouldReturnNewLoansOnFirsPage() {
        assertEquals(loans.subList(0, 1), createChecker(1).getNewLoans());
    }

    @Test
    public void shouldReturnNewLoansAcrossPages() {
        IntStream.range(0, loans.size()).forEach((int i) -> {
            assertEquals(loans.subList(0, i), createChecker(i).getNewLoans());
        });
    }

}