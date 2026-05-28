package com.restaurant.service;

import com.restaurant.model.*;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.List;

public class DroolsService {

    private final KieContainer kieContainer;

    public DroolsService() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        kfs.write(ks.getResources()
                .newClassPathResource("rules/forward/menu-staff-rules.drl"));

        KieBuilder kieBuilder = ks.newKieBuilder(kfs);
        kieBuilder.buildAll();
        kieBuilder.getResults().getMessages().forEach(m ->
                System.out.println("BUILD: " + m.getText()));

        KieModule kieModule = kieBuilder.getKieModule();
        this.kieContainer = ks.newKieContainer(kieModule.getReleaseId());
    }

    public List<Recommendation> runForwardChaining(List<MenuItem> menuItems, List<Waiter> waiters, List<Shift> shifts) {
        KieSession session = kieContainer.newKieSession();
        if (session == null) {
            throw new RuntimeException("KieSession is null");
        }
        List<Recommendation> recommendations = new ArrayList<>();
        session.setGlobal("recommendations", recommendations);

        for (MenuItem item : menuItems) session.insert(item);
        for (Waiter waiter : waiters) session.insert(waiter);
        for (Shift shift : shifts) session.insert(shift);

        session.fireAllRules();
        session.dispose();
        return recommendations;
    }
}
