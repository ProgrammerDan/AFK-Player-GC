package com.github.Kraken3.AFKPGC;

import org.bukkit.entity.EntityType;
import org.bukkit.Material;

import java.util.Map;
import java.util.HashMap;

public class LagCostConfig {
	private Map<Material, Integer> materialCosts = new HashMap<Material, Integer>();
	private Map<EntityType, Integer> entityCosts = new HashMap<EntityType, Integer>();

	private static final LagCostConfig instance = new LagCostConfig();

	public static LagCostConfig getInstance() {
		return instance;
	}

	private LagCostConfig() {}

	public synchronized void setCost(Material material, Integer cost) {
		materialCosts.put(material, cost);
	}

	public synchronized void setCost(EntityType entity, Integer cost) {
		entityCosts.put(entity, cost);
	}

	public synchronized Integer cost(Material material) {
		return materialCosts.containsKey(material) ? materialCosts.get(material) : 0;
	}

	public synchronized Integer cost(EntityType entity) {
		return entityCosts.containsKey(entity) ? entityCosts.get(entity) : 0;
	}

	public synchronized void clearCosts() {
		materialCosts.clear();
		entityCosts.clear();
	}

}
