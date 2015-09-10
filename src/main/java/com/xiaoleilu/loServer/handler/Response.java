package com.xiaoleilu.loServer.handler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;

import com.xiaoleilu.hutool.CharsetUtil;
import com.xiaoleilu.hutool.DateUtil;
import com.xiaoleilu.hutool.FileUtil;
import com.xiaoleilu.hutool.Log;
import com.xiaoleilu.hutool.StrUtil;
import com.xiaoleilu.hutool.http.HttpUtil;
import com.xiaoleilu.loServer.ServerSetting;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

/**
 * 响应对象
 * 
 * @author Looly
 *
 */
public class Response {
	private final static Logger log = Log.get();

	/** 返回内容类型：普通文本 */
	public final static String CONTENT_TYPE_TEXT = "text/plain";
	/** 返回内容类型：HTML */
	public final static String CONTENT_TYPE_HTML = "text/html";
	/** 返回内容类型：XML */
	public final static String CONTENT_TYPE_XML = "text/xml";
	/** 返回内容类型：JAVASCRIPT */
	public final static String CONTENT_TYPE_JAVASCRIPT = "application/javascript";
	/** 返回内容类型：JSON */
	public final static String CONTENT_TYPE_JSON = "application/json";
	public final static String CONTENT_TYPE_JSON_IE = "text/json";

	private ChannelHandlerContext ctx;
	private Request request;

	private HttpVersion httpVersion = HttpVersion.HTTP_1_1;
	private HttpResponseStatus status = HttpResponseStatus.OK;
	private String contentType = CONTENT_TYPE_TEXT;
	private String charset = ServerSetting.getCharset();
	private HttpHeaders headers = new DefaultHttpHeaders();
	private Set<Cookie> cookies = new HashSet<Cookie>();
	private ByteBuf content = Unpooled.EMPTY_BUFFER;

	public Response(ChannelHandlerContext ctx, Request request) {
		this.ctx = ctx;
		this.request = request;
	}

	/**
	 * 设置响应的Http版本号
	 * 
	 * @param httpVersion http版本号对象
	 * @return 自己
	 */
	public Response setHttpVersion(HttpVersion httpVersion) {
		this.httpVersion = httpVersion;
		return this;
	}

