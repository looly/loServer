package com.xiaoleilu.loServer;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;

import org.slf4j.Logger;

import com.xiaoleilu.hutool.Log;
import com.xiaoleilu.hutool.StrUtil;
import com.xiaoleilu.loServer.action.Action;

/**
 * 服务处理
 * @author Looly
 *
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {
	private final static Logger log = Log.get();
	
	private Request request;
	private Response response = Response.build();
	private Action action;
	private boolean isPass;	//跳过不在ActionMap里的请求

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if(isPass) {
			isPass = false; //归位
			return;
		}
		
		if(msg instanceof HttpRequest) {
			handleHttpRequest(ctx, (HttpRequest)msg);
		}else if(msg instanceof HttpContent) {
			handleHttpContent(ctx, (HttpContent)msg);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.error(cause.getMessage(), cause);
		ctx.close();
	}

	/**
	 * 处理Http消息
	 * @param ctx ChannelHandlerContext
	 * @param nettyRequest 请求消息对象
	 */
	private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest nettyRequest) {
		final String path = Request.getPath(nettyRequest.getUri());
		action = ServerSetting.getActionMap().get(path);
		if(null == action) {
			if(StrUtil.isBlank(path) || StrUtil.SLASH.equals(path)) {
				request = Request.build(ctx, nettyRequest);
				//如果未设置根路径，跳转到主页
				response.setContent("Welcome to LoServer.");
			}else {
				log.debug("Pass [{}]", path);
			}
			
			//无对应的Action，Pass掉
			isPass = true;
			writeResponse(ctx);
		}else{
			// 状态100 continue
			if (HttpHeaders.is100ContinueExpected(nettyRequest)) {
				ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
			}
			
			// 构建Request
			request = Request.build(ctx, nettyRequest);
		}
	}
	
	/**
	 * 处理HttpContent内容
	 * @param ctx ChannelHandlerContext
	 * @param HttpContent httpContent
	 */
	private void handleHttpContent(ChannelHandlerContext ctx, HttpContent nettyContent) {
		if(null == request) {
			//有些request跳过了，在此content忽略
			return;
		}
		
		//Post提交表单时只处理非上传文件表单
		if(Request.METHOD_POST.equals(request.getMethod()) && request.isXWwwFormUrlencoded()) {
			final String content = nettyContent.content().toString(ServerSetting.charset());
			QueryStringDecoder decoder = new QueryStringDecoder(content, ServerSetting.charset(), false);
			//加入post中的参数
			request.putParams(decoder);
		}
		
		//最后一个Http包
		if(nettyContent instanceof LastHttpContent) {
			action.doAction(request, response);
			
			writeResponse(ctx);
		}
	}
	
	/**
	 * 将Response写入到客户端
	 * @param ctx ChannelHandlerContext
	 */
	private void writeResponse(ChannelHandlerContext ctx) {
		if(null == request) {
			//request为空表示请求不需要处理，直接关闭连接
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			response.setContent("404 Not Found!");
		}
		FullHttpResponse fullHttpResponse = response.toFullHttpResponse();
		if (null != request && request.isKeepAlive()) {
			fullHttpResponse.headers().set(Names.CONNECTION, Values.KEEP_ALIVE);
			ctx.write(fullHttpResponse);
		} else {
			ctx.write(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
