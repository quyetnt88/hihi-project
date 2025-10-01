package com.example.miniproject1;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    //View
    private EditText etUsername, etPassword;
    private Button btnSignIn;
    //Notify
    private final String REQUIRE = "Required";

    private SoundEffects sfx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);


        //Anh xa
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        //Sound set
        sfx = new SoundEffects();
        sfx.load(this, R.raw.click);
        //Su kien
        btnSignIn.setOnClickListener((View.OnClickListener) this);
    }
    private boolean validate(){
        if(etUsername.getText().toString().isEmpty()){
            etUsername.setError(REQUIRE);
            return false;
        }
        if(etPassword.getText().toString().isEmpty()){
            etPassword.setError(REQUIRE);
            return false;
        }
        return true;
    }
    private void signIn(){

        if(!validate()) return;

        String username = etUsername.getText().toString().trim();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
         if(id == R.id.btnSignIn){
             sfx.play(R.raw.click, 1f);
            signIn();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sfx != null) sfx.release();
    }
}