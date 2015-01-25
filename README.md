## loServer

基于Netty的Http应用服务器

### 介绍
在之前公司的时候有一些小任务是这样的：写一个小Http接口处理一些任务（这个任务常常只需要几行代码）。然后我就开始写个简单的Servlet，挂在Tomcat上。随着时间的推移，Tomcat上的这种小web项目越来越多，最后要更新一个项目就要重新启动Tomcat（没有设置热启动），非常笨重。领导意思是不要写啥都挂在Tomcat上，最好写的这个功能可以独立运行提供服务。于是需求就来了，找个嵌入式的Servlet容器（记得当时年纪小，摆脱不了Servlet容器了……），貌似满足要求的就是Jetty了，也没折腾，后来就不了了之了。

现在想想面对一些压力并不大的请求，可以自己实现一个Http应用服务器来处理简单的Get和Post请求（就是请求文本，响应的也是文本或json），了解了下Http协议，发现解析起来写的东西很多，而且自己写性能也是大问题（性能这个问题为未来考虑），于是考虑用Netty，发现它有Http相关的实现，便按照Example的指示，自己实现了一个应用服务器。思想很简单，在ServerHandler中拦截请求，把Netty的Request对象转换成我自己实现的Request对象，经过用户的Action对象后，生成自己的Response，最后转换为Netty的Response返回给用户。

这个项目中使用到的Netty版本是4.X，毕竟这个版本比较稳定，而且最近也在不停更新，于是采用了。之后如果有时间，会在项目中添加更多功能，希望它最终成为一个完善的高性能的Http服务器。

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
