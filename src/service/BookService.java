package service;

import model.Book;
import model.BookCopy;
import model.User;
import model.Author;
import model.Genre;
import enums.BookCopyStatus;
import repository.AuthorRepository;
import repository.BookCopyRepository;
import repository.BookRepository;
import repository.DataStore;
import repository.GenreRepository;
import repository.InMemoryBookRepository;
import repository.TransactionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BookService {
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final TransactionRepository transactions;
    private final AuditService auditService;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;

    public BookService(BookRepository books, BookCopyRepository copies, TransactionRepository transactions) {
        this(books, copies, transactions, null, null, null);
    }

    public BookService(BookRepository books, BookCopyRepository copies, TransactionRepository transactions,
                       AuditService auditService) {
        this(books, copies, transactions, auditService, null, null);
    }

    public BookService(BookRepository books, BookCopyRepository copies, TransactionRepository transactions,
                       AuditService auditService, AuthorRepository authorRepository, GenreRepository genreRepository) {
        this.books = books;
        this.copies = copies;
        this.transactions = transactions;
        this.auditService = auditService;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
    }

    public AuthorRepository getAuthorRepository() {
        if (authorRepository != null) return authorRepository;
        if (books instanceof InMemoryBookRepository) {
            return ((InMemoryBookRepository) books).getAuthorRepository();
        }
        return null;
    }

    public GenreRepository getGenreRepository() {
        if (genreRepository != null) return genreRepository;
        if (books instanceof InMemoryBookRepository) {
            return ((InMemoryBookRepository) books).getGenreRepository();
        }
        return null;
    }

    public Author getOrCreateAuthor(String authorName) {
        if (authorName == null || authorName.isBlank()) return null;
        String name = authorName.trim();
        AuthorRepository repo = getAuthorRepository();
        if (repo != null) {
            Author existing = repo.findByName(name);
            if (existing != null) return existing;
            Author created = new Author(DataStore.getAuthorId(), name);
            repo.save(created);
            return created;
        }
        return new Author(DataStore.getAuthorId(), name);
    }

    public Genre getOrCreateGenre(String genreName) {
        if (genreName == null || genreName.isBlank()) return null;
        String name = genreName.trim();
        GenreRepository repo = getGenreRepository();
        if (repo != null) {
            Genre existing = repo.findByName(name);
            if (existing != null) return existing;
            Genre created = new Genre(DataStore.getGenreId(), name);
            repo.save(created);
            return created;
        }
        return new Genre(DataStore.getGenreId(), name);
    }

    public List<Book> getAllBooks() {
        return books.findAll();
    }

    public Book getBookById(int bookId) {
        return books.findById(bookId);
    }

    public Book getBookByIsbn(String isbn) {
        return books.findByIsbn(isbn);
    }

    public List<Book> searchBooks(String query) {
        return books.search(query);
    }

    public List<Book> searchById(int bookId) {
        Book book = books.findById(bookId);
        List<Book> result = new ArrayList<>();
        if (book != null) {
            result.add(book);
        }
        return result;
    }

    public List<Book> searchByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) return new ArrayList<>();
        Book book = books.findByIsbn(isbn.trim());
        List<Book> result = new ArrayList<>();
        if (book != null) {
            result.add(book);
        }
        return result;
    }

    public List<Book> searchByTitle(String title) {
        return books.findByTitle(title);
    }

    public List<Book> searchByAuthor(String author) {
        return books.findByAuthor(author);
    }

    public List<Book> searchByGenre(String genre) {
        return books.findByGenre(genre);
    }

    public List<Book> searchByPublisher(String publisher) {
        return books.findByPublisher(publisher);
    }

    public List<Book> searchByDescription(String description) {
        return books.findByDescription(description);
    }

    public List<Book> searchByPublicationYear(int year) {
        return books.findByPublicationYear(year);
    }

    public List<Book> searchByField(String field, String query) {
        if (query == null || field == null) return new ArrayList<>();
        String q = query.trim();
        if (q.isEmpty()) return new ArrayList<>();
        String f = field.trim().toLowerCase();
        if (f.equals("id") ) {
            try {
                int id = Integer.parseInt(q);
                return searchById(id);
            } catch (NumberFormatException e) {
                return new ArrayList<>();
            }
        }
        if (f.equals("isbn")) {
            return searchByIsbn(q);
        }
        if (f.equals("title")) {
            return searchByTitle(q);
        }
        if (f.equals("author") ) {
            return searchByAuthor(q);
        }
        if (f.equals("genre") ) {
            return searchByGenre(q);
        }
        if (f.equals("publisher")) {
            return searchByPublisher(q);
        }
        if (f.equals("description")) {
            return searchByDescription(q);
        }
        if (f.equals("year")) {
            try {
                int year = Integer.parseInt(q);
                return searchByPublicationYear(year);
            } catch (NumberFormatException e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    
    public String getFormattedBookDetails(int bookId) {
        Book book = books.findById(bookId);
        if (book == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("----------------------------------------\n");
        sb.append("Book Details\n");
        sb.append("----------------------------------------\n");
        sb.append("Book ID: ").append(book.getBookId()).append("\n");
        sb.append("ISBN: ").append(book.getIsbn()).append("\n");
        sb.append("Title: ").append(book.getTitle()).append("\n");
        String authorsStr = book.getAuthorsString();
        sb.append("Authors: ").append(authorsStr.isEmpty() ? "N/A" : authorsStr).append("\n");
        String genresStr = book.getGenresString();
        sb.append("Genres: ").append(genresStr.isEmpty() ? "N/A" : genresStr).append("\n");

        sb.append("Publisher: ").append(book.getPublisher() != null && !book.getPublisher().isBlank() ? book.getPublisher() : "N/A").append("\n");
        sb.append("Publication Year: ").append(book.getPublicationYear()).append("\n");
        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            sb.append("Description: ").append(book.getDescription()).append("\n");
        }

        List<BookCopy> copyList = copies.findByBookId(bookId);
        sb.append("\nPhysical Copy Availability:\n");
        sb.append("Total Copies: ").append(copyList.size()).append("\n");
        int available = 0, reserved = 0, issued = 0, lost = 0, damaged = 0;
        for (BookCopy c : copyList) {
            if (c.getStatus() == BookCopyStatus.AVAILABLE) available++;
            else if (c.getStatus() == BookCopyStatus.RESERVED) reserved++;
            else if (c.getStatus() == BookCopyStatus.ISSUED) issued++;
            else if (c.getStatus() == BookCopyStatus.LOST) lost++;
            else if (c.getStatus() == BookCopyStatus.DAMAGED) damaged++;
        }
        sb.append("  - Available: ").append(available).append("\n");
        sb.append("  - Reserved: ").append(reserved).append("\n");
        sb.append("  - Issued: ").append(issued).append("\n");
        sb.append("  - Lost: ").append(lost).append("\n");
        sb.append("  - Damaged: ").append(damaged).append("\n");
        sb.append("----------------------------------------");
        return sb.toString();
    }

    public Author canonicalizeAuthor(Author a) {
        if (a == null || a.getName() == null || a.getName().isBlank()) return null;
        String name = a.getName().trim();
        AuthorRepository repo = getAuthorRepository();
        if (repo == null) return a;

        
        Author existing = repo.findByName(name);
        if (existing != null) {
            return existing;
        }

        
        if (a.getAuthorId() > 0) {
            existing = repo.findById(a.getAuthorId());
            if (existing != null) {
                return existing;
            }
        }

        
        Author canonical;
        if (a.getAuthorId() > 0 && !repo.existsById(a.getAuthorId())) {
            canonical = a;
        } else {
            canonical = new Author(DataStore.getAuthorId(), name);
        }
        repo.save(canonical);
        return canonical;
    }

    public Genre canonicalizeGenre(Genre g) {
        if (g == null || g.getName() == null || g.getName().isBlank()) return null;
        String name = g.getName().trim();
        GenreRepository repo = getGenreRepository();
        if (repo == null) return g;

        
        Genre existing = repo.findByName(name);
        if (existing != null) {
            return existing;
        }

        
        if (g.getGenreId() > 0) {
            existing = repo.findById(g.getGenreId());
            if (existing != null) {
                return existing;
            }
        }

        
        Genre canonical;
        if (g.getGenreId() > 0 && !repo.existsById(g.getGenreId())) {
            canonical = g;
        } else {
            canonical = new Genre(DataStore.getGenreId(), name);
        }
        repo.save(canonical);
        return canonical;
    }

    public void canonicalizeBookAuthors(Book book) {
        if (book == null) return;
        Set<Author> currentAuthors = book.getAuthors();
        if (!currentAuthors.isEmpty()) {
            List<Author> resolvedAuthors = new ArrayList<>();
            for (Author a : currentAuthors) {
                Author canonical = canonicalizeAuthor(a);
                if (canonical != null && !resolvedAuthors.contains(canonical)) {
                    resolvedAuthors.add(canonical);
                }
            }
            book.setAuthors(resolvedAuthors);
        }
    }

    public void canonicalizeBookGenres(Book book) {
        if (book == null) return;
        Set<Genre> currentGenres = book.getGenres();
        if (!currentGenres.isEmpty()) {
            List<Genre> resolvedGenres = new ArrayList<>();
            for (Genre g : currentGenres) {
                Genre canonical = canonicalizeGenre(g);
                if (canonical != null && !resolvedGenres.contains(canonical)) {
                    resolvedGenres.add(canonical);
                }
            }
            book.setGenres(resolvedGenres);
        }
    }

    public void canonicalizeBookAssociations(Book book) {
        canonicalizeBookAuthors(book);
        canonicalizeBookGenres(book);
    }


    public boolean addBook(Book book, int numberOfCopies, User librarian) {
        if (!canManageCatalogue(librarian)) return false;
        String librarianPhone = librarian.getPhone();
        if (book == null || numberOfCopies < 0 || books.existsByIsbn(book.getIsbn())) return false;

        canonicalizeBookAssociations(book);

        books.save(book);
        for (int i = 0; i < numberOfCopies; i++) {
            copies.save(new BookCopy(DataStore.getBookCopyId(), book.getBookId()));
        }
        record(librarianPhone, "BOOK_ADDED", book, "Copies added: " + numberOfCopies);
        return true;
    }



    public boolean deleteBook(int bookId, User librarian) {
        if (!canManageCatalogue(librarian)) return false;
        String librarianPhone = librarian.getPhone();
        Book book = books.findById(bookId);
        if (book == null) return false;
        if (!copies.findByBookId(bookId).isEmpty() || !transactions.findByBookId(bookId).isEmpty()) return false;
        books.deleteById(bookId);
        record(librarianPhone, "BOOK_DELETED", book, "Book removed from catalogue");
        return true;
    }

    private boolean canManageCatalogue(User librarian) {
        return librarian != null && librarian.getStatus() == enums.UserStatus.ACTIVE
                && (librarian.getRole() == enums.UserRole.LIBRARIAN
                    || librarian.getRole() == enums.UserRole.ADMIN);
    }

    private void record(String phone, String action, Book book, String details) {
        if (auditService != null) {
            auditService.record(phone, action, "BOOK", String.valueOf(book.getBookId()), details);
        }
    }

    public String getAddBookFailureReason(Book book, int numberOfCopies, User librarian) {
        if (!canManageCatalogue(librarian)) {
            return "Insufficient permissions. Staff authorization required.";
        }
        if (book == null) {
            return "Book details are required.";
        }
        if (book.getIsbn() == null || book.getIsbn().isBlank()) {
            return "ISBN cannot be empty.";
        }
        if (books.existsByIsbn(book.getIsbn())) {
            return "A book with ISBN '" + book.getIsbn() + "' already exists in the catalogue.";
        }
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            return "Title cannot be empty.";
        }
        if (book.getPublisher() == null || book.getPublisher().isBlank()) {
            return "Publisher cannot be empty.";
        }
        if (numberOfCopies < 1) {
            return "Number of copies must be at least 1.";
        }
        return null;
    }

    public String getDeleteBookFailureReason(int bookId, User librarian) {
        if (!canManageCatalogue(librarian)) {
            return "Insufficient permissions. Staff authorization required.";
        }
        Book book = books.findById(bookId);
        if (book == null) {
            return "Book with ID " + bookId + " was not found.";
        }
        if (!copies.findByBookId(bookId).isEmpty()) {
            return "Cannot delete book: " + copies.findByBookId(bookId).size() + " copy/copies currently exist in inventory. Remove all physical copies first.";
        }
        if (!transactions.findByBookId(bookId).isEmpty()) {
            return "Cannot delete book: Active or historical transaction records exist for this book.";
        }
        return null;
    }
}
