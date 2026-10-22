package ui;

import enums.*;
import model.*;

import java.util.List;

public class Librarian extends Main {

    public static void librarian() {
        if (currentUser == null || currentUser.getStatus() != UserStatus.ACTIVE
                || !authService.hasRole(currentUser, UserRole.LIBRARIAN)) {
            System.out.println("Please log in as Librarian or Admin");
            return;
        }
        while (true) {
            System.out.println("\nLibrarian Menu");
            System.out.println("1.View All Books");
            System.out.println("2.Search Books");
            System.out.println("3.Request Book");
            System.out.println("4.Cancel Request");
            System.out.println("5.Renew Book");
            System.out.println("6.Activate Membership");
            System.out.println("7.Renew Membership");
            System.out.println("8.View Currently Rented Books");
            System.out.println("9.View Borrowing History");
            System.out.println("10.Manage Fines");
            System.out.println("11.Update My Profile");
            System.out.println("12.Add Book");
            System.out.println("13.Remove Book");
            System.out.println("14.View Open Requests / Pickup Queue");
            System.out.println("15.Mark Copy Ready for Pickup");
            System.out.println("16.Issue Book");
            System.out.println("17.Process Return");
            System.out.println("18.Mark Copy Lost/Damaged");
            System.out.println("19.View Defaulters");
            System.out.println("20.View Current Fines");
            System.out.println("21.Manage Current Fines");
            System.out.println("22.Manage Membership Status");
            System.out.println("23.Manage Payments");
            System.out.println("24.Approve Family Join Requests");
            System.out.println("25.Add Member to Family");
            System.out.println("26.Get Family Join Code");
            System.out.println("27.View Audit Logs");
            System.out.println("28.Logout");
            System.out.println("Enter choice");
            int choice = readInt();
            switch (choice) {
                case 1: Member.handleViewAllBooks(); break;
                case 2: Member.handleSearchBooks(); break;
                case 3: Member.runForTargetMember(Member::handleRequestBook); break;
                case 4: Member.runForTargetMember(Member::handleCancelRequest); break;
                case 5: Member.runForTargetMember(Member::handleRenewBook); break;
                case 6: Member.runForTargetMember(Member::handleActivateMembership); break;
                case 7: Member.runForTargetMember(Member::handleRenewMembership); break;
                case 8: Member.runForTargetMember(Member::handleViewCurrentlyRentedBooks); break;
                case 9: Member.runForTargetMember(Member::handleViewBorrowingHistory); break;
                case 10: Member.runForTargetMember(Member::handleManageFines); break;
                case 11: Member.handleUpdateProfile(); break;
                case 12: handleAddBook(); break;
                case 13: handleRemoveBook(); break;
                case 14: handleViewPendingRequests(); break;
                case 15: handleMarkReadyForPickup(); break;
                case 16: handleIssueBook(); break;
                case 17: handleProcessReturn(); break;
                case 18: handleCopyIncident(); break;
                case 19: handleViewDefaulters(); break;
                case 20: viewCurrentFines(); break;
                case 21: manageCurrentFines(); break;
                case 22: handleManageMembershipStatus(); break;
                case 23: handleManagePayments(); break;
                case 24: Member.processFamilyJoinRequest(); break;
                case 25: Member.staffAddFamilyMember(); break;
                case 26: Member.getFamilyJoinCode(); break;
                case 27: handleViewAuditLogs(); break;
                case 28:
                    currentUser = null;
                    System.out.println("Logged out");
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    public static void handleAddBook() {
        System.out.println("Enter ISBN"); String isbn = sc.nextLine().trim();
        System.out.println("Enter title"); String title = sc.nextLine().trim();
        System.out.println("Enter author (comma-separated if multiple)"); String authorInput = sc.nextLine().trim();
        System.out.println("Enter genre (comma-separated if multiple)"); String genreInput = sc.nextLine().trim();
        System.out.println("Enter publisher"); String publisher = sc.nextLine().trim();
        System.out.println("Enter publication year"); int year = readInt();
        System.out.println("Enter description"); String description = sc.nextLine().trim();
        System.out.println("Enter number of copies"); int copies = readInt();
        if (isbn.isEmpty()) {
            System.out.println("Invalid book details: ISBN cannot be empty."); return;
        }
        if (title.isEmpty()) {
            System.out.println("Invalid book details: Title cannot be empty."); return;
        }
        if (authorInput.isEmpty()) {
            System.out.println("Invalid book details: Author cannot be empty."); return;
        }
        if (publisher.isEmpty()) {
            System.out.println("Invalid book details: Publisher cannot be empty."); return;
        }
        if (copies < 1) {
            System.out.println("Invalid book details: Number of copies must be at least 1."); return;
        }
        Book newBook = new Book(isbn, title, publisher, year, description);
        for (String aName : authorInput.split(",")) {
            String aTrimmed = aName.trim();
            if (!aTrimmed.isEmpty()) {
                Author a = bookService.getOrCreateAuthor(aTrimmed);
                if (a != null) newBook.addAuthor(a);
            }
        }
        if (!genreInput.isEmpty()) {
            for (String gName : genreInput.split(",")) {
                String gTrimmed = gName.trim();
                if (!gTrimmed.isEmpty()) {
                    Genre g = bookService.getOrCreateGenre(gTrimmed);
                    if (g != null) newBook.addGenre(g);
                }
            }
        }
        String addReason = bookService.getAddBookFailureReason(newBook, copies, currentUser);
        if (addReason != null) {
            System.out.println("Unable to add book: " + addReason);
        } else if (bookService.addBook(newBook, copies, currentUser)) {
            System.out.println("Book added successfully");
        } else {
            System.out.println("Unable to add book: Operation failed.");
        }
    }

    public static void handleRemoveBook() {
        System.out.println("Enter ISBN to remove"); String removeIsbn = sc.nextLine().trim();
        if (removeIsbn.isEmpty()) {
            System.out.println("ISBN cannot be empty"); return;
        }
        Book removeBook = bookService.getBookByIsbn(removeIsbn);
        if (removeBook == null) {
            System.out.println("Book not found with ISBN: " + removeIsbn);
        } else {
            String deleteReason = bookService.getDeleteBookFailureReason(removeBook.getBookId(), currentUser);
            if (deleteReason != null) {
                System.out.println("Unable to remove book: " + deleteReason);
            } else if (bookService.deleteBook(removeBook.getBookId(), currentUser)) {
                System.out.println("Book removed");
            } else {
                System.out.println("Unable to remove book.");
            }
        }
    }

    public static void handleViewPendingRequests() {
        List<Transaction> pending = bookTransactionService.getAllPendingRequests(currentUser);
        if (pending.isEmpty()) {
            System.out.println("No open requests.");
            return;
        }
        System.out.println("\nOpen Requests");
        System.out.printf("%-8s %-22s %-16s %-20s %-10s%n", "Req ID", "Member", "ISBN", "Status", "Copy ID");
        for (Transaction t : pending) {
            Book book = bookService.getBookById(t.getBookId());
            User member = userRepository.findByPhone(t.getUserPhone());
            String name = member == null ? t.getUserPhone() : member.getName();
            String isbn = book == null ? "-" : book.getIsbn();
            String copy = t.getBookCopyId() > 0 ? String.valueOf(t.getBookCopyId()) : "-";
            System.out.printf("%-8d %-22s %-16s %-20s %-10s%n",
                    t.getTransactionId(), truncate(name, 21), isbn, t.getStatus(), copy);
        }
    }

    public static void handleMarkReadyForPickup() {
        List<Transaction> pending = bookTransactionService.getAllPendingRequests(currentUser);
        List<Transaction> reserved = new java.util.ArrayList<>();
        for (Transaction t : pending) {
            if (t.getStatus() == TransactionStatus.RESERVED) reserved.add(t);
        }
        if (reserved.isEmpty()) {
            System.out.println("No copies awaiting preparation.");
            return;
        }
        System.out.println("Copies to prepare:");
        for (Transaction t : reserved) {
            Book requestedBook = bookService.getBookById(t.getBookId());
            User member = userRepository.findByPhone(t.getUserPhone());
            System.out.println("Request #" + t.getTransactionId() + " | "
                    + (member == null ? t.getUserPhone() : member.getName()) + " | "
                    + (requestedBook == null ? "Unknown book" : requestedBook.getTitle())
                    + " | Copy #" + t.getBookCopyId());
        }
        System.out.println("Request ID (0 to cancel):");
        int requestId = readInt();
        if (requestId == 0) return;
        boolean listed = false;
        for (Transaction transaction : reserved) {
            if (transaction.getTransactionId() == requestId) {
                listed = true;
                break;
            }
        }
        if (!listed) {
            System.out.println("Choose a request ID from the allocated list.");
            return;
        }
        String reason = bookTransactionService.getMarkReadyFailureReason(requestId, currentUser);
        if (reason != null) {
            System.out.println("Unable to mark ready: " + reason);
        } else if (bookTransactionService.markReadyForPickup(requestId, currentUser)) {
            System.out.println("Ready for pickup.");
        } else {
            System.out.println("Could not mark ready.");
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return "Unknown";
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    public static void handleIssueBook() {
        List<Transaction> open = bookTransactionService.getAllPendingRequests(currentUser);
        List<Transaction> ready = new java.util.ArrayList<>();
        for (Transaction t : open) {
            if (t.getStatus() == TransactionStatus.READY_FOR_PICKUP) ready.add(t);
        }
        if (ready.isEmpty()) {
            System.out.println("No books ready for pickup.");
            return;
        }
        System.out.println("Ready to issue:");
        for (Transaction t : ready) {
            Book book = bookService.getBookById(t.getBookId());
            User member = userRepository.findByPhone(t.getUserPhone());
            System.out.println("Request #" + t.getTransactionId() + " | "
                    + (member == null ? t.getUserPhone() : member.getName()) + " | "
                    + (book == null ? "Unknown book" : book.getTitle()) + " | Copy #" + t.getBookCopyId());
        }
        System.out.println("Enter request ID to issue (0 to go back):");
        int issueId = readInt();
        if (issueId == 0) return;
        boolean listed = false;
        for (Transaction transaction : ready) {
            if (transaction.getTransactionId() == issueId) {
                listed = true;
                break;
            }
        }
        if (!listed) {
            System.out.println("Choose a request ID from the ready-for-pickup list.");
            return;
        }
        String issueReason = bookTransactionService.getIssueFailureReason(issueId, currentUser);
        if (issueReason != null) {
            System.out.println("Unable to issue book: " + issueReason);
        } else if (bookTransactionService.issueBook(issueId, currentUser)) {
            System.out.println("Book issued.");
        } else {
            System.out.println("Could not issue book.");
        }
    }

    public static void handleProcessReturn() {
        System.out.println("Member phone:");
        String memberPhone = sc.nextLine().trim();
        if (memberPhone.isEmpty()) { System.out.println("Member phone number cannot be empty."); return; }
        User member = userRepository.findByPhone(memberPhone);
        if (member == null || member.getRole() != UserRole.MEMBER) {
            System.out.println("Member not found."); return;
        }
        System.out.println("Book ISBN:");
        String isbn = sc.nextLine().trim();
        if (isbn.isEmpty()) { System.out.println("ISBN cannot be empty."); return; }
        Book book = bookService.getBookByIsbn(isbn);
        if (book == null) { System.out.println("Book not found."); return; }

        List<Transaction> matchingLoans = bookTransactionService.findIssuedLoansForReturn(memberPhone, isbn, currentUser);
        if (matchingLoans.isEmpty()) {
            System.out.println("No matching issued loan."); return;
        }
        Transaction selectedLoan;
        if (matchingLoans.size() == 1) selectedLoan = matchingLoans.get(0);
        else {
            System.out.println("Select the copy being returned:");
            for (Transaction loan : matchingLoans)
                System.out.println("Transaction #" + loan.getTransactionId() + " | Copy #" + loan.getBookCopyId() + " | Due: " + loan.getDueDate());
            System.out.println("Enter transaction ID (0 to cancel):");
            int id = readInt();
            if (id == 0) return;
            selectedLoan = null;
            for (Transaction loan : matchingLoans) {
                if (loan.getTransactionId() == id) {
                    selectedLoan = loan;
                    break;
                }
            }
            if (selectedLoan == null) { System.out.println("Choose a transaction from the displayed list."); return; }
        }
        System.out.println("Condition: 1.Normal  2.Damaged  3.Lost");
        int condition = readInt();
        if (condition < 1 || condition > 3) { System.out.println("Invalid choice. No changes made."); return; }
        String conditionText = condition == 1 ? "normal" : condition == 2 ? "damaged" : "lost";
        System.out.println("Confirm " + conditionText + " processing for " + book.getTitle() + " | " + member.getName()
                + " | Copy #" + selectedLoan.getBookCopyId() + " (y/n)");
        String confirm = sc.nextLine().trim();
        if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) { System.out.println("Cancelled."); return; }

        if (condition == 1) {
            String reason = bookTransactionService.getReturnFailureReason(selectedLoan.getTransactionId(), currentUser);
            if (reason != null) System.out.println("Unable to process return: " + reason);
            else if (bookTransactionService.returnBook(selectedLoan.getTransactionId(), currentUser))
                System.out.println("Return processed.");
            else System.out.println("Could not process return.");
        } else {
            boolean lost = condition == 3;
            String reason = bookTransactionService.getMarkConditionFailureReason(selectedLoan.getBookCopyId(), currentUser, lost);
            if (reason != null) { System.out.println("Unable to process " + conditionText + " book: " + reason); return; }
            boolean ok = lost ? bookTransactionService.markCopyLost(selectedLoan.getBookCopyId(), currentUser)
                    : bookTransactionService.markCopyDamaged(selectedLoan.getBookCopyId(), currentUser);
            if (ok) System.out.println("Book marked " + conditionText.toUpperCase() + ". Fine created.");
            else System.out.println("Could not process book.");
        }
    }

    public static void handleCopyIncident() {
        System.out.println("Is the borrower identified? (y/n)");
        String answer = sc.nextLine().trim();
        if (answer.equalsIgnoreCase("y")) handleIdentifiedCopyIncident();
        else if (answer.equalsIgnoreCase("n")) handleUnidentifiedCopyIncident();
        else System.out.println("Enter y or n.");
    }

    public static void handleIdentifiedCopyIncident() {
        System.out.println("Member phone:");
        String phone = sc.nextLine().trim();
        User member = userRepository.findByPhone(phone);
        if (member == null || member.getRole() != UserRole.MEMBER) {
            System.out.println("Member not found."); return;
        }
        System.out.println("Copy ID:");
        int copyId = readInt();
        Transaction active = transactionRepository.findActiveByCopyId(copyId);
        if (active == null || active.getStatus() != TransactionStatus.ISSUED || !active.getUserPhone().equals(phone)) {
            System.out.println("No matching issued loan."); return;
        }
        System.out.println("1.Lost  2.Damaged  0.Cancel");
        int choice = readInt();
        if (choice == 0) return;
        if (choice != 1 && choice != 2) { System.out.println("Invalid choice."); return; }
        boolean lost = choice == 1;
        String reason = bookTransactionService.getMarkConditionFailureReason(copyId, currentUser, lost);
        if (reason != null) { System.out.println("Cannot update copy: " + reason); return; }
        boolean success = lost ? bookTransactionService.markCopyLost(copyId, currentUser)
                : bookTransactionService.markCopyDamaged(copyId, currentUser);
        System.out.println(success ? "Copy updated. Fine created." : "Could not update copy.");
    }

    public static void handleUnidentifiedCopyIncident() {
        System.out.println("Borrower cannot be identified. No member fine will be assigned.");
        System.out.println("1.Mark copy lost  2.Mark copy damaged  0.Cancel");
        int choice = readInt();
        if (choice == 0) return;
        if (choice != 1 && choice != 2) { System.out.println("Invalid choice."); return; }
        System.out.println("Enter copy ID:");
        int copyId = readInt();
        boolean lost = choice == 1;
        String reason = bookTransactionService.getMarkConditionFailureReason(copyId, currentUser, lost);
        if (reason != null) { System.out.println("Unable to update copy: " + reason); return; }
        boolean success = lost ? bookTransactionService.markCopyLostUnidentified(copyId, currentUser)
                : bookTransactionService.markCopyDamagedUnidentified(copyId, currentUser);
        if (success) System.out.println("Copy marked " + (lost ? "LOST" : "DAMAGED") + ". No fine was assigned because the borrower is unidentified.");
        else System.out.println("Unable to update copy condition.");
    }

    public static void handleViewDefaulters() {
        List<User> defaulters = policyService.getDefaulters(currentUser);
        if (defaulters.isEmpty()) System.out.println("No defaulters found");
        for (User user : defaulters) System.out.println(user.getName() + " | Phone: " + user.getPhone() + " | Email: " + user.getEmail());
    }

    public static void handleManageMembershipStatus() {
        System.out.println("Enter member phone number"); String memberPhone = sc.nextLine().trim();
        User member = userRepository.findByPhone(memberPhone);
        if (member == null || member.getRole() != UserRole.MEMBER) { System.out.println("Member not found"); return; }
        System.out.println("1.Suspend membership  2.Reactivate membership");
        int membershipChoice = readInt();
        if (membershipChoice == 1) {
            String suspendReason = membershipService.getSuspendFailureReason(currentUser, memberPhone);
            if (suspendReason != null) {
                System.out.println("Unable to update membership status: " + suspendReason);
            } else if (membershipService.suspendMembership(currentUser, memberPhone)) {
                System.out.println("Membership status updated");
            } else {
                System.out.println("Unable to update membership status: Could not suspend active membership.");
            }
        } else if (membershipChoice == 2) {
            String reactivateReason = membershipService.getReactivateFailureReason(currentUser, memberPhone);
            if (reactivateReason != null) {
                System.out.println("Unable to update membership status: " + reactivateReason);
            } else if (membershipService.reactivateMembership(currentUser, memberPhone)) {
                System.out.println("Membership status updated");
            } else {
                System.out.println("Unable to update membership status: Could not reactivate suspended membership.");
            }
        } else {
            System.out.println("Invalid choice");
        }
    }

    public static void handleManagePayments() {
        for (Payment payment : paymentRepository.findAll()) {
            System.out.println("Payment ID: " + payment.getPaymentId() + ", Phone: " + payment.getUserPhone()
                    + ", Amount: Rs." + payment.getAmount() + ", Status: " + payment.getStatus());
        }
        System.out.println("Enter payment ID to refund, or 0 to return"); int paymentId = readInt();
        if (paymentId != 0) {
            String refundReason = paymentService.getRefundFailureReason(paymentId, currentUser);
            if (refundReason != null) {
                System.out.println("Refund failed: " + refundReason);
            } else if (paymentService.refund(paymentId, currentUser)) {
                System.out.println("Refund processed");
            } else {
                System.out.println("Refund failed: Payment gateway error during refund.");
            }
        }
    }

    public static void viewCurrentFines() {
        while (true) {
            System.out.println("\n--- View Current Fines ---");
            System.out.println("1.All Fines");
            System.out.println("2.Pending Fines");
            System.out.println("3.Partially Paid Fines");
            System.out.println("4.Paid Fines");
            System.out.println("5.Waived Fines");
            System.out.println("6.Filter by Member Phone");
            System.out.println("7.Filter by Transaction ID");
            System.out.println("8.Back");
            System.out.println("Enter choice:");

            int filterChoice = readInt();
            if (filterChoice == 8) {
                return;
            }

            List<Fine> finesToDisplay;
            switch (filterChoice) {
                case 1:
                    finesToDisplay = fineService.getAllFines(currentUser);
                    break;
                case 2:
                    finesToDisplay = fineService.getFinesByStatus(FineStatus.PENDING, currentUser);
                    break;
                case 3:
                    finesToDisplay = fineService.getFinesByStatus(FineStatus.PARTIALLY_PAID, currentUser);
                    break;
                case 4:
                    finesToDisplay = fineService.getFinesByStatus(FineStatus.PAID, currentUser);
                    break;
                case 5:
                    finesToDisplay = fineService.getFinesByStatus(FineStatus.WAIVED, currentUser);
                    break;
                case 6:
                    System.out.println("Member phone:");
                    String phone = sc.nextLine().trim();
                    finesToDisplay = fineService.findByUserPhone(phone, currentUser);
                    break;
                case 7:
                    System.out.println("Enter transaction ID:");
                    int txId = readInt();
                    finesToDisplay = fineService.findByTransactionId(txId, currentUser);
                    break;
                default:
                    System.out.println("Invalid choice");
                    continue;
            }

            if (finesToDisplay.isEmpty()) {
                System.out.println("No fines found");
            } else {
                System.out.println("\n--- Fines List (" + finesToDisplay.size() + ") ---");
                for (Fine fine : finesToDisplay) {
                    printFineSummary(fine);
                }
            }
        }
    }

    static void printFineSummary(Fine fine) {
        User member = userRepository.findByPhone(fine.getUserPhone());
        int memberId = fine.getUserId() > 0 ? fine.getUserId() : (member != null ? member.getUserId() : 0);
        String memberIdDisplay = memberId > 0 ? String.valueOf(memberId) : "N/A";
        System.out.println("Fine ID: " + fine.getFineId()
                + " | Member ID: " + memberIdDisplay
                + " | Phone: " + fine.getUserPhone()
                + " | Transaction ID: " + fine.getTransactionId()
                + " | Type: " + fine.getType()
                + " | Original Amount: Rs." + String.format("%.2f", fine.getAmount())
                + " | Paid: Rs." + String.format("%.2f", fine.getAmountPaid())
                + " | Outstanding Balance: Rs." + String.format("%.2f", fine.getRemainingAmount())
                + " | Status: " + fine.getStatus());
    }

    public static void manageCurrentFines() {
        System.out.println("\n--- Manage Current Fines ---");
        System.out.println("Member phone:");
        String phone = sc.nextLine().trim();
        if (phone.isEmpty()) {
            System.out.println("Phone number cannot be empty.");
            return;
        }

        User member = userRepository.findByPhone(phone);
        if (member == null) {
            System.out.println("Member not found with phone: " + phone);
            return;
        }

        while (true) {
            List<Fine> fines = fineService.findByUserPhone(phone, currentUser);
            if (fines == null || fines.isEmpty()) {
                System.out.println("No fines found for member: " + phone);
                return;
            }

            System.out.println("\n--- Fines for Member " + phone + " ---");
            for (int i = 0; i < fines.size(); i++) {
                Fine f = fines.get(i);
                System.out.println((i + 1) + ". Fine ID: " + f.getFineId()
                        + " | Transaction ID: " + f.getTransactionId()
                        + " | Fine Amount: Rs." + String.format(java.util.Locale.ROOT, "%.2f", f.getAmount())
                        + " | Amount Paid: Rs." + String.format(java.util.Locale.ROOT, "%.2f", f.getAmountPaid())
                        + " | Remaining Amount: Rs." + String.format(java.util.Locale.ROOT, "%.2f", f.getRemainingAmount())
                        + " | Status: " + f.getStatus());
            }

            System.out.println("Enter the fine number to manage, or 0 to go back:");
            int selection = readInt();
            if (selection == 0) {
                return;
            }
            if (selection < 1 || selection > fines.size()) {
                System.out.println("Invalid selection. Selection outside the displayed list.");
                continue;
            }

            Fine selectedFine = fines.get(selection - 1);
            int fineId = selectedFine.getFineId();

            manageSelectedFine(fineId);
        }
    }

    private static void manageSelectedFine(int fineId) {
        while (true) {
            Fine fine = fineService.findById(fineId, currentUser);
            if (fine == null) {
                System.out.println("Fine not found with ID: " + fineId);
                return;
            }

            System.out.println("\n--- Fine Details ---");
            System.out.println("Fine ID: " + fine.getFineId()
                    + " | Transaction ID: " + fine.getTransactionId()
                    + " | Fine Amount: Rs." + String.format(java.util.Locale.ROOT, "%.2f", fine.getAmount())
                    + " | Amount Paid: Rs." + String.format(java.util.Locale.ROOT, "%.2f", fine.getAmountPaid())
                    + " | Remaining Amount: Rs." + String.format(java.util.Locale.ROOT, "%.2f", fine.getRemainingAmount())
                    + " | Status: " + fine.getStatus());

            System.out.println("\nActions for Fine ID: " + fineId);
            System.out.println("1.Pay Fine");
            System.out.println("2.Waive Fine");
            System.out.println("3.Back");
            System.out.println("Enter choice:");

            int action = readInt();
            switch (action) {
                case 1:
                    paySelectedFine(fineId);
                    return;
                case 2:
                    waiveSelectedFine(fineId);
                    return;
                case 3:
                case 0:
                    return;
                default:
                    System.out.println("Invalid choice. Please select 1, 2, or 3.");
            }
        }
    }

    private static void paySelectedFine(int fineId) {
        Fine fine = fineService.findById(fineId, currentUser);
        if (fine == null) {
            System.out.println("Fine not found with ID: " + fineId);
            return;
        }

        if (fine.getStatus() == FineStatus.PAID) {
            System.out.println("Fine is already fully paid");
            return;
        }
        if (fine.getStatus() == FineStatus.WAIVED) {
            System.out.println("Fine has been waived and cannot be paid");
            return;
        }

        System.out.println("Remaining Balance: Rs." + String.format(java.util.Locale.ROOT, "%.2f", fine.getRemainingAmount()));
        System.out.println("Enter payment amount:");
        double amount = readDouble();
        if (amount <= 0) {
            System.out.println("Invalid payment amount: Amount must be greater than zero.");
            return;
        }
        if (amount > fine.getRemainingAmount()) {
            System.out.println("Invalid payment amount: Rs." + String.format(java.util.Locale.ROOT, "%.2f", amount)
                    + " exceeds outstanding balance of Rs."
                    + String.format(java.util.Locale.ROOT, "%.2f", fine.getRemainingAmount()) + ".");
            return;
        }

        PaymentMethod method = UiHelpers.choosePaymentMethod();
        if (method == null) {
            System.out.println("Payment cancelled");
            return;
        }

        Payment payment = paymentService.payFine(fine.getUserPhone(), fineId, amount, method);
        if (payment != null && payment.isSuccessful()) {
            System.out.println("Payment recorded successfully (Payment ID: " + payment.getPaymentId()
                    + ", Reference: " + payment.getTransactionReference() + ")");
            Fine updatedFine = fineService.findById(fineId, currentUser);
            if (updatedFine != null) {
                System.out.println("\n--- Updated Fine Details ---");
                System.out.println("Fine ID: " + updatedFine.getFineId()
                        + " | Transaction ID: " + updatedFine.getTransactionId()
                        + " | Fine Amount: Rs." + String.format(java.util.Locale.ROOT, "%.2f", updatedFine.getAmount())
                        + " | Amount Paid: Rs." + String.format(java.util.Locale.ROOT, "%.2f", updatedFine.getAmountPaid())
                        + " | Remaining Amount: Rs." + String.format(java.util.Locale.ROOT, "%.2f", updatedFine.getRemainingAmount())
                        + " | Status: " + updatedFine.getStatus());
            }
        } else {
            String payReason = paymentService.getPayFineFailureReason(fine.getUserPhone(), fineId, amount, method);
            System.out.println("Payment failed: " + (payReason != null ? payReason : "Payment gateway was unable to process the transaction."));
        }
    }

    private static void waiveSelectedFine(int fineId) {
        Fine fine = fineService.findById(fineId, currentUser);
        if (fine == null) {
            System.out.println("Fine not found with ID: " + fineId);
            return;
        }
        if (fine.getStatus() == FineStatus.PAID) {
            System.out.println("Paid fine cannot be waived");
            return;
        }
        if (fine.getStatus() == FineStatus.WAIVED) {
            System.out.println("Fine is already waived");
            return;
        }

        if (fineService.waiveFine(fineId, currentUser)) {
            System.out.println("Fine waived successfully");
            Fine updated = fineService.findById(fineId, currentUser);
            if (updated != null) {
                System.out.println("\n--- Updated Fine Details ---");
                System.out.println("Fine ID: " + updated.getFineId()
                        + " | Transaction ID: " + updated.getTransactionId()
                        + " | Fine Amount: Rs." + String.format(java.util.Locale.ROOT, "%.2f", updated.getAmount())
                        + " | Amount Paid: Rs." + String.format(java.util.Locale.ROOT, "%.2f", updated.getAmountPaid())
                        + " | Remaining Amount: Rs." + String.format(java.util.Locale.ROOT, "%.2f", updated.getRemainingAmount())
                        + " | Status: " + updated.getStatus());
            }
        } else {
            String waiveReason = fineService.getWaiveFineFailureReason(fineId, currentUser);
            System.out.println("Unable to waive fine: " + (waiveReason != null ? waiveReason : "Fine status could not be changed to waived."));
        }
    }
}
