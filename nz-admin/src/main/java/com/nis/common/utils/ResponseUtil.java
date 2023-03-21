package com.nis.common.utils;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ReflectUtil;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class ResponseUtil {

	/**
	 * response 响应文件
	 * @param response
	 * @param file
	 * @throws IORuntimeException
	 * @throws IOException
	 */
	public static void downloadFile(HttpServletResponse response, File file) throws IORuntimeException, IOException {
		response.setContentType(ResponseUtil.getDownloadContentType(file.getName()));
		String fileName = Tool.URLUtil.encode(file.getName(), Tool.CharsetUtil.CHARSET_UTF_8);
//		response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
//		response.addHeader("Content-Length", "" + file.length());
//		response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
		ReflectUtil.invoke(response, "addHeader", "Content-Disposition", "attachment; filename=" + fileName);
		ReflectUtil.invoke(response, "addHeader", "Content-Length", "" + file.length());
		ReflectUtil.invoke(response, "setHeader", "Access-Control-Expose-Headers", "Content-Disposition");
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
		response.setContentType(ResponseUtil.getDownloadContentType(filename));
		String fileName = Tool.URLUtil.encode(filename, Tool.CharsetUtil.CHARSET_UTF_8);
//		response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
//		response.addHeader("Content-Length", "" + data.length);
//		response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
		ReflectUtil.invoke(response, "addHeader", "Content-Disposition", "attachment; filename=" + fileName);
		ReflectUtil.invoke(response, "addHeader", "Content-Length", "" + data.length);
		ReflectUtil.invoke(response, "setHeader", "Access-Control-Expose-Headers", "Content-Disposition");
		Tool.IoUtil.write(response.getOutputStream(), false, data);
	}

	public static String getDownloadContentType(String filename){
		ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(filename);
		return ToolUtil.isEmpty(extensionMatch) ? MediaType.APPLICATION_OCTET_STREAM_VALUE : extensionMatch.getMimeType();
	}
}
