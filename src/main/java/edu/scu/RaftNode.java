package edu.scu;
import java.util.*;

public class RaftNode {

    // Raft Node States
    public enum State {
        LEADER, FOLLOWER, CANDIDATE
    }
    // Class Members
    private State currentState;
    private int currentTerm;
    private Integer votedFor;
    private List<LogEntry> log;
    private int commitIndex;
    private int lastApplied;
    private Map<Integer, Integer> nextIndex;
    private Map<Integer, Integer> matchIndex;
    private int nodeId;
    private Integer currentLeaderId;
    private Timer electionTimer;
    private RaftNode leaderNode; // Reference to the leader node
    private DatabaseManager dbManager; // DatabaseManager instance
    private int receivedVotes; // Number of votes received in the current election term
    private List<Integer> peerNodeIds; // IDs of other nodes in the cluster
    // Timestamp of the last received heartbeat
    private long lastHeartbeatTime;
    private static final Map<Integer, RaftNode> nodeRegistry = new HashMap<>();


    // Constructor
    public RaftNode(int nodeId, List<Integer> peerNodeIds, String dbPath ) {
        this.nodeId = nodeId;
        this.currentState = State.FOLLOWER;
        this.currentTerm = 0;
        this.votedFor = null;
        this.log = new ArrayList<>();
//        log.add(new LogEntry(1, " Sellect"));
        this.commitIndex = 0;
        this.lastApplied = 0;
        this.nextIndex = new HashMap<>();
        this.matchIndex = new HashMap<>();
        this.currentLeaderId = null;
        this.dbManager = new DatabaseManager(dbPath);
        this.receivedVotes = 0;
        this.peerNodeIds = new ArrayList<>(peerNodeIds); // Make a copy of the peerNodeIds list
//        nodeRegistry.put(nodeId, this);
        resetElectionTimeout();
    }

    // Raft Methods
    public synchronized void startElection() {
        currentState = State.CANDIDATE;
        currentTerm++;
        votedFor = nodeId;
        int voteCount = 1;

        for (Integer id : peerNodeIds) {
            RaftNode peer = resolveNodeFromId(id);
            VoteRequest voteRequest = new VoteRequest(currentTerm, nodeId, getLastLogIndex(), getLastLogTerm());
            // Simulate sending the vote request
//            peer.receiveVoteRequest(voteRequest);
        }
        // Simulate sending vote and random select Leader
        Random ran = new Random();
        int nxt = ran.nextInt(5);
        if ( nxt == nodeId) {
            becomeLeader();
        }


        resetElectionTimeout(); //new election
    }

    public synchronized void receiveVoteRequest(VoteRequest request) {
        boolean voteGranted = false;
        if (request.getTerm() >= currentTerm &&
                (votedFor == null || votedFor == request.getCandidateId()) &&
                isCandidateLogUpToDate(request)) {
            votedFor = request.getCandidateId();
            voteGranted = true;
            currentTerm = request.getTerm();
            transitionToFollowerState(); // Reset state as a follower
        }

        // Simulate sending vote response
        sendVoteResponse(request.getCandidateId(), voteGranted);
    }

    public void sendVoteResponse(int candidateId, boolean voteGranted) {
        // This method would normally use network communication
        // For simulation, directly invoke the method on the candidate node
        RaftNode candidate = resolveNodeFromId(candidateId);
        if (candidate != null) {
            candidate.receiveVoteResponse(new VoteResponse(currentTerm, voteGranted));
        }
    }
    public synchronized void receiveVoteResponse(VoteResponse response) {
        // Ignore the response if it's from an older term
        if (response.getTerm() < currentTerm) {
            return;
        }

        // Check if the response is positive and from the current election term
        if (response.isVoteGranted() && response.getTerm() == currentTerm) {
            receivedVotes++;

            // Check if received majority votes
            if (receivedVotes > peerNodeIds.size() / 2) {
                // Transition to leader state if the node received the majority of votes
                becomeLeader();
            }
        }
    }

    public void becomeLeader() {
        // Set the current state to LEADER
        currentState = State.LEADER;

        // Reset vote count for future elections
        receivedVotes = 0;

        // Reset nextIndex and matchIndex for each follower node
        // Assuming nextIndex and matchIndex are maps keyed by follower node IDs
        for (Integer followerId : peerNodeIds) {
            nextIndex.put(followerId, log.size()); // Next index for each follower starts at the end of the leader's log
            matchIndex.put(followerId, 0); // Initially, no entries are known to be replicated on the followers
        }

        // Send initial heartbeats to each follower
        // This also serves as an initial empty AppendEntries RPC to assert authority as the new leader
        sendInitialHeartbeats();
    }

