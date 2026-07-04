package com.restaurant.rules.backward;

import com.restaurant.model.MenuItemFact;
import com.restaurant.model.Waiter;
import com.restaurant.model.WaiterFact;
import lombok.Getter;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Backward-chaining resolver - a plain Java class rather than Drools DRL,
 * since Drools is a forward-chaining engine and doesn't cleanly support
 * goal-driven ("does G hold?") resolution. Waiter goals are keyed by id;
 * menu item goals stay name-keyed since MenuItemFact was never changed.
 */
public class BackwardChainingEngine {

    private final KieSession session;
    @Getter
    private final List<String> trace = new ArrayList<>();

    public BackwardChainingEngine(KieSession session) {
        this.session = session;
    }

    private Optional<WaiterFact> waiterFact(int waiterId) {
        return session.getObjects(o -> o instanceof WaiterFact
                        && ((WaiterFact) o).getWaiterId() == waiterId)
                .stream().map(o -> (WaiterFact) o).findFirst();
    }

    private Optional<MenuItemFact> menuItemFact(String name) {
        return session.getObjects(o -> o instanceof MenuItemFact
                        && name.equals(((MenuItemFact) o).getMenuItemName()))
                .stream().map(o -> (MenuItemFact) o).findFirst();
    }

    private Optional<Waiter> rawWaiter(int waiterId) {
        return session.getObjects(o -> o instanceof Waiter
                        && ((Waiter) o).getId() == waiterId)
                .stream().map(o -> (Waiter) o).findFirst();
    }

    // ---------------------------------------------------------------
    // GOAL: shouldFireWaiter(W)
    //   shouldFireWaiter(W) :- consistentlyBad(W) AND problematic(W)
    //                           AND guestRating(W) < 2.5
    // ---------------------------------------------------------------
    public boolean shouldFireWaiter(int waiterId) {
        trace.clear();
        trace.add("GOAL: should waiter id=" + waiterId + " be fired?");

        Optional<WaiterFact> factOpt = waiterFact(waiterId);
        if (factOpt.isEmpty()) {
            trace.add("  -> No WaiterFact for id=" + waiterId + " (not yet classified). NO.");
            return false;
        }
        WaiterFact fact = factOpt.get();

        boolean consistentlyBad = fact.isConsistentlyBad();
        trace.add("  -> Subgoal: consistentlyBad(id=" + waiterId + ") = " + consistentlyBad);
        if (!consistentlyBad) {
            trace.add("Conclusion: NO, should not be fired (subgoal not satisfied).");
            return false;
        }

        boolean problematic = fact.isProblematic();
        trace.add("  -> Subgoal: problematic(id=" + waiterId + ") = " + problematic);
        if (!problematic) {
            trace.add("Conclusion: NO, should not be fired (subgoal not satisfied).");
            return false;
        }

        double rating = rawWaiter(waiterId).map(Waiter::getGuestRating).orElse(5.0);
        boolean lowRating = rating < 2.5;
        trace.add("  -> Subgoal: guestRating(id=" + waiterId + ") = " + rating + " < 2.5 -> " + lowRating);

        trace.add("Conclusion: " + (lowRating ? "YES, the waiter should be fired." : "NO, should not be fired."));
        return lowRating;
    }

    // ---------------------------------------------------------------
    // GOAL: shouldPromoteItem(I) - still name-keyed
    //   shouldPromoteItem(I) :- highPotential(I) AND NOT bestseller(I)
    //                            AND profitMargin(I) > 0.3
    // ---------------------------------------------------------------
    public boolean shouldPromoteItem(String name) {
        trace.clear();
        trace.add("GOAL: should item '" + name + "' be promoted?");

        Optional<MenuItemFact> factOpt = menuItemFact(name);
        if (factOpt.isEmpty()) {
            trace.add("  -> No MenuItemFact for '" + name + "'. NO.");
            return false;
        }
        MenuItemFact fact = factOpt.get();

        boolean highPotential = fact.isHighPotential();
        trace.add("  -> Subgoal: highPotential('" + name + "') = " + highPotential);
        if (!highPotential) {
            trace.add("Conclusion: NO, should not be promoted (subgoal not satisfied).");
            return false;
        }

        boolean alreadyBestseller = fact.isBestseller();
        trace.add("  -> Subgoal: NOT bestseller('" + name + "') = " + !alreadyBestseller);
        if (alreadyBestseller) {
            trace.add("Conclusion: NO, should not be promoted (already a bestseller).");
            return false;
        }

        double margin = fact.getProfitMargin();
        boolean goodMargin = margin > 0.3;
        trace.add("  -> Subgoal: profitMargin('" + name + "') = " + margin + " > 0.3 -> " + goodMargin);

        trace.add("Conclusion: " + (goodMargin ? "YES, the item should be promoted." : "NO, should not be promoted."));
        return goodMargin;
    }
}
