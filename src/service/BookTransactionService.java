package service;

import enums.BookCopyStatus;
import enums.TransactionStatus;
import model.Book;
import model.BookCopy;
import model.Transaction;
import model.User;
import repository.BookCopyRepository;
import repository.BookRepository;
import repository.DataStore;
import repository.MembershipRepository;
import repository.TransactionRepository;
import java.time.LocalDate;
import java.util.List;

public class BookTransactionService {
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final TransactionRepository transactions;
    private final PolicyService policy;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final FineService fineService;
    private final OverdueFineSchedulerService overdueFineSchedulerService;

    public BookTransactionService(BookRepository books, BookCopyRepository copies, TransactionRepository transactions, PolicyService policy) {
        this(books, copies, transactions, policy, null, null, null);
    }

    public BookTransactionService(BookRepository books, BookCopyRepository copies, TransactionRepository transactions, PolicyService policy,
    AuditService auditService, NotificationService notificationService) {
        this(books, copies, transactions, policy, auditService, notificationService, null);
    }

    public BookTransactionService(BookRepository books, BookCopyRepository copies, TransactionRepository transactions, PolicyService policy,
    AuditService auditService, NotificationService notificationService, FineService fineService) {
        this.books=books;
        this.copies=copies;
        this.transactions=transactions;
        this.policy=policy;
        this.auditService=auditService;
        this.notificationService=notificationService;
        this.fineService=fineService;
        repository.UserRepository userRepo = (policy != null) ? policy.getUserRepository() : null;
        if (this.notificationService != null && this.notificationService.getUserRepository() == null && userRepo != null) {
            this.notificationService.setUserRepository(userRepo);
        }
        if (fineService != null && notificationService != null) {
            this.overdueFineSchedulerService = new OverdueFineSchedulerService(transactions, books, fineService, notificationService, userRepo);
            this.overdueFineSchedulerService.startScheduler();
        } else {
            this.overdueFineSchedulerService = null;
        }
    }

