/**

 *

 *
 * 
 */

package com.nis.common.xss;

import cn.hutool.log.Log;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

/**
 * XSS过滤
 *
 * @author Mark sunlightcs@gmail.com
 */
public class XssFilter implements Filter {

	private final static Log logger = Log.get();

	@Override
	public void init(FilterConfig config) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain){
		XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(
				(HttpServletRequest) request);
		try {
			chain.doFilter(xssRequest, response);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	@Override
	public void destroy() {
	}

}