package com.nis.modules.mission.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class ParamForExecuteInstruct implements Serializable{
	
	private static final long serialVersionUID = 1L;
	/**
	 * 执行文件或命令
	 */	
	private String execCmd;
	/**
	 * 执行文件或命令的参数序列
	 */
	private String [] execParams;
	private String execParamsTmpStr;
	/**
	 * 该命令是否强制执行：Y是，N否
	 */
	private String forceExec;
	/**
	 * 常驻内存标识: Y是，N否
	 */
	private String residentFlag;
	/**
	 * 存放执行结果标识的文件:1、常驻内存的，写入PID，2、非常驻的，写执行结果：结果标识（0 成功 1 失败）|结果描述
	 */
	private String execResult;
	/**
	 * 回传标识: Y是，N否
	 */
	private String returnFlag;
	/**
	 * 回传文件或目录路径
	 */
	private String returnPath;
	/**
	 * 最终结果获取最大等待时间（单位：分钟）
	 */
	private String maxWaitTime;
	/**
	 * 执行文件或命令的用户名
	 */
	private String username;
	/**
	 * 用户密码
	 */
	private String pw;
	
	/**
	 * 类型
	 */
	private Long commandType;
	/**
	 * 返回结果最大长度	不能为空，缺省1024，返回结果的最大长度
	 */
	private Long maxReturn;  

	
	//--批量备份
	private String  backup;        //需要备份的文件
	private String  backupTo;      //备份到的路径
	private String[] except;	   
	private String exceptTmpStr;   //排除文件
	private String isAbs;          //是否为绝对路径  1-是
	
	//--批量更新
	private String source;
	private String cover;
	private String isCreateCover;
	private String[] delete;
	private String deleteTmpStr;
	//--批量停用
	private String pidFile;
	private String[] pidFiles;
	private String commandEnname;   // 具体任务名称
}
