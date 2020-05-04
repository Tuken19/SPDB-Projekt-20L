package com.example.spdb;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button button_get_started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        button_get_started.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent MapActivityintent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(MapActivityintent);
            }
        });
    }

    private void init(){
        button_get_started = findViewById(R.id.button_get_started);
    }
}
