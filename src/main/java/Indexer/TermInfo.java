package Indexer;

import java.util.ArrayList;
import java.util.List;

public class TermInfo {
    String term;
    int frequency;
    private final List<Integer> positions;
    int docId;

    TermInfo(String term, int frequency, int docId) {
        this.term = term;
        this.frequency = frequency;
        this.positions = new ArrayList<>();
        this.docId = docId;
    }

    public List<Integer> getPositions() {
        return positions;
    }
    public void addPosition(int position) {
        this.positions.add(position);
    }
    public int getFrequency() {
        return frequency;
    }
}
