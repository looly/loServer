package com.xiaoleilu.loServer.action;

import com.xiaoleilu.loServer.Request;
import com.xiaoleilu.loServer.Response;

public class ExampleAction implements Action{

	@Override
	public void doAction(Request request, Response response) {
		String a = request.getParam("a");
		response.setContent("response a: " + a);
	}

}