    private void sendUserNotification(String phone, String message) {
        if (notificationService == null || phone == null || phone.isBlank()) return;
        String trimmed = phone.trim();
        User user = (policy != null && policy.getUserRepository() != null) ? policy.getUserRepository().findByPhone(trimmed) : null;
        if (user == null && policy != null && policy.getUserRepository() != null && trimmed.matches("\\d+")) {
            try {
                user = policy.getUserRepository().findById(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {}
        }
        if (user == null && notificationService.getUserRepository() != null) {
            user = notificationService.getUserRepository().findByPhone(trimmed);
            if (user == null && trimmed.matches("\\d+")) {
                try {
                    user = notificationService.getUserRepository().findById(Integer.parseInt(trimmed));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (user != null && user.getName() != null && !user.getName().isBlank()) {
            notificationService.send(user.getName(), user.getPhone(), message);
        } else {
            notificationService.send(trimmed, message);
        }
    }

    public void processOverdueTransactions() {
        if (overdueFineSchedulerService != null) {
            overdueFineSchedulerService.processOverdueTransactions();
        }
    }


    private void record(String phone, String action, Transaction t, String details) {
        if (auditService != null) auditService.record(phone, action, "TRANSACTION", String.valueOf(t.getTransactionId()), details);
    }



    public Transaction requestBook(String phone, int bookId) {
        if (phone == null || phone.isBlank())
            return null;
        if (!policy.canRequestBook(phone) || books.findById(bookId) == null)
            return null;
        Transaction transaction = new Transaction(DataStore.getTransactionId(), phone, bookId);
        transactions.save(transaction);
        record(phone, "BOOK_REQUESTED", transaction, "Book ID " + bookId);
        autoReserveAvailableCopies(bookId);
        Transaction updated = transactions.findById(transaction.getTransactionId());
        if (updated == null || updated.getStatus() != TransactionStatus.RESERVED) {
            sendUserNotification(phone, "Your request is queued.");
        }
        return updated == null ? transaction : updated;
    }

    public Transaction requestBookByIsbn(String phone, String isbn) {
        Book book = books.findByIsbn(isbn);
        return book == null ? null : requestBook(phone, book.getBookId());
    }

    public Transaction requestBookByIsbn(String phone, String isbn, User caller) {
        Book book = books.findByIsbn(isbn);
        return book == null ? null : requestBook(phone, book.getBookId());
    }

    public boolean markReadyForPickup(int transactionId, User staff) {
        if (!policy.canManageInventory(staff))
            return false;
        Transaction t = transactions.findById(transactionId);
        if (t == null || t.getStatus() != TransactionStatus.RESERVED)
            return false;
        BookCopy copy = copies.findById(t.getBookCopyId());
        if (copy == null || copy.getStatus() != BookCopyStatus.RESERVED || copy.getBookId() != t.getBookId())
            return false;
        t.markReadyForPickup();
        transactions.save(t);
        record(staff.getPhone(), "BOOK_READY_FOR_PICKUP", t, "Copy ID " + t.getBookCopyId());
        sendUserNotification(t.getUserPhone(), "Your requested book is ready for pickup.");
        return true;
    }


    public boolean issueBook(int transactionId, User librarian) {
        if (!policy.canManageInventory(librarian))
            return false;
        Transaction t = transactions.findById(transactionId);
        if (t == null || t.getStatus() != TransactionStatus.READY_FOR_PICKUP || !policy.canIssueBook(t.getUserPhone()))
            return false;
        BookCopy copy = copies.findById(t.getBookCopyId());
        if (copy == null || copy.getStatus() != BookCopyStatus.RESERVED || copy.getBookId() != t.getBookId())
            return false;
        t.issue(LocalDate.now().plusDays(policy.getLoanPeriodDaysForUser(t.getUserPhone())));
        copy.issue();
        copies.save(copy);
        transactions.save(t);
        record(librarian.getPhone(), "BOOK_ISSUED", t, "Copy ID " + copy.getBookCopyId() + ", due " + t.getDueDate());
        sendUserNotification(t.getUserPhone(), "Book issued. Due date: " + t.getDueDate());
        return true;
    }

    public boolean markCopyLost(int bookCopyId, User librarian) {
        return updateCopyCondition(bookCopyId, librarian, true, true);
    }

    public boolean markCopyDamaged(int bookCopyId, User librarian) {
        return updateCopyCondition(bookCopyId, librarian, false, true);
    }

    public boolean markCopyLostUnidentified(int bookCopyId, User librarian) {
        return updateCopyCondition(bookCopyId, librarian, true, false);
    }

    public boolean markCopyDamagedUnidentified(int bookCopyId, User librarian) {
        return updateCopyCondition(bookCopyId, librarian, false, false);
    }

    private boolean updateCopyCondition(int bookCopyId, User librarian, boolean lost, boolean createMemberFine) {
        if (!policy.canManageInventory(librarian))
            return false;
        String librarianPhone = librarian.getPhone();
        BookCopy copy = copies.findById(bookCopyId);
        if (copy == null || copy.getStatus() == BookCopyStatus.LOST || copy.getStatus() == BookCopyStatus.DAMAGED)
            return false;
        Transaction activeResult = transactions.findActiveByCopyId(bookCopyId);
        Transaction transaction = activeResult;
        boolean wasIssued = transaction != null && transaction.getStatus() == TransactionStatus.ISSUED;
        String action = lost ? "BOOK_COPY_MARKED_LOST" : "BOOK_COPY_MARKED_DAMAGED";

        if (wasIssued && createMemberFine && fineService == null)
            return false;
        if ((copy.getStatus() == BookCopyStatus.RESERVED) && transaction == null)
            return false;
        if (transaction != null && transaction.getStatus() == TransactionStatus.ISSUED && copy.getStatus() != BookCopyStatus.ISSUED)
            return false;
        if (transaction != null && (transaction.getStatus() == TransactionStatus.RESERVED || transaction.getStatus() == TransactionStatus.READY_FOR_PICKUP) && copy.getStatus() != BookCopyStatus.RESERVED)
            return false;

        if (transaction != null) {
            if (wasIssued) {
                if (createMemberFine && fineService != null) fineService.createOverdueFine(transaction.getTransactionId());
                if (lost) transaction.markLost();
                else transaction.markDamaged();
            } else {
                transaction.cancel();
            }
            transactions.save(transaction);
        }

        if (lost) copy.markLost();
        else copy.markDamaged();
        copies.save(copy);

        if (auditService != null) {
            String details = "Copy marked " + (lost ? "lost" : "damaged") + " by staff";
            if (transaction != null) details += "; transaction ID " + transaction.getTransactionId();
            auditService.record(librarianPhone, action, "BOOK_COPY", String.valueOf(bookCopyId), details);
        }

        if (wasIssued && fineService != null && createMemberFine) {
            if (lost) fineService.createLostBookFine(transaction.getTransactionId());
            else fineService.createDamagedBookFine(transaction.getTransactionId());
            sendUserNotification(transaction.getUserPhone(), lost ? "Your issued book has been marked lost. Please check your outstanding fine." : "Your issued book has been marked damaged. Please check your outstanding fine.");
        } else if (wasIssued && !createMemberFine && auditService != null) {
            auditService.record(librarianPhone, "UNIDENTIFIED_BOOK_INCIDENT", "BOOK_COPY", String.valueOf(bookCopyId),
                    "Copy marked " + (lost ? "lost" : "damaged") + " without assigning a fine to a member");
        }

        autoReserveAvailableCopies(copy.getBookId());
        return true;
    }

    public boolean returnBook(int transactionId, User staff) {
        if (!policy.canManageInventory(staff)) return false;
        Transaction t = transactions.findById(transactionId);
        if (t == null || t.getStatus() != TransactionStatus.ISSUED) return false;
        BookCopy copy = copies.findById(t.getBookCopyId());
        if (copy == null || copy.getStatus() != BookCopyStatus.ISSUED || copy.getBookId() != t.getBookId()) return false;

        LocalDate asOfDate = overdueFineSchedulerService != null ? overdueFineSchedulerService.getCurrentDate() : LocalDate.now();
        if (t.getDueDate() != null && asOfDate.isAfter(t.getDueDate())) {
            if (fineService == null) return false;
            fineService.createOverdueFine(t.getTransactionId(), asOfDate);
        }

        t.returnBook();
        copy.returnCopy();
        copies.save(copy);
        transactions.save(t);

        record(staff.getPhone(), "BOOK_RETURNED", t, "Copy ID " + copy.getBookCopyId());
        sendUserNotification(t.getUserPhone(), "Your returned book has been processed successfully.");

        autoReserveAvailableCopies(t.getBookId());
        return true;
    }

    public boolean cancelRequest(String phone, String isbn, User caller) {
        if (phone == null || phone.isBlank() || isbn == null || isbn.isBlank() || caller == null) return false;

        Book book = books.findByIsbn(isbn.trim());
        if (book == null) return false;

        Transaction match = null;
        for (Transaction transaction : transactions.findByUserPhone(phone)) {
            if (transaction.getBookId() != book.getBookId())
                continue;
            TransactionStatus status = transaction.getStatus();
            if (status != TransactionStatus.REQUESTED && status != TransactionStatus.RESERVED && status != TransactionStatus.READY_FOR_PICKUP)
                continue;

            if (match != null)
                return false; 
            match = transaction;
        }

        return match != null && cancelRequest(match.getTransactionId(), caller);
    }



    public boolean cancelRequest(int transactionId, User caller) {
        Transaction t = transactions.findById(transactionId);
        if (t == null || (t.getStatus() != TransactionStatus.REQUESTED && t.getStatus() != TransactionStatus.RESERVED && t.getStatus() != TransactionStatus.READY_FOR_PICKUP)) {
            return false;
        }
        if (caller == null || !policy.isOwnerOrStaff(caller, t.getUserPhone())) {
            return false;
        }
        int bookId = t.getBookId();
        int copyId = t.getBookCopyId();
        t.cancel();
        if (copyId > 0) {
            BookCopy copy = copies.findById(copyId);
            if (copy != null && copy.getStatus() == BookCopyStatus.RESERVED) {
                copy.releaseReservation();
                copies.save(copy);
            }
        }
        transactions.save(t);
        record(caller != null ? caller.getPhone() : t.getUserPhone(), "BOOK_REQUEST_CANCELLED", t, "Copy ID " + copyId);
        sendUserNotification(t.getUserPhone(), "Request cancelled.");
        autoReserveAvailableCopies(bookId);
        return true;
    }



    public boolean renewBook(int transactionId, User caller) {
        Transaction transaction = transactions.findById(transactionId);
        if (transaction == null || transaction.getStatus() != TransactionStatus.ISSUED
                || transaction.getDueDate() == null || transaction.isOverdue()) {
            return false;
        }
        if (caller == null || !policy.isOwnerOrStaff(caller, transaction.getUserPhone())) {
            return false;
        }
        if (!policy.canRequestBook(transaction.getUserPhone())) {
            return false;
        }
        if (fineService != null && fineService.hasPendingFineForUser(transaction.getUserPhone())) {
            return false;
        }
        transaction.renew(transaction.getDueDate().plusDays(policy.getLoanPeriodDaysForUser(transaction.getUserPhone())));
        transactions.save(transaction);
        record(caller != null ? caller.getPhone() : transaction.getUserPhone(), "BOOK_RENEWED", transaction, "New due date " + transaction.getDueDate());
        sendUserNotification(transaction.getUserPhone(), "Book renewed. New due date: " + transaction.getDueDate());
        return true;
    }


    public List<Transaction> getAllPendingRequests(User staff) {
        if (!policy.canManageInventory(staff)) return new java.util.ArrayList<>();
        return findPendingRequests();
    }


    private List<Transaction> findPendingRequests() {
        java.util.List<Transaction> result = new java.util.ArrayList<>();
        java.util.List<Transaction> requested = new java.util.ArrayList<>();
        java.util.List<Transaction> other = new java.util.ArrayList<>();
        for (Transaction transaction : transactions.findAll()) {
            if (transaction.getStatus() == TransactionStatus.REQUESTED) {
                requested.add(transaction);
            } else if (transaction.getStatus() == TransactionStatus.RESERVED
                    || transaction.getStatus() == TransactionStatus.READY_FOR_PICKUP) {
                other.add(transaction);
            }
        }
        requested.sort(getRequestPriorityComparator());
        result.addAll(requested);
        result.addAll(other);
        return result;
    }

    public List<Transaction> getCurrentlyBorrowedBooks(String phone, User caller) {
        if (caller == null || !policy.isOwnerOrStaff(caller, phone)) return new java.util.ArrayList<>();
        return findCurrentlyBorrowedBooks(phone);
    }






    private java.util.List<Transaction> findCurrentlyBorrowedBooks(String phone) {
        java.util.List<Transaction> result = new java.util.ArrayList<>();
        for (Transaction transaction : transactions.findByUserPhone(phone)) {
            if (transaction.getStatus() == TransactionStatus.ISSUED) result.add(transaction);
        }
        return result;
    }

    public java.util.List<Transaction> getBorrowingHistory(String phone, User caller) {
        if (caller == null || !policy.isOwnerOrStaff(caller, phone))
            return new java.util.ArrayList<>();
        return transactions.findByUserPhone(phone);
    }






    public Transaction getNextEligibleRequest(int bookId) {
        java.util.List<Transaction> candidates = new java.util.ArrayList<>();
        for (Transaction t : transactions.findQueuedRequestsByBookId(bookId)) {
            if (t.getStatus() == TransactionStatus.REQUESTED && policy.canRequestBook(t.getUserPhone())) {
                candidates.add(t);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(getRequestPriorityComparator());
        return candidates.get(0);
    }

    public Transaction getNextEligibleRequest(int bookId, User staff) {
        if (!policy.canManageInventory(staff)) return null;
        return getNextEligibleRequest(bookId);
    }

    private java.util.Comparator<Transaction> getRequestPriorityComparator() {
        return (t1, t2) -> {
            int p1 = policy != null ? policy.getRequestQueuePriority(t1.getUserPhone()) : 2;
            int p2 = policy != null ? policy.getRequestQueuePriority(t2.getUserPhone()) : 2;
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
            if (t1.getRequestDate() != null && t2.getRequestDate() != null) {
                int dateCmp = t1.getRequestDate().compareTo(t2.getRequestDate());
                if (dateCmp != 0) {
                    return dateCmp;
                }
            }
            return Integer.compare(t1.getTransactionId(), t2.getTransactionId());
        };
    }

    private void autoReserveAvailableCopies(int bookId) {
        while (true) {
            BookCopy copy = copies.findAvailableCopy(bookId);
            Transaction next = getNextEligibleRequest(bookId);
            if (copy == null || next == null) return;
            if (transactions.findActiveByCopyId(copy.getBookCopyId()) != null) return;
            try {
                copy.reserve();
                next.reserve(copy.getBookCopyId());
                copies.save(copy);
                transactions.save(next);
                record("SYSTEM", "BOOK_COPY_AUTO_RESERVED", next, "Copy ID " + copy.getBookCopyId());
                sendUserNotification(next.getUserPhone(), "Your book is reserved for pickup.");
            } catch (RuntimeException ex) {
                return;
            }
        }
    }

    public String getMarkReadyFailureReason(int transactionId, User staff) {
        if (!policy.canManageInventory(staff)) {
            return "Not allowed.";
        }
        Transaction t = transactions.findById(transactionId);
        if (t == null) {
            return "Transaction not found.";
        }
        if (t.getStatus() != TransactionStatus.RESERVED) {
            return "Request is not reserved.";
        }
        BookCopy copy = copies.findById(t.getBookCopyId());
        if (copy == null) {
            return "Reserved copy not found.";
        }
        if (copy.getStatus() != BookCopyStatus.RESERVED) {
            return "Copy is not reserved.";
        }
        if (copy.getBookId() != t.getBookId()) {
            return "Copy does not match request.";
        }
        return null;
    }

    public String getIssueFailureReason(int transactionId, User librarian) {
        if (!policy.canManageInventory(librarian)) {
            return "Not allowed.";
        }
        Transaction t = transactions.findById(transactionId);
        if (t == null) {
            return "Transaction not found.";
        }
        if (t.getStatus() != TransactionStatus.READY_FOR_PICKUP) {
            return "Book is not ready for pickup.";
        }
        if (!policy.canIssueBook(t.getUserPhone())) {
            List<String> reasons = policy.getIssueEligibilityFailureReasons(t.getUserPhone());
            return "Member not eligible: " + String.join(", ", reasons);
        }
        BookCopy copy = copies.findById(t.getBookCopyId());
        if (copy == null) {
            return "Reserved copy not found.";
        }
        if (copy.getStatus() != BookCopyStatus.RESERVED) {
            return "Copy is not reserved.";
        }
        if (copy.getBookId() != t.getBookId()) {
            return "Copy does not match loan.";
        }
        return null;
    }

    public List<Transaction> findIssuedLoansForReturn(String memberPhone, String isbn, User staff) {
        java.util.List<Transaction> result = new java.util.ArrayList<>();
        if (!policy.canManageInventory(staff) || memberPhone == null || memberPhone.isBlank()
                || isbn == null || isbn.isBlank()) {
            return result;
        }
        Book book = books.findByIsbn(isbn.trim());
        if (book == null) return result;
        String normalizedPhone = memberPhone.trim();
        for (Transaction transaction : transactions.findByUserPhone(normalizedPhone)) {
            if (transaction.getBookId() == book.getBookId()
                    && transaction.getStatus() == TransactionStatus.ISSUED) {
                BookCopy copy = copies.findById(transaction.getBookCopyId());
                if (copy != null && copy.getStatus() == BookCopyStatus.ISSUED
                        && copy.getBookId() == transaction.getBookId()) {
                    result.add(transaction);
                }
            }
        }
        result.sort(java.util.Comparator.comparing(Transaction::getDueDate,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        return result;
    }

    public String getReturnFailureReason(int transactionId, User staff) {
        if (!policy.canManageInventory(staff)) {
            return "Not allowed.";
        }
        Transaction t = transactions.findById(transactionId);
        if (t == null) {
            return "Transaction not found.";
        }
        if (t.getStatus() != TransactionStatus.ISSUED) {
            return "Loan is not issued.";
        }
        BookCopy copy = copies.findById(t.getBookCopyId());
        if (copy == null) {
            return "Book copy not found.";
        }
        if (copy.getStatus() != BookCopyStatus.ISSUED) {
            return "Copy is not issued.";
        }
        if (copy.getBookId() != t.getBookId()) {
            return "Copy does not match loan.";
        }
        LocalDate asOfDate = overdueFineSchedulerService != null ? overdueFineSchedulerService.getCurrentDate() : LocalDate.now();
        if (t.getDueDate() != null && asOfDate.isAfter(t.getDueDate()) && fineService == null) {
            return "Fine service unavailable.";
        }
        return null;
    }

    public String getRenewFailureReason(int transactionId, User caller) {
        Transaction transaction = transactions.findById(transactionId);
        if (transaction == null) {
            return "Loan transaction with ID " + transactionId + " not found.";
        }
        if (transaction.getStatus() != TransactionStatus.ISSUED) {
            return "Loan is not active.";
        }
        if (transaction.getDueDate() == null || transaction.isOverdue()) {
            LocalDate due = transaction.getDueDate();
            long days = due != null ? java.time.temporal.ChronoUnit.DAYS.between(due, LocalDate.now()) : 0;
            return "Loan overdue by " + days + " day(s). Return it and settle fines.";
        }
        if (caller == null || !policy.isOwnerOrStaff(caller, transaction.getUserPhone())) {
            return "Not allowed.";
        }
        if (fineService != null && fineService.hasPendingFineForUser(transaction.getUserPhone())) {
            return "Settle outstanding fines first.";
        }
        if (!policy.canRequestBook(transaction.getUserPhone())) {
            List<String> reasons = policy.getBorrowingEligibilityFailureReasons(transaction.getUserPhone());
            return "Member not eligible: " + String.join(", ", reasons);
        }
        return null;
    }

    public String getMarkConditionFailureReason(int bookCopyId, User librarian, boolean lost) {
        if (!policy.canManageInventory(librarian)) {
            return "Not allowed.";
        }
        BookCopy copy = copies.findById(bookCopyId);
        if (copy == null) {
            return "Copy not found.";
        }
        if (copy.getStatus() == BookCopyStatus.LOST) {
            return "Copy already marked lost.";
        }
        if (copy.getStatus() == BookCopyStatus.DAMAGED) {
            return "Copy already marked damaged.";
        }
        Transaction transaction = transactions.findActiveByCopyId(bookCopyId);
        boolean wasIssued = transaction != null && transaction.getStatus() == TransactionStatus.ISSUED;
        if (wasIssued && fineService == null) {
            return "Fine service unavailable.";
        }
        if (copy.getStatus() == BookCopyStatus.RESERVED && transaction == null) {
            return "Reservation record missing.";
        }
        if (transaction != null && transaction.getStatus() == TransactionStatus.ISSUED && copy.getStatus() != BookCopyStatus.ISSUED) {
            return "Copy status does not match loan.";
        }
        if (transaction != null && (transaction.getStatus() == TransactionStatus.RESERVED || transaction.getStatus() == TransactionStatus.READY_FOR_PICKUP) && copy.getStatus() != BookCopyStatus.RESERVED) {
            return "Copy status does not match reservation.";
        }
        return null;
    }
}
