package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class CollectSnapshot implements Serializable {
    public int sumOffFishies;

    public CollectSnapshot() {
        sumOffFishies = 0;
    }

    public void addFishies(int amount) {
        sumOffFishies += amount;
    }

    public int getFishies() {
        return sumOffFishies;
    }
}
