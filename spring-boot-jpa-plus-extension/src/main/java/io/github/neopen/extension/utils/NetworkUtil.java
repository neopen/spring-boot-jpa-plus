package io.github.neopen.extension.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 网络请求信息工具。
 */
public class NetworkUtil {

    /** 当前工具类的日志记录器。 */
    private static Logger logger = LoggerFactory.getLogger(NetworkUtil.class);

    /**
     * 获取请求客户端的 IP 地址。
     *
     * <p>依次检查常见代理请求头，无法从请求头获取时使用远程地址。
     * 转发地址包含多个值时取第一个非 {@code unknown} 值。</p>
     *
     * @param request 当前 HTTP 请求
     * @return 客户端 IP 地址
     * @throws IOException 获取请求地址过程中发生 I/O 错误时抛出
     */
	public final static String getIpAddress(HttpServletRequest request) throws IOException {
        String ip = request.getHeader("X-Forwarded-For");
		if (logger.isInfoEnabled()) {
			logger.info("getIpAddress(HttpServletRequest) - X-Forwarded-For - String ip=" + ip);
		}

		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("Proxy-Client-IP");
				if (logger.isInfoEnabled()) {
					logger.info("getIpAddress(HttpServletRequest) - Proxy-Client-IP - String ip=" + ip);
				}
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("WL-Proxy-Client-IP");
				if (logger.isInfoEnabled()) {
					logger.info("getIpAddress(HttpServletRequest) - WL-Proxy-Client-IP - String ip=" + ip);
				}
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("HTTP_CLIENT_IP");
				if (logger.isInfoEnabled()) {
					logger.info("getIpAddress(HttpServletRequest) - HTTP_CLIENT_IP - String ip=" + ip);
				}
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getHeader("HTTP_X_FORWARDED_FOR");
				if (logger.isInfoEnabled()) {
					logger.info("getIpAddress(HttpServletRequest) - HTTP_X_FORWARDED_FOR - String ip=" + ip);
				}
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = request.getRemoteAddr();
				if (logger.isInfoEnabled()) {
					logger.info("getIpAddress(HttpServletRequest) - getRemoteAddr - String ip=" + ip);
				}
			}
		} else if (ip.length() > 15) {
			String[] ips = ip.split(",");
			for (int index = 0; index < ips.length; index++) {
				String strIp = (String) ips[index];
				if (!("unknown".equalsIgnoreCase(strIp))) {
					ip = strIp;
					break;
				}
			}
		}
		return ip;
	}
}