    public void sendInitialHeartbeats() {
        for (Integer followerId : peerNodeIds) {
            // Create an empty AppendEntries RPC (heartbeat) for each follower
            AppendEntriesRPC heartbeat = new AppendEntriesRPC(currentTerm, nodeId, log.size() - 1, getLastLogTerm(), Collections.emptyList(), commitIndex);

            // Directly invoke the follower's method to handle the heartbeat
            RaftNode follower = resolveNodeFromId(followerId);
            if (follower != null) {
                follower.receiveAppendEntries(heartbeat);
            }
        }
    }

    public void appendEntries(String command) {
        if (currentState != State.LEADER) {
            return; // or throw an exception/handle later
        }

        // Create a new log entry with the command and the current term
        LogEntry newEntry = new LogEntry(currentTerm, command);
        log.add(newEntry);

        // Replicate the new entry to each follower
        for (Integer id : peerNodeIds) {
            RaftNode follower = resolveNodeFromId(id);
//            System.out.println("Follower from  " + nodeId + " is : " + follower.nodeId);
            replicateLogToNode(follower);
        }
    }

    public void processClientRequest(String command) {
        if (currentState != State.LEADER) {
            throw new IllegalStateException("Only the leader can process client requests.");
        }
        appendEntries(command);
        LogEntry newEntry = new LogEntry(currentTerm, command);
        applyEntryToStateMachine(newEntry);

    }

    private void replicateLogToNode(RaftNode follower) {
        // Prepare the data for the AppendEntriesRPC
        int nextIndexForFollower = nextIndex.getOrDefault(follower.nodeId, 0);
//        System.out.println("nextIndexForFollower : " +  nextIndexForFollower);
        List<LogEntry> entriesToSend = log.subList(nextIndexForFollower, log.size());
        int prevLogIndex = nextIndexForFollower - 1;
        int prevLogTerm = prevLogIndex >= 0 ? log.get(prevLogIndex).getTerm() : 0;

//        System.out.println("prevLogTerm : " + prevLogTerm);

        // Create the AppendEntriesRPC object
        AppendEntriesRPC rpc = new AppendEntriesRPC(currentTerm, nodeId, prevLogIndex, prevLogTerm, entriesToSend, commitIndex);

        // Simulate sending the AppendEntriesRPC to the follower
        follower.receiveAppendEntries(rpc);
    }

    public synchronized void receiveAppendEntries(AppendEntriesRPC rpc) {
        boolean success = false;

        // Check if the term in the RPC is at least as large as the node's current term
        if (rpc.getTerm() >= currentTerm) {
            // Update current term and reset election timer
            currentTerm = rpc.getTerm();
            resetElectionTimeout();

            // Check log consistency
            int prevLogIndex = rpc.getPrevLogIndex();
//            System.out.println("rpc preve"+ prevLogIndex);
            int prevLogTerm = rpc.getPrevLogTerm();
            if (isLogConsistent(prevLogIndex, prevLogTerm)) {
                // Append any new entries not already in the log
                List<LogEntry> entries = rpc.getEntries();
                appendEntriesToLog(entries, prevLogIndex);

                // Update commit index
                if (rpc.getLeaderCommit() > commitIndex) {
                    commitIndex = Math.min(rpc.getLeaderCommit(), log.size() - 1);
                }
                success = true;
            }
        }

        // Prepare and directly send a response back to the leader
//        AppendEntriesResponse response = new AppendEntriesResponse(currentTerm, success, nodeId);
//        sendAppendEntriesResponse(response);
    }

    private boolean isLogConsistent(int prevLogIndex, int prevLogTerm) {
        if (prevLogIndex < 0) return false;
        // Check if the log is shorter than prevLogIndex
        if (log.size() <= prevLogIndex) {
//            System.out.println(prevLogIndex);
//            System.out.println("prelogInd");
            return false;
        }
        System.out.println("isLogConsistent prevLogIndex  : " + prevLogIndex);

        // Check if log term at prevLogIndex matches prevLogTerm
        return log.get(prevLogIndex).getTerm() == prevLogTerm;
    }

    private void appendEntriesToLog(List<LogEntry> entries, int prevLogIndex) {
        int index = prevLogIndex + 1;
        // Remove any conflicting entries and append new entries
        while (index < log.size() && !entries.isEmpty()) {
            if (log.get(index).getTerm() != entries.get(0).getTerm()) {
                // Remove the conflicting entry and all following entries
                log.subList(index, log.size()).clear();
                break;
            }
            index++;
            entries.remove(0);
        }
        // Append any remaining new entries
        log.addAll(entries);
    }
    private void sendAppendEntriesResponse(AppendEntriesResponse response) {
        // Directly call the method on the leader node object
        leaderNode.receiveAppendEntriesResponse(response);
    }


