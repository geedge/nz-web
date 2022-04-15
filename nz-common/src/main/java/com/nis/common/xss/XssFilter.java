/**

 *

 *
 * 
 */

package com.nis.common.xss;

import cn.hutool.log.Log;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

/**
 * XSS过滤
 *
 */
public class XssFilter implements Filter {

	private final static Log logger = Log.get();

	@Override
	public void init(FilterConfig config) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException{
		XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(
				(HttpServletRequest) request);
		chain.doFilter(xssRequest, response);
	}

	@Override
	public void destroy() {
	}

}