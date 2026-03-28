package com.nuramin.calculator.unitconverter;

import com.nuramin.calculator.unitconverter.exceptions.DuplicateUnitException;
import com.nuramin.calculator.unitconverter.exceptions.UnitNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/** O(1) lookup registry for all units with dynamic registration support. */
public final class UnitRegistry {
    private final Map<String, UnitDefinition> unitsByName = new ConcurrentHashMap<>();
    private final Map<String, String> baseByCategory = new ConcurrentHashMap<>();

    public synchronized void register(String unitKey, UnitDefinition unit) throws DuplicateUnitException {
        String key = normalize(unitKey);
        if (unitsByName.containsKey(key)) {
            throw new DuplicateUnitException("Duplicate unit registration attempted: " + unitKey);
        }
        unitsByName.put(key, unit);
        if (unit.isBaseUnit()) {
            baseByCategory.put(normalize(unit.getCategory()), key);
        }
    }

    public UnitDefinition get(String unitKey) throws UnitNotFoundException {
        UnitDefinition def = unitsByName.get(normalize(unitKey));
        if (def == null) throw new UnitNotFoundException("Unit not found: " + unitKey);
        return def;
    }

    public List<String> getCategories() {
        return new ArrayList<>(new TreeSet<>(baseByCategory.keySet()));
    }

    public List<String> getUnitsForCategory(String category) {
        if (category == null) return Collections.emptyList();
        String cat = normalize(category);
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, UnitDefinition> e : unitsByName.entrySet()) {
            if (normalize(e.getValue().getCategory()).equals(cat)) {
                result.add(e.getKey());
            }
        }
        Collections.sort(result);
        return result;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }
}
