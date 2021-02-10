package com.delinnovtech.allsxvideodownloader;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.delinnovtech.allsxvideodownloader.R;

public class SplashActivity extends AppCompatActivity {

     ImageView appLogo;
     TextView logoTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        Window window = this.getWindow();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(ContextCompat.getColor(this,R.color.black));
            window.setNavigationBarColor(ContextCompat.getColor(this,R.color.black));
        }



        appLogo=findViewById(R.id.app_Logo);
        logoTxt=findViewById(R.id.logo_text);



        Animation aniFade = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.logo_animation);
        aniFade.setDuration(2000);
        appLogo.startAnimation(aniFade);
        Animation anim_title = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.title_animation);
        anim_title.setDuration(2000);
        logoTxt.startAnimation(anim_title);





        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 2000);
    }
}