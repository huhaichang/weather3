package com.example.huhaichang.weather3;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.huhaichang.weather3.db.City;
import com.example.huhaichang.weather3.db.County;
import com.example.huhaichang.weather3.db.Province;
import com.example.huhaichang.weather3.widget.OkhttpUtil;
import com.example.huhaichang.weather3.widget.ToastUtil;
import com.example.huhaichang.weather3.widget.Utility;

import org.litepal.LitePal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by huhaichang on 2019/8/16.
 */

public class ChooseAreaFragment extends android.support.v4.app.Fragment {
    private TextView mTVTitle;
    private Button mBtBack;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    //服务器查询数据需要时间
    private ProgressDialog progressDialog;
    //查询数据用
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    //设置选择的级别
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private int currentLevel;//当前级别
    //选中的类型
    private Province selectedProvince; //需要省和市的id
    private City selectedCity;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        mTVTitle = view.findViewById(R.id.tv_title);
        mBtBack = view.findViewById(R.id.bt_back);
        listView = view.findViewById(R.id.list_view);
        if(getContext()!=null){
        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        }
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //设置listView的点击 和返回按钮的点击
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //根据等级判断在哪里（省市县）
                if(currentLevel==LEVEL_PROVINCE){
                    //选中的省绑定数据库
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if(currentLevel==LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if(currentLevel==LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();
                    Intent intent = new Intent(getActivity(),WeatherActivity.class);
                    intent.putExtra("weather_id",weatherId);
                    startActivity(intent);
                    getActivity().finish();
                }

            }
        });
        mBtBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTY){
                    queryCities();
                }
                else if(currentLevel==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    //查询省数据放如dataList
    private void queryProvinces(){
        mTVTitle.setText("中国");
        mBtBack.setVisibility(View.GONE);
        //先从数据库查询没用 就去服务器请求后在去数据库查询
        provinceList = LitePal.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province: provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0); //第0个位置开始按顺序放
            currentLevel = LEVEL_PROVINCE; //当前选中省列表 根据级别可以
        }else{
            String address = "http://guolin.tech/api/china";
            //去服务器查询
            queryFromServer(address,"province");
        }
    }

    //查询市数据放如dataList
    private void queryCities(){
        mTVTitle.setText(selectedProvince.getProvinceName());
        mBtBack.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceId = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for (City city :cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else {
            //地址多了个id
            String address = "http://guolin.tech/api/china/"+selectedProvince.getProvinceCode();
            queryFromServer(address,"city");
        }

    }

    //查询县数据放如dataList
    private void queryCounties(){
        mTVTitle.setText(selectedCity.getCityName());
        mBtBack.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityId = ?",String.valueOf(selectedCity.getId())).find(County.class);
       // if ( cityList.size() > 0)  这边错了找半天
        if(countyList.size()>0){
            dataList.clear();
            for (County county :countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else {
            //地址多了个id
            String address = "http://guolin.tech/api/china/"+selectedProvince.getProvinceCode()+"/"+selectedCity.getCityCode();
            queryFromServer(address,"county");
        }
    }

    //根据地区area判断查询哪里的数据
    private  void queryFromServer(String address,final String area){
        //查询数据需要时间弄个进度框来提示
        showProgressDialog();
        OkhttpUtil.sendHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                boolean result =false;
                //根据地区看请求哪里的数据
                if("province".equals(area)){
                    //调用方法放到数据库 如果正常result=true
                    result = Utility.handleProvinceResponse(responseData);
                }
                if("city".equals(area)){
                    //调用方法放到数据库 如果正常result=true
                    result = Utility.handleCityResponse(responseData,selectedProvince.getId());
                }
                if("county".equals(area)){
                    //调用方法放到数据库 如果正常result=true
                    result = Utility.handleCountyResponse(responseData,selectedCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(area)){
                                queryProvinces();
                            }
                            if("city".equals(area)){
                                queryCities();

                            }
                            if("county".equals(area)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
            //请求失败
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        ToastUtil.showMsg(getContext(),"加载失败");
                    }
                });
            }

        });
    }

    private void showProgressDialog(){
        if(progressDialog==null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    private void closeProgressDialog(){
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }
}
