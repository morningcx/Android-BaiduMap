package com.example.layouttest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.overlayutil.BusLineOverlay;
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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

public class MainActivity extends Activity implements OnClickListener,OnGetPoiSearchResultListener, 
OnGetBusLineSearchResultListener,SensorEventListener, OnMapClickListener{
	/**
	 * 读取实时公交坐标的时间间隔ms
	 */
	private final static int TIME=500; 
	//实时相关
    private boolean reminder=false;//是否开启到站提醒
    private LatLng reminderSt;//到站提醒的车站坐标
    BitmapDescriptor busImage;//公交bitmap，用完要回收
    private Marker busMaker;//公交maker
    private boolean canMoving=true;//是否可以开启实时公交
    //其他
	private SensorManager sensorManager;// 方向传感器相关
	private Sensor sensor;
	private LocationClient mLocClient;// 定位相关
	private boolean isFirstLoc = true;// 是否首次定位
	private MyLocationData locData;
	private String mCurrentCity=null;//当前城市
	private int city=0;//延迟2秒
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentAccracy;
    private Double lastX = 0.0;//前一次的角度
	private BuslineMap buslineMap;
	private BuslineList buslineList;
	private int nodeIndex = -2; // 节点索引,供浏览节点时使用
    private BusLineResult route = null; // 记录poi数据，上下车站显示时调用
    private List<String> busLineIDList = null;
    private int busLineIndex = 0;// 搜索相关，公交路线的条数
    private PoiSearch mSearch = null; // 搜索模块，也可去掉地图模块独立使用
    private BusLineSearch mBusLineSearch = null;
    private BusLineOverlay overlay; // 公交路线绘制对象
    private EditText editCity;
    private EditText editSearchKey;
    private MyLocationListenner myListenner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SDKInitializer.initialize(getApplicationContext());
		setContentView(R.layout.activity_main);
		myListenner = new MyLocationListenner();
        editCity = (EditText) findViewById(R.id.city);
        editSearchKey = (EditText) findViewById(R.id.searchkey);
		Button mapbt=(Button) findViewById(R.id.buslinemap);
		Button listBt=(Button) findViewById(R.id.buslinelist);
		mapbt.setOnClickListener(this);
		listBt.setOnClickListener(this);
		//fragment设置
		FragmentManager fm = getFragmentManager();
		FragmentTransaction transaction = fm.beginTransaction();
		buslineMap=new BuslineMap();
		buslineList=new BuslineList();
		transaction.add(R.id.buslineframe, buslineMap);
		transaction.add(R.id.buslineframe, buslineList);
		transaction.show(buslineMap);
		transaction.hide(buslineList);
		transaction.commit();
		//公交搜索设置
		mSearch = PoiSearch.newInstance();//创建检索实例
        mSearch.setOnGetPoiSearchResultListener(this);//添加监听器
        mBusLineSearch = BusLineSearch.newInstance();//创建 公交检索实例
        mBusLineSearch.setOnGetBusLineSearchResultListener(this);
        busLineIDList = new ArrayList<String>();//创建一个顺序表    
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
	
    /**
     * 接受公交位置，开始移动或者结束移动
     */
    private Handler hander=new Handler(){
    	public void handleMessage(android.os.Message msg) {
    		switch (msg.what) {
			case 0:
				LatLng l=(LatLng) msg.obj;
				busMaker.setPosition(l);//移动
				//如果设置了到站提醒
				if (reminder) {
					//判断车站坐标和公交坐标远近
					int num=Arrive.arrive(l, reminderSt);
					//如果公交车到站
					if (num==0) {
						Builder builder=new Builder(MainActivity.this,AlertDialog.THEME_HOLO_LIGHT);
						builder.setTitle("到站提醒");
						builder.setMessage("公交车到站了！祝你一路顺风！");
						builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							};
						});
						builder.show();
						buslineMap.currentRS.setText("当前提醒站点\n无");
						//到站提醒以后要重置相关数据，否则下次执行会出错
						reminder=false;//关闭到站提醒
					}
					//如果公交车临近
					else if (num==1) {
						Toast.makeText(MainActivity.this,
								"预计将在一分钟内到站，请做好上车准备", Toast.LENGTH_SHORT).show();
					}
				}
				break;
            case 1:
            	//线程结束，每次关闭实时公交显示都会传msg.what=1的值，然后调用这个方法
				buslineMap.showBus.setText("开启实时公交");
				busMaker.remove();//清除图像
				busImage.recycle();//回收
				busImage=null;
            	break;
			}
    	};
    };
    
	/**
	 * 读取公交车坐标线程
	 */
	public class BusThread extends Thread{
		@Override
		public void run() {
			int i=0;
			//当canMoving为true时才执行，结束只需=false
			while(canMoving&&i<route.getSteps().get(0).getWayPoints().size()){
				try {
					LatLng latLng=route.getSteps().get(0).getWayPoints().get(i++);
					//传信息置hander，what为0
					Message msg=Message.obtain(hander, 0, latLng);
					msg.sendToTarget();
					Thread.sleep(TIME);
				} catch (InterruptedException e) {
					Toast.makeText(MainActivity.this,
							"读取实时公交信息发生错误", Toast.LENGTH_SHORT).show();
				}
			}
			canMoving=false;//正常循环后还是true，这种情况另外考虑，故每次结束都设false
			Message msg=Message.obtain(hander, 1);//传信息置hander，what为1
			msg.sendToTarget();
			super.run();
		}
	}
	
	@Override
	protected void onStart() {
		buslineMap.mBaiduMap=buslineMap.mMapView.getMap();//获取地图
		// 开启定位图层
		buslineMap.mBaiduMap.setMyLocationEnabled(true);
		buslineMap.mBaiduMap.setMyLocationConfiguration(
				new MyLocationConfiguration(LocationMode.NORMAL, true, null));
		// 隐藏百度的LOGO
		View child = buslineMap.mMapView.getChildAt(1);
		if (child != null && (child instanceof ImageView || child instanceof ZoomControls))
			child.setVisibility(View.INVISIBLE);
		//点击地图
		buslineMap.mBaiduMap.setOnMapClickListener(this);
		overlay = new BusLineOverlay(buslineMap.mBaiduMap);//用于显示一条公交详情结果的Overlay，这是构造函数
        buslineMap.mBaiduMap.setOnMarkerClickListener(overlay);//将overlay添加在地图上
		buslineMap.mBtnPre.setVisibility(View.INVISIBLE);//隐藏上一站和下一站按钮
		buslineMap.mBtnNext.setVisibility(View.INVISIBLE);
		//公交实时数据
		final BusThread bus=new BusThread();
		buslineMap.showBus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
					if (route!=null) {
						if (bus.isAlive()) {   //关闭线程
							canMoving=false;//一个条件就够了，结束会自动清理
						}
						else{   //开启线程
							if (busImage==null) {
								//初始化全局 bitmap 信息，不用时及时 recycle
								busImage=BitmapDescriptorFactory.fromResource(R.drawable.movingbus);
							}
							//设置Marker初始位置和图片
							MarkerOptions options=new MarkerOptions().
									position(route.getSteps().get(0).getWayPoints().get(0)).
									icon(busImage).zIndex(10);
							busMaker=(Marker) buslineMap.mBaiduMap.addOverlay(options);
							canMoving=true;
							buslineMap.showBus.setText("关闭实时公交");
							bus.start();
						}
					}
			}
		});
		super.onStart();
	}

	/**
	 * 定位内部类
	 * 
	 */
    public class MyLocationListenner implements BDLocationListener { 
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null || buslineMap.mMapView == null) 
                return;  // map view 销毁后不在处理新接收的位置
            mCurrentCity=location.getCity();//获取城市
            mCurrentLat = location.getLatitude();
            mCurrentLon = location.getLongitude();
            mCurrentAccracy = location.getRadius();
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(mCurrentDirection)//方向0-360°通过传感器获取
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            buslineMap.mBaiduMap.setMyLocationData(locData);
            //是否是第一次定位
            if (isFirstLoc) {
                isFirstLoc = false; 
                LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(17);
                //移动至定位中心点
                buslineMap.mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));   
            }
          //延迟2秒，避免在地图从北京平移至当前位置的过程中因重置text而打断平移
            if (city<=2) {
              if (city==1) {
            	if(mCurrentCity==null) 
                	Toast.makeText(MainActivity.this, "定位失败",Toast.LENGTH_LONG).show();
                else {
                	Toast.makeText(MainActivity.this, "定位成功！当前城市："+mCurrentCity,Toast.LENGTH_LONG).show();
                	editCity.setText(mCurrentCity);
    		    }
			 } city++; }
        }
        public void onReceivePoi(BDLocation poiLocation) {}
    }
	
    /**
     * 上下车站节点的onclick,车站list点击事件
     * <p>
     * 索引加减和气泡显示
     * </p>
     * @param v
     */
	public void nodeClick(View v) { 
		if (nodeIndex < -1 || route == null|| nodeIndex >= route.getStations().size()) 
            return;
        TextView popupText = new TextView(this);
        popupText.setBackgroundResource(R.drawable.popup);
        popupText.setTextColor(0xff000000);//黑色
        popupText.setGravity(Gravity.CENTER_HORIZONTAL);
        popupText.setPadding(20, 10, 20, 0);
        if (buslineMap.mBtnPre.equals(v) && nodeIndex > 0) 
        	//如果获取的view为上一个节点按钮，索引减
            nodeIndex--;
        if (buslineMap.mBtnNext.equals(v) && nodeIndex < (route.getStations().size() - 1)) 
        	//如果获取的view为下一个节点按钮，索引加
            nodeIndex++; 
        if (nodeIndex >= 0) {
            // 移动到指定索引的坐标
        	buslineMap.mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(route
                    .getStations().get(nodeIndex).getLocation()));
            // 弹出泡泡，"第"+(nodeIndex+1)+"站："
            popupText.setText(route.getStations().get(nodeIndex).getTitle());
            buslineMap.mBaiduMap.showInfoWindow(new InfoWindow(popupText, route.getStations()
                    .get(nodeIndex).getLocation(), 0));//0偏移量
        }
    }
	
	/**
	 * search的onclick方法
	 * 点击search的操作
	 * @param v
	 */
	public void searchButtonProcess(View v) {
		//切换公交信息的时候关闭提醒
		reminder=false;
		buslineMap.currentRS.setText("当前提醒站点\n无");
		//关闭线程，以防未关闭实时公交导致错误
		canMoving=false;
        busLineIDList.clear();//清空公交list表格
        busLineIndex = 0;//第n条公交搜索路线，此处为0
        buslineMap.mBtnPre.setVisibility(View.INVISIBLE);//隐藏上一站和下一站按钮
		buslineMap.mBtnNext.setVisibility(View.INVISIBLE);
        // 发起poi检索，从得到所有poi中找到公交线路类型的poi，再使用该poi的uid进行公交详情搜索
        mSearch.searchInCity((new PoiCitySearchOption()).city(
                editCity.getText().toString())
                        .keyword(editSearchKey.getText().toString()));
    }
	
	@Override
	public void onClick(View v) { 
		//切换fragment
		FragmentManager fm = getFragmentManager();
		FragmentTransaction transaction = fm.beginTransaction();
		switch (v.getId()) {
		case R.id.buslinelist:
			transaction.hide(buslineMap);
			transaction.show(buslineList);
			break;
        case R.id.buslinemap:
        	transaction.hide(buslineList);
        	transaction.show(buslineMap);
			break;
		default:
			break;
		}
		transaction.commit();
	}

	/**
	 * 获取busline的动作
	 * 显示listview和map映射
	 */
	@Override
	public void onGetBusLineResult(final BusLineResult result) {
		//对Map的操作
		buslineMap.mBaiduMap.clear();//清空地图
        route = result;//把result赋值给BusLineResult点击上下车站使用
        overlay.removeFromMap();//将所有Overlay 从 地图上消除
        overlay.setData(result);//设置公交路线数据
        overlay.addToMap();//将overplay添加到地图上
        overlay.zoomToSpan();//缩放地图，使所有Overlay都在合适的视野内
        //上下站视为可见
        buslineMap.mBtnPre.setVisibility(View.VISIBLE);
        buslineMap.mBtnNext.setVisibility(View.VISIBLE);
        //对List的操作
        List<Map<String, Object>> station=new ArrayList<Map<String,Object>>();
        buslineList.busTitle.setText(result.getBusLineName());//获取公交路线名称
        buslineList.busInfo.setText("首 "+result.getStartTime().getHours()+":"+
                getMinutes(result.getStartTime().getMinutes())+
        		"  末 "+result.getEndTime().getHours()+":"+
                getMinutes(result.getEndTime().getMinutes())+
        		"  票价 "+result.getBasePrice()+"元"+
                "  总站数 "+result.getStations().size()+"站");
        //获取最近车站指数
        int num=closestStation(route);
        //创建map，添加各个车站名称
        for(nodeIndex=0;nodeIndex<route.getStations().size();nodeIndex++){
        	Map<String, Object> map=new HashMap<String, Object>();
        	map.put("picture", R.drawable.itemicon);
        	map.put("stationname",route.getStations().get(nodeIndex).getTitle());
        	if (nodeIndex==num) {
        		//判断最近车站覆盖并添加(离我最近)信息
				map.put("stationname", route.getStations().get(nodeIndex).getTitle()+"(离我最近)");
			}
        	station.add(map);
        }
        //设置一个SimpleAdapter
        buslineList.busStationList.setAdapter(new SimpleAdapter(this, station, R.layout.stationitem,
        		new String[]{"picture","stationname"},new int[]{R.id.stationpicture,R.id.stationitem}));
        //添加item点击监听器
        buslineList.busStationList.setOnItemClickListener(new OnItemClickListener() {
            //item点击事件
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				setStationP(position);
			}
		});
        //长按设置到站提醒
        buslineList.busStationList.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
				//到站提醒dialog
				Builder builder=new Builder(MainActivity.this,AlertDialog.THEME_HOLO_LIGHT);
				builder.setTitle("到站提醒");
				builder.setMessage("是否开启“"+result.getStations().get(position).getTitle()+"”站的到站提醒");
				//确定按钮
				builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {		
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//车站坐标
						reminder=true;
						Arrive.hasReturn=false;//临近提醒
						reminderSt=result.getStations().get(position).getLocation();//获取提醒站点的位置
						buslineMap.currentRS.setText("当前提醒站点\n"+
						result.getStations().get(position).getTitle());//设置Text
						Toast.makeText(MainActivity.this,"已成功开启“"+ 
								result.getStations().get(position).getTitle()+"”站的到站提醒", 
								Toast.LENGTH_SHORT).show();
						setStationP(position);
					}
				});
				//取消按钮
				builder.setNegativeButton("取消", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.show();
				return true;
			}
		});
        //标记，将公交车站点设为-1，之后显示上下站按钮并且判断触发监听事件
        nodeIndex = -1;
        buslineList.busLineNote.setText("");
        Toast.makeText(this, result.getBusLineName(),Toast.LENGTH_SHORT).show();//获取公交路线名称并显示
	}
	/**
	 * 显示map 隐藏list 并触发产生气泡的方法
	 * @param position
	 */
	private void setStationP(int position){
		nodeIndex=position;
		nodeClick(null);
		FragmentManager fm = getFragmentManager();
		FragmentTransaction transaction = fm.beginTransaction();
		transaction.hide(buslineList);
    	transaction.show(buslineMap);
    	transaction.commit();
	}
	
	/**
	 * 离我最近
	 * @return 最近的车站index
	 */
	private int closestStation(BusLineResult route){
		Double distance=100.0;
		int num=-1;
        for(int i=0;i<route.getStations().size();i++){
        	Double d=Math.pow(route.getStations().get(i).getLocation().latitude-mCurrentLat, 2)+
        			Math.pow(route.getStations().get(i).getLocation().longitude-mCurrentLon, 2);
        	if (d<distance) {
				distance=d;
				num=i;
			}
        }
		return num;
	}
	/**
	 * 分钟转换
	 * @param i
	 * @return 分钟为0时返回00，其余返回原值
	 */
	private String getMinutes(int i){
		if(i==0)
		    return "00";
		else
			return ""+i;
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
	public void onGetPoiResult(PoiResult result) { 
		//获取poi的动作
		closeKeyboard();
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			noResultClear();
            return;
        }
        busLineIDList.clear();//清空公交路线list
        for (PoiInfo poi : result.getAllPoi()) // 遍历所有poi，找到类型为公交线路的poi
            if (poi.type == PoiInfo.POITYPE.BUS_LINE|| poi.type == PoiInfo.POITYPE.SUBWAY_LINE) 
                busLineIDList.add(poi.uid);//如果遍历的type为bus或者subway，则添加list
        //如果poi中没有相关信息则return
        if (busLineIDList.size()==0) {
        	noResultClear();
            return;
		}
        searchNextBusline(null);//调用上下行方法，对地图进行新公交路线的显示更新（其实公交指数不一定是从0开始的）
        route = null;//有新的result，则将route设null
	}
	
	/**
	 * 没有结果，清除控件
	 */
	private void noResultClear(){
		buslineList.busTitle.setText("");
        buslineList.busInfo.setText("");
        buslineList.busStationList.setAdapter(null);
		buslineList.busLineNote.setText("公交信息搜索结果");
        Toast.makeText(this, "抱歉，未找到结果",Toast.LENGTH_LONG).show();
        buslineMap.mBaiduMap.clear();
	}
	
	/**
	 * 上下行的onclick方法
	 * 显示下一条搜索到的公交路线
	 * @param v
	 */
	public void searchNextBusline(View v) {
		//切换公交信息的时候关闭提醒
		reminder=false;
		buslineMap.currentRS.setText("当前提醒站点\n无");
		//关闭线程，以防未关闭实时公交导致错误
		canMoving=false;
		if (busLineIndex >= busLineIDList.size())
            busLineIndex = 0;//如果越界，则路线指数归0，继续点击则循环显示
        if (busLineIndex >= 0 && busLineIndex < busLineIDList.size()
                && busLineIDList.size() > 0) {
        	BusLineSearchOption busOption=(new BusLineSearchOption()//mBusLineSearch改变的过程中调用onGetBusLineResult方法 
                    .city(((EditText) findViewById(R.id.city)).getText()
                            .toString()).uid(busLineIDList.get(busLineIndex)));
            mBusLineSearch.searchBusLine(busOption);//显示下一条公交路线
			}
            busLineIndex++;
        }

	@Override
	public void onSensorChanged(SensorEvent event) {
        double x = event.values[0];
        //角度变化的绝对值大于1°
        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;
            locData = new MyLocationData.Builder()
                    .accuracy(mCurrentAccracy)
                    .direction(mCurrentDirection)
                    .latitude(mCurrentLat)
                    .longitude(mCurrentLon).build();
            buslineMap.mBaiduMap.setMyLocationData(locData);
        }
        lastX = x;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		buslineMap.mMapView.onResume();
		sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	protected void onPause() {
		buslineMap.mMapView.onPause();
		sensorManager.unregisterListener(this);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		//关闭线程
		canMoving=false;
		//回收
		busImage.recycle();
		busImage=null;
		// 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        buslineMap.mBaiduMap.setMyLocationEnabled(false);
        buslineMap.mMapView.onDestroy();
        buslineMap.mMapView = null;
        //公交相关关闭
		mSearch.destroy();
        mBusLineSearch.destroy();
		super.onDestroy();
	}

	@Override
	public void onMapClick(LatLng arg0) {
		buslineMap.mBaiduMap.hideInfoWindow();//点地图，隐藏泡泡
	}

	@Override
	public boolean onMapPoiClick(MapPoi arg0) {
		return false;
	}

	@Override
	public void onGetPoiDetailResult(PoiDetailResult arg0) {}

	@Override
	public void onGetPoiIndoorResult(PoiIndoorResult arg0) {}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

}
