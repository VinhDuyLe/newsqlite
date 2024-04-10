package edu.scu;


public class LogEntry {
    private final int term;       // The term when the entry was received by the leader
    private final String command; // The command to be executed (e.g., a database operation)


    public LogEntry(int term, String command) {
        this.term = term;
        this.command = command;
    }

    public int getTerm() {
        return term;
    }

    public String getCommand() {
        return command;
    }

    // Optionally, override toString for easier debugging
    @Override
    public String toString() {
        return "LogEntry{" +
                "term=" + term +
                ", command='" + command + '\'' +
                '}';
    }
}
