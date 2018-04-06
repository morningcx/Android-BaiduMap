package com.example.poisearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.baidu.mapapi.search.busline.BusLineResult;
import com.baidu.mapapi.search.busline.BusLineSearch;
import com.baidu.mapapi.search.busline.BusLineSearchOption;
import com.baidu.mapapi.search.busline.OnGetBusLineSearchResultListener;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.TransitRouteLine.TransitStep;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 搜索公交类型路线的详细信息，获取公交路线view
 * <p>有时候其中一个会搜不出结果，导致详细车站、票价和总票价显示不出来，原因不明(可能和网络有关)</p>
 *
 */
public class BusRouteDetail implements OnGetPoiSearchResultListener,OnGetBusLineSearchResultListener{
    /**
     * 路线总票价
     */
	public static float totalBusPrice=0;
	/**
	 * 展开文本
	 */
	private final static String show="▼  "; 
	/**
	 * 收起文本
	 */
	private final static String hide="▲  "; 
	/**
	 * 记录搜索到的公交信息数量
	 */
	public static int stepNum=0;
	/**
	 * Poi搜索模块，也可去掉地图模块独立使用
	 */
    private PoiSearch mSearch = null; 
    /**
     * 公交信息搜索模块，也可去掉地图模块独立使用
     */
    private BusLineSearch mBusLineSearch = null;
    /**
     * poi结果list
     */
    private List<String> busLineIDList = null;
    /**
     * 途径车站list
     */
    private List<String> busStationList = null;
    /**
     * 起始车站，终点车站，车名
     */
    private String start;
    /**
     * 终点车站
     */
    private String end;
    /**
     * 车名
     */
    private String bus;	
    /**
     * 显示途径车站按钮
     */
    private Button bt;
    /**
     * 显示途径车站按钮的信息
     */
    private String stationBtInfo;
    /**
     * 途径车站信息textview
     */
    private TextView station;
    /**
     * 整个视图
     */
    private View view;
    /**
     * 途径公交车站数
     */
    private int stationNum=0;
    Context context;
	/**
	 * 构造函数
	 * <p>
	 * 构造view对象
	 * </p>
	 * @param context
	 * @param step
	 */
	public BusRouteDetail(Context context,TransitStep step) {
		this.context=context;
		view=View.inflate(context, R.layout.busroute_list, null);
		stationNum=step.getVehicleInfo().getPassStationNum();
		bus=step.getVehicleInfo().getTitle();
		start=step.getEntrance().getTitle();
		end=step.getExit().getTitle();
		setInfo();
		initSearch();
	}
	
	/**
	 * 公交搜索设置
	 */
	private void initSearch() {
		mSearch = PoiSearch.newInstance();//创建检索实例
		mSearch.setOnGetPoiSearchResultListener(this);//添加监听器
		mBusLineSearch = BusLineSearch.newInstance();//创建 公交检索实例
		mBusLineSearch.setOnGetBusLineSearchResultListener(this);
		busLineIDList = new ArrayList<String>();//创建一个搜索表 
		busStationList = new LinkedList<String>();//车站表
		mSearch.searchInCity((new PoiCitySearchOption()).city(MainActivity.inputCity).keyword(bus));
	}
	
