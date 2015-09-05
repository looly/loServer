package com.xiaoleilu.loServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.xiaoleilu.hutool.CharsetUtil;
import com.xiaoleilu.hutool.CollectionUtil;
import com.xiaoleilu.hutool.Conver;
import com.xiaoleilu.hutool.DateUtil;
import com.xiaoleilu.hutool.Log;
import com.xiaoleilu.hutool.StrUtil;
import com.xiaoleilu.hutool.http.HttpUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

/**
 * Http请求对象
 * @author Looly
 *
 */
public class Request {
	
	public static final String METHOD_DELETE = "DELETE";
	public static final String METHOD_HEAD = "HEAD";
	public static final String METHOD_GET = "GET";
	public static final String METHOD_OPTIONS = "OPTIONS";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_PUT = "PUT";
	public static final String METHOD_TRACE = "TRACE";
	
	private String protocolVersion;
	private String uri;
	private String path;
	private String method;
	private String ip;
	private Map<String, String> headers = new HashMap<String, String>();
	private Map<String, List<String>> params = new HashMap<String, List<String>>();
	private Map<String, Cookie> cookies = new HashMap<String, Cookie>();
	
	/**
	 * 获得版本信息
	 * @return 版本
	 */
	public String getProtocolVersion() {
		return protocolVersion;
	}
	
	/**
	 * 获得URI（带参数的路径）
	 * @return URI
	 */
	public String getUri() {
		return uri;
	}
	
	/**
	 * @return 获得path（不带参数的路径）
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * 获得Http方法
	 * @return Http method
	 */
	public String getMethod() {
		return method;
	}
	
	/**
	 * 获得IP地址
	 * @return IP地址
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * 获得所有头信息
	 * @return 头信息Map
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}
	
	/**
	 * 使用ISO8859_1字符集获得Header内容<br>
	 * 由于Header中很少有中文，故一般情况下无需转码
	 * 
	 * @param headerKey 头信息的KEY
	 * @return 值
	 */
	public String getHeader(String headerKey) {
		return headers.get(headerKey);
	}
	
	/**
	 * @return 是否为普通表单（application/x-www-form-urlencoded）
	 */
	public boolean isXWwwFormUrlencoded() {
		return "application/x-www-form-urlencoded".equals(getHeader("Content-Type"));
	}
	
	/**
	 * 获得指定的Cookie
	 * 
	 * @param name cookie名
	 * @return Cookie对象
	 */
	public Cookie getCookie(String name) {
		return cookies.get(name);
	}
	
	/**
	 * @return 获得所有Cookie信息
	 */
	public Map<String, Cookie> getCookies() {
		return this.cookies;
	}
	
