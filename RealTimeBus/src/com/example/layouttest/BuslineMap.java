package com.example.layouttest;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapView;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class BuslineMap extends Fragment{
	TextView currentRS;
	Button showBus;
	MapView mMapView=null;
	BaiduMap mBaiduMap = null;
	public Button mBtnPre = null; // 上一个节点
    Button mBtnNext = null; // 下一个节点
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view=inflater.inflate(R.layout.buslinemap, container, false);
		mBtnPre = (Button)view.findViewById(R.id.pre);
		mBtnNext=(Button)view.findViewById(R.id.next);
		mMapView = (MapView) view.findViewById(R.id.bmapView);
		showBus=(Button) view.findViewById(R.id.showmovingbt);
		currentRS=(TextView) view.findViewById(R.id.current_remind_station);
		return view;
	}

}
