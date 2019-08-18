package com.example.huhaichang.weather3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.huhaichang.weather3.gson.Forecast;
import com.example.huhaichang.weather3.gson.Weather;
import com.example.huhaichang.weather3.widget.OkhttpUtil;
import com.example.huhaichang.weather3.widget.ToastUtil;
import com.example.huhaichang.weather3.widget.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

//先去服务器返回一个json数据在用sharePreference存储   然后解析
public class WeatherActivity extends AppCompatActivity {
    private ScrollView mSl;
    private TextView mTVCityName;
    private TextView mTVUpdateTime;
    private TextView mTVNowTemperature;
    private TextView mTVWeatherInfo;
    private LinearLayout mLLforecast;
    private TextView mTVAQI;
    private TextView mTVPM25;
    private TextView mTVComfort;
    private TextView mTVCarWash;
    private TextView mTVSport;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private ImageView mIVBackground;
    private Button mBtGoCity;
    //下拉刷新 也得获取citiId
    public SwipeRefreshLayout swipeRefreshLayout;
    private String mWeatherId;
    //滑动菜单
    public DrawerLayout drawerLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //系统标题背景覆盖掉(android:以上)
        if(Build.VERSION.SDK_INT>=21){
        View decotView = getWindow().getDecorView();
        decotView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        mSl = findViewById(R.id.sl_1);
        mTVCityName = findViewById(R.id.tv_cityName);
        mTVUpdateTime = findViewById(R.id.tv_updateTime);
        mTVNowTemperature = findViewById(R.id.tv_nowTemperature);
        mTVWeatherInfo = findViewById(R.id.tv_weather_info);
        mLLforecast = findViewById(R.id.ll_forecast);
        mTVAQI = findViewById(R.id.tv_aqi);
        mTVPM25 = findViewById(R.id.tv_pm25);
        mTVComfort = findViewById(R.id.tv_comfort);
        mTVCarWash = findViewById(R.id.tv_car_wash);
        mTVSport = findViewById(R.id.tv_sport);
        mIVBackground = findViewById(R.id.iv_background);
        mBtGoCity = findViewById(R.id.bt_goCity);
        drawerLayout = findViewById(R.id.dl_1);
        swipeRefreshLayout = findViewById(R.id.SRL_1);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        //采用SharedPreference(读取数据)（存储的数据没有解析是源网站数据）
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = mSharedPreferences.getString("bing_pic",null);
        if(bingPic!=null){
            Glide.with(WeatherActivity.this).load(bingPic).into(mIVBackground);
        }else{
            loadBingPic();
        }
        String weatherString = mSharedPreferences.getString("weather",null);
        //判断是否存储没用就去服务器请求
        if(weatherString!=null){
            //如果之前存储了就把读取的数据拿去解析
            Weather weather = Utility.handleWeatherResponse(weatherString);
            //解析完后的对象就去ui设置了
            mWeatherId = weather.basic.weatherId;
              showWeatherInfo(weather);

        }else{
            mWeatherId = getIntent().getStringExtra("weather_id");
            //服务器返回的数据需要weather_id(随便解析)
            String weatherId = getIntent().getStringExtra("weather_id");  //通过点击事件传值
            requestWeather(weatherId);
        }
        //按钮点击事件开启滑动菜单
        mBtGoCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        //下拉刷新事件
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //刷新天气 （就是重新请求服务器）
                requestWeather(mWeatherId);
            }
        });
    }

    public void requestWeather(final String weatherId){
        //S6版本String url ="https://free-api.heweather.net/s6/weather/now?location="+weatherId+"&key=8f2fff01e60947b6a42c7c8f994761bd";
        //s5测试专用还可以用https://free-api.heweather.com/v5/weather?city=CN101010100&key=32d1c829ed7d483086f4f5b4d5947cef
    String url ="https://free-api.heweather.com/v5/weather?city="+weatherId+"&key=32d1c829ed7d483086f4f5b4d5947cef";
      //  String url = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=9aabfc1b70bb4c1288b4bc1f00e13f1a";
        OkhttpUtil.sendHttpRequest(url, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
           //可能不存在不能放外面     mWeatherId = weather.basic.weatherId;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //请求解析成功的话
                        if (weather !=null && "ok".equals(weather.status)){
                        //把未解析的放入数据库里
                        mEditor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                        mEditor.putString("weather",responseText);
                        mEditor.apply();
                        //设置ui
                        showWeatherInfo(weather);
                            mWeatherId = weather.basic.weatherId;
                        }else {
                            ToastUtil.showMsg(WeatherActivity.this,"获取天气信息失败1");
                        }
                        //请求完了关闭
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
                loadBingPic();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.showMsg(WeatherActivity.this,"获取天气信息失败");
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

        });
    }
    //请求返回图片地址
    private void loadBingPic(){
        String photoUrl ="http://guolin.tech/api/bing_pic";
        OkhttpUtil.sendHttpRequest(photoUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                    final String photoAdress = response.body().string();
                    //存入sharedperference数据
                mEditor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                mEditor.putString("bing_pic",photoAdress);
                mEditor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(photoAdress).into(mIVBackground);
                    }
                });
            }
        });
    }

    private void showWeatherInfo(Weather weather){
        //设置title
        mTVCityName.setText(weather.basic.cityName);
        mTVUpdateTime.setText(weather.basic.update.updateTime.split(" ")[1]);//不要年月日 就弄时分
        //今日状态
        mTVNowTemperature.setText(weather.now.temperature+"°C");
        mTVWeatherInfo.setText(weather.now.more.info);
        //设置预报
        mLLforecast.removeAllViews();
        //列表里的每存在一个对象就把对象的值弄到forecast_item里在添加到LinerLayout里面去
        for(Forecast forecast:weather.forecastList){
            View view = LayoutInflater.from(WeatherActivity.this).inflate(R.layout.forecast_item,mLLforecast,false);
            TextView dateText = view.findViewById(R.id.tv_date);
            TextView infoText = view.findViewById(R.id.tv_info);
            TextView maxText = view.findViewById(R.id.tv_max);
            TextView minText = view.findViewById(R.id.tv_min);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            maxText.setText(forecast.temperature.min);
            mLLforecast.addView(view);//添加到LinerLayout里面去
        }
        //设置sqi指数
        if(weather.aqi!=null){
            mTVAQI.setText(weather.aqi.city.aqi);
            mTVPM25.setText(weather.aqi.city.pm25);
        }
        //建议
        String comfort = "舒适度: " + weather.suggestion.comfort.info;
        String carWash = "洗车指数: " + weather.suggestion.carWash.info;
        String sport = "运动建议: " + weather.suggestion.sport.info;
       mTVComfort.setText(comfort);
        mTVCarWash.setText(carWash);
        mTVSport.setText(sport);
    }
}
