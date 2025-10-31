package com.example.bluechat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SplashActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create WebView programmatically
        webView = new WebView(this);
        setContentView(webView);

        // Configure WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        // Add JavaScript interface
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        // Set WebViewClient to handle page loading
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Page loaded, animations will handle the transition
            }
        });

        // Load the HTML content
        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang=\"tr\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Blue Chat - Splash Screen</title>\n" +
                "    <style>\n" +
                "        * {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            box-sizing: border-box;\n" +
                "        }\n" +
                "\n" +
                "        body {\n" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;\n" +
                "            overflow: hidden;\n" +
                "            background: #FFFFFF;\n" +
                "            height: 100vh;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            position: relative;\n" +
                "        }\n" +
                "\n" +
                "        /* Subtle gradient background */\n" +
                "        .background {\n" +
                "            position: absolute;\n" +
                "            width: 100%;\n" +
                "            height: 100%;\n" +
                "            background: \n" +
                "                radial-gradient(circle at 20% 20%, rgba(219, 234, 254, 0.4) 0%, transparent 50%),\n" +
                "                radial-gradient(circle at 80% 80%, rgba(191, 219, 254, 0.3) 0%, transparent 50%),\n" +
                "                radial-gradient(circle at 50% 50%, rgba(239, 246, 255, 0.5) 0%, transparent 70%);\n" +
                "            animation: bgBreath 6s ease-in-out infinite alternate;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes bgBreath {\n" +
                "            0% {\n" +
                "                transform: scale(1);\n" +
                "                opacity: 0.8;\n" +
                "            }\n" +
                "            100% {\n" +
                "                transform: scale(1.05);\n" +
                "                opacity: 1;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Floating geometric shapes */\n" +
                "        .shape {\n" +
                "            position: absolute;\n" +
                "            opacity: 0.08;\n" +
                "            animation: shapeFloat 15s ease-in-out infinite;\n" +
                "        }\n" +
                "\n" +
                "        .shape-circle {\n" +
                "            width: 300px;\n" +
                "            height: 300px;\n" +
                "            border-radius: 50%;\n" +
                "            background: linear-gradient(135deg, #3b82f6, #60a5fa);\n" +
                "            top: -50px;\n" +
                "            right: -50px;\n" +
                "            animation-delay: 0s;\n" +
                "        }\n" +
                "\n" +
                "        .shape-square {\n" +
                "            width: 200px;\n" +
                "            height: 200px;\n" +
                "            border-radius: 50px;\n" +
                "            background: linear-gradient(135deg, #2563eb, #3b82f6);\n" +
                "            bottom: -30px;\n" +
                "            left: -30px;\n" +
                "            animation-delay: 2s;\n" +
                "            transform: rotate(45deg);\n" +
                "        }\n" +
                "\n" +
                "        @keyframes shapeFloat {\n" +
                "            0%, 100% {\n" +
                "                transform: translateY(0);\n" +
                "            }\n" +
                "            50% {\n" +
                "                transform: translateY(-30px);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Particle dots */\n" +
                "        .particles {\n" +
                "            position: absolute;\n" +
                "            width: 100%;\n" +
                "            height: 100%;\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "\n" +
                "        .particle {\n" +
                "            position: absolute;\n" +
                "            width: 4px;\n" +
                "            height: 4px;\n" +
                "            background: linear-gradient(135deg, #3b82f6, #60a5fa);\n" +
                "            border-radius: 50%;\n" +
                "            opacity: 0;\n" +
                "            animation: particleFloat 8s ease-in-out infinite;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes particleFloat {\n" +
                "            0% {\n" +
                "                transform: translateY(100vh) scale(0);\n" +
                "                opacity: 0;\n" +
                "            }\n" +
                "            20% {\n" +
                "                opacity: 0.6;\n" +
                "            }\n" +
                "            80% {\n" +
                "                opacity: 0.6;\n" +
                "            }\n" +
                "            100% {\n" +
                "                transform: translateY(-100px) scale(1);\n" +
                "                opacity: 0;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Main container */\n" +
                "        .splash-container {\n" +
                "            position: relative;\n" +
                "            text-align: center;\n" +
                "            z-index: 10;\n" +
                "            animation: containerFadeIn 1s ease-out;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes containerFadeIn {\n" +
                "            from {\n" +
                "                opacity: 0;\n" +
                "                transform: translateY(30px);\n" +
                "            }\n" +
                "            to {\n" +
                "                opacity: 1;\n" +
                "                transform: translateY(0);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Logo section */\n" +
                "        .logo-wrapper {\n" +
                "            position: relative;\n" +
                "            margin-bottom: 50px;\n" +
                "            perspective: 1200px;\n" +
                "        }\n" +
                "\n" +
                "        /* Circular glow behind logo */\n" +
                "        .logo-glow-outer {\n" +
                "            position: absolute;\n" +
                "            width: 240px;\n" +
                "            height: 240px;\n" +
                "            left: 50%;\n" +
                "            top: 50%;\n" +
                "            transform: translate(-50%, -50%);\n" +
                "            background: radial-gradient(circle, rgba(59, 130, 246, 0.15) 0%, transparent 70%);\n" +
                "            border-radius: 50%;\n" +
                "            animation: glowPulse 3s ease-in-out infinite;\n" +
                "            filter: blur(30px);\n" +
                "        }\n" +
                "\n" +
                "        @keyframes glowPulse {\n" +
                "            0%, 100% {\n" +
                "                transform: translate(-50%, -50%) scale(1);\n" +
                "                opacity: 0.5;\n" +
                "            }\n" +
                "            50% {\n" +
                "                transform: translate(-50%, -50%) scale(1.2);\n" +
                "                opacity: 0.8;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Rotating rings */\n" +
                "        .ring-container {\n" +
                "            position: absolute;\n" +
                "            width: 200px;\n" +
                "            height: 200px;\n" +
                "            left: 50%;\n" +
                "            top: 50%;\n" +
                "            transform: translate(-50%, -50%);\n" +
                "        }\n" +
                "\n" +
                "        .ring {\n" +
                "            position: absolute;\n" +
                "            width: 100%;\n" +
                "            height: 100%;\n" +
                "            border: 2px solid;\n" +
                "            border-radius: 50%;\n" +
                "            border-color: #3b82f6 transparent transparent transparent;\n" +
                "        }\n" +
                "\n" +
                "        .ring-1 {\n" +
                "            animation: ringRotate 3s linear infinite;\n" +
                "            opacity: 0.3;\n" +
                "        }\n" +
                "\n" +
                "        .ring-2 {\n" +
                "            animation: ringRotate 4s linear infinite reverse;\n" +
                "            opacity: 0.2;\n" +
                "            border-color: #60a5fa transparent transparent transparent;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes ringRotate {\n" +
                "            0% {\n" +
                "                transform: rotate(0deg);\n" +
                "            }\n" +
                "            100% {\n" +
                "                transform: rotate(360deg);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Main logo card */\n" +
                "        .logo-card {\n" +
                "            position: relative;\n" +
                "            width: 170px;\n" +
                "            height: 170px;\n" +
                "            margin: 0 auto;\n" +
                "            background: #FFFFFF;\n" +
                "            border-radius: 45px;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            box-shadow: \n" +
                "                0 20px 60px rgba(59, 130, 246, 0.2),\n" +
                "                0 10px 30px rgba(0, 0, 0, 0.05),\n" +
                "                inset 0 1px 0 rgba(255, 255, 255, 0.8);\n" +
                "            border: 1px solid rgba(59, 130, 246, 0.1);\n" +
                "            animation: logoFloat 4s ease-in-out infinite;\n" +
                "            transform-style: preserve-3d;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes logoFloat {\n" +
                "            0%, 100% {\n" +
                "                transform: translateY(0) rotateY(0deg);\n" +
                "            }\n" +
                "            25% {\n" +
                "                transform: translateY(-10px) rotateY(5deg);\n" +
                "            }\n" +
                "            75% {\n" +
                "                transform: translateY(-10px) rotateY(-5deg);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Chat bubble icon */\n" +
                "        .chat-bubble-wrapper {\n" +
                "            position: relative;\n" +
                "            width: 90px;\n" +
                "            height: 90px;\n" +
                "        }\n" +
                "\n" +
                "        .chat-bubble-main {\n" +
                "            width: 80px;\n" +
                "            height: 70px;\n" +
                "            background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);\n" +
                "            border-radius: 28px 28px 28px 10px;\n" +
                "            position: relative;\n" +
                "            box-shadow: \n" +
                "                0 10px 40px rgba(59, 130, 246, 0.4),\n" +
                "                inset 0 2px 10px rgba(255, 255, 255, 0.3);\n" +
                "            animation: bubbleBounce 2s ease-in-out infinite;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes bubbleBounce {\n" +
                "            0%, 100% {\n" +
                "                transform: scale(1) translateY(0);\n" +
                "            }\n" +
                "            50% {\n" +
                "                transform: scale(1.05) translateY(-5px);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Dots inside bubble */\n" +
                "        .chat-dots {\n" +
                "            position: absolute;\n" +
                "            top: 50%;\n" +
                "            left: 50%;\n" +
                "            transform: translate(-50%, -50%);\n" +
                "            display: flex;\n" +
                "            gap: 9px;\n" +
                "        }\n" +
                "\n" +
                "        .chat-dot {\n" +
                "            width: 10px;\n" +
                "            height: 10px;\n" +
                "            background: #FFFFFF;\n" +
                "            border-radius: 50%;\n" +
                "            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);\n" +
                "            animation: dotBounce 1.4s ease-in-out infinite;\n" +
                "        }\n" +
                "\n" +
                "        .chat-dot:nth-child(1) {\n" +
                "            animation-delay: 0s;\n" +
                "        }\n" +
                "\n" +
                "        .chat-dot:nth-child(2) {\n" +
                "            animation-delay: 0.2s;\n" +
                "        }\n" +
                "\n" +
                "        .chat-dot:nth-child(3) {\n" +
                "            animation-delay: 0.4s;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes dotBounce {\n" +
                "            0%, 60%, 100% {\n" +
                "                transform: translateY(0) scale(1);\n" +
                "            }\n" +
                "            30% {\n" +
                "                transform: translateY(-8px) scale(1.1);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Brand text */\n" +
                "        .brand-section {\n" +
                "            margin-bottom: 60px;\n" +
                "        }\n" +
                "\n" +
                "        .app-name {\n" +
                "            font-size: 72px;\n" +
                "            font-weight: 800;\n" +
                "            background: linear-gradient(135deg, #1e40af 0%, #3b82f6 50%, #60a5fa 100%);\n" +
                "            -webkit-background-clip: text;\n" +
                "            -webkit-text-fill-color: transparent;\n" +
                "            background-clip: text;\n" +
                "            margin-bottom: 16px;\n" +
                "            letter-spacing: -3px;\n" +
                "            position: relative;\n" +
                "            animation: textShine 3s ease-in-out infinite;\n" +
                "            background-size: 200% auto;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes textShine {\n" +
                "            0%, 100% {\n" +
                "                background-position: 0% center;\n" +
                "            }\n" +
                "            50% {\n" +
                "                background-position: 100% center;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        .app-tagline {\n" +
                "            font-size: 16px;\n" +
                "            color: #64748b;\n" +
                "            font-weight: 500;\n" +
                "            letter-spacing: 3px;\n" +
                "            text-transform: uppercase;\n" +
                "        }\n" +
                "\n" +
                "        /* Loading section */\n" +
                "        .loading-wrapper {\n" +
                "            width: 320px;\n" +
                "            margin: 0 auto;\n" +
                "        }\n" +
                "\n" +
                "        /* Progress bar */\n" +
                "        .progress-container {\n" +
                "            width: 100%;\n" +
                "            height: 6px;\n" +
                "            background: #f1f5f9;\n" +
                "            border-radius: 10px;\n" +
                "            overflow: hidden;\n" +
                "            position: relative;\n" +
                "            box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.06);\n" +
                "        }\n" +
                "\n" +
                "        .progress-fill {\n" +
                "            height: 100%;\n" +
                "            background: linear-gradient(90deg, \n" +
                "                #3b82f6 0%,\n" +
                "                #60a5fa 50%,\n" +
                "                #3b82f6 100%\n" +
                "            );\n" +
                "            background-size: 200% 100%;\n" +
                "            border-radius: 10px;\n" +
                "            width: 0%;\n" +
                "            animation: progressGrow 3s ease-out forwards, progressShimmer 2s ease-in-out infinite;\n" +
                "            box-shadow: 0 0 20px rgba(59, 130, 246, 0.5);\n" +
                "            position: relative;\n" +
                "        }\n" +
                "\n" +
                "        .progress-fill::after {\n" +
                "            content: '';\n" +
                "            position: absolute;\n" +
                "            top: 0;\n" +
                "            left: 0;\n" +
                "            right: 0;\n" +
                "            height: 100%;\n" +
                "            background: linear-gradient(90deg, \n" +
                "                transparent 0%,\n" +
                "                rgba(255, 255, 255, 0.6) 50%,\n" +
                "                transparent 100%\n" +
                "            );\n" +
                "            animation: shimmerMove 1.5s ease-in-out infinite;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes progressGrow {\n" +
                "            0% {\n" +
                "                width: 0%;\n" +
                "            }\n" +
                "            100% {\n" +
                "                width: 100%;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        @keyframes progressShimmer {\n" +
                "            0% {\n" +
                "                background-position: 200% 0;\n" +
                "            }\n" +
                "            100% {\n" +
                "                background-position: -200% 0;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        @keyframes shimmerMove {\n" +
                "            0% {\n" +
                "                transform: translateX(-100%);\n" +
                "            }\n" +
                "            100% {\n" +
                "                transform: translateX(200%);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Loading info */\n" +
                "        .loading-info {\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            align-items: center;\n" +
                "            margin-top: 20px;\n" +
                "        }\n" +
                "\n" +
                "        .loading-label {\n" +
                "            font-size: 14px;\n" +
                "            color: #64748b;\n" +
                "            font-weight: 600;\n" +
                "            letter-spacing: 1.5px;\n" +
                "            text-transform: uppercase;\n" +
                "        }\n" +
                "\n" +
                "        .loading-percentage {\n" +
                "            font-size: 14px;\n" +
                "            color: #3b82f6;\n" +
                "            font-weight: 700;\n" +
                "            font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;\n" +
                "        }\n" +
                "\n" +
                "        /* Version info */\n" +
                "        .version-container {\n" +
                "            position: absolute;\n" +
                "            bottom: 50px;\n" +
                "            left: 50%;\n" +
                "            transform: translateX(-50%);\n" +
                "            background: #f8fafc;\n" +
                "            border: 1px solid #e2e8f0;\n" +
                "            padding: 10px 24px;\n" +
                "            border-radius: 25px;\n" +
                "            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);\n" +
                "        }\n" +
                "\n" +
                "        .version-label {\n" +
                "            font-size: 12px;\n" +
                "            color: #94a3b8;\n" +
                "            font-weight: 600;\n" +
                "            letter-spacing: 1.5px;\n" +
                "        }\n" +
                "\n" +
                "        /* Decorative elements */\n" +
                "        .deco-dot {\n" +
                "            position: absolute;\n" +
                "            width: 8px;\n" +
                "            height: 8px;\n" +
                "            background: linear-gradient(135deg, #3b82f6, #60a5fa);\n" +
                "            border-radius: 50%;\n" +
                "            opacity: 0.4;\n" +
                "        }\n" +
                "\n" +
                "        .deco-dot-1 {\n" +
                "            top: 15%;\n" +
                "            left: 10%;\n" +
                "            animation: decoFloat 4s ease-in-out infinite;\n" +
                "        }\n" +
                "\n" +
                "        .deco-dot-2 {\n" +
                "            top: 25%;\n" +
                "            right: 15%;\n" +
                "            animation: decoFloat 5s ease-in-out infinite;\n" +
                "            animation-delay: 1s;\n" +
                "        }\n" +
                "\n" +
                "        .deco-dot-3 {\n" +
                "            bottom: 20%;\n" +
                "            left: 12%;\n" +
                "            animation: decoFloat 6s ease-in-out infinite;\n" +
                "            animation-delay: 2s;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes decoFloat {\n" +
                "            0%, 100% {\n" +
                "                transform: translateY(0);\n" +
                "            }\n" +
                "            50% {\n" +
                "                transform: translateY(-20px);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        /* Fade out animation for transition */\n" +
                "        .fade-out {\n" +
                "            animation: fadeOut 0.5s ease-out forwards;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes fadeOut {\n" +
                "            from {\n" +
                "                opacity: 1;\n" +
                "            }\n" +
                "            to {\n" +
                "                opacity: 0;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"background\"></div>\n" +
                "    \n" +
                "    <!-- Geometric shapes -->\n" +
                "    <div class=\"shape shape-circle\"></div>\n" +
                "    <div class=\"shape shape-square\"></div>\n" +
                "    \n" +
                "    <!-- Particles -->\n" +
                "    <div class=\"particles\" id=\"particles\"></div>\n" +
                "    \n" +
                "    <!-- Decorative dots -->\n" +
                "    <div class=\"deco-dot deco-dot-1\"></div>\n" +
                "    <div class=\"deco-dot deco-dot-2\"></div>\n" +
                "    <div class=\"deco-dot deco-dot-3\"></div>\n" +
                "    \n" +
                "    <div class=\"splash-container\" id=\"splashContainer\">\n" +
                "        <!-- Logo -->\n" +
                "        <div class=\"logo-wrapper\">\n" +
                "            <div class=\"logo-glow-outer\"></div>\n" +
                "            <div class=\"ring-container\">\n" +
                "                <div class=\"ring ring-1\"></div>\n" +
                "                <div class=\"ring ring-2\"></div>\n" +
                "            </div>\n" +
                "            <div class=\"logo-card\">\n" +
                "                <div class=\"chat-bubble-wrapper\">\n" +
                "                    <div class=\"chat-bubble-main\">\n" +
                "                        <div class=\"chat-dots\">\n" +
                "                            <div class=\"chat-dot\"></div>\n" +
                "                            <div class=\"chat-dot\"></div>\n" +
                "                            <div class=\"chat-dot\"></div>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        \n" +
                "        <!-- Brand -->\n" +
                "        <div class=\"brand-section\">\n" +
                "            <div class=\"app-name\">Blue Chat</div>\n" +
                "            <div class=\"app-tagline\">Connect Beyond Words</div>\n" +
                "        </div>\n" +
                "        \n" +
                "        <!-- Loading -->\n" +
                "        <div class=\"loading-wrapper\">\n" +
                "            <div class=\"progress-container\">\n" +
                "                <div class=\"progress-fill\"></div>\n" +
                "            </div>\n" +
                "            <div class=\"loading-info\">\n" +
                "                <span class=\"loading-label\">Hazırlanıyor</span>\n" +
                "                <span class=\"loading-percentage\" id=\"percentage\">0%</span>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <!-- Version info -->\n" +
                "    <div class=\"version-container\">\n" +
                "        <span class=\"version-label\">v1.0.0</span>\n" +
                "    </div>\n" +
                "</body>\n" +
                "<script>\n" +
                "    // Generate random particles\n" +
                "    function createParticles() {\n" +
                "        const particlesContainer = document.getElementById('particles');\n" +
                "        const numParticles = 20;\n" +
                "        for (let i = 0; i < numParticles; i++) {\n" +
                "            const particle = document.createElement('div');\n" +
                "            particle.className = 'particle';\n" +
                "            particle.style.left = Math.random() * 100 + '%';\n" +
                "            particle.style.animationDelay = Math.random() * 8 + 's';\n" +
                "            particlesContainer.appendChild(particle);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    // Update progress percentage\n" +
                "    function updateProgress() {\n" +
                "        const percentageElement = document.getElementById('percentage');\n" +
                "        let progress = 0;\n" +
                "        const interval = setInterval(() => {\n" +
                "            progress += Math.random() * 5 + 1; // Random increment\n" +
                "            if (progress >= 100) {\n" +
                "                progress = 100;\n" +
                "                clearInterval(interval);\n" +
                "            }\n" +
                "            percentageElement.textContent = Math.floor(progress) + '%';\n" +
                "        }, 100);\n" +
                "    }\n" +
                "\n" +
                "    // Initialize on load\n" +
                "    window.onload = function() {\n" +
                "        createParticles();\n" +
                "        updateProgress();\n" +
                "        // Auto-transition after 4 seconds\n" +
                "        setTimeout(() => {\n" +
                "            const container = document.getElementById('splashContainer');\n" +
                "            container.classList.add('fade-out');\n" +
                "            setTimeout(() => {\n" +
                "                if (window.Android && window.Android.finishSplash) {\n" +
                "                    window.Android.finishSplash();\n" +
                "                }\n" +
                "            }, 500); // Wait for fade-out animation\n" +
                "        }, 4000);\n" +
                "    };\n" +
                "</script>\n" +
                "</html>";

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }

    // Inner class for JavaScript interface
    public class WebAppInterface {
        @JavascriptInterface
        public void finishSplash() {
            runOnUiThread(() -> {
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            });
        }
    }
}
