package edu.scu;

public class VoteRequest {
    private final int term;          // The candidate's current term
    private final int candidateId;   // The ID of the candidate requesting the vote
    private final int lastLogIndex;  // The index of the candidate’s last log entry
    private final int lastLogTerm;   // The term of the candidate’s last log entry

    public VoteRequest(int term, int candidateId, int lastLogIndex, int lastLogTerm) {
        this.term = term;
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    // Getters
    public int getTerm() {
        return term;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public int getLastLogTerm() {
        return lastLogTerm;
    }

    // Optionally, override toString for easier debugging
    @Override
    public String toString() {
        return "VoteRequest{" +
                "term=" + term +
                ", candidateId=" + candidateId +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                '}';
    }
}
