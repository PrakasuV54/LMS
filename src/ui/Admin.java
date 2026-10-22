package ui;

import enums.*;
import model.*;

public class Admin extends Main {

    public static void admin() {
        if (currentUser == null || currentUser.getStatus() != UserStatus.ACTIVE || !authService.hasRole(currentUser, UserRole.ADMIN)) {
            System.out.println("Please log in as Admin"); return;
        }
        while (true) {
            System.out.println("\nAdmin Menu");
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
            System.out.println("24.Dashboard");
            System.out.println("25.Manage Users");
            System.out.println("26.Manage Policies");
            System.out.println("27.Manage Membership Plans");
            System.out.println("28.Approve Family Join Requests");
            System.out.println("29.Add Member to Family");
            System.out.println("30.Get Family Join Code");
            System.out.println("31.View Audit Logs");
            System.out.println("32.Logout");
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
                case 12: Librarian.handleAddBook(); break;
                case 13: Librarian.handleRemoveBook(); break;
                case 14: Librarian.handleViewPendingRequests(); break;
                case 15: Librarian.handleMarkReadyForPickup(); break;
                case 16: Librarian.handleIssueBook(); break;
                case 17: Librarian.handleProcessReturn(); break;
                case 18: Librarian.handleCopyIncident(); break;
                case 19: Librarian.handleViewDefaulters(); break;
                case 20: Librarian.viewCurrentFines(); break;
                case 21: Librarian.manageCurrentFines(); break;
                case 22: Librarian.handleManageMembershipStatus(); break;
                case 23: Librarian.handleManagePayments(); break;
                case 24: handleDashboard(); break;
                case 25: handleManageUsers(); break;
                case 26: handleManagePolicies(); break;
                case 27: handleManageMembershipPlans(); break;
                case 28: Member.processFamilyJoinRequest(); break;
                case 29: Member.staffAddFamilyMember(); break;
                case 30: Member.getFamilyJoinCode(); break;
                case 31: handleViewAuditLogs(); break;
                case 32:
                    currentUser = null;
                    System.out.println("Logged out");
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    public static void handleDashboard() {
        System.out.println("Enter year for dashboard"); int year = readInt();
        AnalyticsDashboard dashboard = analyticsService.getDashboardForYear(year);
        System.out.println("Member/user registrations by age group for " + year + ": " + dashboard.getRegistrationsByAgeGroup());
        System.out.println("Member/user registrations by gender for " + year + ": " + dashboard.getRegistrationsByGender());
        System.out.println("Most borrowed books by year: " + dashboard.getMostBorrowedBooksForYear());
        System.out.println("Most borrowed books by month: " + dashboard.getMostBorrowedBooksByMonth());
        System.out.println("Most borrowed books by quarter: " + dashboard.getMostBorrowedBooksByQuarter());
        System.out.println("Total users: " + userRepository.findAll().size());
        double membershipEarnings = 0, finePayments = 0;
        for (Payment payment : paymentRepository.findAll()) if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (payment.isMembershipPayment()) membershipEarnings += payment.getAmount(); else finePayments += payment.getAmount();
        }
        System.out.println("Membership earnings: Rs." + membershipEarnings);
        System.out.println("Fine payment earnings: Rs." + finePayments);
    }

    public static void handleManageUsers() {
        System.out.println("1.View users  2.Add librarian  3.Add admin  4.Activate account  5.Deactivate account  6.Change role  7.Delete user");
        int userChoice = readInt();
        if (userChoice == 1) {
            java.util.List<User> users = userService.getAllUsers(currentUser);
            if (users == null || users.isEmpty()) {
                System.out.println("No users found.");
            } else {
                System.out.println("\n--- Registered Users (" + users.size() + ") ---");
                for (User user : users) {
                    System.out.println("User ID: " + user.getUserId()
                            + " | Name: " + user.getName()
                            + " | Email: " + user.getEmail()
                            + " | Phone: " + user.getPhone()
                            + " | Role: " + user.getRole()
                            + " | Status: " + user.getStatus()
                            + " | Created: " + user.getCreatedAt());
                }
            }
        } else if (userChoice == 2 || userChoice == 3) {
            System.out.println("Enter name"); String name = sc.nextLine().trim();
            System.out.println("Enter email"); String email = sc.nextLine().trim();
            System.out.println("Enter phone"); String phone = sc.nextLine().trim();
            System.out.println("Enter password"); String password = sc.nextLine();
            UserRole role = userChoice == 2 ? UserRole.LIBRARIAN : UserRole.ADMIN;
            String reason = userService.getAddStaffFailureReason(currentUser, name, email, phone, password, role);
            if (reason != null) {
                System.out.println("Unable to create user: " + reason);
            } else {
                boolean success = userChoice == 2 ? userService.addLibrarian(currentUser, name, email, phone, password)
                        : userService.addAdmin(currentUser, name, email, phone, password);
                System.out.println(success ? "User created" : "Unable to create user: Failed to save user account.");
            }
        } else if (userChoice >= 4 && userChoice <= 7) {
            System.out.println("Enter user ID"); int userId = readInt();
            UserRole targetRole = null;
            if (userChoice == 6) {
                System.out.println("1.Member 2.Librarian 3.Admin"); int roleChoice = readInt();
                targetRole = roleChoice == 1 ? UserRole.MEMBER : roleChoice == 2 ? UserRole.LIBRARIAN : roleChoice == 3 ? UserRole.ADMIN : null;
            }
            String updateReason = userService.getUserUpdateFailureReason(currentUser, userId, userChoice, targetRole);
            if (updateReason != null) {
                System.out.println("Unable to update user: " + updateReason);
            } else {
                boolean success = false;
                if (userChoice == 4) success = userService.activateAccount(currentUser, userId);
                if (userChoice == 5) success = userService.deactivateAccount(currentUser, userId);
                if (userChoice == 6 && targetRole != null) success = userService.changeUserRole(currentUser, userId, targetRole);
                if (userChoice == 7) success = userService.deleteUser(currentUser, userId);
                System.out.println(success ? "User updated" : "Unable to update user: The requested account change could not be applied.");
            }
        } else System.out.println("Invalid choice");
    }

    public static void handleManagePolicies() {
        java.util.List<MembershipPlan> plans = membershipPlanService.getAllPlans();
        if (plans.isEmpty()) {
            System.out.println("No membership plans exist. Create a plan first.");
            return;
        }
        System.out.println("\\nSelect the plan whose policies you want to manage:");
        for (MembershipPlan plan : plans) {
            System.out.println("Plan ID: " + plan.getPlanId() + " | " + plan.getPlanName()
                    + " | Max books: " + plan.getMaxBooks()
                    + " | Loan days: " + plan.getLoanPeriodDays()
                    + " | Fine/day: Rs." + plan.getFinePerDay()
                    + " | Lost: Rs." + plan.getLostBookCharge()
                    + " | Damaged: Rs." + plan.getDamagedBookCharge());
        }
        System.out.println("Enter Plan ID (0 to cancel)");
        int planId = readInt();
        if (planId == 0) return;
        MembershipPlan selected = membershipPlanService.findById(planId);
        if (selected == null) {
            System.out.println("Policy update failed: Plan ID not found.");
            return;
        }

        System.out.println("Managing policies for " + selected.getPlanName() + " (ID " + planId + ")");
        System.out.println("1.Maximum books (" + selected.getMaxBooks() + ")");
        System.out.println("2.Loan days (" + selected.getLoanPeriodDays() + ")");
        System.out.println("3.Fine per day (Rs." + selected.getFinePerDay() + ")");
        System.out.println("4.Lost book charge (Rs." + selected.getLostBookCharge() + ")");
        System.out.println("5.Damaged book charge (Rs." + selected.getDamagedBookCharge() + ")");
        int choice = readInt();

        int maxBooks = selected.getMaxBooks();
        int loanDays = selected.getLoanPeriodDays();
        double fine = selected.getFinePerDay();
        double lost = selected.getLostBookCharge();
        double damaged = selected.getDamagedBookCharge();

        switch (choice) {
            case 1:
                System.out.println("Enter maximum books allowed for " + selected.getPlanName());
                maxBooks = readInt();
                if (maxBooks < 1) { System.out.println("Maximum books must be at least 1."); return; }
                break;
            case 2:
                System.out.println("Enter loan period in days for " + selected.getPlanName());
                loanDays = readInt();
                if (loanDays < 1) { System.out.println("Loan period must be at least 1 day."); return; }
                break;
            case 3:
                System.out.println("Enter fine per overdue day for " + selected.getPlanName());
                fine = readDouble();
                if (fine < 0) { System.out.println("Fine cannot be negative."); return; }
                break;
            case 4:
                System.out.println("Enter lost book charge for " + selected.getPlanName());
                lost = readDouble();
                if (lost < 0) { System.out.println("Charge cannot be negative."); return; }
                break;
            case 5:
                System.out.println("Enter damaged book charge for " + selected.getPlanName());
                damaged = readDouble();
                if (damaged < 0) { System.out.println("Charge cannot be negative."); return; }
                break;
            default:
                System.out.println("Invalid choice");
                return;
        }

        MembershipPlan updated = membershipPlanService.updatePlanPolicies(currentUser, planId,
                maxBooks, loanDays, fine, lost, damaged);
        System.out.println(updated != null
                ? "Policy updated for " + updated.getPlanName() + " only."
                : "Policy update failed: verify administrator permissions and plan criteria.");
    }

    public static void handleManageMembershipPlans() {
        System.out.println("1.View All Plans  2.Create Plan");
        int planChoice = readInt();
        if (planChoice == 1) {
            for (MembershipPlan p : membershipPlanService.getAllPlans()) {
                System.out.println("Plan ID: " + p.getPlanId() + ", Name: " + p.getPlanName()
                        + ", Amount: Rs." + p.getAmount()
                        + ", Max books: " + p.getMaxBooks()
                        + ", Loan days: " + p.getLoanPeriodDays()
                        + ", Fine/day: Rs." + p.getFinePerDay()
                        + ", Lost charge: Rs." + p.getLostBookCharge()
                        + ", Damaged charge: Rs." + p.getDamagedBookCharge());
            }
        } else if (planChoice == 2) {
            System.out.println("Enter plan name");
            String planName = sc.nextLine().trim();
            System.out.println("Enter plan amount");
            double planAmount = readDouble();
            System.out.println("Enter maximum books that can be borrowed at once");
            int maxBooks = readInt();
            System.out.println("Enter loan period in days");
            int loanDays = readInt();
            System.out.println("Enter fine per overdue day (Rs.; 0 to disable)");
            double finePerDay = readDouble();
            System.out.println("Enter lost book charge (Rs.; 0 if not applicable)");
            double lostCharge = readDouble();
            System.out.println("Enter damaged book charge (Rs.; 0 if not applicable)");
            double damagedCharge = readDouble();

            String planReason = membershipPlanService.getCreatePlanFailureReason(currentUser, planName, planAmount);
            if (planReason != null) {
                System.out.println("Unable to create plan: " + planReason);
            } else if (maxBooks < 1 || loanDays < 1 || finePerDay < 0 || lostCharge < 0 || damagedCharge < 0) {
                System.out.println("Unable to create plan: maximum books and loan days must be at least 1; charges cannot be negative.");
            } else {
                MembershipPlan created = membershipPlanService.createPlan(currentUser, planName, planAmount,
                        maxBooks, loanDays, finePerDay, lostCharge, damagedCharge);
                System.out.println(created != null ? "Plan created successfully."
                        : "Unable to create plan.");
            }
        } else {
            System.out.println("Invalid choice");
        }
    }
}
