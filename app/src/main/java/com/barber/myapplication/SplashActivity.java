package com.barber.myapplication;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import android.widget.ImageView;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Enlazamos el ImageView del layout
        ImageView splashGif = findViewById(R.id.splashGif);

        // Cargamos el GIF usando Glide
        Glide.with(this)
                .asGif()
                .load(R.drawable.logo) // Asegúrate de tener un archivo GIF en res/drawable
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(splashGif);

        // Esperar 5 segundos antes de pasar a la siguiente actividad
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Cierra la SplashActivity
        }, 5000); // 5000ms = 5 segundos
    }
}