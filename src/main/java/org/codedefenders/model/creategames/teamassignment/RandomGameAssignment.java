package org.codedefenders.model.creategames.teamassignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RandomGameAssignment extends GameAssignment {
    @Override
    public List<List<Integer>> assignGames(Collection<Integer> userIds, int numGames) {
        List<Integer> list = new ArrayList<>(userIds);
        Collections.shuffle(list);
        return splitSortedList(list, numGames);
    }
}
