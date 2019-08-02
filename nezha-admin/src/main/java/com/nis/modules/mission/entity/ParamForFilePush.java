package com.nis.modules.mission.entity;

import java.io.Serializable;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ParamForFilePush implements Serializable{
	private static final long serialVersionUID = 1L;
		/**
		 * 推送文件名
		 */
		private String fileName;
		/**
		 * 处理后的文件名称
		 */
		private String upFileName;
		/**
		 * 推送目的地
		 */
		private String destPath;
		/**
		 * 推送文件的MD5值
		 */
		private String md5Value;
		/**
		 * 是否覆盖：Y是，N否
		 */
		private String isCover;
		/**
		 * 推送文件的属主（只针对Linux有效）
		 */
		private String username;
		/**
		 * 属主密码
		 */
		private String pw;
		/**
		 * 推送文件的属群（只针对Linux有效）
		 */
		private String groupName;
		/**
		 * 推送文件拥有的权限（只针对Linux有效）
		 */
		private String permisson;
}