	/**
	 * 设置view控件信息
	 */
	public void setInfo(){
		TextView startStation=(TextView) view.findViewById(R.id.start_station);
		TextView title=(TextView) view.findViewById(R.id.via_bus_title);
		TextView endStation=(TextView) view.findViewById(R.id.end_station);
		bt=(Button) view.findViewById(R.id.via_busstation_bt);
		station=(TextView) view.findViewById(R.id.via_busstation_info);
		station.setVisibility(View.GONE);
		//设置起点、终点、公交车名
		startStation.setText(start);
		endStation.setText(end);
		title.setText(bus);
		//设置按钮监听器控制途径车站的展开和隐藏
		bt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
					if (station.getVisibility()==View.GONE) {
						station.setVisibility(View.VISIBLE);
						bt.setText(stationBtInfo+hide);
					}
					else{
						station.setVisibility(View.GONE);
						bt.setText(stationBtInfo+show);
					}
			}
		});
	}
	
	@Override
	public void onGetPoiResult(PoiResult result) {
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			//发生未知错误1，2经检测在有时候搜不出结果的情况下也不会弹出（也不知道哪里出错了）
			Toast.makeText(context, "发生未知错误:1", Toast.LENGTH_SHORT).show();
            return;
        }
		// 遍历所有poi，找到类型为公交线路的poi
        for (PoiInfo poi : result.getAllPoi()) 
        	//如果遍历的type为bus或者subway，则添加list
            if (poi.type == PoiInfo.POITYPE.BUS_LINE|| poi.type == PoiInfo.POITYPE.SUBWAY_LINE) 
                busLineIDList.add(poi.uid);
        if (busLineIDList.size()==0) {
        	Toast.makeText(context, "发生未知错误:2", Toast.LENGTH_SHORT).show();
            return;
		}
		//查找busline
		BusLineSearchOption busOption=(new BusLineSearchOption()
                .city(MainActivity.inputCity).uid(busLineIDList.get(0)));
        mBusLineSearch.searchBusLine(busOption);
	}
	/**
	 * 这个方法只是投机取巧，当搜索到的第一条公交路线方向与路线方向不一致时，搜索到并不是真正的
	 * 车站顺序，当公交车站是环线或者是上下行路线不对称，车站没有搜索到的情况下，会发生异常退出
	 * 比如     嘉兴：嘉善k215路
	 */
	@Override
	public void onGetBusLineResult(BusLineResult result) { //搜不到的情况下不执行
		boolean reverse=false;
		//判断始终车站是否被找到
		int startNode=0; 
		int endNode=0;
		for(int i=0;i<result.getStations().size();i++){
			if (comPare(start, result.getStations().get(i).getTitle())) {
				startNode=1;
				if (endNode==1) //终点站先被找到则需要反转
					reverse=true;
			}
			else if (comPare(end, result.getStations().get(i).getTitle()))
				endNode=1;
			//起始站或者终点站其中一个被查找到就添加list
			if (startNode+endNode==1) {
				busStationList.add(result.getStations().get(i).getTitle());
			}
			else if (startNode+endNode==2) {
				//添加最后一站
				busStationList.add(result.getStations().get(i).getTitle());
				if (reverse) 
					Collections.reverse(busStationList);//反转
				break;
			}
		}
		//防止车站名称不对称出现异常退出，并且下拉列表为空
		if (busStationList.size()!=0) {
		    //获取途径车站信息
		    StringBuffer s=new StringBuffer();
		    //第一站添加上车显示
		    s.append(busStationList.get(0)).append("(上车)\n");
		    //第二站开始到倒数第二站正常循环添加
		    for(int i=1;i<busStationList.size()-1;i++)
		         s.append(busStationList.get(i)).append("\n");
		    //最后一站添加下车显示
		    s.append(busStationList.get(busStationList.size()-1)).append("(下车)");
		    station.setText(s.toString());
		}
		//添加按钮信息
		stationBtInfo=result.getBasePrice()+"元  "+//路线票价
		        stationNum+"站  ";//路段站数
		bt.setText(stationBtInfo+show);
		//添加总票价信息
		totalBusPrice+=result.getBasePrice(); 
		//每进行总票价的一次运算，stepNum加1，当其值等于路线中公交路段的数量时，显示总票价
		stepNum++;
		if (stepNum==StepInfo.busStepNum) {
			MainActivity.planTitle2.append(" • "+totalBusPrice+"元");
			stepNum=0;
		}
		//用完搜索destroy()
		mSearch.destroy();
		mBusLineSearch.destroy();
	}
	
	/**
	 * 判断s1和s2是否相等
	 * @param s1
	 * @param s2
	 * @return 相等true 不等false
	 */
	private boolean comPare(String s1,String s2){
		return filter(s1).equals(filter(s2)) ? true : false;
	}
	
	/**
	 * 过滤车站名中"站"后面的内容，因为百度地图获取的始终车站名乱七八糟，不完全等于实际遍历车站名，
	 * 所以过滤出的结果在大部分情况下可以正常运行，只有当公交车站列表里存在2个及以上过滤站名一样
	 * 的车站，则busStationList的长度不正确。
	 * 例如:
	 * <p>(获取站名)—(实际站名) → (return)</p>
	 * <p>瓶窑南站—瓶窑南站 → 瓶窑南(完全相等,为了一致还是需要过滤)</p>
	 * <p>梁家村站—梁家村 → 梁家村</p>
	 * <p>良渚站(A口)—良渚 → 良渚</p>
	 * <p>特殊情况(只要其他车站过滤后不相等，仍能正确判断):</p>
	 * <p>城站火车站站—城站火车站 → 城</p>
	 * <p>站前路公交车站站—站前路公交车站 → ""</p>
	 * @param string
	 * @return 过滤站名
	 */
	private String filter(String string){
		StringBuffer stringBuilder=new StringBuffer(string);
		if (stringBuilder.indexOf("站")!=-1) 
			stringBuilder.delete(stringBuilder.indexOf("站"), stringBuilder.length());
		return stringBuilder.toString();
	}
	
	public View getView() {
		return view;
	}
	
	@Override
	public void onGetPoiDetailResult(PoiDetailResult arg0) {}

	@Override
	public void onGetPoiIndoorResult(PoiIndoorResult arg0) {}

}
