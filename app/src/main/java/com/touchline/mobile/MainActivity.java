package com.touchline.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Insets;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.DisplayCutout;
import android.widget.FrameLayout;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.webkit.*;
import android.widget.Toast;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private static final String ORIGIN = "https://app.touchline.local";
    private WebView web;
    private FrameLayout content;
    private volatile boolean active;
    private boolean pageReady;
    private final Set<HttpURLConnection> connections = ConcurrentHashMap.newKeySet();
    private final Semaphore slots = new Semaphore(4);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(21,40,45));
        getWindow().setNavigationBarColor(Color.rgb(20,39,46));
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(21,40,45));
        content = new FrameLayout(this);
        content.setBackgroundColor(Color.rgb(21,40,45));
        content.addView(web, new FrameLayout.LayoutParams(-1, -1));
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        // Resize the WebView itself: its HTML dialogs and fixed navigation share this safe viewport.
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                int bars = WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout();
                int handled = bars | WindowInsets.Type.ime();
                Insets safe = insets.getInsets(handled);
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                // Deliver zeroed insets so WebView does not add the same spacing a second time.
                return new WindowInsets.Builder(insets).setInsets(handled, Insets.NONE)
                    .setInsetsIgnoringVisibility(bars, Insets.NONE).setDisplayCutout(null).build();
            }
            int left = insets.getSystemWindowInsetLeft(), top = insets.getSystemWindowInsetTop();
            int right = insets.getSystemWindowInsetRight(), bottom = insets.getSystemWindowInsetBottom();
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                left = Math.max(left, cutout.getSafeInsetLeft()); top = Math.max(top, cutout.getSafeInsetTop());
                right = Math.max(right, cutout.getSafeInsetRight()); bottom = Math.max(bottom, cutout.getSafeInsetBottom());
            }
            view.setPadding(left, top, right, bottom);
            return insets.consumeSystemWindowInsets().consumeStableInsets().consumeDisplayCutout();
        });
        setContentView(content);
        content.requestApplyInsets();
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void subscriptions(String value) {
                try {JSONObject p=new JSONObject(value);MatchAlerts.configure(MainActivity.this,p);runOnUiThread(()->{if(p.optBoolean("enabled")&&p.optBoolean("prompt")&&android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},4101);else if(p.optBoolean("enabled")&&p.optBoolean("prompt")&&!MatchAlerts.permitted(MainActivity.this))startActivity(new android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()));notifyPermission();});}catch(Exception ignored){}
            }
            @JavascriptInterface public void ready(String value){runOnUiThread(()->{pageReady=true;deliverMatch();notifyPermission();});}
            @JavascriptInterface public void notificationStatus(String value) {runOnUiThread(()->notifyPermission());}
            @JavascriptInterface public void openArticle(String value) {
                try {Uri uri=Uri.parse(new JSONObject(value).getString("url"));String host=uri.getHost();if(!"https".equals(uri.getScheme())||host==null||!(host.equals("espn.com")||host.endsWith(".espn.com")||(host.equals("www.dongqiudi.com")&&uri.getPath().matches("/article/[0-9]+")))||uri.getUserInfo()!=null)return;runOnUiThread(()->{try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,uri));}catch(Exception ignored){}});}catch(Exception ignored){}
            }
            @JavascriptInterface public void appearance(String value) {
                try {JSONObject p=new JSONObject(value);boolean dark=p.optBoolean("dark",true);int color=Color.parseColor(p.getString("background"));runOnUiThread(()->{if(web==null)return;web.setBackgroundColor(color);content.setBackgroundColor(color);getWindow().setStatusBarColor(color);getWindow().setNavigationBarColor(color);setSystemBarAppearance(dark);});}catch(Exception ignored){}
            }
            @JavascriptInterface public void openUpdate(String value) {
                try {String url=new JSONObject(value).getString("url");if(!url.matches("https://github\\.com/AEGO-WRJ/touchline-mobile/releases/download/v[0-9]+\\.[0-9]+\\.[0-9]+/Touchline-Android-v[0-9]+\\.[0-9]+\\.[0-9]+\\.apk"))return;runOnUiThread(()->{try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){Toast.makeText(MainActivity.this,"未找到浏览器，请从仓库下载更新",Toast.LENGTH_LONG).show();}});}catch(Exception ignored){}
            }
            @JavascriptInterface public void setTeamIcon(String value) {
                try {String id=new JSONObject(value).optString("id");if(!id.isEmpty()&&!id.matches("[0-9]+"))return;runOnUiThread(()->switchIcon(id));}catch(Exception ignored){}
            }
            @JavascriptInterface public void announce(String value) {
                try { String text = new JSONObject(value).optString("text"); runOnUiThread(()->{ if(active) Toast.makeText(MainActivity.this,text,Toast.LENGTH_LONG).show(); }); } catch(Exception ignored) {}
            }
        }, "Touchline");
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view,String url){notifyPermission();}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest r) { return !r.getUrl().toString().startsWith(ORIGIN+"/"); }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest r) {
                Uri uri=r.getUrl();
                if(!"app.touchline.local".equals(uri.getHost())) return null;
                try {
                    if("/api".equals(uri.getPath())) return api(uri.getQueryParameter("url"));
                    String path=uri.getPath().substring(1);
                    if(path.isEmpty())path="index.html";
                    if(path.matches("crest-[0-9]+[.]png")) {
                        String id=path.substring(6,path.length()-4);
                        int resource=getResources().getIdentifier("team_"+id,"drawable",getPackageName());
                        if(resource==0)return error(404);
                        return new WebResourceResponse("image/png",null,getResources().openRawResource(resource));
                    }
                    if(path.contains("..")||path.contains("/")||!Arrays.asList("index.html","style.css","app.mjs","domain.mjs","transport.mjs","appearance.mjs","cache-policy.mjs","cache-store.mjs","info-model.mjs","info.mjs","news-model.mjs","news.mjs","match-data.mjs","match-events.mjs","round-cache.mjs","startup.mjs","catalog.json").contains(path))return error(404);
                    String mime=path.endsWith(".mjs")?"text/javascript":path.endsWith(".css")?"text/css":path.endsWith(".json")?"application/json":"text/html";
                    Map<String,String> headers=new HashMap<>();
                    headers.put("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' https://a.espncdn.com https://images.fotmob.com https://*.qunliao.info; connect-src 'self'; object-src 'none'; base-uri 'none'");
                    return new WebResourceResponse(mime,"UTF-8",200,"OK",headers,getAssets().open(path));
                } catch(Exception e) { return error(503); }
            }
        });
        web.loadUrl(ORIGIN+"/index.html");
    }
    private void setSystemBarAppearance(boolean dark) {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            if (controller != null) controller.setSystemBarsAppearance(dark ? 0 : light, light);
        } else {
            View decor = getWindow().getDecorView();
            int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decor.setSystemUiVisibility((decor.getSystemUiVisibility() & ~light) | (dark ? 0 : light));
        }
    }
    private WebResourceResponse api(String address) throws Exception {
        if(!active||address==null)return error(503);
        URL url=new URL(address);
        boolean update=url.getHost().equals("api.github.com")&&url.getPath().equals("/repos/AEGO-WRJ/touchline-mobile/releases/latest");
        boolean news=url.getHost().equals("api.dongqiudi.com")&&url.getPath().matches("/app/tabs/web/[0-9]+[.]json");
        if(!"https".equals(url.getProtocol())||(!update&&!news&&!Arrays.asList("site.api.espn.com","www.fotmob.com","data.fotmob.com").contains(url.getHost()))||url.getUserInfo()!=null)return error(403);
        if(!slots.tryAcquire(18,TimeUnit.SECONDS))return error(503);
        HttpURLConnection c=null;
        try {
            if(!active)return error(503);
            c=(HttpURLConnection)url.openConnection();connections.add(c);
            c.setConnectTimeout(12000);c.setReadTimeout(18000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","TouchlineMobile/1.4");c.setRequestProperty("Accept","application/json");c.setRequestProperty("Accept-Encoding","gzip");
            if(c.getResponseCode()!=200)return error(503);
            InputStream stream=c.getInputStream();if("gzip".equalsIgnoreCase(c.getContentEncoding()))stream=new GZIPInputStream(stream);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream input=stream){byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){if(!active||bytes.size()+n>16000000)return error(503);bytes.write(buffer,0,n);}}
            return new WebResourceResponse("application/json","UTF-8",200,"OK",Collections.singletonMap("Cache-Control","no-store"),new ByteArrayInputStream(bytes.toByteArray()));
        } finally {if(c!=null){connections.remove(c);c.disconnect();}slots.release();}
    }
    private WebResourceResponse error(int status) {return new WebResourceResponse("application/json","UTF-8",status,"Unavailable",Collections.emptyMap(),new ByteArrayInputStream("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
    private void switchIcon(String id) {
        String alias=getPackageName()+"."+(id.isEmpty()?"DefaultIcon":"Team"+id);
        android.content.SharedPreferences saved=getSharedPreferences("launcher",MODE_PRIVATE);
        String previous=saved.getString("alias",getPackageName()+".DefaultIcon");if(alias.equals(previous))return;
        android.content.pm.PackageManager pm=getPackageManager();
        try {
            android.content.ComponentName next=new android.content.ComponentName(this,alias);
            pm.getActivityInfo(next,android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS);
            pm.setComponentEnabledSetting(next,android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,android.content.pm.PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(new android.content.ComponentName(this,previous),android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,android.content.pm.PackageManager.DONT_KILL_APP);
            saved.edit().putString("alias",alias).apply();
        }catch(Exception e){Toast.makeText(this,"此球队图标暂不可用",Toast.LENGTH_LONG).show();}
    }
    private void notifyPermission(){if(web!=null&&pageReady)web.evaluateJavascript("window.notificationState&&window.notificationState("+MatchAlerts.permitted(this)+")",null);}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);notifyPermission();}
    @Override protected void onNewIntent(android.content.Intent intent){super.onNewIntent(intent);setIntent(intent);deliverMatch();}
    private void deliverMatch(){if(web==null||!pageReady)return;String id=getIntent().getStringExtra("matchId"),league=getIntent().getStringExtra("league");if(id==null||league==null||!id.matches("[0-9]{1,12}")||!league.matches("[a-z0-9._]{1,40}"))return;getIntent().removeExtra("matchId");getIntent().removeExtra("league");web.evaluateJavascript("window.openNotifiedMatch&&window.openNotifiedMatch("+JSONObject.quote(id)+","+JSONObject.quote(league)+")",null);}
    @Override protected void onResume(){super.onResume();active=true;notifyPermission();if(web!=null){web.onResume();web.evaluateJavascript("window.lifecycle&&window.lifecycle(true)",null);}}
    @Override protected void onPause(){active=false;if(web!=null){web.evaluateJavascript("window.lifecycle&&window.lifecycle(false)",null);web.onPause();}for(HttpURLConnection c:connections)c.disconnect();super.onPause();}
    @Override public void onBackPressed(){web.evaluateJavascript("window.goBack?window.goBack():false",handled->{if(!"true".equals(handled))finish();});}
    @Override protected void onDestroy(){for(HttpURLConnection c:connections)c.disconnect();if(web!=null){web.removeJavascriptInterface("Touchline");web.destroy();web=null;}super.onDestroy();}
}
