package com.xiaoleilu.loServer.handler;

import java.io.IOException;

import com.xiaoleilu.hutool.Log;
import com.xiaoleilu.hutool.Singleton;
import com.xiaoleilu.hutool.log.LogWrapper;
import com.xiaoleilu.loServer.ServerSetting;
import com.xiaoleilu.loServer.action.Action;
import com.xiaoleilu.loServer.action.FileAction;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Action处理单元
 * 
 * @author Looly
 */
public class ActionHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private final static LogWrapper log = Log.get();

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {

		final Request request = Request.build(ctx, fullHttpRequest);
		final Response response = Response.build(ctx, request);

		Action action = ServerSetting.getAction(request.getPath());
		if (null == action) {
			// 非Action方法，调用静态文件读取
			action = Singleton.get(FileAction.class);
		}

		action.doAction(request, response);
		
		//如果发送请求未被出发，则出发之，否则执行一次触发
		if(false ==response.isSent()){
			response.send();
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(cause instanceof IOException){
			log.warn("{}", cause);
		}else{
			super.exceptionCaught(ctx, cause);
		}
	}
}
