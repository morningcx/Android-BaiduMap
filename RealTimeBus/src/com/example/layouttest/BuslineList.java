package com.example.layouttest;

import android.app.Fragment;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class BuslineList extends Fragment{
	TextView busTitle;//公交车名称
	TextView busInfo;//首末时间，票价信息
	TextView busLineNote;//提示信息
	ListView busStationList;//车站列表
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view=inflater.inflate(R.layout.buslinelist, container, false);
		busTitle=(TextView) view.findViewById(R.id.bustitle);
		busInfo=(TextView) view.findViewById(R.id.businfo);
		busStationList=(ListView) view.findViewById(R.id.buslist);
		busLineNote=(TextView) view.findViewById(R.id.buslinenote);
		return view;
	}

}
