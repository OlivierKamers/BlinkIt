package com.choosemuse.example.libmuse;

public class ScoreResponse {
    public final long id;
    public final boolean beatHighScore;
    public final long previousId;

    public ScoreResponse(long id, boolean beatHighScore, long previousId) {
        this.id = id;
        this.beatHighScore = beatHighScore;
        this.previousId = previousId;
    }
}