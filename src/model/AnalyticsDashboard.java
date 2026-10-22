package model;

import java.util.Map;

public class AnalyticsDashboard {
    private final Map<String, Map<String, Integer>> mostBorrowedBooksByMonth;
    private final Map<String, Map<String, Integer>> mostBorrowedBooksByQuarter;
    private final Map<String, Integer> mostBorrowedBooksForYear;
    private final Map<String, Integer> registrationsByAgeGroup;
    private final Map<String, Integer> registrationsByGender;

    public AnalyticsDashboard(Map<String, Map<String, Integer>> mostBorrowedBooksByMonth,
                              Map<String, Map<String, Integer>> mostBorrowedBooksByQuarter,
                              Map<String, Integer> mostBorrowedBooksForYear,
                              Map<String, Integer> registrationsByAgeGroup,
                              Map<String, Integer> registrationsByGender) {
        this.mostBorrowedBooksByMonth = mostBorrowedBooksByMonth;
        this.mostBorrowedBooksByQuarter = mostBorrowedBooksByQuarter;
        this.mostBorrowedBooksForYear = mostBorrowedBooksForYear;
        this.registrationsByAgeGroup = registrationsByAgeGroup;
        this.registrationsByGender = registrationsByGender;
    }

    public Map<String, Map<String, Integer>> getMostBorrowedBooksByMonth() {
        return mostBorrowedBooksByMonth;
    }

    public Map<String, Map<String, Integer>> getMostBorrowedBooksByQuarter() {
        return mostBorrowedBooksByQuarter;
    }

    public Map<String, Integer> getMostBorrowedBooksForYear() {
        return mostBorrowedBooksForYear;
    }

    public Map<String, Integer> getRegistrationsByAgeGroup() {
        return registrationsByAgeGroup;
    }

    public Map<String, Integer> getRegistrationsByGender() {
        return registrationsByGender;
    }
}
