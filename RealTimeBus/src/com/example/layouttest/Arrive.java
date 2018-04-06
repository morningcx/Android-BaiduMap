package com.example.layouttest;

import com.baidu.mapapi.model.LatLng;

public class Arrive {
	static boolean hasReturn=false;
	/**
	 * 判断两个距离点的远近，用于判断公交距离
	 * @param aLng
	 * @param bLng
	 * @return 
	 * 0 到站
	 * 1 临近
	 * 100 较远
	 */
    public static int arrive(LatLng aLng,LatLng bLng){
    	Double d=Math.pow(aLng.latitude-bLng.latitude,2)+
    			Math.pow(aLng.longitude-bLng.longitude, 2);
    	if (d<0.0000001){//到站
    		//临近提醒初始化为false，下次触发重新开始判断
    		Arrive.hasReturn=false;
    		return 0;
    	}	
    	//临近提醒，0.000015差不多相差300-500米左右的距离
    	else if (d<0.000015) { 
    		if (hasReturn) //已经临近提醒
				return 100;
			else{
				hasReturn=true;
				return 1;
			}
		}
    	else //距离较远
    		return 100;
    }
}