	/**
	 * 响应状态码<br>
	 * 使用io.netty.handler.codec.http.HttpResponseStatus对象
	 * 
	 * @param status 状态码
	 * @return 自己
	 */
	public Response setStatus(HttpResponseStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * 响应状态码
	 * 
	 * @param status 状态码
	 * @return 自己
	 */
	public Response setStatus(int status) {
		return setStatus(HttpResponseStatus.valueOf(status));
	}

	/**
	 * 设置Content-Type
	 * 
	 * @param contentType Content-Type
	 * @return 自己
	 */
	public Response setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	/**
	 * 设置返回内容的字符集编码
	 * 
	 * @param charset 编码
	 * @return 自己
	 */
	public Response setCharset(String charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * 增加响应的Header<br>
	 * 重复的Header将被叠加
	 * 
	 * @param name 名
	 * @param value 值，可以是String，Date， int
	 * @return 自己
	 */
	public Response addHeader(String name, Object value) {
		headers.add(name, value);
		return this;
	}

	/**
	 * 设置响应的Header<br>
	 * 重复的Header将被替换
	 * 
	 * @param name 名
	 * @param value 值，可以是String，Date， int
	 * @return 自己
	 */
	public Response setHeader(String name, Object value) {
		headers.set(name, value);
		return this;
	}

	/**
	 * 设置响应体长度
	 * 
	 * @param contentLength 响应体长度
	 * @return 自己
	 */
	public Response setContentLength(long contentLength) {
		setHeader(Names.CONTENT_LENGTH, contentLength);
		return this;
	}

	/**
	 * 设置是否长连接
	 * 
	 * @return 自己
	 */
	public Response setKeepAlive() {
		setHeader(Names.CONNECTION, Values.KEEP_ALIVE);
		return this;
	}

	// --------------------------------------------------------- Cookie start
	/**
	 * 设定返回给客户端的Cookie
	 * 
	 * @param cookie
	 * @return 自己
	 */
	public Response addCookie(Cookie cookie) {
		cookies.add(cookie);
		return this;
	}

	/**
	 * 设定返回给客户端的Cookie
	 * 
	 * @param name Cookie名
	 * @param value Cookie值
	 * @return 自己
	 */
	public Response addCookie(String name, String value) {
		return addCookie(new DefaultCookie(name, value));
	}

	/**
	 * 设定返回给客户端的Cookie
	 * 
	 * @param name cookie名
	 * @param value cookie值
	 * @param maxAgeInSeconds -1: 关闭浏览器清除Cookie. 0: 立即清除Cookie. n>0 : Cookie存在的秒数.
	 * @param path Cookie的有效路径
	 * @param domain the Cookie可见的域，依据 RFC 2109 标准
	 * @return 自己
	 */
	public Response addCookie(String name, String value, int maxAgeInSeconds, String path, String domain) {
		Cookie cookie = new DefaultCookie(name, value);
		if (domain != null) {
			cookie.setDomain(domain);
		}
		cookie.setMaxAge(maxAgeInSeconds);
		cookie.setPath(path);
		return addCookie(cookie);
	}

	/**
	 * 设定返回给客户端的Cookie<br>
	 * Path: "/"<br>
	 * No Domain
	 * 
	 * @param name cookie名
	 * @param value cookie值
	 * @param maxAgeInSeconds -1: 关闭浏览器清除Cookie. 0: 立即清除Cookie. n>0 : Cookie存在的秒数.
	 * @return 自己
	 */
	public Response addCookie(String name, String value, int maxAgeInSeconds) {
		return addCookie(name, value, maxAgeInSeconds, "/", null);
	}
	// --------------------------------------------------------- Cookie end

	/**
	 * 设置响应文本内容
	 * 
	 * @param contentText 响应的文本
	 * @return 自己
	 */
	public Response setContent(String contentText) {
		this.content = Unpooled.copiedBuffer(contentText, Charset.forName(charset));
		return this;
	}

	/**
	 * 设置响应文本内容
	 * 
	 * @param contentBytes 响应的字节
	 * @return 自己
	 */
	public Response setContent(byte[] contentBytes) {
		return setContent(Unpooled.copiedBuffer(contentBytes));
	}

	/**
	 * 设置响应文本内容
	 * 
	 * @param contentBytes 响应的字节
	 * @return 自己
	 */
	public Response setContent(ByteBuf byteBuf) {
		this.content = byteBuf;
		return this;
	}

	/**
	 * Sets the Date and Cache headers for the HTTP Response
	 *
	 * @param response HTTP response
	 * @param fileToCache file to extract content type
	 */
	/**
	 * 设置日期和过期时间
	 * 
	 * @param lastModify 上一次修改时间
	 * @param httpCacheSeconds 缓存时间，单位秒
	 */
	public void setDateAndCache(long lastModify, int httpCacheSeconds) {
		SimpleDateFormat formatter = new SimpleDateFormat(DateUtil.HTTP_DATETIME_PATTERN, Locale.US);
		formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

		// Date header
		Calendar time = new GregorianCalendar();
		setHeader(Names.DATE, formatter.format(time.getTime()));

		// Add cache headers
		time.add(Calendar.SECOND, httpCacheSeconds);

		setHeader(Names.EXPIRES, formatter.format(time.getTime()));
		setHeader(Names.CACHE_CONTROL, "private, max-age=" + httpCacheSeconds);
		setHeader(Names.LAST_MODIFIED, formatter.format(DateUtil.date(lastModify)));
	}

	// -------------------------------------------------------------------------------------- build HttpResponse start
	/**
	 * 转换为Netty所用Response<br>
	 * 不包括content，一般用于返回文件类型的响应
	 * 
	 * @return DefaultHttpResponse
	 */
	protected DefaultHttpResponse toDefaultHttpResponse() {
		final DefaultHttpResponse defaultHttpResponse = new DefaultHttpResponse(httpVersion, status);

		// headers
		final HttpHeaders httpHeaders = defaultHttpResponse.headers().add(headers);

		// Cookies
		for (Cookie cookie : cookies) {
			httpHeaders.add(Names.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
		}

		return defaultHttpResponse;
	}

	/**
	 * 转换为Netty所用Response<br>
	 * 用于返回一般类型响应（文本）
	 * 
	 * @return FullHttpResponse
	 */
	protected FullHttpResponse toFullHttpResponse() {
		final FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpVersion, status, content);

		// headers
		final HttpHeaders httpHeaders = fullHttpResponse.headers().add(headers);
		httpHeaders.set(Names.CONTENT_TYPE, StrUtil.format("{};charset={}", contentType, charset));
		httpHeaders.set(Names.CONTENT_ENCODING, charset);
		httpHeaders.set(Names.CONTENT_LENGTH, content.readableBytes());

		// Cookies
		for (Cookie cookie : cookies) {
			httpHeaders.add(Names.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
		}

		return fullHttpResponse;
	}
	// -------------------------------------------------------------------------------------- build HttpResponse end

	// -------------------------------------------------------------------------------------- send start
	/**
	 * 发送响应到客户端
	 * 
	 * @return ChannelFuture
	 */
	public ChannelFuture send() {
		if (request != null && request.isKeepAlive()) {
			setKeepAlive();
			return ctx.writeAndFlush(this.toFullHttpResponse());
		} else {
			return sendAndClose();
		}
	}

	/**
	 * 发送给到客户端并关闭ChannelHandlerContext
	 * 
	 * @return ChannelFuture
	 */
	public ChannelFuture sendAndClose() {
		return ctx.writeAndFlush(this.toFullHttpResponse()).addListener(ChannelFutureListener.CLOSE);
	}
	// -------------------------------------------------------------------------------------- send end

	// ---------------------------------------------------------------------------- special response start
	/**
	 * 发送文件
	 * 
	 * @param file 文件
	 * @throws IOException
	 */
	public void sendFile(File file) throws IOException {
		final RandomAccessFile raf = new RandomAccessFile(file, "r");
		long fileLength = raf.length();

		// 发送头
		setContentLength(fileLength);
		String contentType = HttpUtil.getMimeType(file.getName());
		if(StrUtil.isBlank(contentType)){
			//无法识别默认使用数据流
			contentType = "application/octet-stream";
		}
		setContentType(contentType);
		log.debug("Content-Type: {}, Content-Length: {}", contentType, fileLength);

		ctx.write(this.toDefaultHttpResponse());

		ctx
			.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise())
			.addListener(new ChannelProgressiveFutureListener(){
				@Override
				public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
					log.debug("Transfer progress: {} / {}", progress, total);
				}
	
				@Override
				public void operationComplete(ChannelProgressiveFuture future) {
					FileUtil.close(raf);
					log.debug("Transfer complete.");
				}
			});

		final ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		if (false == request.isKeepAlive()) {
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	/**
	 * 302 重定向
	 * 
	 * @param uri 重定向到的URI
	 */
	public void sendRedirect(String uri) {
		this.setStatus(HttpResponseStatus.FOUND).setHeader(Names.LOCATION, uri).send();
	}

	/**
	 * 304 文件未修改
	 * 
	 * @param uri 重定向到的URI
	 */
	public void sendNotModified() {
		this.setStatus(HttpResponseStatus.NOT_MODIFIED).setHeader(Names.DATE, DateUtil.formatHttpDate(DateUtil.date())).send();
	}

	/**
	 * 发送错误消息
	 * 
	 * @param status 错误状态码
	 * @param msg 消息内容
	 */
	public void sendError(HttpResponseStatus status, String msg) {
		if (ctx.channel().isActive()) {
			this.setStatus(status).setContent(msg).send();
		}
	}

	/**
	 * 发送404 Not Found
	 * 
	 * @param msg 消息内容
	 */
	public void sendNotFound(String msg) {
		sendError(HttpResponseStatus.NOT_FOUND, msg);
	}

	/**
	 * 发送500 Internal Server Error
	 * 
	 * @param msg 消息内容
	 */
	public void sendServerError(String msg) {
		sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, msg);
	}

	// ---------------------------------------------------------------------------- special response end

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("headers: ").append(headers).append("\r\n");
		sb.append("content: ").append(StrUtil.str(content, CharsetUtil.UTF_8));

		return sb.toString();
	}

	// ---------------------------------------------------------------------------- static method start
	/**
	 * 构建Response对象
	 * 
	 * @param ctx ChannelHandlerContext
	 * @param request 请求对象
	 * @return Response对象
	 */
	protected static Response build(ChannelHandlerContext ctx, Request request) {
		return new Response(ctx, request);
	}

	/**
	 * 构建Response对象，Request对象为空，将无法获得某些信息<br>
	 * 1. 无法使用长连接
	 * 
	 * @param ctx ChannelHandlerContext
	 * @return Response对象
	 */
	protected static Response build(ChannelHandlerContext ctx) {
		return new Response(ctx, null);
	}
	// ---------------------------------------------------------------------------- static method end
}
