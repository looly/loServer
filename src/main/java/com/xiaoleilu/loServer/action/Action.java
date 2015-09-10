package com.xiaoleilu.loServer.action;

import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;

public interface Action {
	public void doAction(Request request, Response response);
}
