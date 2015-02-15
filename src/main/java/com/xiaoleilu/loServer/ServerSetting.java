package com.xiaoleilu.loServer;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.xiaoleilu.hutool.ClassUtil;
import com.xiaoleilu.hutool.Log;
import com.xiaoleilu.hutool.StrUtil;
import com.xiaoleilu.loServer.action.Action;

/**
 * 全局设定文件
 * @author xiaoleilu
 *
 */
public class ServerSetting {
	private static Logger log = Log.get();
	
	//-------------------------------------------------------- Default value start
	/** 默认的字符集编码 */
	public final static String DEFAULT_CHARSET = "utf-8";
	//-------------------------------------------------------- Default value end
	
	/** 字符编码 */
	private static String charset = DEFAULT_CHARSET;
	/** 端口 */
	private static int port = 8090;
	/** Action映射表 */
	private static Map<String, Action> actionMap = new ConcurrentHashMap<String, Action>();
	
	/**
	 * @return 获取编码
	 */
	public static String getCharset() {
		return charset;
	}
	/**
	 * 设置编码
	 * @param charset 编码
	 */
	public static void setCharset(String charset) {
		ServerSetting.charset = charset;
	}
	
	/**
	 * @return 监听端口
	 */
	public static int getPort() {
		return port;
	}
	/**
	 * 设置监听端口
	 * @param port 端口
	 */
	public static void setPort(int port) {
		ServerSetting.port = port;
	}
	/**
	 * @return 获取ActionMap
	 */
	public static Map<String, Action> getActionMap() {
		return actionMap;
	}
	/**
	 * 设置ActionMap
	 * @param actionMap ActionMap
	 */
	public static void setActionMap(Map<String, Action> actionMap) {
		ServerSetting.actionMap = actionMap;
	}
	
	/**
	 * 增加Action类
	 * @param path 拦截路径（必须以"/"开头）
	 * @param action Action类
	 */
	public static void addAction(String path, Action action) {
		if(StrUtil.isBlank(path) || null == action) {
			log.warn("Added blank action, pass it.");
			return;
		}
		//所有路径必须以 "/" 开头，如果没有则补全之
		if(false == path.startsWith(StrUtil.SLASH)) {
			path = StrUtil.SLASH + path;
		}
		
		ServerSetting.actionMap.put(path, action);
	}
	
	/**
	 * 增加Action类
	 * @param path 拦截路径（必须以"/"开头）
	 * @param actionClass Action类
	 */
	public static void addAction(String path, Class<? extends Action> actionClass) {
		addAction(path, (Action)ClassUtil.newInstance(actionClass));
	}
	
	/**
	 * @return 字符集
	 */
	public static Charset charset() {
		return Charset.forName(charset);
	}
}
