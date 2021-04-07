package com.nis.common.utils;

import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

import cn.hutool.core.io.IORuntimeException;

public class ResponseUtil {

	public static void downloadFile(HttpServletResponse response,File file) throws IORuntimeException, IOException {
		response.reset();
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM.toString());
		String fileName = Tool.URLUtil.encode(file.getName(), Tool.CharsetUtil.UTF_8);
		response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
		response.addHeader("Content-Length", "" + file.length());
		Tool.FileUtil.writeToStream(file, response.getOutputStream());
	}
	
}
