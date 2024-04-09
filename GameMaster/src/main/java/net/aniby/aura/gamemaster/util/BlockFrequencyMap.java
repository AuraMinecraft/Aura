package net.aniby.aura.gamemaster.util;

import lombok.Getter;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class BlockFrequencyMap implements Comparator<Block> {
    public static HashMap<Block, Integer> getFrequencies(Iterable<Block> list) {
        HashMap<Block, Integer> frequencies = new HashMap<>();
        for (Block element : list) {
            int count = frequencies.getOrDefault(element, 1);
            frequencies.put(element, count + 1);
        }
        return frequencies;
    }


    Map<Block, Integer> frequencies;

    public BlockFrequencyMap(Iterable<Block> list) {
        this.frequencies = getFrequencies(list);
    }

    public BlockFrequencyMap(Map<Block, Integer> frequencies) {
        this.frequencies = frequencies;
    }

    public int compare(Block a, Block b) {
        if (frequencies.get(a) >= frequencies.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }

    public ArrayList<Block> getCommon(int threshold) {
        ArrayList<Block> common = new ArrayList<>();
        for (Block key : this.frequencies.keySet()) {
            if (this.frequencies.get(key) >= threshold) {
                common.add(key);
            }
        }
        return common;
    }

    public ArrayList<Block> getMostCommonNotLower(int minimum) {
        int maxInMap = this.frequencies.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int maxValue = Math.max(maxInMap, minimum);
        return this.frequencies.keySet().stream()
                .filter(k -> this.frequencies.get(k) == maxValue)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Block> getMostCommon() {
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
