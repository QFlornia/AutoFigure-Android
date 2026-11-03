package org.research.autofigurecompanion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String PREFS = "autofigure_companion_settings";
    private static final String KEY_FRONTEND = "frontend_url";
    private static final String KEY_BACKEND = "backend_url";

    private SharedPreferences preferences;
    private LinearLayout root;
    private LinearLayout content;
    private WebView webView;
    private String frontendUrl = "";
    private String backendUrl = "";
    private android.webkit.ValueCallback<Uri[]> fileChooserCallback;
    private static final int FILE_CHOOSER_REQUEST = 2026;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        frontendUrl = preferences.getString(KEY_FRONTEND, "");
        backendUrl = preferences.getString(KEY_BACKEND, "");
        buildShell();
        if (isValidHttpUrl(frontendUrl) && isValidHttpUrl(backendUrl)) {
            showWebApp();
        } else {
            showSettings(true);
        }
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247, 249, 252));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(16), dp(8), dp(12), dp(8));
        topBar.setBackgroundColor(Color.rgb(23, 50, 77));

        TextView title = new TextView(this);
        title.setText("AutoFigure Companion");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        topBar.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button settings = new Button(this);
        settings.setText("服务器设置");
        settings.setTextSize(12);
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> showSettings(false));
        topBar.addView(settings, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)));

        root.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private void showSettings(boolean firstRun) {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        content.removeAllViews();

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(26), dp(22), dp(26));
        scroll.addView(form);

        TextView heading = new TextView(this);
        heading.setText(firstRun ? "连接你的 AutoFigure 服务" : "服务器设置");
        heading.setTextColor(Color.rgb(23, 50, 77));
        heading.setTextSize(22);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        form.addView(heading, margin(0, 0, 0, 10));

        TextView intro = new TextView(this);
        intro.setText("此 APK 是非官方 Android WebView 客户端。请先部署项目的网页前端和 Python 后端，再填写两个可从手机访问的地址。不能填写 localhost（那会指向手机本身）。");
        intro.setTextColor(Color.rgb(75, 87, 101));
        intro.setTextSize(14);
        form.addView(intro, margin(0, 0, 0, 22));

        addFieldLabel(form, "网页前端地址");
        EditText frontendInput = new EditText(this);
        frontendInput.setSingleLine(true);
        frontendInput.setText(frontendUrl);
        frontendInput.setHint("https://your-frontend.example.com");
        frontendInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        styleInput(frontendInput);
        form.addView(frontendInput, margin(0, 6, 0, 18));

        addFieldLabel(form, "Python 后端 API 地址");
        EditText backendInput = new EditText(this);
        backendInput.setSingleLine(true);
        backendInput.setText(backendUrl);
        backendInput.setHint("https://your-backend.example.com");
        backendInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        styleInput(backendInput);
        form.addView(backendInput, margin(0, 6, 0, 8));

        TextView note = new TextView(this);
        note.setText("示例：网页前端 https://fig.example.com；后端 https://api.example.com。生产环境请使用 HTTPS，并在后端配置 AUTOFIGURE_ALLOWED_ORIGINS 允许前端域名。");
        note.setTextColor(Color.rgb(100, 110, 122));
        note.setTextSize(12);
        form.addView(note, margin(0, 0, 0, 22));

        Button connect = new Button(this);
        connect.setText("保存并连接");
        connect.setAllCaps(false);
        connect.setTextSize(16);
        connect.setOnClickListener(v -> {
            String front = normalizeUrl(frontendInput.getText().toString());
            String back = normalizeUrl(backendInput.getText().toString());
            if (!isValidHttpUrl(front) || !isValidHttpUrl(back)) {
                Toast.makeText(this, "请填写有效的 http:// 或 https:// 地址", Toast.LENGTH_LONG).show();
                return;
            }
            if (front.contains("localhost") || front.contains("127.0.0.1") ||
                    back.contains("localhost") || back.contains("127.0.0.1")) {
                Toast.makeText(this, "手机不能通过 localhost 访问电脑服务器，请改用服务器域名或局域网 IP", Toast.LENGTH_LONG).show();
                return;
            }
            frontendUrl = front;
            backendUrl = back;
            preferences.edit().putString(KEY_FRONTEND, frontendUrl)
                    .putString(KEY_BACKEND, backendUrl).apply();
            showWebApp();
        });
        form.addView(connect, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        TextView attribution = new TextView(this);
        attribution.setText("Based on the open-source AutoFigure project. Unofficial third-party client; not affiliated with or endorsed by the original authors.");
        attribution.setTextColor(Color.GRAY);
        attribution.setTextSize(11);
        form.addView(attribution, margin(0, 28, 0, 0));

        content.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    }

    private void showWebApp() {
        if (webView != null) {
            webView.destroy();
        }
        content.removeAllViews();
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    String host = uri.getHost();
                    Uri home = Uri.parse(frontendUrl);
                    // Keep the frontend inside the app. Open unrelated top-level links externally.
                    if (host != null && host.equalsIgnoreCase(home.getHost())) return false;
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (ActivityNotFoundException ignored) { }
                    return true;
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // The web app reads this at request time when calling the Flask API.
                String safeBackend = JSONObject.quote(backendUrl);
                view.evaluateJavascript(
                        "localStorage.setItem('autofigure_backend_url', " + safeBackend + ");true;", null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view,
                    android.webkit.ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException e) {
                    fileChooserCallback = null;
                    Toast.makeText(MainActivity.this, "此设备没有可用的文件选择器", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });

        content.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        webView.loadUrl(frontendUrl);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileChooserCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            fileChooserCallback.onReceiveValue(results);
            fileChooserCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else if (!frontendUrl.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage("退出应用？")
                    .setNegativeButton("取消", (d, w) -> d.dismiss())
                    .setPositiveButton("退出", (d, w) -> finish())
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private void addFieldLabel(LinearLayout parent, String value) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextColor(Color.rgb(35, 49, 64));
        label.setTextSize(14);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        parent.addView(label);
    }

    private void styleInput(EditText input) {
        input.setTextSize(14);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setBackgroundColor(Color.WHITE);
    }

    private LinearLayout.LayoutParams margin(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String normalizeUrl(String value) {
        String clean = value == null ? "" : value.trim();
        while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
        return clean;
    }

    private boolean isValidHttpUrl(String value) {
        if (value == null || value.isEmpty()) return false;
        Uri uri = Uri.parse(value);
        String scheme = uri.getScheme();
        return uri.getHost() != null && scheme != null &&
                (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
    }
}
