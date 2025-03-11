package com.example.expressify;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Objects;

public class PredictionActivity extends AppCompatActivity {

    private TextView predictionText;
    private TextView health;

    private Button home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);
        String mostFreq = getIntent().getStringExtra("mostFreq");
        predictionText = findViewById(R.id.prediction);
        home = findViewById(R.id.home);
        health = findViewById(R.id.health);
        String bruh = "You are feeling " + mostFreq;
        switch (mostFreq){


            case "Happy/Neutral":
                health.setText(R.string.health1);
                break;
            case "Sad/Disgust":
                health.setText(R.string.sad);
                break;
            case "Fear":
                health.setText(R.string.fear);
                break;

            case "Happy":
                health.setText(R.string.health1);
                break;

            case "Surprise":
                health.setText(R.string.surprise);
                break;

            case "Angry":

                health.setText(R.string.angry);
                break;


            default:
                health.setText(R.string.health1);
                break;



        }

        predictionText.setText(bruh);

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PredictionActivity.this,MainActivity.class));
            }
        });




    }
}