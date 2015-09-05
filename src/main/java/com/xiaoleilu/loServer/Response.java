package com.xiaoleilu.loServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.HttpHeaders.Names;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import com.xiaoleilu.hutool.StrUtil;

/**
 * 响应对象
 * @author Looly
 *
 */
public class Response {
	
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
	
	private HttpVersion httpVersion = HttpVersion.HTTP_1_1;
	private HttpResponseStatus status = HttpResponseStatus.OK;
	private String contentType = CONTENT_TYPE_TEXT;
	private String charset = ServerSetting.getCharset();
	private HttpHeaders headers = new DefaultHttpHeaders();
	private Set<Cookie> cookies = new HashSet<Cookie>();
	private ByteBuf content = Unpooled.EMPTY_BUFFER;
	
	/**
	 * 设置响应的Http版本号
	 * @param httpVersion http版本号对象
	 */
	public void setHttpVersion(HttpVersion httpVersion) {
		this.httpVersion = httpVersion;
	}
	
	/**
	 * 响应状态码<br>
	 * 使用io.netty.handler.codec.http.HttpResponseStatus对象
	 * @param status 状态码
	 */
	public void setStatus(HttpResponseStatus status) {
		this.status = status;
	}
	
	/**
	 * 设置Content-Type
	 * @param contentType Content-Type
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	/**
	 * 响应状态码
	 * @param status 状态码
	 */
	public void setStatus(int status) {
		setStatus(HttpResponseStatus.valueOf(status));
	}
	
	/**
	 * 设置返回内容的字符集编码
	 * @param charset 编码
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}
	
	/**
	 * 设置响应的Header
	 * @param name 名
	 * @param value 值，可以是String，Date， int
	 */
	public void addHeader(String name, Object value) {
		headers.add(name, value);
	}
	
	// --------------------------------------------------------- Cookie start
		/**
		 * 设定返回给客户端的Cookie
		 * 
		 * @param cookie
		 */
		public void addCookie(Cookie cookie) {
			cookies.add(cookie);
		}

		/**
		 * 设定返回给客户端的Cookie
		 * 
		 * @param name Cookie名
		 * @param value Cookie值
		 */
		public void addCookie(String name, String value) {
			addCookie(new DefaultCookie(name, value));
		}

		/**
		 * 设定返回给客户端的Cookie
		 * 
		 * @param name cookie名
		 * @param value cookie值
		 * @param maxAgeInSeconds -1: 关闭浏览器清除Cookie. 0: 立即清除Cookie. n>0 : Cookie存在的秒数.
		 * @param path Cookie的有效路径
		 * @param domain the domain name within which this cookie is visible; form is according to RFC 2109
		 */
		public void addCookie(String name, String value, int maxAgeInSeconds, String path, String domain) {
			Cookie cookie = new DefaultCookie(name, value);
			if (domain != null) {
				cookie.setDomain(domain);
			}
			cookie.setMaxAge(maxAgeInSeconds);
			cookie.setPath(path);
			addCookie(cookie);
		}

		/**
		 * 设定返回给客户端的Cookie<br>
		 * Path: "/"<br>
		 * No Domain
		 * 
		 * @param name cookie名
		 * @param value cookie值
		 * @param maxAgeInSeconds -1: 关闭浏览器清除Cookie. 0: 立即清除Cookie. n>0 : Cookie存在的秒数.
		 */
		public void addCookie(String name, String value, int maxAgeInSeconds) {
			addCookie(name, value, maxAgeInSeconds, "/", null);
		}
		// --------------------------------------------------------- Cookie end
		
		/**
		 * 设置响应文本内容
		 * @param contentText 响应的文本
		 */
		public void setContent(String contentText) {
			this.content = Unpooled.copiedBuffer(contentText, Charset.forName(charset));
		}
		
		/**
		 * 设置响应文本内容
		 * @param contentBytes 响应的字节
		 */
		public void setContent(byte[] contentBytes) {
			this.content = Unpooled.copiedBuffer(contentBytes);
		}
		
		/**
		 * 转换为Netty所用Response
		 * @return FullHttpResponse
		 */
		protected FullHttpResponse toFullHttpResponse() {
			final FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpVersion, status, content);
			
			//headers
			final HttpHeaders httpHeaders = fullHttpResponse.headers().add(headers);
			httpHeaders.set(Names.CONTENT_TYPE, StrUtil.format("{};charset={}", contentType, charset));
			httpHeaders.set(Names.CONTENT_ENCODING, charset);
			httpHeaders.set(Names.CONTENT_LENGTH, content.readableBytes());
			
			//Cookies
			for (Cookie cookie : cookies) {
				httpHeaders.add(Names.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
			}
			
			return fullHttpResponse;
		}
		
		/**
		 * 构建Response对象
		 * @return Response对象
		 */
		protected static Response build() {
			return new Response();
		}
}
