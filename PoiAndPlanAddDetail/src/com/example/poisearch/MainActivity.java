package com.example.poisearch;

import java.util.ArrayList;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRouteLine.TransitStep;
import com.baidu.mapapi.search.route.TransitRouteLine.TransitStep.TransitRouteStepType;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionResult.SuggestionInfo;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

public class MainActivity extends Activity implements OnGetSuggestionResultListener,OnGetPoiSearchResultListener,
OnClickListener,OnGetRoutePlanResultListener,SensorEventListener,OnMapClickListener{
	//地图设置
	MapView mMapView;
	BaiduMap mBaiduMap;
	//控件设置
	public LinearLayout planLinear;
	private Button p1,p2,p3,p4,p5;//五个方案按钮
	private Button planMapBt,planChoiceBt;//显示地图和路线方案按钮
	private Button prePlan,nextPlan;//上下方案
	private Button preStep,nextStep;//上下步骤
	private TextView startPlace;//起点
	private TextView endPlace;//终点
	private TextView popText;
	private TextView planNote;
	public TextView planTitle1;//两个方案标题text
	public static TextView planTitle2;
	private AlertDialog poiDlg;//poi详细信息dig
	private EditText poiCity;//输入的城市
	private AutoCompleteTextView poiKey;//自动补全textview
	private ArrayList<String> suggest;//获取suggest的列表
	private ArrayAdapter<String> sugAdapter=null;//对auotocomplete的适配器
	//搜索模块
	public static String inputCity;//输入城市，BusRouteDetail会用到
	public String inputKey;//输入关键字
	private SuggestionSearch mSuggestionSearch=null;//suggest搜索对象
	private PoiSearch mPoiSearch=null;//poi搜索对象
	private int pageNum=0;//默认poi搜索结果显示第一页
	private PoiPlanMap poiPlanMap=new PoiPlanMap(); //地图的fragment
	private PoiPlanChoice poiPlanChoice=new PoiPlanChoice(); //路线显示的fragment
	private RoutePlanSearch RSearch = null;// 搜索模块，也可去掉地图模块独立使用
	private LatLng endLoc=null;//起始地的地址(定位点的经纬度)
	private LatLng startLoc=null;//目的地的地址
	private PlanNode startNode;
	private PlanNode endNode;
	private int planNum=0;
	private RouteLine route=null;
	private String destName;//目的地名称
	private int nodeIndex=-1;//节点
	private int planSize=0;
	// 定位相关
	private SensorManager sensorManager;// 方向传感器相关
	private Sensor sensor;
	private LocationClient mLocClient;//定位对象
	private boolean isFirstLoc = true;// 是否首次定位
	private MyLocationData locData;//放定位数据的容器
	private String mCurrentCity=null;//当前城市
	private int city=0;//延迟2秒
	private MyLocationListenner myListenner= new MyLocationListenner();//获取定位数据的内部类
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;//经度
    private double mCurrentLon = 0.0;//纬度
    private float mCurrentAccracy;
    private Double lastX = 0.0;//前一次的角度
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SDKInitializer.initialize(getApplicationContext());
		setContentView(R.layout.activity_main);
		//设置view
		planMapBt=(Button) findViewById(R.id.poiplanmapbt);
		planChoiceBt=(Button) findViewById(R.id.poiplantextbt);
		planMapBt.setOnClickListener(this);
		planChoiceBt.setOnClickListener(this);
		poiCity=(EditText) findViewById(R.id.poicity);
		poiKey=(AutoCompleteTextView) findViewById(R.id.searchkey);
		//设置fragment
		FragmentManager fm=getFragmentManager();
		FragmentTransaction transaction=fm.beginTransaction();
		transaction.add(R.id.poiplanframe, poiPlanMap);//加到内存？
		transaction.add(R.id.poiplanframe, poiPlanChoice);
		transaction.hide(poiPlanChoice);
		transaction.show(poiPlanMap);
		transaction.commit();
		//初始化路线搜索模块
        RSearch=RoutePlanSearch.newInstance();
        RSearch.setOnGetRoutePlanResultListener(this);
		//初始化poi搜索结果模块，注册搜索事件监听
		mPoiSearch=PoiSearch.newInstance();
		mPoiSearch.setOnGetPoiSearchResultListener(this);
		//初始化poi建议搜索列表模块，注册建议搜索事件监听
		mSuggestionSearch=SuggestionSearch.newInstance();
		mSuggestionSearch.setOnGetSuggestionResultListener(this);
		//配置默认适配器
		sugAdapter=new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line);
		poiKey.setAdapter(sugAdapter);
		poiKey.setThreshold(1);//输入长度为1即弹出suggestion
		//当key内容改变
		poiKey.addTextChangedListener(new TextWatcher() {	
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length()<=0) return;//长度为0退出
				mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())//对suggest进行搜索
						.keyword(s.toString())
						.city(poiCity.getText().toString())
						.citylimit(true));//城市限制true
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void afterTextChanged(Editable s) {}
		});
		// 定位初始化设置
		mLocClient = new LocationClient(this);
		mLocClient.registerLocationListener(myListenner);
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);
		option.setIsNeedAddress(true);
		option.setCoorType("bd09ll");//可选，设置返回经纬度坐标类型
		option.setScanSpan(1000);//定位间隔时间，需设置1000ms以上才有效
		mLocClient.setLocOption(option);
		mLocClient.start();
		// 方向传感器
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
	}
	
	@Override
	protected void onStart() {
		mBaiduMap=mMapView.getMap();//初始化地图
		// 设置地图点击监听器
		mBaiduMap.setOnMapClickListener(this);
		// 开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
		mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration
				(LocationMode.NORMAL,true, null));
		// 隐藏百度的LOGO
		View child = mMapView.getChildAt(1);
		if (child != null && (child instanceof ImageView || child instanceof ZoomControls))
			child.setVisibility(View.INVISIBLE);
		super.onStart();
	}
	
	/**
	 * 内部类，获取城市经纬度等信息，实现定位功能
	 */
    public class MyLocationListenner implements BDLocationListener { 
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null || mMapView == null) 
                return;  // map view 销毁后不在处理新接收的位置
            mCurrentCity=location.getCity();//获取城市
            mCurrentLat = location.getLatitude();//经度
            mCurrentLon = location.getLongitude();//纬度
            mCurrentAccracy = location.getRadius();
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(mCurrentDirection)//方向0-360°通过传感器获取
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false; 
                LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(17);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));   
            }
            //延迟2秒，避免在地图从北京平移至当前位置的过程中因重置text而打断平移
            if (city<=2) {
              if (city==1) {
            	if(mCurrentCity==null) 
                	Toast.makeText(MainActivity.this, "定位失败",Toast.LENGTH_LONG).show();
                else {
                	Toast.makeText(MainActivity.this, "定位成功！当前城市："+mCurrentCity,Toast.LENGTH_LONG).show();
                	poiCity.setText(mCurrentCity);
    		    }
			} city++; }
        }
        public void onReceivePoi(BDLocation poiLocation) {}
    }
    
	/**
	 * 获取输入建议列表
	 * <p>
	 * 只要autotext发生变化就会触发
	 * </p>
	 */
	@Override
	public void onGetSuggestionResult(SuggestionResult result) { 
		if (result == null || result.getAllSuggestions() == null) return; //suggest为空退出
		suggest=new ArrayList<String>();
		for(SuggestionInfo info:result.getAllSuggestions())  //遍历所有suggestions
			if (info!=null) 
				suggest.add(info.key);
		sugAdapter=new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, suggest);
		poiKey.setAdapter(sugAdapter);
		sugAdapter.notifyDataSetChanged();//刷新suggest列表
	}
	
	/**
	 * 查询按钮的onclick方法
	 * @param view
	 */
	public void searchButtonProcess(View view){ 
		viewPreNextBt(View.INVISIBLE);
		showPlanBt(0);//隐藏所有planBt
		closeKeyboard();
		planLinear.removeAllViews();//移除所有view
		viewChoiceTxBt(View.INVISIBLE);
		planNote.setVisibility(View.VISIBLE);
		//获取城市和key
		inputCity=poiCity.getText().toString();
		inputKey=poiKey.getText().toString();
		//触发poiresult，默认搜索结果页面数为0
		mPoiSearch.searchInCity((new PoiCitySearchOption()).city(inputCity)
				.keyword(inputKey).pageNum(pageNum));
	}
	
	/**
	 * 获取PoiResult并显示在地图上
	 * <p>
	 * 按下查询按钮就会触发
	 * </p>
	 */
	@Override
	public void onGetPoiResult(PoiResult result) { 
		mBaiduMap.clear(); //清空地图
		if (result==null||result.error==SearchResult.ERRORNO.RESULT_NOT_FOUND) {
			Toast.makeText(this, "抱歉，未找到结果", Toast.LENGTH_LONG).show();
			return;
		}
		if (result.error==SearchResult.ERRORNO.NO_ERROR) { //result没有错误
				//创建overlay对象，默认显示10个poi信息
				PoiOverlay overlay=new PoiOverlay(mBaiduMap){ 
					@Override
					public boolean onPoiClick(int i) { //需要重写，按下poi气泡的方法
						super.onPoiClick(i);
						PoiInfo poi = getPoiResult().getAllPoi().get(i);
						//触发poidetailresult
			            mPoiSearch.searchPoiDetail((new PoiDetailSearchOption()).poiUid(poi.uid));
						return true;
					}
				};
				mBaiduMap.setOnMarkerClickListener(overlay);//监听器，当overlay改变，地图也随之改变
				overlay.setData(result);
				overlay.addToMap();
				overlay.zoomToSpan();//缩放至合适大小
		}
	}

	/**
	 * 获取Poi详情信息并弹出Dialog
	 * <p>
	 * 点击poi泡泡后触发
	 * </p>
	 */
	@Override
	public void onGetPoiDetailResult(PoiDetailResult result) { 
		if (result.error != SearchResult.ERRORNO.NO_ERROR) 
            Toast.makeText(this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        else {
        	//获取result一系列的信息
        	startLoc=new LatLng(mCurrentLat, mCurrentLon);//起点经纬度
        	endLoc=new LatLng(result.getLocation().latitude,result.getLocation().longitude);//终点经纬度
        	destName=result.getName();
        	String info="详细信息："+result.getAddress()+
        			"\n\n标签："+result.getTag()+
        			"\n\n人均消费："+result.getPrice()+
        			"元\n\n综合评分："+result.getOverallRating()+
            		"分\n\n联系电话："+result.getTelephone();
        	//建立poi信息的dlg
        	AlertDialog.Builder builder=new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_LIGHT);//light主题
        	View view=LayoutInflater.from(this).inflate(R.layout.poidetaildlg, null);
        	TextView poiName=(TextView) view.findViewById(R.id.poiname);
        	TextView poiInfo=(TextView) view.findViewById(R.id.poiinfo);
        	poiName.setText(destName);
        	poiInfo.setText(info);
        	Button gotopoi=(Button) view.findViewById(R.id.poigoto);
        	//点击"到这里去"，获取poi经纬度，路线规划
        	gotopoi.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					planNum=0;
					startNode=PlanNode.withLocation(startLoc);
					endNode=PlanNode.withLocation(endLoc);
					RSearch.transitSearch((new TransitRoutePlanOption())
							.city(poiCity.getText().toString())
							.from(startNode)//起始地点
						    .to(endNode));//目的地点
					poiDlg.dismiss();
				}
			});
        	Button poiExit=(Button) view.findViewById(R.id.poiexit);
        	//点击"我再看看"则退出
        	poiExit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					poiDlg.dismiss();		
				}
			});
        	builder.setView(view);
        	poiDlg=builder.show();
        }
	}
	
	/**
	 * 获取公交路线结果，显示地图和Text
	 */
	@Override
	public void onGetTransitRouteResult(TransitRouteResult result) { 
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
            return;
        }
		if (result.error==SearchResult.ERRORNO.NO_ERROR) {
			mBaiduMap.clear(); //清空地图
			viewPreNextBt(View.VISIBLE);
			planSize=result.getRouteLines().size();
		    showPlanBt(planSize); //显示相应数量按钮,并隐藏剩余按钮
		    //隐藏notetext
		    planNote.setVisibility(View.INVISIBLE);
		    route=result.getRouteLines().get(planNum); //获取第planNum种路线方案
		    //清零
			StepInfo.busStepNum=0;//一个方案中的公交路段的数量
			BusRouteDetail.totalBusPrice=0;//一个方案的总票价
			BusRouteDetail.stepNum=0;//记录以及搜索到的公交数量
			//获取title1和title2
		    StepInfo info=new StepInfo(route);
		    viewChoiceTxBt(View.VISIBLE);
		    planTitle1.setText(info.getTitle1());
		    planTitle2.setText(info.getTitle2());
		    //显示目的地
		    endPlace.setText(destName+"(终点)");
		    Toast.makeText(this,info.getTitle1(), Toast.LENGTH_SHORT).show();
		    planLinear.removeAllViews();
		    //显示各个步骤详情
		    for(int i=0;i<route.getAllStep().size();i++){
			    Object step=route.getAllStep().get(i); //获取第planNum种路线的第i种步骤
			    TransitStep tStep=(TransitStep) step;
			    if (tStep.getStepType()==TransitRouteStepType.WAKLING) {
					View view=View.inflate(this, R.layout.walkroute_list, null);
					TextView text=(TextView) view.findViewById(R.id.walkroute_info);
					text.setText(tStep.getInstructions());
					tStep.getDuration();
					planLinear.addView(view);
				}
			    else{
			    	planLinear.addView((new BusRouteDetail(this, tStep)).getView());
			    }
		    }
		    //地图显示路线
		    TransitRouteOverlay overlay=new TransitRouteOverlay(mBaiduMap);
		    mBaiduMap.setOnMarkerClickListener(overlay);//当overlay改变，则map改变
		    overlay.setData(result.getRouteLines().get(planNum));
            overlay.addToMap();
            overlay.zoomToSpan();
            nodeIndex=-1; //每次路线规划完nodeindex置-1，从第一步开始
		}
	}
	
	/**
	 * 路线步骤的onclick方法
	 * @param v
	 */
	public void stepNodeClick(View v){
		if (route==null||route.getAllStep()==null) 
			return;
		//范围内执行
		if (v.getId()==R.id.nextstep && nodeIndex<(route.getAllStep().size()-1)) 
			nodeIndex++;
		if (v.getId()==R.id.prestep && nodeIndex>0) 
			nodeIndex--;
		if (nodeIndex>=0&&nodeIndex<route.getAllStep().size()) {
			Object step = route.getAllStep().get(nodeIndex);
			String string=((TransitStep) step).getInstructions();
			LatLng latLng=((TransitRouteLine.TransitStep) step).getEntrance().getLocation();
			//移动图标到中心
			mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(latLng));
			//设置text
			popText = new TextView(this);
	        popText.setBackgroundResource(R.drawable.popup);
	        popText.setTextColor(0xFF000000);
	        popText.setGravity(Gravity.CENTER_HORIZONTAL);
	        popText.setPadding(20, 10, 20, 0);
	        popText.setText(string);
	        //显示text
	        mBaiduMap.showInfoWindow(new InfoWindow(popText, latLng, 0));
		}
	}
	
	/**
	 * fragment隐藏和显示
	 */
	@Override
	public void onClick(View v) { 
		FragmentManager fm=getFragmentManager();
		FragmentTransaction transaction=fm.beginTransaction();
		switch (v.getId()) {
		case R.id.poiplanmapbt:
			transaction.hide(poiPlanChoice);
			transaction.show(poiPlanMap);
			break;
		case R.id.poiplantextbt:
			transaction.show(poiPlanChoice);
			transaction.hide(poiPlanMap);
			break;
		default:
			break;
		}
		transaction.commit();
	}
	
	/**
	 * 路线步骤方案text的fragment
	 */
	private class PoiPlanChoice extends Fragment implements OnClickListener{ 
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view=inflater.inflate(R.layout.poi_plan_choice, container,false);
			planLinear=(LinearLayout) view.findViewById(R.id.planlinear);
			planNote=(TextView) view.findViewById(R.id.plannote);
			startPlace=(TextView) view.findViewById(R.id.startplace);
			endPlace=(TextView) view.findViewById(R.id.endplace);
			planTitle1=(TextView) view.findViewById(R.id.plan_title1);
			planTitle2=(TextView) view.findViewById(R.id.plan_title2);
			p1=(Button) view.findViewById(R.id.plan1);
			p2=(Button) view.findViewById(R.id.plan2);
			p3=(Button) view.findViewById(R.id.plan3);
			p4=(Button) view.findViewById(R.id.plan4);
			p5=(Button) view.findViewById(R.id.plan5);
			viewChoiceTxBt(View.INVISIBLE);
        	//五个方案按钮添加监听器
			Button bt[]={p1,p2,p3,p4,p5};
			for(int i=0;i<5;i++)
				bt[i].setOnClickListener(this);
			showPlanBt(0);//隐藏所有planBt
			return view;
		}
		
		/**
		 * 设置planNum的数值，获取第planNum种路线方案
		 */
		@Override
		public void onClick(View v) { 
			switch (v.getId()) {
			case R.id.plan1:
				planNum=0;
				break;
			case R.id.plan2:
				planNum=1;
				break;
			case R.id.plan3:
				planNum=2;
				break;
			case R.id.plan4:
				planNum=3;
				break;
			case R.id.plan5:
				planNum=4;
				break;
			default:
				break;
			}
			//执行搜索
			RSearch.transitSearch((new TransitRoutePlanOption())
					.city(poiCity.getText().toString())
					.from(startNode)//起始地点
				    .to(endNode));//目的地点
		}
	}
	
	/**
	 * 显示num种方案的按钮
	 */
	private void showPlanBt(int num){ 
		Button bt[]={p1,p2,p3,p4,p5};
		for(int i=0;i<num;i++)
			bt[i].setVisibility(View.VISIBLE);
		for(int i=num;i<5;i++)
			bt[i].setVisibility(View.INVISIBLE);
	}
	
	/**
	 * 路线搜索的map的fragment
	 */
	private class PoiPlanMap extends Fragment implements OnClickListener{ 
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view=inflater.inflate(R.layout.poi_plan_map, container,false);
			mMapView=(MapView) view.findViewById(R.id.mapView);
			//前后方案
			prePlan=(Button) view.findViewById(R.id.preplan);
			nextPlan=(Button) view.findViewById(R.id.nextplan);
			//前后步骤
			preStep=(Button) view.findViewById(R.id.prestep);
			nextStep=(Button) view.findViewById(R.id.nextstep);
			//隐藏所有
			viewPreNextBt(View.INVISIBLE);
			prePlan.setOnClickListener(this);
			nextPlan.setOnClickListener(this);
			return view;
		}
        
		/**
		 * 前后方案onclick方法
		 */
	    @Override
	    public void onClick(View v) {
			switch (v.getId()) {
			case R.id.preplan:
				if(planNum>0)
				    planNum--;
				else
					return;
				break;
	        case R.id.nextplan:
	        	if(planNum<planSize-1)
	        	    planNum++;
	        	else
					return;
				break;
			}
			//执行搜索
			RSearch.transitSearch((new TransitRoutePlanOption())
					.city(poiCity.getText().toString())
					.from(startNode)//起始地点
				    .to(endNode));//目的地点
	    }
	}
	
	/**
	 * 隐藏或显示map的fragment上下方案、上下步骤四个按钮
	 */
	private void viewPreNextBt(int view){
		prePlan.setVisibility(view);
		nextPlan.setVisibility(view);
		preStep.setVisibility(view);
		nextStep.setVisibility(view);
	}
	
	/**
	 * 隐藏或显示choice的fragment里的所有view，除了notetext
	 */
	private void viewChoiceTxBt(int view){
		startPlace.setVisibility(view);
		endPlace.setVisibility(view);
		planTitle1.setVisibility(view);
		planTitle2.setVisibility(view);
		planLinear.setVisibility(view);
	}
	/**
	 * 关闭软键盘
	 */
	private void closeKeyboard() { 
        View view = getWindow().peekDecorView();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

	@Override
	protected void onPause() {
		mMapView.onPause();
		sensorManager.unregisterListener(this);
		super.onPause();
	}
	
    @Override
    protected void onResume() {
    	mMapView.onResume();
    	sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    	super.onResume();
    }
    
	@Override
	protected void onDestroy() {
		// 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
		mMapView.onDestroy();
		mMapView=null;
		mPoiSearch.destroy();
        mSuggestionSearch.destroy();
        RSearch.destroy();
		super.onDestroy();
	}
	
	/**
	 * 方向传感器
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		double x = event.values[0];
        if (Math.abs(x - lastX) > 1.0) {//角度变化的绝对值大于1°
            mCurrentDirection = (int) x;
            locData = new MyLocationData.Builder()
                    .accuracy(mCurrentAccracy)
                    .direction(mCurrentDirection)
                    .latitude(mCurrentLat)
                    .longitude(mCurrentLon).build();
            mBaiduMap.setMyLocationData(locData);
        }
        lastX = x;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	
	/**
	 * 点击地图空白，隐藏泡泡
	 */
	@Override
	public void onMapClick(LatLng arg0) {
		mBaiduMap.hideInfoWindow();
	}
	
	/**
	 * 点击地图Poi标识，Toast
	 */
	@Override
	public boolean onMapPoiClick(MapPoi arg0) {
		Toast.makeText(this, arg0.getName(), Toast.LENGTH_SHORT).show();
		return true;
	}

	@Override
	public void onGetPoiIndoorResult(PoiIndoorResult arg0) {}

	@Override
	public void onGetBikingRouteResult(BikingRouteResult arg0) {}

	@Override
	public void onGetDrivingRouteResult(DrivingRouteResult arg0) {}

	@Override
	public void onGetIndoorRouteResult(IndoorRouteResult arg0) {}

	@Override
	public void onGetMassTransitRouteResult(MassTransitRouteResult arg0) {}

	@Override
	public void onGetWalkingRouteResult(WalkingRouteResult arg0) {}

}
