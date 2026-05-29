package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import java.util.List;

public interface FindAllBulletsByIdsBoundary {

    List<BulletOutput> execute(List<String> ids);
}
