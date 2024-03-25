package net.aniby.aura.tool;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class DoubleFrequencyMap implements Comparator<String> {
    public static HashMap<String, Double> getFrequencies(Iterable<String> list) {
        HashMap<String, Double> frequencies = new HashMap<>();
        for (String element : list) {
            double count = frequencies.getOrDefault(element, 1.0);
            frequencies.put(element, count + 1);
        }
        return frequencies;
    }


    @Getter
    HashMap<String, Double> frequencies;

    public DoubleFrequencyMap(Iterable<String> list) {
        this.frequencies = getFrequencies(list);
    }

    public DoubleFrequencyMap(HashMap<String, Double> frequencies) {
        this.frequencies = frequencies;
    }

    public int compare(String a, String b) {
        if (frequencies.get(a) >= frequencies.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }

    public ArrayList<String> getCommon(int threshold) {
        ArrayList<String> common = new ArrayList<>();
        for (String key : this.frequencies.keySet()) {
            if (this.frequencies.get(key) >= threshold) {
                common.add(key);
            }
        }
        return common;
    }

    public ArrayList<String> getMostCommonNotLower(double minimum) {
        double maxInMap = this.frequencies.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double maxValue = Math.max(maxInMap, minimum);
        return this.frequencies.keySet().stream()
                .filter(k -> this.frequencies.get(k) == maxValue)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<String> getMostCommon() {
        double maxValue = this.frequencies.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        return this.frequencies.keySet().stream()
                .filter(k -> this.frequencies.get(k) == maxValue)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public String toString() {
        return this.frequencies.toString();
    }
}
