package com.choosemuse.example.libmuse;

import android.util.Log;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Game implements MotionListener {
    static int START_SPEED = 4;
    static int STOP_SPEED = 1;
    static double FACTOR_SPEED = 30.0;

    private String name;
    private String phone;

    Motion currentMotion;

    long motionStartTime;

    int score;

    boolean finished;
    boolean started;

    boolean finishedMove;

    Retrofit retrofit;
    GameListener listener;

    public Game(GameListener listener) {
        retrofit = new Retrofit.Builder().baseUrl("https://26bb8100.ngrok.io/").addConverterFactory(GsonConverterFactory.create()).build();

        this.listener = listener;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void start() {
        started = true;
        finishedMove = true;
        setNextMotion();
    }

    public void setNextMotion() {
        currentMotion = Motion.getRandomMotion();
        motionStartTime = System.currentTimeMillis();
        if (!finishedMove) {
            endGame();
        } else {
            finishedMove = false;
            this.listener.onNextMove();
        }
    }

    public double getSpeed() {
        return Math.max(START_SPEED - (START_SPEED - STOP_SPEED) * score / FACTOR_SPEED, 1.0) * 1000;
    }

    @Override
    public boolean onMotion(Motion m, long duration) {
        Log.d("foobar", "2");
        if (!finished && !finishedMove && timeRemaining() <= 0) {
            endGame();
            return false;
        }
        Log.d("foobar", "3");
        // ignore short blinks
        if (finishedMove || (m == Motion.BLINK && m != currentMotion && duration < 110L))
            return true;
        Log.d("foobar", "4");

        if (!finished && m == currentMotion && timeRemaining() <= getSpeed()) {
            Log.d("foobar", "5");
            score++;
            finishedMove = true;
        } else {
            Log.d("foobar", "6");
            Log.d("foobar", "Should have done " + currentMotion.toString() + " instead of " + m.toString());
            endGame();
        }
        return false;
    }

    public double timeRemaining() {
        return System.currentTimeMillis() - motionStartTime;
    }

    /**
     * Send a message to be used in the voice call
     *
     * @param from    The id of the score entry of this user
     * @param to      The id of the score entry of the previous high score
     * @param message The message to send
     */
    public void sendMessage(int from, int to, String message) {
        MessageSend messageSend = retrofit.create(MessageSend.class);
        try {
            messageSend.sendMessage(from, to, message).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void endGame() {
        if (finished)
            return;
        finished = true;
        ScoreLog scoreLog = retrofit.create(ScoreLog.class);
        scoreLog.scoreResponse(score, phone, name).enqueue(new Callback<ScoreResponse>() {
            @Override
            public void onResponse(Call<ScoreResponse> call, Response<ScoreResponse> response) {
                Log.d("foobar", "response");
            }

            @Override
            public void onFailure(Call<ScoreResponse> call, Throwable t) {
                Log.d("foobar", "failure");
            }
        });
        listener.onGameOver();
    }
}