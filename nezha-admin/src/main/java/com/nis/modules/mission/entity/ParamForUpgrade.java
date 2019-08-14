package com.nis.modules.mission.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class ParamForUpgrade implements Serializable{

	private static final long serialVersionUID = 1L;
	/**
	 * 指定更新需要的文件，如果是相对路径，则相对推送目的地
	 */
	private String fileName;
	/**
	 * 处理过后的文件名称
	 */
	private String upFileName;
	/**
	 * 指定文件覆盖目录
	 */
	private String cover;
	/**
	 * 是否创建覆盖目录
	 */
	private String isCreateCover;
	/**
	 * 如果更新源文件是个压缩包：Y 按绝对路径解压即在根目录下解压，N 按进入备份目录解压（这个属性只针对Linux有效，Windows下只按进入备份目录解压）
	 */
	private String isAbs;
	/**
	 * 备份目录，如果是相对路径，则相对覆盖目录
	 */
	private List<Backup> backups;
	/**
	 * 恢复目录
	 */
	private List<CoverUpdate> recoverys;
	/**
	 * 需要删除的目录，如果是相对路径，则相对覆盖目录
	 */
	private String []  delete;
	/**
	 * 进程PID文件全路径
	 */
	private String pidFile;
	/**
	 * 进程启动文件全路径
	 */
	private String startupFile;
	/**
	 * 执行文件的参数序列
	 */
	private String[] execParams;
	/**
	 * 该命令是否强制执行：Y是，N否
	 */
	private String forceExec;
	/**
	 * 最终结果获取最大等待时间（单位：分钟）
	 */
	private String maxWaitTime;
	/**
	 * 文件的属主或运行的用户（只针对Linux有效）
	 */
	private String username;
	/**
	 * 用记密码
	 */
	private String pw;
	/**
	 * 文件的属组（只针对Linux有效）
	 */
	private String groupName;
	/**
	 * 权限设置
	 */
	private String permisson;
	/**
	 * 配合页面修改用的属性
	 */
	private String  deleteTmpStr;
	private String execParamsTmpStr;
	/**
	 * 推送文件的MD5值
	 */
	private String md5Value;
	
	private Long version;  // 用于逆向任务标识  储存被逆向任务的id  只有逆向任务此属性有值
}
