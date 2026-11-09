package com.touchline.mobile;

import android.app.*;
import android.app.job.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;
import org.json.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/** Shared primary-team widget. Network work runs in a cancellable JobService. */
public final class MatchWidget extends AppWidgetProvider {
    private static final int JOB=4201;
    private static final String REFRESH="com.touchline.mobile.WIDGET_REFRESH";
    private static volatile JSONObject catalog;
    static android.content.SharedPreferences store(Context c){return c.getSharedPreferences("match-widget",Context.MODE_PRIVATE);}
    static JSONObject catalog(Context c) throws Exception {if(catalog==null)try(InputStream in=c.getAssets().open("catalog.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);catalog=new JSONObject(out.toString("UTF-8"));}return catalog;}
    static JSONObject team(Context c,String id){try{JSONArray all=catalog(c).getJSONArray("teams");for(int i=0;i<all.length();i++)if(id.equals(all.getJSONObject(i).optString("Id")))return all.getJSONObject(i);}catch(Exception ignored){}return null;}
    static String name(Context c,String value){try{return catalog(c).getJSONObject("names").optString(value,value);}catch(Exception e){return value;}}
    static int[] ids(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,MatchWidget.class));}
    static synchronized void configure(Context c,JSONObject value){String id=value.optString("id");if(!id.isEmpty()&&team(c,id)==null)return;boolean changed=!id.equals(store(c).getString("team","")),dark=value.optBoolean("dark",true);if(!changed&&dark==store(c).getBoolean("dark",true))return;
        android.content.SharedPreferences.Editor edit=store(c).edit().putString("team",id).putBoolean("dark",dark);if(changed){edit.remove("games").remove("updated").remove("error");c.getSystemService(JobScheduler.class).cancel(JOB);}edit.apply();render(c);if(changed)request(c);
    }
    static void request(Context c){if(ids(c).length==0||team(c,store(c).getString("team",""))==null)return;JobScheduler jobs=c.getSystemService(JobScheduler.class);if(jobs.getPendingJob(JOB)==null)jobs.schedule(new JobInfo.Builder(JOB,new ComponentName(c,RefreshJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setOverrideDeadline(0).build());}
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){render(c);request(c);}
    @Override public void onReceive(Context c,Intent intent){super.onReceive(c,intent);if(REFRESH.equals(intent.getAction())){render(c);request(c);}}
    @Override public void onDisabled(Context c){c.getSystemService(JobScheduler.class).cancel(JOB);}
    static String leagueName(String id){switch(id){case "eng.1":return "英超";case "esp.1":return "西甲";case "ita.1":return "意甲";case "ger.1":return "德甲";case "fra.1":return "法甲";case "uefa.champions":return "欧冠";case "usa.1":return "美职联";case "chn.1":return "中超";case "eng.fa":return "足总杯";case "eng.league_cup":return "英联赛杯";case "ger.dfb_pokal":return "德国杯";case "esp.copa_del_rey":return "国王杯";default:return "主队比赛";}}
    static String date(long time){return DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai")).format(Instant.ofEpochMilli(time));}
    static String score(JSONObject side){Object v=side.opt("score");return v instanceof JSONObject?((JSONObject)v).optString("displayValue",((JSONObject)v).optString("value","—")):v==null?"—":v.toString();}
    static JSONObject side(JSONObject game,String side){JSONArray list=game.optJSONArray("competitors");if(list!=null)for(int i=0;i<list.length();i++)if(side.equals(list.optJSONObject(i).optString("homeAway")))return list.optJSONObject(i);return null;}
    static long time(String value){try{return OffsetDateTime.parse(value).toInstant().toEpochMilli();}catch(Exception e){return 0;}}
    static String state(JSONObject event){return event.optString("state");}
    static List<JSONObject> games(Context c){List<JSONObject> list=new ArrayList<>();try{JSONArray saved=new JSONArray(store(c).getString("games","[]"));for(int i=0;i<saved.length();i++)list.add(saved.getJSONObject(i));}catch(Exception ignored){}list.sort(Comparator.comparingLong(m->m.optLong("time")));return list;}
    static JSONObject choose(List<JSONObject> list,long now){for(JSONObject m:list)if("in".equals(state(m))&&m.optLong("time")>now-86400000L)return m;JSONObject recent=null;for(JSONObject m:list)if("post".equals(state(m))&&m.optLong("time")>now-2*86400000L)recent=m;for(JSONObject m:list)if("pre".equals(state(m))&&m.optLong("time")>=now-3600000L&&m.optLong("time")<now+6*3600000L)return m;if(recent!=null)return recent;for(JSONObject m:list)if("pre".equals(state(m))&&m.optLong("time")>=now-3600000L)return m;return null;}
    static void logo(Context c,RemoteViews v,int view,String id){int res=id.matches("[0-9]+")?c.getResources().getIdentifier("team_"+id,"drawable",c.getPackageName()):0;v.setImageViewResource(view,res==0?R.mipmap.launcher:res);}
    static void render(Context c){int[] widgets=ids(c);if(widgets.length==0)return;android.content.SharedPreferences p=store(c);JSONObject t=team(c,p.getString("team",""));boolean dark=p.getBoolean("dark",true);RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.match_widget);
        v.setInt(R.id.widget_root,"setBackgroundResource",dark?R.drawable.widget_dark:R.drawable.widget_light);for(int id:new int[]{R.id.widget_title,R.id.widget_home,R.id.widget_away,R.id.widget_next,R.id.widget_status,R.id.widget_updated,R.id.widget_refresh})v.setTextColor(id,Color.parseColor(dark?"#D6E5E9":"#273D47"));v.setTextColor(R.id.widget_score,Color.parseColor(dark?"#75EFBD":"#08734C"));
        v.setTextViewText(R.id.widget_title,t==null?"看球 · 主队比赛":name(c,t.optString("Name")));logo(c,v,R.id.widget_badge,t==null?"":t.optString("Id"));
        long now=System.currentTimeMillis(),updated=p.getLong("updated",0);List<JSONObject> all=games(c);JSONObject match=choose(all,now);if(updated<now-86400000L)match=null;
        if(match!=null&&t!=null)v.setTextViewText(R.id.widget_title,name(c,t.optString("Name"))+" · "+match.optString("leagueName"));
        String hint=t==null?"打开 App，在关注页添加主队":"暂无近期赛程 · 点击刷新";v.setTextViewText(R.id.widget_home,match==null?"":match.optString("homeName"));v.setTextViewText(R.id.widget_away,match==null?"":match.optString("awayName"));
        logo(c,v,R.id.widget_home_logo,match==null?"":match.optString("home"));logo(c,v,R.id.widget_away_logo,match==null?"":match.optString("away"));v.setViewVisibility(R.id.widget_home_logo,match==null?View.INVISIBLE:View.VISIBLE);v.setViewVisibility(R.id.widget_away_logo,match==null?View.INVISIBLE:View.VISIBLE);
        v.setTextViewText(R.id.widget_score,match==null?"看球":"pre".equals(state(match))?"VS":match.optString("score"));v.setTextViewText(R.id.widget_status,match==null?hint:match.optString("status"));
        String next="点击查看比赛详情";if(match!=null){next=match.optString("leagueName")+" · "+date(match.optLong("time"));for(JSONObject m:all)if(!m.optString("id").equals(match.optString("id"))&&"pre".equals(state(m))&&m.optLong("time")>Math.max(now,match.optLong("time"))){next="下场 "+date(m.optLong("time"))+" · "+m.optString("homeName")+" vs "+m.optString("awayName");break;}}
        v.setTextViewText(R.id.widget_next,t==null?"先打开 App，在关注页添加主队":next);v.setTextViewText(R.id.widget_updated,updated==0?"等待更新 · 北京时间":(p.getBoolean("error",false)?"更新失败 · ":"")+"数据 "+date(updated)+" · 定期更新");
        Intent open=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);if(match!=null)open.putExtra("matchId",match.optString("id")).putExtra("league",match.optString("league"));v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,4201,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        Intent refresh=new Intent(c,MatchWidget.class).setAction(REFRESH);v.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(c,4202,refresh,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));AppWidgetManager.getInstance(c).updateAppWidget(widgets,v);
    }
    public static final class RefreshJob extends JobService {
        private final ExecutorService executor=Executors.newSingleThreadExecutor();private Future<?> task;private volatile boolean stopped;private volatile HttpURLConnection connection;
        @Override public boolean onStartJob(JobParameters p){stopped=false;task=executor.submit(()->{try{load();}finally{if(!stopped)jobFinished(p,false);}});return true;}
        @Override public boolean onStopJob(JobParameters p){stopped=true;if(connection!=null)connection.disconnect();if(task!=null)task.cancel(true);return false;}
        @Override public void onDestroy(){stopped=true;if(connection!=null)connection.disconnect();executor.shutdownNow();super.onDestroy();}
        JSONObject fetch(String url) throws Exception {HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();connection=c;try{c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setInstanceFollowRedirects(false);if(c.getResponseCode()!=200)throw new IOException();ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(stopped||Thread.currentThread().isInterrupted()||out.size()+n>8000000)throw new IOException();out.write(b,0,n);}}return new JSONObject(out.toString("UTF-8"));}finally{c.disconnect();connection=null;}}
        void load(){String id=store(this).getString("team","");JSONObject t=team(this,id);if(t==null||ids(this).length==0)return;try{Map<String,JSONObject> matches=new LinkedHashMap<>();for(boolean future:new boolean[]{false,true}){JSONObject data=fetch("https://site.api.espn.com/apis/site/v2/sports/soccer/"+t.getString("League")+"/teams/"+id+"/schedule?fixture="+future);if(!id.equals(data.getJSONObject("team").optString("id")))throw new IOException();JSONArray events=data.getJSONArray("events");for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i),game=e.getJSONArray("competitions").getJSONObject(0),h=side(game,"home"),a=side(game,"away");if(h==null||a==null||(!id.equals(h.optString("id"))&&!id.equals(a.optString("id"))))continue;JSONObject status=game.optJSONObject("status");if(status==null)status=e.optJSONObject("status");if(status==null)continue;JSONObject type=status.getJSONObject("type");long kickoff=time(e.optString("date"));if(kickoff<System.currentTimeMillis()-3*86400000L||kickoff>System.currentTimeMillis()+60*86400000L)continue;
                    String league=e.optJSONObject("league")==null?t.getString("League"):e.getJSONObject("league").optString("slug",t.getString("League")),state=type.optString("state"),label="pre".equals(state)?date(kickoff):"post".equals(state)?"已结束":status.optString("displayClock","进行中");String kind=type.optString("name");if(kind.contains("POSTPONED"))label="延期";if(kind.contains("CANCELED"))label="取消";if(kind.contains("HALFTIME"))label="中场";if(leagueName(league).equals("主队比赛")||!e.optString("id").matches("[0-9]{1,12}"))continue;
                    matches.put(e.getString("id"),new JSONObject().put("id",e.getString("id")).put("league",league).put("leagueName",leagueName(league)).put("time",kickoff).put("state",state).put("status",label).put("home",h.optString("id")).put("away",a.optString("id")).put("homeName",name(this,h.getJSONObject("team").optString("displayName"))).put("awayName",name(this,a.getJSONObject("team").optString("displayName"))).put("score",score(h)+" : "+score(a)));
                }}synchronized(MatchWidget.class){if(stopped||!id.equals(store(this).getString("team",""))||ids(this).length==0)return;store(this).edit().putString("games",new JSONArray(matches.values()).toString()).putLong("updated",System.currentTimeMillis()).putBoolean("error",false).apply();render(this);}
            }catch(Exception e){synchronized(MatchWidget.class){if(!stopped&&id.equals(store(this).getString("team",""))){store(this).edit().putBoolean("error",true).apply();render(this);}}}
        }
    }
}
