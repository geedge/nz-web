package com.nis.common.utils;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

import cn.hutool.core.io.IORuntimeException;

public class ResponseUtil {

	/**
	 * response 响应文件
	 * @param response
	 * @param file
	 * @throws IORuntimeException
	 * @throws IOException
	 */
	public static void downloadFile(HttpServletResponse response, File file) throws IORuntimeException, IOException {
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM.toString());
		String fileName = Tool.URLUtil.encode(file.getName(), Tool.CharsetUtil.UTF_8);
		response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
		response.addHeader("Content-Length", "" + file.length());
		response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
		Tool.FileUtil.writeToStream(file, response.getOutputStream());
	}

	/**
	 * reponse 下载 byte数据
	 * @param response
	 * @param filename
	 * @param data
	 * @throws IORuntimeException
	 * @throws IOException
	 */
	public static void downloadFile(HttpServletResponse response, String filename, byte[] data)
			throws IORuntimeException, IOException {
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM.toString());
		String fileName = Tool.URLUtil.encode(filename, Tool.CharsetUtil.UTF_8);
		response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
		response.addHeader("Content-Length", "" + data.length);
		response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
		Tool.IoUtil.write(response.getOutputStream(), false, data);
	}
}
