package net.aniby.aura.tool;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class FrequencyMap implements Comparator<String> {
    public static HashMap<String, Integer> getFrequencies(Iterable<String> list) {
        HashMap<String, Integer> frequencies = new HashMap<>();
        for (String element : list) {
            int count = frequencies.getOrDefault(element, 1);
            frequencies.put(element, count + 1);
        }
        return frequencies;
    }


    @Getter
    HashMap<String, Integer> frequencies;

    public FrequencyMap(Iterable<String> list) {
        this.frequencies = getFrequencies(list);
    }

    public FrequencyMap(HashMap<String, Integer> frequencies) {
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

    public ArrayList<String> getMostCommon() {
        int maxValue = this.frequencies.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return this.frequencies.keySet().stream()
                .filter(k -> this.frequencies.get(k) == maxValue)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public String toString() {
        return this.frequencies.toString();
    }
}
