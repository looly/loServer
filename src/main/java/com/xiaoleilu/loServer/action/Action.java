package com.xiaoleilu.loServer.action;

import com.xiaoleilu.loServer.Request;
import com.xiaoleilu.loServer.Response;

public interface Action {
	public void doAction(Request request, Response response);
}
