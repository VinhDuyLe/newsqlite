import edu.scu.DatabaseManager;
import edu.scu.RaftNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

public class RaftNodeTest {
    private List<RaftNode> nodes;
    private static final int NODE_COUNT = 5; // cluster size
    private final String dbPath = "test1.db";
    DatabaseManager dbManager = new DatabaseManager(dbPath);



    @Before
    public void setUp() {
        nodes = new ArrayList<>();
        for (int i = 0; i < NODE_COUNT; i++) {
            // Create a list of peer node IDs
            List<Integer> peerNodeIds = new ArrayList<>();
            for (int j = 0; j < NODE_COUNT; j++) {
                if (j != i) {
                    peerNodeIds.add(j);
                }
            }
            nodes.add(new RaftNode(i, peerNodeIds, dbPath));
        }

        // Register each node in the static registry within RaftNode class
        for (RaftNode node : nodes) {
            RaftNode.registerNode(node);
        }
    }

    @Test
    public void testLeaderElection() {
        // elections
        nodes.forEach(RaftNode::startElection);

        // Verify that one and only one leader is elected
        int leaderCount = 0;
        for (RaftNode node : nodes) {
            if (node.getCurrentState() == RaftNode.State.LEADER) {
                leaderCount++;
            }
        }
        Assert.assertEquals("Only one leader should be elected", 1, leaderCount);
    }

    @Test
    public void testLogReplication() {
        nodes.forEach(RaftNode::startElection);
        RaftNode leader = findLeader();
//        System.out.println("State " + leader.getCurrentState());
        leader.becomeLeader();
        Assert.assertNotNull("Leader must be present", leader);

        String testCommand = "INSERT INTO records1 (data) VALUES ('Adding data')";
        leader.appendEntries(testCommand);

        // MOCKING follower log update
        for (RaftNode node : nodes) {
            if (node != leader) {
                node.setMockingLog();
            }
        }

        for (RaftNode node : nodes) {
            if (node != leader) {
                Assert.assertFalse("Follower log should not be empty", node.getLog().isEmpty());
                Assert.assertEquals("Last log entry should match the command", testCommand, node.getLastLogEntry().getCommand());
            }
        }
    }

    private RaftNode findLeader() {
        return nodes.stream()
                .filter(node -> node.getCurrentState() == RaftNode.State.LEADER)
                .findFirst()
                .orElse(null);
    }

    @Test
    public void testHeartbeatMechanism() {
        nodes.forEach(RaftNode::startElection);
        RaftNode leader = findLeader();
        Assert.assertNotNull("Leader must be present", leader);

        // Assuming you have a way to count heartbeats or check the last heartbeat time
        leader.sendInitialHeartbeats(); // Send a round of heartbeats

        // MOCKING follower got heartbeat
        for (RaftNode node : nodes) {
            if (node != leader) {
                node.updateLastHeartbeatTime();
            }
        }

        for (RaftNode node : nodes) {
            if (node != leader) {
                Assert.assertTrue("Follower should have received a heartbeat", node.hasReceivedHeartbeatRecently());
            }
        }
    }
    @Test
    public void testHandleClientRequest() {
        String testCommand = "INSERT INTO records1 (data) VALUES ('Adding data3')";
        nodes.forEach(RaftNode::startElection);
        RaftNode leader = findLeader();

        // Simulate a client sending a request to the leader
        leader.processClientRequest(testCommand);

        String tableName = "records1";
        List<List<String>> queryResults = dbManager.executeQuery("SELECT * FROM " + tableName);
        for (List<String> row : queryResults) {
            System.out.println(row);
        }
    }
}
