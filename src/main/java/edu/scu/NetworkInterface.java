package edu.scu;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class NetworkInterface {
    private int port;
    private RaftNode raftNode;
    private ExecutorService executorService;

    public NetworkInterface(int port, RaftNode raftNode) {
        this.port = port;
        this.raftNode = raftNode;
        this.executorService = Executors.newCachedThreadPool();
    }

    public void startServer() {
        executorService.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(new ClientHandler(clientSocket));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendVoteRequest(int destinationPort, VoteRequest voteRequest) {
        executorService.submit(() -> {
            try (Socket socket = new Socket("localhost", destinationPort);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.writeObject(voteRequest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendVoteResponse(int candidatePort, VoteResponse response) {
        try (Socket socket = new Socket("localhost", candidatePort); // Assuming all nodes are on 'localhost'
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(response);
        } catch (IOException e) {
            e.printStackTrace();
            // Handle exceptions appropriately
        }
    }

    public void sendAppendEntries(int destinationPort, AppendEntriesRPC appendEntries) {
        executorService.submit(() -> {
            try (Socket socket = new Socket("localhost", destinationPort);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.writeObject(appendEntries);
            } catch (IOException e) {
                e.printStackTrace();
                // Handle the exception (e.g., retry mechanism, logging)
            }
        });
    }

    public void sendAppendEntriesResponse(int leaderPort, AppendEntriesResponse response) {
        try (Socket socket = new Socket("localhost", leaderPort); // Replace with actual leader address if needed
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendObject(int destinationPort, Serializable object) {
        executorService.submit(() -> {
            try (Socket socket = new Socket("localhost", destinationPort);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.writeObject(object);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    private class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
                Object request = in.readObject();
                // Handle different types of requests (e.g., VoteRequest, AppendEntriesRPC)
                if (request instanceof VoteRequest) {
//                    raftNode.handleVoteRequest((VoteRequest) request);
                } else if (request instanceof AppendEntriesRPC) {
//                    raftNode.handleAppendEntries((AppendEntriesRPC) request);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
