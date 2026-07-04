package com.restaurant.service;

import com.restaurant.model.*;
import org.kie.api.KieServices;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.KieBase;

import java.util.ArrayList;
import java.util.List;

public class DroolsService {
    private final KieBase kieBase;

    public DroolsService() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write(ks.getResources()
                .newClassPathResource("rules/forward/menu-staff-rules.drl"));
        kfs.write(ks.getResources()
                .newClassPathResource("rules/cep/cep.drl"));

        KieBuilder kieBuilder = ks.newKieBuilder(kfs);
        kieBuilder.buildAll();
        kieBuilder.getResults().getMessages().forEach(m ->
                System.out.println("BUILD: " + m.getText()));

        KieModule kieModule = kieBuilder.getKieModule();
        KieContainer kieContainer = ks.newKieContainer(kieModule.getReleaseId());

        // No kmodule.xml here, so STREAM mode (required for CEP time
        // windows in cep.drl) has to be set explicitly in code instead
        // of via eventProcessingMode="stream".
        KieBaseConfiguration kbConf = ks.newKieBaseConfiguration();
        kbConf.setOption(EventProcessingOption.STREAM);
        this.kieBase = kieContainer.newKieBase(kbConf);
    }

    /**
     * Use this for the Swing app: ONE session that stays alive for the
     * whole run, so backward-chaining queries and CEP time windows see
     * consistent state across multiple user actions. Do NOT dispose()
     * this after a single fireAllRules() call - hold onto it.
     */
    public KieSession newPersistentSession(List<Recommendation> recommendations) {
        KieSession session = kieBase.newKieSession();
        session.setGlobal("recommendations", recommendations);
        return session;
    }

    /**
     * Original one-shot batch method - still fine for a script/test that
     * just wants forward-chaining recommendations for a fixed dataset and
     * doesn't need backward chaining or CEP afterward, since the session
     * is disposed immediately after firing.
     */
    public List<Recommendation> runForwardChaining(List<MenuItem> menuItems, List<Waiter> waiters, List<Shift> shifts) {
        KieSession session = kieBase.newKieSession();
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
