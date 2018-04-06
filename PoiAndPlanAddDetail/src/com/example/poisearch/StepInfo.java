package com.example.poisearch;

import java.text.DecimalFormat;

import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.VehicleInfo;
import com.baidu.mapapi.search.route.TransitRouteLine.TransitStep;
import com.baidu.mapapi.search.route.TransitRouteLine.TransitStep.TransitRouteStepType;

public class StepInfo {
	/**
	 * 记录路线中公交路线数量
	 */
	public static int busStepNum=0;

	private RouteLine route;
	/**
	 * 路线标题1
	 * <p>
	 * 路线中所有需要乘坐的公交车/地铁
	 * </p>
	 */
	private StringBuilder title1=new StringBuilder();
	/**
	 * 路线标题2
	 * <p>
	 * 路线中所有详细信息
	 * </p>
	 */
	private String title2;
	/**
	 * 总耗时
	 */
	private String totalTime;
	/**
	 * 总距离(title2没有添加)
	 */
	private String totalDistance;
	/**
	 * 步行距离
	 */
	private int walkingDistance=0;
	/**
	 * 步行时间
	 */
	private int walkingTime=0;
	/**
	 * 乘坐车站总数
	 */
	private int totalStation=0;
	/**
	 * 构造方法
	 * @param route
	 * 分析RouteLine信息
	 */
	public StepInfo(RouteLine route) {
		this.route=route;
		totalTime=aboutTime(route.getDuration());
		totalDistance=aboutDistance(route.getDistance());
		getInfo();
	}
	
	private void getInfo(){
		for(int i=0;i<route.getAllStep().size();i++){
			//获取第planNum种路线的第i种步骤
			Object step=route.getAllStep().get(i); 
		    TransitStep tStep=(TransitStep) step;
		    //获取类型为步行
		    if (tStep.getStepType()==TransitRouteStepType.WAKLING) {
				walkingDistance+=tStep.getDistance();
				walkingTime+=tStep.getDuration();
			}
		    else{
		    	VehicleInfo v=tStep.getVehicleInfo();
		    	totalStation+=v.getPassStationNum(); //经过车站数
		    	title1.append(v.getTitle()+">"); //公交名称
		    	busStepNum++;
		    }
		}
		title1.deleteCharAt(title1.length()-1);
	}
	
	/**
	 * 路线时间换算
	 * @param time
	 * @return 估计时间(String)
	 */
	public static String aboutTime(int time){ 
		String string;
		if (time/3600==0) 
			string=""+time/60 +"分钟";
		else
			string=""+time/3600+"小时"+(time%3600)/60 +"分钟";
		return string;
	}
	
	/**
	 * 路线距离换算
	 * @param dist
	 * @return 估计距离(String)
	 */
	public static String aboutDistance(int dist){ 
		String string;
		if(dist/1000==0)
			string=""+dist+"m";
		else{
			float s=dist;
			DecimalFormat decimalFormat=new DecimalFormat(".0");//保留1位小数
			string=""+decimalFormat.format(s/1000)+"km";
		}
		return string;	
	}
	
	public StringBuilder getTitle1() {
		return title1;
	}
	
	public String getTitle2() { 
		title2=getTotalTime()+//总时间
			" • 步行"+aboutDistance(getWalkingDistance())+//步行距离
			"  "+aboutTime(getWalkingTime())+//步行时间
			" • "+getTotalStation()+"站";//总乘坐站数
		return title2;
	}
	
	public int getTotalStation() {
		return totalStation;
	}
	
	public int getWalkingDistance() {
		return walkingDistance;
	}
	
	public int getWalkingTime() {
		return walkingTime;
	}
	
	public String getTotalTime() {
		return totalTime;
	}
	
	public String getTotalDistance() {
		return totalDistance;
	}
}
