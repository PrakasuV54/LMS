package repository;

import model.MembershipPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InMemoryMembershipPlanRepository implements MembershipPlanRepository {
    private final DataStore dataStore;

    public InMemoryMembershipPlanRepository(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void save(MembershipPlan plan) {
        if (plan == null) return;
        dataStore.membershipPlans.put(plan.getPlanId(), plan);
    }

    @Override
    public MembershipPlan findById(int planId) {
        return dataStore.membershipPlans.get(planId);
    }

    @Override
    public MembershipPlan findByName(String planName) {
        if (planName == null) {
            return null;
        }
        String key = normalizePlanName(planName);
        for (MembershipPlan plan : dataStore.membershipPlans.values()) {
            if (normalizePlanName(plan.getPlanName()).equals(key)) {
                return plan;
            }
        }
        return null;
    }

    private String normalizePlanName(String planName) {
        String key = planName.trim().toLowerCase(Locale.ROOT);
        if (key.endsWith(" plan")) {
            key = key.substring(0, key.length() - 5).trim();
        }
        return key;
    }

    @Override
    public List<MembershipPlan> findAll() {
        return new ArrayList<>(dataStore.membershipPlans.values());
    }

    @Override
    public boolean existsById(int planId) {
        return dataStore.membershipPlans.containsKey(planId);
    }
}
