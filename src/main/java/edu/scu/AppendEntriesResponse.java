package edu.scu;

public class AppendEntriesResponse {
    private final int term;            // The term of the follower sending the response
    private final boolean success;     // Indicates if the entries were successfully appended
    private final int responderId;     // ID of the follower node sending this response

    public AppendEntriesResponse(int term, boolean success, int responderId) {
        this.term = term;
        this.success = success;
        this.responderId = responderId;
    }

    public int getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getResponderId() {
        return responderId;
    }
}
