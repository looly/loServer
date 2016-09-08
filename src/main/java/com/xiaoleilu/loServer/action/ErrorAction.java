package com.xiaoleilu.loServer.action;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.StaticLog;
import com.xiaoleilu.hutool.util.StrUtil;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 错误堆栈Action类
 * @author Looly
 *
 */
public class ErrorAction implements Action{
	private static final Log log = StaticLog.get();
	
	public final static String ERROR_PARAM_NAME = "_e";
	
	private final static String TEMPLATE_ERROR = "<!DOCTYPE html><html><head><title>LoServer - Error report</title><style>h1,h3 {color:white; background-color: gray;}</style></head><body><h1>HTTP Status {} - {}</h1><hr size=\"1\" noshade=\"noshade\" /><p>{}</p><hr size=\"1\" noshade=\"noshade\" /><h3>LoServer</h3></body></html>";

	@Override
	public void doAction(Request request, Response response) {
		Object eObj = request.getObjParam(ERROR_PARAM_NAME);
		if(eObj == null){
			response.sendError(HttpResponseStatus.NOT_FOUND, "404 File not found!");
			return;
		}
		
		if(eObj instanceof Exception){
			Exception e = (Exception)eObj;
			log.error("Server action internal error!", e);
			final StringWriter writer = new StringWriter();
			// 把错误堆栈储存到流中
			e.printStackTrace(new PrintWriter(writer));
			String content = writer.toString().replace("\tat", "&nbsp;&nbsp;&nbsp;&nbsp;\tat");
			content = content.replace("\n", "<br/>\n");
			content = StrUtil.format(TEMPLATE_ERROR, 500, request.getUri(), content);
			
			response.sendServerError(content);
		}
	}

}