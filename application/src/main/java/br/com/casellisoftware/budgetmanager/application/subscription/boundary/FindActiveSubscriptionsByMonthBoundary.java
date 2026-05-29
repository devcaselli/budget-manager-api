package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

import java.time.YearMonth;
import java.util.List;

public interface FindActiveSubscriptionsByMonthBoundary {

    List<SubscriptionOutput> execute(YearMonth month, String ownerId);
}