	/**
	 * @return 客户浏览器是否为IE
	 */
	public boolean isIE() {
		String userAgent = getHeader("User-Agent");
		if(StrUtil.isNotBlank(userAgent)) {
			userAgent = userAgent.toUpperCase();
			if(userAgent.contains("MSIE") || userAgent.contains("TRIDENT")) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param name 参数名
	 * @return 获得请求参数
	 */
	public String getParam(String name) {
		final List<String> values = params.get(name);
		if(CollectionUtil.isEmpty(values)) {
			return null;
		}
		return values.get(0);
	}
	
	/**
	 * 获得GET请求参数<br>
	 * 会根据浏览器类型自动识别GET请求的编码方式从而解码<br>
	 * 考虑到Servlet容器中会首先解码，给定的charsetOfServlet就是Servlet设置的解码charset<br>
	 * charsetOfServlet为null则默认的ISO_8859_1
	 * @param name 参数名
	 * @param charsetOfServlet Servlet容器中的字符集
	 * @return 获得请求参数
	 */
	public String getParam(String name, Charset charsetOfServlet) {
		if(null == charsetOfServlet) {
			charsetOfServlet = Charset.forName(CharsetUtil.ISO_8859_1);
		}
		
		String destCharset = CharsetUtil.UTF_8;
		if(isIE()) {
			//IE浏览器GET请求使用GBK编码
			destCharset = CharsetUtil.GBK;
		}
		
		String value = getParam(name);
		if(METHOD_GET.equalsIgnoreCase(method)) {
			value = CharsetUtil.convert(value, charsetOfServlet.toString(), destCharset);
		}
		return value;
	}
	
	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得请求参数
	 */
	public String getParam(String name, String defaultValue) {
		String param = getParam(name);
		return StrUtil.isBlank(param) ? defaultValue : param;
	}
	
	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Integer类型请求参数
	 */
	public Integer getIntParam(String name, Integer defaultValue) {
		return Conver.toInt(getParam(name), defaultValue);
	}
	
	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得long类型请求参数
	 */
	public Long getLongParam(String name, Long defaultValue) {
		return Conver.toLong(getParam(name), defaultValue);
	}
	
	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Double类型请求参数
	 */
	public Double getDoubleParam(String name, Double defaultValue) {
		return Conver.toDouble(getParam(name), defaultValue);
	}
	
	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Float类型请求参数
	 */
	public Float getFloatParam(String name, Float defaultValue) {
		return Conver.toFloat(getParam(name), defaultValue);
	}
	
	/**
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Boolean类型请求参数
	 */
	public Boolean getBoolParam(String name, Boolean defaultValue) {
		return Conver.toBool(getParam(name), defaultValue);
	}
	
	/**
	 * 格式：<br>
	* 1、yyyy-MM-dd HH:mm:ss <br>
	* 2、yyyy-MM-dd <br>
	* 3、HH:mm:ss <br>
	* 
	 * @param name 参数名
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Date类型请求参数，默认格式：
	 */
	public Date getDateParam(String name, Date defaultValue) {
		String param = getParam(name);
		return StrUtil.isBlank(param) ? defaultValue : DateUtil.parse(param);
	}
	
	/**
	 * @param name 参数名
	 * @param format 格式
	 * @param defaultValue 当客户端未传参的默认值
	 * @return 获得Date类型请求参数
	 */
	public Date getDateParam(String name, String format, Date defaultValue) {
		String param = getParam(name);
		return StrUtil.isBlank(param) ? defaultValue : DateUtil.parse(param, format);
	}

	/**
	 * 获得请求参数<br>
	 * 数组类型值，常用于表单中的多选框
	 * 
	 * @param name 参数名
	 * @return 数组
	 */
	public List<String> getArrayParam(String name) {
		return params.get(name);
	}

	/**
	 * 获得所有请求参数
	 * 
	 * @return Map
	 */
	public Map<String, List<String>> getParams() {
		return params;
	}
	
	/**
	 * @return 是否为长连接
	 */
	public boolean isKeepAlive() {
		final String connectionHeader = getHeader(Names.CONNECTION);
		//无论任何版本Connection为close时都关闭连接
		if(Values.CLOSE.equalsIgnoreCase(connectionHeader)) {
			return false;
		}
		
		//HTTP/1.0只有Connection为Keep-Alive时才会保持连接
		if(HttpVersion.HTTP_1_0.text().equals(getProtocolVersion())) {
			if(false == Values.KEEP_ALIVE.equalsIgnoreCase(connectionHeader)) {
				return false;
			}
		}
		//HTTP/1.1默认打开Keep-Alive
		return true;
	}
	
	//--------------------------------------------------------- Protected method start
	/**
	 * 填充参数
	 * @param decoder QueryStringDecoder
	 */
	protected void putParams(QueryStringDecoder decoder) {
		if(null != decoder) {
			for (Entry<String, List<String>> entry : decoder.parameters().entrySet()) {
				this.params.put(entry.getKey(), entry.getValue());
			}
		}
	}
	
	/**
	 * 填充头部信息和Cookie信息
	 * @param headers HttpHeaders
	 */
	protected void putHeadersAndCookies(HttpHeaders headers) {
		for (Entry<String, String> entry : headers) {
			this.headers.put(entry.getKey(), entry.getValue());
		}
		
		//Cookie
		final String cookieString =this.headers.get(Names.COOKIE);
		if(StrUtil.isNotBlank(cookieString)) {
			final Set<Cookie> cookies = ServerCookieDecoder.LAX .decode(cookieString);
			for (Cookie cookie : cookies) {
				this.cookies.put(cookie.name(), cookie);
			}
		}
	}
	//--------------------------------------------------------- Protected method end
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("\r\nprotocolVersion: ").append(protocolVersion).append("\r\n");
		sb.append("uri: ").append(uri).append("\r\n");
		sb.append("path: ").append(path).append("\r\n");
		sb.append("method: ").append(method).append("\r\n");
		sb.append("ip: ").append(ip).append("\r\n");
		sb.append("headers: ").append(headers).append("\r\n");
		sb.append("params: \r\n");
		for ( Entry<String, List<String>> entry : params.entrySet()) {
			sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
		}
		
		return sb.toString();
	}
	
	/**
	 * 构建Request对象
	 * @param ctx ChannelHandlerContext
	 * @param nettyRequest Netty的HttpRequest
	 * @return Request
	 */
	protected final static Request build(ChannelHandlerContext ctx, HttpRequest nettyRequest) {
		final Request request = new Request();

		//request basic
		request.uri = nettyRequest.getUri();
		request.path = getPath(request.uri);
		request.protocolVersion = nettyRequest.getProtocolVersion().text();
		request.method = nettyRequest.getMethod().name();
		
		//request headers
		request.putHeadersAndCookies(nettyRequest.headers());

		//request URI parameters
		request.putParams(new QueryStringDecoder(request.uri));
		
		//IP
		String ip = request.getHeader("X-Forwarded-For");
		if(StrUtil.isNotBlank(ip)){
			ip = HttpUtil.getMultistageReverseProxyIp(ip);
		}else{
			final InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
			ip = insocket.getAddress().getHostAddress();
		}
		request.ip = ip;
		
		return request;
	}
	
	/**
	 * 从uri中获得path
	 * @param uriStr uri
	 * @return path
	 */
	public final static String getPath(String uriStr) {
		URI uri = null;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			Log.error("", e);
			return null;
		}
		
		return uri.getPath();
	}
}
