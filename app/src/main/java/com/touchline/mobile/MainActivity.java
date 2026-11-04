package com.touchline.mobile;

import android.app.Activity;
import android.os.Bundle;
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
    private volatile boolean active;
    private final Set<HttpURLConnection> connections = ConcurrentHashMap.newKeySet();
    private final Semaphore slots = new Semaphore(4);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(21,40,45));
        getWindow().setNavigationBarColor(Color.rgb(20,39,46));
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(21,40,45));
        web.setOnApplyWindowInsetsListener((v,insets)-> { v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets.consumeSystemWindowInsets(); });
        setContentView(web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void appearance(String value) {
                try {JSONObject p=new JSONObject(value);boolean dark=p.optBoolean("dark",true);int color=Color.parseColor(p.getString("background"));runOnUiThread(()->{if(web==null)return;web.setBackgroundColor(color);getWindow().setStatusBarColor(color);getWindow().setNavigationBarColor(color);getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);});}catch(Exception ignored){}
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
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest r) { return !r.getUrl().toString().startsWith(ORIGIN+"/"); }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest r) {
                Uri uri=r.getUrl();
                if(!"app.touchline.local".equals(uri.getHost())) return null;
                try {
                    if("/api".equals(uri.getPath())) return api(uri.getQueryParameter("url"));
                    String path=uri.getPath().substring(1);
                    if(path.isEmpty())path="index.html";
                    if(path.contains("..")||path.contains("/")||!Arrays.asList("index.html","style.css","app.mjs","domain.mjs","transport.mjs","appearance.mjs","cache-policy.mjs","cache-store.mjs","catalog.json").contains(path))return error(404);
                    String mime=path.endsWith(".mjs")?"text/javascript":path.endsWith(".css")?"text/css":path.endsWith(".json")?"application/json":"text/html";
                    Map<String,String> headers=new HashMap<>();
                    headers.put("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src https://a.espncdn.com; connect-src 'self'; object-src 'none'; base-uri 'none'");
                    return new WebResourceResponse(mime,"UTF-8",200,"OK",headers,getAssets().open(path));
                } catch(Exception e) { return error(503); }
            }
        });
        web.loadUrl(ORIGIN+"/index.html");
    }
    private WebResourceResponse api(String address) throws Exception {
        if(!active||address==null)return error(503);
        URL url=new URL(address);
        boolean update=url.getHost().equals("api.github.com")&&url.getPath().equals("/repos/AEGO-WRJ/touchline-mobile/releases/latest");
        if(!"https".equals(url.getProtocol())||(!update&&!Arrays.asList("site.api.espn.com","www.fotmob.com","data.fotmob.com").contains(url.getHost()))||url.getUserInfo()!=null)return error(403);
        if(!slots.tryAcquire(18,TimeUnit.SECONDS))return error(503);
        HttpURLConnection c=null;
        try {
            if(!active)return error(503);
            c=(HttpURLConnection)url.openConnection();connections.add(c);
            c.setConnectTimeout(12000);c.setReadTimeout(18000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","TouchlineMobile/1.0");c.setRequestProperty("Accept","application/json");c.setRequestProperty("Accept-Encoding","gzip");
            if(c.getResponseCode()!=200)return error(503);
            InputStream stream=c.getInputStream();if("gzip".equalsIgnoreCase(c.getContentEncoding()))stream=new GZIPInputStream(stream);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream input=stream){byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){if(!active||bytes.size()+n>16000000)return error(503);bytes.write(buffer,0,n);}}
            return new WebResourceResponse("application/json","UTF-8",new ByteArrayInputStream(bytes.toByteArray()));
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
    @Override protected void onResume(){super.onResume();active=true;if(web!=null){web.onResume();web.evaluateJavascript("window.lifecycle&&window.lifecycle(true)",null);}}
    @Override protected void onPause(){active=false;if(web!=null){web.evaluateJavascript("window.lifecycle&&window.lifecycle(false)",null);web.onPause();}for(HttpURLConnection c:connections)c.disconnect();super.onPause();}
    @Override public void onBackPressed(){web.evaluateJavascript("window.goBack?window.goBack():false",handled->{if(!"true".equals(handled))finish();});}
    @Override protected void onDestroy(){for(HttpURLConnection c:connections)c.disconnect();if(web!=null){web.removeJavascriptInterface("Touchline");web.destroy();web=null;}super.onDestroy();}
}
