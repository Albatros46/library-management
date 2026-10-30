package com.library.loan;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reading report for one member, built from the categories of the books they borrowed.
 * It reflects borrowing, not proof that the books were actually read.
 */
public record MemberStats(String memberId,
                          long totalBorrowed,
                          long totalPages,
                          long currentlyBorrowed,
                          long overdue,
                          long returned,
                          List<Group> byCategory,
                          List<Group> byAuthor) {

    public record Group(String key, String name, long count, long pages) {}

    static MemberStats of(String memberId, List<Loan> all) {
        List<Loan> borrowed = all.stream().filter(l -> LoanStatus.BORROWED.contains(l.getStatus())).toList();
        return new MemberStats(memberId,
                borrowed.size(),
                borrowed.stream().mapToLong(Loan::getPageCount).sum(),
                borrowed.stream().filter(l -> LoanStatus.ON_LOAN.contains(l.getStatus())).count(),
                borrowed.stream().filter(l -> l.getStatus() == LoanStatus.OVERDUE).count(),
                borrowed.stream().filter(l -> l.getStatus() == LoanStatus.RETURNED).count(),
                group(borrowed, l -> nullTo(l.getCategory(), "Diğer"), l -> nullTo(l.getCategory(), "Diğer")),
                group(borrowed, l -> nullTo(l.getAuthorId(), "?"), l -> nullTo(l.getAuthorName(), "?")));
    }

    private static List<Group> group(List<Loan> loans, java.util.function.Function<Loan, String> key,
                                     java.util.function.Function<Loan, String> name) {
        Map<String, List<Loan>> grouped = loans.stream().collect(Collectors.groupingBy(key));
        return grouped.entrySet().stream()
                .map(e -> new Group(e.getKey(), name.apply(e.getValue().getFirst()), e.getValue().size(),
                        e.getValue().stream().mapToLong(Loan::getPageCount).sum()))
                .sorted(Comparator.comparingLong(Group::count).reversed().thenComparing(Group::name))
                .toList();
    }

    private static String nullTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
