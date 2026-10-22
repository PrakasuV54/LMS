package ui;
import model.*;
import enums.*;
import service.AnalyticsService;
import service.AuthService;
import service.BookService;
import service.BookTransactionService;

import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import repository.*;
import service.*;
import authentication.*;
import paymentStrategy.*;

import  repository.SyntheticData.*;

public class Main {

    static Scanner sc = new Scanner(System.in);
    static User currentUser = null;

    public static int readInt() {
        while (true) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number:");
            }
        }
    }

    public static double readDouble() {
        while (true) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid amount:");
            }
        }
    }



    public static void main(String[] args) {
        SyntheticData data = new SyntheticData(userRepository,membershipPlanRepository,bookService);
        data.syntheticData();
        while (true) {
            System.out.println("\nSelect User");
            System.out.println("1.Member");
            System.out.println("2.Librarian");
            System.out.println("3.Admin");
            System.out.println("4.Exit");
            System.out.println();
            int userChoice = readInt();
            switch (userChoice) {
                case 1:
                    Member.member();
                    break;
                case 2:
                    if (staffLogin(UserRole.LIBRARIAN)) {
                        if (currentUser != null && currentUser.getRole() == UserRole.ADMIN) {
                            Admin.admin();
                        } else {
                            Librarian.librarian();
                        }
                    }
                    break;
                case 3:
                    if (staffLogin(UserRole.ADMIN)) {
                        Admin.admin();
                    }
                    break;
                case 4:
                    System.out.println("Exiting Library Management System. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid Choice");
            }
        }
    }

    public static void handleViewAuditLogs() {
        while (true) {
            System.out.println("\nAudit Logs");
            System.out.println("1. View all");
            System.out.println("2. Filter by date");
            System.out.println("3. Filter by week");
            System.out.println("4. Filter by month");
            System.out.println("5. Filter by year");
            System.out.println("6. Filter by role");
            System.out.println("0. Back");
            System.out.println("Enter choice:");
            int choice = readInt();
            if (choice == 0) return;

            List<Audit> logs = auditService.findAll();
            LocalDateTime start = null;
            LocalDateTime end = null;
            UserRole roleFilter = null;

            try {
                switch (choice) {
                    case 1:
                        break;
                    case 2: {
                        System.out.println("Date (YYYY-MM-DD):");
                        LocalDate date = LocalDate.parse(sc.nextLine().trim());
                        start = date.atStartOfDay();
                        end = date.plusDays(1).atStartOfDay();
                        break;
                    }
                    case 3: {
                        System.out.println("Enter any date in the week (YYYY-MM-DD):");
                        LocalDate date = LocalDate.parse(sc.nextLine().trim());
                        LocalDate monday = date.minusDays(date.getDayOfWeek().getValue() - 1);
                        start = monday.atStartOfDay();
                        end = monday.plusDays(7).atStartOfDay();
                        break;
                    }
                    case 4: {
                        System.out.println("Month (YYYY-MM):");
                        YearMonth month = YearMonth.parse(sc.nextLine().trim());
                        start = month.atDay(1).atStartOfDay();
                        end = month.plusMonths(1).atDay(1).atStartOfDay();
                        break;
                    }
                    case 5: {
                        System.out.println("Year (YYYY):");
                        int year = Integer.parseInt(sc.nextLine().trim());
                        start = LocalDate.of(year, 1, 1).atStartOfDay();
                        end = LocalDate.of(year + 1, 1, 1).atStartOfDay();
                        break;
                    }
                    case 6: {
                        System.out.println("Role: 1.Member  2.Librarian  3.Admin");
                        int roleChoice = readInt();
                        if (roleChoice < 1 || roleChoice > 3) {
                            System.out.println("Invalid role.");
                            continue;
                        }
                        roleFilter = roleChoice == 1 ? UserRole.MEMBER
                                : roleChoice == 2 ? UserRole.LIBRARIAN : UserRole.ADMIN;
                        break;
                    }
                    default:
                        System.out.println("Invalid choice.");
                        continue;
                }
            } catch (NumberFormatException | java.time.DateTimeException e) {
                System.out.println("Invalid date or year.");
                continue;
            }

            List<Audit> filtered = new ArrayList<>();
            for (Audit audit : logs) {
                LocalDateTime timestamp = audit.getTimestamp();
                if (start != null && (timestamp.isBefore(start) || !timestamp.isBefore(end))) continue;
                if (roleFilter != null) {
                    User actor = audit.getUserPhone() == null ? null : userRepository.findByPhone(audit.getUserPhone());
                    if (actor == null || actor.getRole() != roleFilter) continue;
                }
                filtered.add(audit);
            }

            if (filtered.isEmpty()) {
                System.out.println("No audit records found.");
                continue;
            }

            filtered.sort(Comparator.comparing(Audit::getTimestamp).reversed());
            System.out.printf("%-7s %-20s %-12s %-18s %-20s %-19s %s%n",
                    "ID", "Actor phone", "Role", "Action", "Entity", "Timestamp", "Details");
            for (Audit audit : filtered) {
                User actor = audit.getUserPhone() == null ? null : userRepository.findByPhone(audit.getUserPhone());
                String actorRole = actor == null ? "UNKNOWN" : actor.getRole().name();
                String entity = audit.getEntityType() + (audit.getEntityId() == null ? "" : "#" + audit.getEntityId());
                System.out.printf("%-7d %-20s %-12s %-18s %-20s %-19s %s%n",
                        audit.getAuditId(),
                        audit.getUserPhone() == null ? "-" : audit.getUserPhone(),
                        actorRole,
                        truncateAudit(audit.getAction(), 17),
                        truncateAudit(entity, 19),
                        audit.getTimestamp().toString().replace('T', ' '),
                        audit.getDetails() == null ? "-" : audit.getDetails());
            }
            System.out.println("Records: " + filtered.size());
        }
    }

    private static String truncateAudit(String value, int maxLength) {
        if (value == null) return "-";
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    private static boolean staffLogin(UserRole requiredRole) {
        System.out.println("Enter phone or email");
        String identifier = sc.nextLine().trim();
        System.out.println("Enter password");
        String password = sc.nextLine();

        if (identifier.isEmpty() || password.isEmpty()) {
            System.out.println("Login Failed: Phone/Email and password cannot be empty. Please enter your credentials.");
            return false;
        }

        User user = authService.login(identifier, password);
        if (user == null) {
            User existing = userRepository.findByPhone(identifier);
            if (existing == null) existing = userRepository.findByEmail(identifier);
            if (existing != null && existing.getStatus() == UserStatus.INACTIVE) {
                System.out.println("Login Failed: Your account has been deactivated. Please contact the administrator.");
            } else {
                System.out.println("Login Failed: Invalid credentials. Please check your phone/email and password.");
            }
            return false;
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            currentUser = null;
            System.out.println("Login Failed: Your account has been deactivated. Please contact the administrator.");
            return false;
        }

        if (user.getRole() == UserRole.MEMBER) {
            currentUser = null;
            System.out.println("Login Failed: This account is registered as MEMBER. Please log in via the Member menu.");
            return false;
        }

        if (requiredRole == UserRole.ADMIN && user.getRole() != UserRole.ADMIN) {
            currentUser = null;
            System.out.println("Login Failed: This account is registered as " + user.getRole() + ". Administrator privileges required.");
            return false;
        }

        currentUser = user;
        System.out.println("Login successful");
        return true;
    }



    




    static DataStore dataStore = new DataStore();
    static UserRepository userRepository = new InMemoryUserRepository(dataStore);
    static BookRepository bookRepository = new InMemoryBookRepository(dataStore);
    static BookCopyRepository bookCopyRepository = new InMemoryBookCopyRepository(dataStore);
    static TransactionRepository transactionRepository = new InMemoryTransactionRepository(dataStore);
    static MembershipRepository membershipRepository = new InMemoryMembershipRepository(dataStore);
    static FineRepository fineRepository = new InMemoryFineRepository(dataStore);
    static PaymentRepository paymentRepository = new InMemoryPaymentRepository(dataStore);
    static MembershipPlanRepository membershipPlanRepository = new InMemoryMembershipPlanRepository(dataStore);
    static AuditLogRepository auditLogRepository = new InMemoryAuditLogRepository(dataStore);
    static AuditService auditService = new AuditService(auditLogRepository);
    static NotificationService notificationService = new NotificationService();
    static PolicyService policyService = new PolicyService(userRepository, membershipRepository, transactionRepository, fineRepository, auditService, membershipPlanRepository);
    static FineService fineService = new FineService(fineRepository, transactionRepository, policyService, auditService);
    static UserService userService = new UserService(userRepository, auditService);
    static AuthService authService = new AuthService(userService, userRepository, new PasswordAuthentication(), auditService);
    static BookService bookService = new BookService(bookRepository, bookCopyRepository, transactionRepository, auditService);
    static BookTransactionService bookTransactionService = new BookTransactionService(bookRepository, bookCopyRepository, transactionRepository, policyService, auditService, notificationService, fineService);
    static PaymentService paymentService = new PaymentService(paymentRepository, fineRepository, new Razorpay(), auditService, membershipRepository, transactionRepository);
    static MembershipPlanService membershipPlanService = new MembershipPlanService(membershipPlanRepository, userRepository);
    static MembershipService membershipService = new MembershipService(userRepository, membershipRepository, membershipPlanService, paymentRepository, new Razorpay(), auditService, notificationService);
    static AnalyticsService analyticsService = new AnalyticsService(bookRepository, transactionRepository, userRepository);

}
