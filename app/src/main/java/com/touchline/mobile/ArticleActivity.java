package com.touchline.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;

/** Isolated web reader: remote articles never receive the app's JavaScript bridge. */
public final class ArticleActivity extends Activity {
    private WebView web;
    private ProgressBar progress;
    private TextView title;
    private Button retry;
    private String initialUrl;

    private static boolean allowed(Uri uri) {
        String host=uri.getHost();
        if(!"https".equals(uri.getScheme())||host==null||uri.getUserInfo()!=null||uri.getPort()!=-1)return false;
        return host.equals("cctv.com")||host.endsWith(".cctv.com")||host.equals("dongqiudi.com")||host.endsWith(".dongqiudi.com")||host.equals("espn.com")||host.endsWith(".espn.com");
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        initialUrl=getIntent().getStringExtra("url");
        if(initialUrl==null||!allowed(Uri.parse(initialUrl))){finish();return;}
        boolean dark=getSharedPreferences("match-widget",MODE_PRIVATE).getBoolean("dark",true);
        int background=Color.parseColor(dark?"#152128":"#F4F6F8"),foreground=Color.parseColor(dark?"#D6E5E9":"#273D47");
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(background);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
        Button back=new Button(this);back.setText("‹ 返回");back.setTextColor(foreground);back.setBackgroundColor(Color.TRANSPARENT);back.setOnClickListener(v->goBack());bar.addView(back,new LinearLayout.LayoutParams(dp(86),dp(52)));
        title=new TextView(this);title.setText("原文阅读");title.setTextColor(foreground);title.setTextSize(15);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button close=new Button(this);close.setText("关闭");close.setTextColor(foreground);close.setBackgroundColor(Color.TRANSPARENT);close.setOnClickListener(v->finish());bar.addView(close,new LinearLayout.LayoutParams(dp(72),dp(52)));root.addView(bar);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        retry=new Button(this);retry.setText("加载失败，点此重试");retry.setVisibility(View.GONE);retry.setOnClickListener(v->web.reload());root.addView(retry,new LinearLayout.LayoutParams(-1,-2));
        web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        getWindow().setStatusBarColor(background);getWindow().setNavigationBarColor(background);
        root.setOnApplyWindowInsetsListener((view,insets)->{
            if(Build.VERSION.SDK_INT>=30){int bars=WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout(),handled=bars|WindowInsets.Type.ime();Insets safe=insets.getInsets(handled);view.setPadding(safe.left,safe.top,safe.right,safe.bottom);return new WindowInsets.Builder(insets).setInsets(handled,Insets.NONE).setInsetsIgnoringVisibility(bars,Insets.NONE).setDisplayCutout(null).build();}
            int l=insets.getSystemWindowInsetLeft(),t=insets.getSystemWindowInsetTop(),r=insets.getSystemWindowInsetRight(),b=insets.getSystemWindowInsetBottom();DisplayCutout cutout=insets.getDisplayCutout();if(cutout!=null){l=Math.max(l,cutout.getSafeInsetLeft());t=Math.max(t,cutout.getSafeInsetTop());r=Math.max(r,cutout.getSafeInsetRight());b=Math.max(b,cutout.getSafeInsetBottom());}view.setPadding(l,t,r,b);return insets.consumeSystemWindowInsets().consumeStableInsets().consumeDisplayCutout();
        });
        setContentView(root);root.requestApplyInsets();
        if(Build.VERSION.SDK_INT>=30){WindowInsetsController controller=getWindow().getInsetsController();if(controller!=null){int flags=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;controller.setSystemBarsAppearance(dark?0:flags,flags);}}
        else getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setGeolocationEnabled(false);settings.setMediaPlaybackRequiresUserGesture(true);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){if(!allowed(request.getUrl())){if(request.isForMainFrame())Toast.makeText(ArticleActivity.this,"此链接无法在阅读页中打开",Toast.LENGTH_SHORT).show();return true;}return false;}
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon){progress.setVisibility(View.VISIBLE);retry.setVisibility(View.GONE);}
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame()){retry.setVisibility(View.VISIBLE);progress.setVisibility(View.GONE);}}
            @Override public void onReceivedHttpError(WebView view,WebResourceRequest request,WebResourceResponse response){if(request.isForMainFrame()&&response.getStatusCode()>=400)retry.setVisibility(View.VISIBLE);}
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int value){progress.setProgress(value);progress.setVisibility(value>=100?View.GONE:View.VISIBLE);}
            @Override public void onReceivedTitle(WebView view,String value){title.setText(value==null||value.isEmpty()?"原文阅读":value);}
            @Override public void onPermissionRequest(PermissionRequest request){request.deny();}
        });
        web.setDownloadListener((url,userAgent,disposition,mime,length)->Toast.makeText(this,"该链接不是可阅读的网页",Toast.LENGTH_SHORT).show());
        if(state==null||web.restoreState(state)==null)web.loadUrl(initialUrl);
    }
    private void goBack(){if(web!=null&&web.canGoBack())web.goBack();else finish();}
    @Override public void onBackPressed(){goBack();}
    @Override protected void onSaveInstanceState(Bundle state){if(web!=null)web.saveState(state);super.onSaveInstanceState(state);}
    @Override protected void onPause(){if(web!=null)web.onPause();super.onPause();}
    @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();}
    @Override protected void onDestroy(){if(web!=null){web.stopLoading();web.destroy();web=null;}super.onDestroy();}
}
