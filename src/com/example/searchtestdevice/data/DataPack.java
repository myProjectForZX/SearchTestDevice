package com.example.searchtestdevice.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.nio.charset.Charset;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

public class DataPack {
	private static final String TAG = "DataPack";
	public static final byte DATA_HEAD = 0x51;
	
	//HOST 为手持端
	//DEVICE  为设备端
	//建立连接流程
	//1. HOST端发送一个搜索请求
	//2. DEVICE反馈一个响应包，  HOST端接收到响应包之后 需要发送一个密码到DEVICE端
	//3. DEVICE在确认之后就可以建立正常发送数据连接。
    public static final byte PACKET_TYPE_FIND_HOST_REQ = 0x10; // 搜索请求
    public static final byte PACKET_TYPE_FIND_DEVICE_RSP = 0x11; // 搜索响应
    public static final byte PACKET_TYPE_FIND_HOST_CHK = 0x12; // 搜索确认
    public static final byte PACKET_TYPE_FIND_DEVICE_CHK = 0x13; // 搜索确认
    public static final byte PACKET_TYPE_FIND_HOST_CHK_2 = 0x14; // 搜索最终确认
    
    public static final byte PACKET_TYPE_SEND_RECV_DATA  = 0x15; // 发送和接收数据
 
    public static final byte PACKET_DATA_TYPE_DEVICE_PASS = 0x20;
    public static final byte PACKET_DATA_TYPE_DEVICE_RESULT = 0x21;
    public static final byte PACKET_DATA_TYPE_DEVICE_NAME = 0x22;
    public static final byte PACKET_DATA_TYPE_DEVICE_TIME = 0x23;
    public static final byte PACKET_DATA_TYPE_DEVICE_LANG = 0x24;
    public static final byte PACKET_DATA_TYPE_DEVICE_ETIP = 0x25;
    public static final byte PACKET_DATA_TYPE_DEVICE_AUDI = 0x26;
    public static final byte PACKET_DATA_TYPE_DEVICE_CONT = 0x27;
    
    public static final String PACKET_CHK_RESULT_OK = "OK";
    public static final String PACKET_CHK_RESULT_BAD = "BAD";
    
    public static final String DEVICE_PASSWORD = "123456";
	/**
     * 打包响应报文
     * 协议：DATA_HEAD + packType(1) + data(n)
     *  data: 由n组数据，每组的组成结构type(1) + length(4) + data(length)
     *  dataType 数组和 dataContent数组数据要一一对应。
     */
    public static byte[] packData(byte packType, byte[] dataType, String[] dataContent) {
        byte[] data = new byte[4096];
        int offset = 0;
        data[offset++] = DATA_HEAD;
        data[offset++] = packType;
 
        if(dataType != null && dataContent != null) {
        	if(dataType.length != dataContent.length) {
        		Log.i(TAG, "datatype mismatch with datacontent");
        		return new byte[0];
        	}
        
        	for (int i = 0; i < dataType.length;  ++i) {
        		byte[] temp = getBytesFromType(dataType[i], dataContent[i]);
        		System.arraycopy(temp, 0, data, offset, temp.length);
        		offset += temp.length;
        	}
        }

        byte[] retVal = new byte[offset];
        System.arraycopy(data, 0, retVal, 0, offset);
 
        return retVal;
    }
    
