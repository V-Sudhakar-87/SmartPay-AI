
package com.smartpay.ai;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private View greenReveal;
    private View logoContainer;
    private android.widget.ImageView tvLogoSymbol;
    private TextView tvAppName;
    private TextView tvTagline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        greenReveal = findViewById(R.id.greenReveal);
        logoContainer = findViewById(R.id.logoContainer);

        tvLogoSymbol = findViewById(R.id.tvLogoSymbol);
        tvAppName = findViewById(R.id.tvAppName);
        tvTagline = findViewById(R.id.tvTagline);

        startSplashAnimation();
    }

    private void startSplashAnimation() {

        // Initial logo animation
        logoContainer.setAlpha(0f);
        logoContainer.setScaleX(0.7f);
        logoContainer.setScaleY(0.7f);

        AnimatorSet logoAnimation = new AnimatorSet();

        logoAnimation.playTogether(
                ObjectAnimator.ofFloat(
                        logoContainer, "alpha", 0f, 1f
                ),
                ObjectAnimator.ofFloat(
                        logoContainer, "scaleX", 0.7f, 1f
                ),
                ObjectAnimator.ofFloat(
                        logoContainer, "scaleY", 0.7f, 1f
                )
        );

        logoAnimation.setDuration(1200);
        logoAnimation.setInterpolator(
                new AccelerateDecelerateInterpolator()
        );

        logoAnimation.addListener(
                new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startGreenRevealAnimation();
                    }
                }
        );

        logoAnimation.start();
    }

    private void startGreenRevealAnimation() {

        greenReveal.setVisibility(View.VISIBLE);

        greenReveal.setScaleX(0f);
        greenReveal.setScaleY(0f);

        AnimatorSet revealAnimation = new AnimatorSet();

        revealAnimation.playTogether(
                ObjectAnimator.ofFloat(
                        greenReveal, "scaleX", 0f, 1.15f
                ),
                ObjectAnimator.ofFloat(
                        greenReveal, "scaleY", 0f, 1.15f
                )
        );

        revealAnimation.setDuration(2000);
        revealAnimation.setInterpolator(
                new AccelerateDecelerateInterpolator()
        );

        revealAnimation.addListener(
                new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {

                        openHomeScreen();
                    }
                }
        );

        revealAnimation.start();
    }

    private void openHomeScreen() {

        Intent intent = new Intent(
                SplashActivity.this,
                MainActivity.class
        );

        startActivity(intent);

        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        finish();
    }
}
