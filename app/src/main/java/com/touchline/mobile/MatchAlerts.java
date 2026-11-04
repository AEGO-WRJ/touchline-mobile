package com.touchline.mobile;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Opt-in, system-scheduled checks. No always-running service or exact alarm. */
public final class MatchAlerts extends JobService {
    private static final int JOB=4101;
    private static final String CHANNEL="matches";
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private volatile Future<?> task;
    private volatile HttpURLConnection connection;
    private volatile boolean stopped;
    private static android.content.SharedPreferences store(Context c){return c.getSharedPreferences("match-alerts",MODE_PRIVATE);}
    private static JSONArray read(Context c){try{return new JSONArray(store(c).getString("matches","[]"));}catch(Exception e){return new JSONArray();}}
    static boolean permitted(Context c){return c.getSystemService(NotificationManager.class).areNotificationsEnabled()&&(Build.VERSION.SDK_INT<33||c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED);}
    static synchronized void configure(Context c,JSONObject value) throws JSONException {
        JSONArray incoming=value.optJSONArray("matches"),old=read(c),next=new JSONArray();
        if(incoming!=null)for(int i=0;i<Math.min(incoming.length(),100);i++){
            JSONObject m=incoming.optJSONObject(i);if(m==null)continue;
            String id=m.optString("id"),league=m.optString("league");long time=m.optLong("time");
            if(!id.matches("[0-9]{1,12}")||!league.matches("[a-z0-9._]{1,40}")||time<System.currentTimeMillis()-86400000L)continue;
            JSONObject item=new JSONObject().put("id",id).put("league",league).put("time",time).put("title",m.optString("title").substring(0,Math.min(180,m.optString("title").length())));
            for(int j=0;j<old.length();j++){JSONObject o=old.getJSONObject(j);if(id.equals(o.optString("id"))&&league.equals(o.optString("league"))){for(String k:new String[]{"state","score","reminded","done"})if(o.has(k))item.put(k,o.get(k));break;}}
            next.put(item);
        }
        store(c).edit().putString("matches",next.toString()).putBoolean("enabled",value.optBoolean("enabled")).commit();
        JobScheduler jobs=c.getSystemService(JobScheduler.class);
        if(!value.optBoolean("enabled")||next.length()==0){jobs.cancel(JOB);return;}
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,"订阅比赛",NotificationManager.IMPORTANCE_DEFAULT));
        if(jobs.getPendingJob(JOB)==null)jobs.schedule(new JobInfo.Builder(JOB,new ComponentName(c,MatchAlerts.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60*1000L).setPersisted(true).build());
    }
    @Override public boolean onStartJob(JobParameters params){stopped=false;task=executor.submit(()->{try{check();}finally{if(!stopped&&!Thread.currentThread().isInterrupted())jobFinished(params,false);}});return true;}
    @Override public boolean onStopJob(JobParameters params){stopped=true;if(connection!=null)connection.disconnect();if(task!=null)task.cancel(true);return true;}
    @Override public void onDestroy(){stopped=true;if(connection!=null)connection.disconnect();executor.shutdownNow();super.onDestroy();}
    private void check(){
        JSONArray list=read(this);long now=System.currentTimeMillis();
        for(int i=0;i<list.length()&&!stopped&&!Thread.currentThread().isInterrupted();i++)try{
            JSONObject m=list.getJSONObject(i);long kickoff=m.optLong("time");
            if(m.optBoolean("done")||kickoff>now+30*60*1000L||kickoff<now-86400000L)continue;
            if(!store(this).getBoolean("enabled",false)||!permitted(this))return;
            JSONObject root=fetch("https://site.api.espn.com/apis/site/v2/sports/soccer/"+m.getString("league")+"/summary?event="+m.getString("id"));
            JSONObject header=root.getJSONObject("header");if(!m.getString("id").equals(header.optString("id")))continue;
            JSONObject game=header.getJSONArray("competitions").getJSONObject(0),status=game.getJSONObject("status"),type=status.getJSONObject("type");String state=type.optString("state"),statusName=type.optString("name");
            if(!state.matches("pre|in|post"))continue;
            JSONArray sides=game.getJSONArray("competitors");JSONObject h=null,a=null;for(int j=0;j<sides.length();j++){JSONObject t=sides.getJSONObject(j);if("home".equals(t.optString("homeAway")))h=t;else if("away".equals(t.optString("homeAway")))a=t;}if(h==null||a==null)continue;
            String score=score(h)+" : "+score(a),message=null;
            String date=game.optString("date");try{kickoff=java.time.Instant.parse(date).toEpochMilli();}catch(Exception ignored){}
            boolean exceptional=statusName.matches(".*(POSTPONED|CANCELED|SUSPENDED|ABANDONED).*"),remind=false;
            if(!exceptional&&"pre".equals(state)&&!m.optBoolean("reminded")&&kickoff>=now&&kickoff<=now+1800000L){message="即将开赛（30 分钟内），请查看最新赛程";remind=true;}
            if(!exceptional&&"in".equals(state)&&(!state.equals(m.optString("state"))||!score.equals(m.optString("score"))))message=("in".equals(m.optString("state"))?"比分更新：":"比赛进行中：")+score;
            if(!exceptional&&"post".equals(state)&&!"post".equals(m.optString("state")))message="比赛结束："+score;
            update(m,state,score,kickoff,message,remind,"post".equals(state)&&!exceptional);
        }catch(Exception ignored){/* Keep previous snapshot and retry on the next scheduled run. */}
        prune();
    }
    private static String score(JSONObject t){Object s=t.opt("score");if(s instanceof JSONObject)return ((JSONObject)s).optString("displayValue",((JSONObject)s).optString("value","—"));return s==null?"—":String.valueOf(s);}
    private JSONObject fetch(String address) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();connection=c;
        try{c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","TouchlineMobile/1.3");if(c.getResponseCode()!=200)throw new IOException();ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(stopped||Thread.currentThread().isInterrupted()||out.size()+n>8000000)throw new IOException();out.write(buffer,0,n);}}return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));}finally{c.disconnect();connection=null;}
    }
    private void update(JSONObject match,String state,String score,long time,String message,boolean reminded,boolean done) throws JSONException {
        synchronized(MatchAlerts.class){if(stopped||Thread.currentThread().isInterrupted()||!store(this).getBoolean("enabled",false))return;JSONArray all=read(this);for(int i=0;i<all.length();i++){JSONObject m=all.getJSONObject(i);if(!m.optString("id").equals(match.optString("id"))||!m.optString("league").equals(match.optString("league")))continue;
            if(message!=null&&!permitted(this))return;
            if(message!=null){Intent intent=new Intent(this,MainActivity.class).putExtra("matchId",m.getString("id")).putExtra("league",m.getString("league")).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);int code=(m.optString("league")+m.optString("id")).hashCode();PendingIntent open=PendingIntent.getActivity(this,code,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification note=new Notification.Builder(this,CHANNEL).setSmallIcon(com.touchline.mobile.R.drawable.notification_ball).setContentTitle(m.optString("title","订阅比赛")).setContentText(message).setStyle(new Notification.BigTextStyle().bigText(message+"\n定期检查结果，可能有延迟")).setContentIntent(open).setAutoCancel(true).build();getSystemService(NotificationManager.class).notify(code,note);}
            m.put("state",state).put("score",score).put("time",time).put("done",done);if(reminded)m.put("reminded",true);store(this).edit().putString("matches",all.toString()).commit();break;
        }}
    }
    private void prune(){synchronized(MatchAlerts.class){JSONArray all=read(this),keep=new JSONArray();boolean work=false;for(int i=0;i<all.length();i++){JSONObject m=all.optJSONObject(i);if(m!=null&&m.optLong("time")>System.currentTimeMillis()-86400000L){keep.put(m);work|=!m.optBoolean("done");}}store(this).edit().putString("matches",keep.toString()).commit();if(!work)getSystemService(JobScheduler.class).cancel(JOB);}}
}
