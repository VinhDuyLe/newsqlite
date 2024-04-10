package edu.scu;

import java.util.List;

public class AppendEntriesRPC {
    private final int term; // Leader's term
    private final int leaderId; // Leader's ID
    private final int prevLogIndex; // Index of log entry immediately preceding new ones
    private final int prevLogTerm; // Term of prevLogIndex entry
    private final List<LogEntry> entries; // Log entries to store (empty for heartbeat)
    private final int leaderCommit; // Leader's commit index

    public AppendEntriesRPC(int term, int leaderId, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    // Getters
    public int getTerm() {
        return term;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }
}