    public synchronized void receiveAppendEntriesResponse(AppendEntriesResponse response) {
        // Check if the response is from the current term
        if (response.getTerm() != currentTerm) {
            return; // Ignore responses from older terms
        }

        // Update the nextIndex and matchIndex for the follower
        int followerId = response.getResponderId();
        if (response.isSuccess()) {
            // If successful, update nextIndex and matchIndex for the follower
            int lastIndex = getLastLogIndex();
            nextIndex.put(followerId, lastIndex + 1);
            matchIndex.put(followerId, lastIndex);
        } else {
            // If unsuccessful, decrement nextIndex for the follower
            int currentNextIndex = nextIndex.getOrDefault(followerId, 1);
            if (currentNextIndex > 1) {
                nextIndex.put(followerId, currentNextIndex - 1);
            }
        }

        // Check if we can commit any new entries
        updateCommitIndex();
    }

    private void updateCommitIndex() {
        int[] matchIndexes = matchIndex.values().stream().mapToInt(i -> i).toArray();
        Arrays.sort(matchIndexes);
        int newCommitIndex = matchIndexes[matchIndexes.length / 2];
        if (newCommitIndex > commitIndex && log.get(newCommitIndex).getTerm() == currentTerm) {
            commitIndex = newCommitIndex;
            // Apply the newly committed entries to the state machine
            applyCommittedEntries();
        }
    }

    private void applyCommittedEntries() {
        while (lastApplied < commitIndex) {
            lastApplied++;
            LogEntry entry = log.get(lastApplied);
            // Apply the entry to the state machine to execute
            applyEntryToStateMachine(entry);
        }
    }

    private void applyEntryToStateMachine(LogEntry entry) {
        String command = entry.getCommand();

        // Check if the command is a query or an update
        if (isSelectQuery(command)) {
            // For SELECT queries
            List<List<String>> results = dbManager.executeQuery(command);
            for (List<String> row : results) {
                System.out.println(row);
            }
        } else {
            // For INSERT, UPDATE, DELETE, and other SQL statements
            dbManager.executeUpdate(command);
        }
    }

    private boolean isSelectQuery(String sql) {
        return sql.trim().toUpperCase().startsWith("SELECT");
    }

    // HELPER Methods
    private void resetElectionTimeout() {
        if (electionTimer != null) {
            electionTimer.cancel();
        }
        electionTimer = new Timer();
        electionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                startElection();
            }
        }, getElectionTimeout());
    }

    private long getElectionTimeout() {
        // Return a randomized timeout duration
        // Typically, this duration is between 150ms and 300ms
        return 150 + (long) (Math.random() * 150);
    }

    private int getLastLogIndex() {
        return log.isEmpty() ? 0 : log.size() - 1;
    }

    private int getLastLogTerm() {
        return log.isEmpty() ? 0 : log.get(log.size() - 1).getTerm();
    }

    private boolean isCandidateLogUpToDate(VoteRequest voteRequest) {
        int lastLogIndex = getLastLogIndex();
        int lastLogTerm = getLastLogTerm();
        return (voteRequest.getLastLogTerm() > lastLogTerm) ||
                (voteRequest.getLastLogTerm() == lastLogTerm && voteRequest.getLastLogIndex() >= lastLogIndex);
    }

    private void transitionToFollowerState() {
        currentState = State.FOLLOWER;
        votedFor = null;
        resetElectionTimeout();
    }

    // Method to set the leader node reference
    public void setLeaderNode(RaftNode leaderNode) {
        this.leaderNode = leaderNode;
    }

    public State getCurrentState() {
        return currentState;
    }

    public List<LogEntry> getLog() {
        return new ArrayList<>(log); // Returns a copy of the log to maintain encapsulation
    }

    // MOCKING set log
    public void setMockingLog() {
        log.add(new LogEntry(1, "INSERT INTO records1 (data) VALUES ('Adding data')"));
        log.add(new LogEntry(2, "INSERT INTO records1 (data) VALUES ('Adding data')"));
    }

    public LogEntry getLastLogEntry() {
        if (!log.isEmpty()) {
            return log.get(log.size() - 1); // Return the last entry
        }
        return null; // Return null if the log is empty
    }

    public List<Integer> getPeerNodeIds() {
        return peerNodeIds;
    }

    public int getNodeId() {
        return nodeId;
    }

    // Static method to add a RaftNode instance to the registry
    public static void registerNode(RaftNode node) {
        nodeRegistry.put(node.getNodeId(), node);
    }

    private RaftNode resolveNodeFromId(int nodeId) {
        return nodeRegistry.get(nodeId);
    }


    // Method to update the timestamp when a heartbeat is received
    public synchronized void updateLastHeartbeatTime() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    // Method to check if a heartbeat has been received recently
    public synchronized boolean hasReceivedHeartbeatRecently() {
        long currentTime = System.currentTimeMillis();
        long elapsedTimeSinceLastHeartbeat = currentTime - lastHeartbeatTime;
        long heartbeatTimeout = 300; // Timeout period in milliseconds

        return elapsedTimeSinceLastHeartbeat < heartbeatTimeout;
    }
}
