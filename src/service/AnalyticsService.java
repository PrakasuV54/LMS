package service;

import model.AnalyticsDashboard;
import model.Book;
import model.Transaction;
import model.User;
import repository.BookRepository;
import repository.TransactionRepository;
import repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnalyticsService {
    private final BookRepository books;
    private final TransactionRepository transactions;
    private final UserRepository users;

    public AnalyticsService(BookRepository books, TransactionRepository transactions, UserRepository users) {
        this.books = books;
        this.transactions = transactions;
        this.users = users;
    }

    public Map<String, Integer> getMostBorrowedBooks(LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return result;
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        for (Transaction transaction : transactions.findAll()) {
            LocalDateTime issueDate = transaction.getIssueDate();
            if (issueDate == null || issueDate.isBefore(start) || !issueDate.isBefore(end)) {
                continue;
            }

            Book book = books.findById(transaction.getBookId());
            if (book != null) {
                result.put(book.getTitle(), result.getOrDefault(book.getTitle(), 0) + 1);
            }
        }
        return sortByBorrowCount(result);
    }

    public Map<String, Integer> getMostBorrowedBooksForMonth(YearMonth month) {
        if (month == null) return new LinkedHashMap<>();
        return getMostBorrowedBooks(month.atDay(1), month.atEndOfMonth());
    }

    public Map<String, Integer> getMostBorrowedBooksForQuarter(int year, int quarter) {
        if (quarter < 1 || quarter > 4) return new LinkedHashMap<>();
        int firstMonth = (quarter - 1) * 3 + 1;
        LocalDate start = LocalDate.of(year, firstMonth, 1);
        return getMostBorrowedBooks(start, start.plusMonths(3).minusDays(1));
    }

    public Map<String, Integer> getMostBorrowedBooksForYear(int year) {
        return getMostBorrowedBooks(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }


    public AnalyticsDashboard getDashboardForYear(int year) {
        Map<String, Map<String, Integer>> booksByMonth = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> booksByQuarter = new LinkedHashMap<>();

        for (int month = 1; month <= 12; month++) {
            YearMonth period = YearMonth.of(year, month);
            String monthName = period.getMonth().toString();
            booksByMonth.put(monthName, getMostBorrowedBooksForMonth(period));
        }

        for (int quarter = 1; quarter <= 4; quarter++) {
            String quarterName = "Q" + quarter;
            booksByQuarter.put(quarterName, getMostBorrowedBooksForQuarter(year, quarter));
        }

        Map<String, Integer> byAge = new LinkedHashMap<>();
        Map<String, Integer> byGender = new LinkedHashMap<>();
        LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        for (User user : users.findAll()) {
            LocalDateTime created = user.getCreatedAt();
            if (created.isBefore(start) || !created.isBefore(end)) continue;
            int age = user.getAge();
            String ageGroup = age <= 0 ? "AGE_NOT_PROVIDED" : age <= 17 ? "0-17" : age <= 25 ? "18-25" : age <= 35 ? "26-35" : age <= 50 ? "36-50" : "51+";
            byAge.put(ageGroup, byAge.getOrDefault(ageGroup, 0) + 1);
            String gender = user.getGender() == null || user.getGender().isBlank() ? "UNSPECIFIED" : user.getGender();
            byGender.put(gender, byGender.getOrDefault(gender, 0) + 1);
        }
        return new AnalyticsDashboard(booksByMonth, booksByQuarter, getMostBorrowedBooksForYear(year), byAge, byGender);
    }

    private Map<String, Integer> sortByBorrowCount(Map<String, Integer> counts) {
        Map<String, Integer> sorted = new LinkedHashMap<>();
        java.util.List<Map.Entry<String, Integer>> entries = new java.util.ArrayList<>(counts.entrySet());
        for (int i = 1; i < entries.size(); i++) {
            Map.Entry<String, Integer> current = entries.get(i);
            int j = i - 1;
            while (j >= 0 && entries.get(j).getValue() < current.getValue()) {
                entries.set(j + 1, entries.get(j));
                j--;
            }
            entries.set(j + 1, current);
        }
        for (Map.Entry<String, Integer> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }
}
