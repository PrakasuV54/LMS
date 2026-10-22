package ui;

import enums.PaymentMethod;
import model.Book;

import java.util.List;

public class UiHelpers extends Main {

    static void searchBooks() {
        while (true) {
            System.out.println("\nSearch Options:");
            System.out.println("1.General Search");
            System.out.println("2.Search by Field");
            System.out.println("3.Back");
            System.out.println("Enter choice:");

            int choice = readInt();
            if (choice == 1) {
                generalSearch();
            } else if (choice == 2) {
                searchByField();
            } else if (choice == 3) {
                return;
            } else {
                System.out.println("Invalid choice");
            }
        }
    }

    static void generalSearch() {
        while (true) {
            List<Book> results = executeGeneralSearchQuery();
            if (results == null) {
                return;
            }
            handleBookListInteraction(results);
        }
    }

    static void searchByField() {
        while (true) {
            List<Book> results = executeFieldSearchQuery();
            if (results == null) {
                return;
            }
            handleBookListInteraction(results);
        }
    }

    static List<Book> executeGeneralSearchQuery() {
        while (true) {
            System.out.println("\n--- General Search ---");
            System.out.println("Enter search query (or press Enter to go back):");
            String query = sc.nextLine().trim();
            if (query.isEmpty()) {
                return null;
            }

            List<Book> results = bookService.searchBooks(query);
            if (results.isEmpty()) {
                System.out.println("No books found matching the search criteria.");
                continue;
            }

            return results;
        }
    }

    static List<Book> executeFieldSearchQuery() {
        while (true) {
            System.out.println("\nSearch by Field:");
            System.out.println("1.Book ID");
            System.out.println("2.ISBN");
            System.out.println("3.Title");
            System.out.println("4.Author");
            System.out.println("5.Genre");
            System.out.println("6.Publisher");
            System.out.println("7.Description");
            System.out.println("8.Publication Year");
            System.out.println("9.Back");
            System.out.println("Enter choice:");

            int choice = readInt();
            if (choice == 9) {
                return null;
            }

            String field;
            String promptLabel;
            switch (choice) {
                case 1: field = "id"; promptLabel = "Book ID"; break;
                case 2: field = "isbn"; promptLabel = "ISBN"; break;
                case 3: field = "title"; promptLabel = "Title"; break;
                case 4: field = "author"; promptLabel = "Author"; break;
                case 5: field = "genre"; promptLabel = "Genre"; break;
                case 6: field = "publisher"; promptLabel = "Publisher"; break;
                case 7: field = "description"; promptLabel = "Description"; break;
                case 8: field = "year"; promptLabel = "Publication Year"; break;
                default:
                    System.out.println("Invalid choice");
                    continue;
            }

            System.out.println("Enter " + promptLabel + " search query:");
            String query = sc.nextLine().trim();
            if (query.isEmpty()) {
                System.out.println("Search query cannot be empty");
                continue;
            }

            List<Book> results = bookService.searchByField(field, query);
            if (results.isEmpty()) {
                System.out.println("No books found matching the search criteria.");
                continue;
            }

            return results;
        }
    }

    static void handleBookListInteraction(List<Book> currentBooks) {
        List<Book> displayed = currentBooks;
        while (true) {
            if (displayed.isEmpty()) {
                System.out.println("\nNo books to display.");
            } else {
                System.out.println("\n--- Books (" + displayed.size() + ") ---");
                for (Book b : displayed) {
                    System.out.println(b);
                }
            }

            System.out.println("\nOptions:");
            System.out.println("1.View Book Details");
            System.out.println("2.Request a Book from These Results");
            System.out.println("3.General Search");
            System.out.println("4.Search by Field");
            System.out.println("5.Return to Full Catalogue");
            System.out.println("6.Back");
            System.out.println("Enter choice:");

            int action = readInt();
            switch (action) {
                case 1:
                    viewDetailsMenu();
                    break;
                case 2:
                    if (displayed == null || displayed.isEmpty()) {
                        System.out.println("No books available to request.");
                        break;
                    }
                    System.out.println("Enter the Book ID from the displayed results (0 to cancel):");
                    int requestedBookId = readInt();
                    if (requestedBookId == 0) break;
                    Book selectedBook = null;
                    for (Book book : displayed) {
                        if (book.getBookId() == requestedBookId) {
                            selectedBook = book;
                            break;
                        }
                    }
                    if (selectedBook == null) {
                        System.out.println("That Book ID is not in the displayed results. Search again or choose a listed ID.");
                    } else {
                        Member.requestBookForSelectedBook(selectedBook);
                    }
                    break;
                case 3:
                    List<Book> genResults = executeGeneralSearchQuery();
                    if (genResults != null) {
                        displayed = genResults;
                    }
                    break;
                case 4:
                    List<Book> fieldResults = executeFieldSearchQuery();
                    if (fieldResults != null) {
                        displayed = fieldResults;
                    }
                    break;
                case 5:
                    displayed = bookService.getAllBooks();
                    if (displayed.isEmpty()) {
                        System.out.println("No Books Found");
                        return;
                    }
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    static void viewDetailsMenu() {
        System.out.println("Enter Book ID to view details:");
        int bookId = readInt();
        String details = bookService.getFormattedBookDetails(bookId);
        if (details == null) {
            System.out.println("Book with ID " + bookId + " not found.");
        } else {
            System.out.println("\n" + details);
        }
    }

    static PaymentMethod choosePaymentMethod() {
        System.out.println("Select Payment Method");
        System.out.println("1.UPI");
        System.out.println("2.Card");
        System.out.println("3.Net Banking");
        int choice = readInt();

        switch (choice) {
            case 1:
                return PaymentMethod.UPI;
            case 2:
                return PaymentMethod.CARD;
            case 3:
                return PaymentMethod.NET_BANKING;
            default:
                System.out.println("Invalid payment method. Please select 1 (UPI), 2 (Card), or 3 (Net Banking).");
                return null;
        }
    }
}
