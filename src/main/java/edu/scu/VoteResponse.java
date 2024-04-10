package edu.scu;

public class VoteResponse {
    private final int term;            // The current term of the node sending the response
    private final boolean voteGranted; // Indicates if the vote was granted

    public VoteResponse(int term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public int getTerm() {
        return term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    // Optionally, override toString for easier debugging
    @Override
    public String toString() {
        return "VoteResponse{" +
                "term=" + term +
                ", voteGranted=" + voteGranted +
                '}';
    }
}
