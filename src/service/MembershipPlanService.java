package service;

import model.MembershipPlan;
import model.User;
import enums.UserRole;
import enums.UserStatus;
import repository.UserRepository;
import repository.DataStore;
import repository.MembershipPlanRepository;
import java.util.List;


public class MembershipPlanService {
    public static final String DEFAULT_STANDARD_PLAN_NAME = "Standard Plan";
    public static final double DEFAULT_STANDARD_PLAN_PRICE = 399.0;
    public static final String DEFAULT_PREMIUM_PLAN_NAME = "Premium Plan";
    public static final double DEFAULT_PREMIUM_PLAN_PRICE = 599.0;
    public static final String DEFAULT_FAMILY_PLAN_NAME = "Family Plan";
    public static final double DEFAULT_FAMILY_PLAN_PRICE = 1599.0;
    public static final String DEFAULT_FAMILY_PREMIUM_PLAN_NAME = "Family Premium";
    public static final double DEFAULT_FAMILY_PREMIUM_PLAN_PRICE = 2399.0;

    private final MembershipPlanRepository membershipPlanRepository;
    private final UserRepository users;


    public MembershipPlanService(MembershipPlanRepository membershipPlanRepository, UserRepository users) {
        if (membershipPlanRepository == null) {
            throw new IllegalArgumentException("Membership plan repository is required");
        }
        this.membershipPlanRepository = membershipPlanRepository;
        this.users = users;
    }

    public MembershipPlan createPlan(User admin, String planName, double amount) {
        return createPlan(admin, planName, amount, MembershipPlan.resolveDefaultMaxBooks(planName));
    }

    public MembershipPlan createPlan(User admin, String planName, double amount, int maxBooks) {
        return createPlan(admin, planName, amount, maxBooks, 14, 10.0, 500.0, 100.0);
    }

    public MembershipPlan createPlan(User admin, String planName, double amount, int maxBooks,
                                     int loanPeriodDays, double finePerDay,
                                     double lostBookCharge, double damagedBookCharge) {
        if (!isAdmin(admin) || planName == null || planName.isBlank() || amount <= 0
                || maxBooks < 1 || loanPeriodDays < 1 || finePerDay < 0
                || lostBookCharge < 0 || damagedBookCharge < 0)
            return null;
        for (MembershipPlan existing : membershipPlanRepository.findAll()) {
            if (existing.getPlanName().equalsIgnoreCase(planName.trim())) return null;
        }
        int planId = DataStore.getMembershipPlanId();
        MembershipPlan plan = new MembershipPlan(planId, planName, amount, maxBooks,
                loanPeriodDays, finePerDay, lostBookCharge, damagedBookCharge);
        membershipPlanRepository.save(plan);
        return plan;
    }

    public MembershipPlan updatePlanPolicies(User admin, int planId, int maxBooks,
                                             int loanPeriodDays, double finePerDay,
                                             double lostBookCharge, double damagedBookCharge) {
        if (!isAdmin(admin) || maxBooks < 1 || loanPeriodDays < 1 || finePerDay < 0
                || lostBookCharge < 0 || damagedBookCharge < 0) return null;
        MembershipPlan existing = membershipPlanRepository.findById(planId);
        if (existing == null) return null;
        MembershipPlan updated = new MembershipPlan(existing.getPlanId(), existing.getPlanName(),
                existing.getAmount(), maxBooks, loanPeriodDays, finePerDay,
                lostBookCharge, damagedBookCharge);
        membershipPlanRepository.save(updated);
        return updated;
    }

    private boolean isAdmin(User admin) {
        if (users == null || admin == null || admin.getStatus() != UserStatus.ACTIVE
                || admin.getRole() != UserRole.ADMIN) return false;
        User stored = users.findByPhone(admin.getPhone());
        return stored != null && stored.getUserId() == admin.getUserId()
                && stored.getStatus() == UserStatus.ACTIVE && stored.getRole() == UserRole.ADMIN;
    }

    public MembershipPlan findById(int planId) {
        return membershipPlanRepository.findById(planId);
    }

    public MembershipPlan findByName(String planName) {
        return membershipPlanRepository.findByName(planName);
    }

    public List<MembershipPlan> getAllPlans() {
        return membershipPlanRepository.findAll();
    }

    public String getCreatePlanFailureReason(User admin, String planName, double amount) {
        if (!isAdmin(admin)) {
            return "Insufficient permissions. Active administrator privileges required.";
        }
        if (planName == null || planName.isBlank()) {
            return "Plan name cannot be empty.";
        }
        if (amount <= 0) {
            return "Plan amount must be greater than zero.";
        }
        return null;
    }
}
