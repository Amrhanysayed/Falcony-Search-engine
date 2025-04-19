package Indexer;

import java.util.ArrayList;
import java.util.List;

public class TermInfo {
    String term;
    int frequency;
    private List<Integer> positions;
    int docId;

    TermInfo(String term, int frequency, int docId, List<Integer> positions) {
        this.term = term;
        this.frequency = frequency;
        this.positions = positions;
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
    public int getDocId() {
        return docId;
    }
    public void setTermInfo(TermInfo tInfo) {
        this.term = tInfo.term;
        this.frequency = tInfo.frequency;
        this.positions = tInfo.positions;
    }
}
