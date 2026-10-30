package com.library.loan;

import java.util.EnumSet;
import java.util.Set;

public enum LoanStatus {
    /** Lending requested, waiting for staff approval. */
    PENDING,
    /** Book is with the member and not yet late. */
    ACTIVE,
    /** Due date passed and the book is still with the member. */
    OVERDUE,
    /** Member brought the book back, waiting for staff to confirm. */
    RETURN_PENDING,
    RETURNED,
    REJECTED;

    /** Statuses in which the member physically holds the book. */
    public static final Set<LoanStatus> ON_LOAN = EnumSet.of(ACTIVE, OVERDUE, RETURN_PENDING);
    /** Statuses that count as a real borrowing for reading reports. */
    public static final Set<LoanStatus> BORROWED = EnumSet.of(ACTIVE, OVERDUE, RETURN_PENDING, RETURNED);
}
