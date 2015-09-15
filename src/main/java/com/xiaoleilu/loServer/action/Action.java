package com.xiaoleilu.loServer.action;

import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;

/**
 * 请求处理接口<br>
 * 当用户请求某个Path，则调用相应Action的doAction方法
 * @author Looly
 *
 */
public interface Action {
	public void doAction(Request request, Response response);
}
