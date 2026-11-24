package com.example.trivia;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class InformationActivity extends AppCompatActivity {

    WebView webView;
    Button btnBackToMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        webView = findViewById(R.id.webView);
        btnBackToMenu = findViewById(R.id.btnBackToMenu);

        // Load Wikipedia trivia page
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://en.wikipedia.org/wiki/Trivia");

        btnBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.stopLoading();
                webView.destroy();
                finish();
            }
        });
    }
}