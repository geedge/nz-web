/**

 *

 *
 * 
 */

package com.nis.common.xss;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * XSS过滤
 *
 * @author Mark sunlightcs@gmail.com
 */
public class XssFilter implements Filter {

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
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
	}

}