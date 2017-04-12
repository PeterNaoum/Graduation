package com.example.peter.graddraft;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * Created by Peter on 5/14/2016.
 */
public class Splash extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        final ImageView splash = (ImageView)findViewById(R.id.splashim);
//        final TextView splash=(TextView)findViewById(R.id.splashtext);
//        Typeface custom_font = Typeface.createFromAsset(getAssets(), "Sofia-Regular.otf");
//        splash.setTypeface(custom_font);
        final Animation animation= AnimationUtils.loadAnimation(getBaseContext(),R.anim.rotate);
//        final Animation animation2= AnimationUtils.loadAnimation(getBaseContext(),R.anim.fade);
        splash.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
//                splash.startAnimation(animation2);
                finish();
                Intent i=new Intent(Splash.this,MainActivity.class);
                startActivity(i);


            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}
