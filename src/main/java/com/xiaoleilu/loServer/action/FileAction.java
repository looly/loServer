package com.xiaoleilu.loServer.action;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.xiaoleilu.hutool.DateUtil;
import com.xiaoleilu.hutool.FileUtil;
import com.xiaoleilu.hutool.Log;
import com.xiaoleilu.hutool.ReUtil;
import com.xiaoleilu.hutool.StrUtil;
import com.xiaoleilu.loServer.ServerSetting;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;

import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 默认的主页Action，当访问主页且没有定义主页Action时，调用此Action
 * 
 * @author Looly
 *
 */
public class FileAction implements Action {
	private static final Logger log = Log.get();

	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
	private static final SimpleDateFormat HTTP_DATE_FORMATER = new SimpleDateFormat(DateUtil.HTTP_DATETIME_PATTERN, Locale.US);

	@Override
	public void doAction(Request request, Response response) {
		if (false == Request.METHOD_GET.equalsIgnoreCase(request.getMethod())) {
			response.sendError(HttpResponseStatus.METHOD_NOT_ALLOWED, "Please use GET method to request file!");
			return;
		}
		
		if(ServerSetting.isRootAvailable() == false){
			response.sendError(HttpResponseStatus.NOT_FOUND, "404 Root dir not avaliable!");
			return;
		}

		final File file = getFileByPath(request.getPath());
		log.debug("Client [{}] get file [{}]", request.getIp(), file.getPath());
		
		// 隐藏文件，跳过
		if (file.isHidden() || !file.exists()) {
			response.sendError(HttpResponseStatus.NOT_FOUND, "404 File not found!");
			return;
		}

		// 非文件，跳过
		if (false == file.isFile()) {
			response.sendError(HttpResponseStatus.FORBIDDEN, "403 Forbidden!");
			return;
		}

		// Cache Validation
		String ifModifiedSince = request.getHeader(Names.IF_MODIFIED_SINCE);
		if (StrUtil.isNotBlank(ifModifiedSince)) {
			Date ifModifiedSinceDate = null;
			try {
				ifModifiedSinceDate = DateUtil.parse(ifModifiedSince, HTTP_DATE_FORMATER);
			} catch (Exception e) {
				log.warn("If-Modified-Since header parse error: {}", e.getMessage());
			}
			if(ifModifiedSinceDate != null) {
				// 只对比到秒一级别
				long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
				long fileLastModifiedSeconds = file.lastModified() / 1000;
				if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
					log.debug("File {} not modified.", file.getPath());
					response.sendNotModified();
					return;
				}
			}
		}
		
		try {
			response.sendFile(file);
		} catch (IOException e) {
			String msg = StrUtil.format("Can not get file {}", file.getName());
			log.error(msg, e);
			response.sendError(HttpResponseStatus.FORBIDDEN, msg);
		}
	}
	
	/**
	 * 通过URL中的path获得文件的绝对路径
	 * 
	 * @param httpPath Http请求的Path
	 * @return 文件绝对路径
	 */
	public static File getFileByPath(String httpPath) {
		// Decode the path.
		try {
			httpPath = URLDecoder.decode(httpPath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new Error(e);
		}

		if (httpPath.isEmpty() || httpPath.charAt(0) != '/') {
			return null;
		}

		// 路径安全检查
		if (httpPath.contains("/.") || httpPath.contains("./") || httpPath.charAt(0) == '.' || httpPath.charAt(httpPath.length() - 1) == '.' || ReUtil.isMatch(INSECURE_URI, httpPath)) {
			return null;
		}

		// 转换为绝对路径
		return FileUtil.file(ServerSetting.getRoot(), httpPath);
	}
}