    /**
     * 解析报文
     * 协议：DATA_HEAD + packType(1) + data(n)
     *  data: 由n组数据，每组的组成结构type(1) + length(4) + data(length)
     */
    private static boolean parsePack(byte data[], int dataOffSet, byte needCheckPackType, Handler handler) {
    	boolean result = false;
        if (data == null) {
        	Log.e(TAG, "---------------------->  data = null");
            return result;
        }
/*        String ip = pack.getAddress().getHostAddress();
        int port = pack.getPort();
        for (DeviceBean d : mDeviceSet) {
            if (d.getIp().equals(ip)) {
                return false;
            }
        }*/
        int dataLen = data.length;
        int offset = 0;
        byte packType = 0x00;
        byte dataType;
        int len;
        /*DeviceBean device = null;*/
 
        if (dataLen < 2) {
        	Log.e(TAG, "---------------------->  datalen < 2");
            return result;
        }

        System.arraycopy(data, dataOffSet, data, 0, dataLen);
 
        //to check packType is right.
        if (data[offset++] != DATA_HEAD  || (packType = data[offset++]) != needCheckPackType) {
        	Log.e(TAG, "----------------------> dataType mismath");
            return result;
        }
        Log.e(TAG, "----------------------> packType : "  + packType);
		switch (packType) {
		case PACKET_TYPE_FIND_HOST_REQ:
			// host
			// 如果是接收到搜索请求标志位 确认是否是有效请求
			result = true;
			Log.e(TAG, "----------------------> PACKET_TYPE_FIND_HOST_REQ");
			break;
		case PACKET_TYPE_FIND_DEVICE_RSP:
				// device
				// send rsp package PACKET_TYPE_FIND_DEVICE_RSP
			break;
		case PACKET_TYPE_FIND_HOST_CHK:
			// host
			// need send password to device. then send PACKET_TYPE_FIND_HOST_CHK
		case PACKET_TYPE_FIND_DEVICE_CHK:
			// device
			// check password if true send PACKET_TYPE_FIND_DEVICE_CHK
			// else close
		case PACKET_TYPE_SEND_RECV_DATA:
			// host update UI
			// to get Data.
			while (offset + 5 < dataLen) {
				dataType = data[offset++];
				len = data[offset++] & 0xFF;
				len |= (data[offset++] << 8);
				len |= (data[offset++] << 16);
				len |= (data[offset++] << 24);

				if (offset + len > dataLen) {
					break;
				}
				
				String resultString = new String(data, offset, len, Charset.forName("UTF-8"));
				
				switch (dataType) {
				case PACKET_DATA_TYPE_DEVICE_PASS:
					if(DEVICE_PASSWORD.equals(resultString)) {
						result = true;
					} else {
						result = false;
					}
					
					break;
				case PACKET_DATA_TYPE_DEVICE_RESULT:
					if(DataPack.PACKET_CHK_RESULT_OK.equals(resultString)) {
						result = true;
					} else {
						result = false;
					}
					break;
				case PACKET_DATA_TYPE_DEVICE_NAME:
					/*
					 * String name = new String(data, offset, len,
					 * Charset.forName("UTF-8")); device = new DeviceBean();
					 * device.setName(name); device.setIp(ip);
					 * device.setPort(port);
					 */
					break;
				case PACKET_DATA_TYPE_DEVICE_TIME:
					/*
					 * String room = new String(data, offset, len,
					 * Charset.forName("UTF-8")); if (device != null) {
					 * device.setRoom(room); }
					 */
				case PACKET_DATA_TYPE_DEVICE_LANG:
					break;
				case PACKET_DATA_TYPE_DEVICE_ETIP:
					break;
				case PACKET_DATA_TYPE_DEVICE_AUDI:
					break;
				case PACKET_DATA_TYPE_DEVICE_CONT:
					break;
				default:
					break;
				}
				offset += len;
			}
			break;

		default:
			Log.e(TAG, "----------------------> default");
			break;
		}
		Log.e(TAG, "----------------------> result : " + result);
        return result;
    }
    
 
    private static byte[] getBytesFromType(byte type, String val) {
        byte[] retVal = new byte[0];
        if (val != null) {
            byte[] valBytes = val.getBytes(Charset.forName("UTF-8"));
            retVal = new byte[5 + valBytes.length];
            retVal[0] = type;
            retVal[1] = (byte) valBytes.length;
            retVal[2] = (byte) (valBytes.length >> 8 );
            retVal[3] = (byte) (valBytes.length >> 16);
            retVal[4] = (byte) (valBytes.length >> 24);
            System.arraycopy(valBytes, 0, retVal, 5, valBytes.length);
        }
        return retVal;
    }
 
    //用于udp数据包解析
    public static boolean parseDatagramPacket(DatagramPacket dataPacket, byte needCheckPackType, Handler handler) {
    	return parsePack(dataPacket.getData(), dataPacket.getOffset(), needCheckPackType, handler);
    }
    
    //用于tcp数据包解析
    public static boolean parseServiceSocktPackagt(InputStream inputStream, Handler handler) {
    	return parsePack(input2byte(inputStream), 0, PACKET_TYPE_SEND_RECV_DATA, handler);
    }

	private static final byte[] input2byte(InputStream inStream) {
		ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
		byte[] buff = new byte[100];
		int rc = 0;
		try {
			while ((rc = inStream.read(buff, 0, 100)) > 0) {
				swapStream.write(buff, 0, rc);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] in2b = swapStream.toByteArray();
		return in2b;
	}

    /**
     * 获取本机在Wifi中的IP
     */
    private String getOwnWifiIP(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wm.isWifiEnabled()) {
            return "";
        }
 
        // 需加权限：android.permission.ACCESS_WIFI_STATE
        WifiInfo wifiInfo = wm.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        String ipAddr = int2Ip(ipInt);
        Log.i(TAG, "device-@@@zjun: 本机IP=" + ipAddr);
        return int2Ip(ipInt);
    }
 
    /**
     * 把int表示的ip转换成字符串ip
     */
    private String int2Ip(int i) {
        return String.format("%d.%d.%d.%d", i & 0xFF, (i >> 8) & 0xFF, (i >> 16) & 0xFF, (i >> 24) & 0xFF);
    }
}
