## loServer

基于Netty的Http应用服务器

Netty版本是4.X

### 使用方法
1. 新建一个类实现Action接口，例如我新建了一个ExampleAction
2. 调用ServerSetting.addAction("/example", ExampleAction.class);增加请求路径和Action的映射关系
3. ServerSetting.setPort(8090);设置监听端口
4. LoServer.start();启动服务
5. 在浏览器中访问http://localhost:8090/example既可

### 代码

	package com.xiaoleilu.loServer.example;

	import com.xiaoleilu.loServer.LoServer;
	import com.xiaoleilu.loServer.Request;
	import com.xiaoleilu.loServer.Response;
	import com.xiaoleilu.loServer.ServerSetting;

	/**
	 * loServer样例程序<br>
	 * Action对象用于处理业务流程，类似于Servlet对象<br>
	 * 在启动服务器前必须将path和此Action加入到ServerSetting的ActionMap中<br>
	 * 使用ServerSetting.setPort方法设置监听端口，此处设置为8090（如果不设置则使用默认的8090端口）
	 * 然后调用LoServer.start()启动服务<br>
	 * 在浏览器中访问http://localhost:8090/example?a=b既可在页面上显示response a: b
	 * @author Looly
	 *
	 */
	public class ExampleAction implements Action{

		@Override
		public void doAction(Request request, Response response) {
			String a = request.getParam("a");
			response.setContent("response a: " + a);
		}

		public static void main(String[] args) {
			ServerSetting.addAction("/example", ExampleAction.class);
			ServerSetting.setPort(8090);
			LoServer.start();
		}
	}
