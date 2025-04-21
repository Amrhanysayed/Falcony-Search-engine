package Utils;

import java.util.List;

public class Posting {
    int frequency;
    private List<Integer> positions;
    public String docId;

    public Posting(String token, int frequency, String docId, List<Integer> positions) {
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

    public String getDocId() {
        return docId;
    }

    public void setTokenInfo(Posting tInfo) {
        this.frequency = tInfo.frequency;
        this.positions = tInfo.positions;
    }
}
