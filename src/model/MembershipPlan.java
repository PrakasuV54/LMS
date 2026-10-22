package model;

import java.util.Locale;

public class MembershipPlan {
    private final int planId;
    private final String planName;
    private final double amount;
    private final int maxBooks;
    private final int loanPeriodDays;
    private final double finePerDay;
    private final double lostBookCharge;
    private final double damagedBookCharge;

    public MembershipPlan(int planId, String planName, double amount) {
        this(planId, planName, amount, resolveDefaultMaxBooks(planName), 14, 10.0, 500.0, 100.0);
    }

    public MembershipPlan(int planId, String planName, double amount, int maxBooks) {
        this(planId, planName, amount, maxBooks > 0 ? maxBooks : resolveDefaultMaxBooks(planName), 14, 10.0, 500.0, 100.0);
    }

    public MembershipPlan(int planId, String planName, double amount, int maxBooks,
                          int loanPeriodDays, double finePerDay,
                          double lostBookCharge, double damagedBookCharge) {
        if (planName == null || planName.isBlank())
            throw new IllegalArgumentException("Plan name cannot be blank");
        if (amount <= 0)
            throw new IllegalArgumentException("Plan amount must be positive");
        if (maxBooks < 1) throw new IllegalArgumentException("Maximum books must be at least 1");
        if (loanPeriodDays < 1) throw new IllegalArgumentException("Loan period must be at least 1 day");
        if (finePerDay < 0 || lostBookCharge < 0 || damagedBookCharge < 0)
            throw new IllegalArgumentException("Charges cannot be negative");
        this.planId = planId;
        this.planName = planName.trim();
        this.amount = amount;
        this.maxBooks = maxBooks;
        this.loanPeriodDays = loanPeriodDays;
        this.finePerDay = finePerDay;
        this.lostBookCharge = lostBookCharge;
        this.damagedBookCharge = damagedBookCharge;
    }

    public static int resolveDefaultMaxBooks(String planName) {
        if (planName == null) return 3;
        String lower = planName.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("family") && lower.contains("premium")) return 15;
        if (lower.contains("family")) return 12;
        if (lower.contains("premium")) return 5;
        return 3;
    }

    public int getPlanId() { return planId; }
    public String getPlanName() { return planName; }
    public double getAmount() { return amount; }
    public int getMaxBooks() { return maxBooks; }
    public int getMaxIssuedBooks() { return maxBooks; }
    public int getLoanPeriodDays() { return loanPeriodDays; }
    public double getFinePerDay() { return finePerDay; }
    public double getLostBookCharge() { return lostBookCharge; }
    public double getDamagedBookCharge() { return damagedBookCharge; }
}
