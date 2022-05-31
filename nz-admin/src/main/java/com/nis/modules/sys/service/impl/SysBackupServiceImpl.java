package com.nis.modules.sys.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysBackupService;
import org.mariadb.jdbc.MariaDbBlob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

@Service("sysBackupService")
public class SysBackupServiceImpl implements SysBackupService {

    private static Log log = Log.get();
    
    @Autowired
    private  SysConfigDao sysConfigDao;
    
    @Autowired
    private DataSource dataSource;
    
    @Value("${nezha.exportMaxSize:10000}")
    private Integer exportMaxSize;
    
	@Override
	public Object queryInfo() {
		Object result = null;
		SysConfigEntity config = sysConfigDao.queryByKey("backup_config");
		if(ToolUtil.isNotEmpty(config) && ToolUtil.isNotEmpty(config.getParamValue())) {
			String paramValue = config.getParamValue();
			result = JSON.parseObject(paramValue, Object.class);
		}
		return result;
	}

	@Override
	public void save(Map<String, Object> params) {
		SysConfigEntity config = sysConfigDao.queryByKey("backup_config");
		if(ToolUtil.isNotEmpty(config)) {
			sysConfigDao.updateValueByKey("backup_config", JSON.toJSONString(params));
		}else {
			config = new SysConfigEntity();
			config.setParamKey("backup_config");
			config.setParamValue(JSON.toJSONString(params));
			config.setStatus("1");
			sysConfigDao.insert(config);
		}
	}

	@Override
	public List<Object> queryBackupList() {
		List<Object> result = null;
		SysConfigEntity config = sysConfigDao.queryByKey("backup_file_path");
		String path = Constant.BACKUP_ROOT_PATH;
		if(ToolUtil.isNotEmpty(config) && ToolUtil.isNotEmpty(config.getParamValue())) {
			path = config.getParamValue();
		}
		boolean directory = FileUtil.isDirectory(path);
		if(directory) {
			List<File> loopFiles = FileUtil.loopFiles(path);
			if(ToolUtil.isNotEmpty(loopFiles)) {
				List<File> sortFile = loopFiles.stream().sorted(Comparator.comparing(File::lastModified).reversed()).collect(Collectors.toList());
				result = new ArrayList<Object>();
				for(File file : sortFile) {
					Map<String,Object> data =new HashMap<String,Object>();
					data.put("fileName", file.getName());
					data.put("time", DateUtil.date(file.lastModified()));
					data.put("size", file.length());
					result.add(data);
				}
			}
		}
		return result;
	}

    @Override
    public void backupData() throws Exception {
    	String sqlFilename = null;
    	Connection conn = null;
    	Statement stmtInfo = null, stmtData = null;
	    ResultSet rsInfo = null, rsData = null;
    	try {
			conn = dataSource.getConnection();
			String dbName = conn.getCatalog();
			//存放文件目录
	        SysConfigEntity config = sysConfigDao.queryByKey("backup_file_path");
			String path = Constant.BACKUP_ROOT_PATH;
			if(ToolUtil.isNotEmpty(config) && ToolUtil.isNotEmpty(config.getParamValue())) {
				path = config.getParamValue();
			}
			
	        //文件名
	        sqlFilename = path + File.separator +  String.format("%s_%s.sql", dbName, DateUtil.format(new Date() , "yyyyMMdd_hh_mm_ss"));
	        BufferedWriter writer = FileUtil.getWriter(sqlFilename, "UTF-8", false);
		    stmtInfo = conn.createStatement();
		    /*
	         * 获取所有表名称 
	         */
	        List<String> tableNames =new ArrayList<String>(); 
	        rsInfo = stmtInfo.executeQuery(String.format("SHOW FULL TABLES FROM `%s` WHERE TABLE_TYPE = 'BASE TABLE'", dbName));
	        // 遍历所有表
	        while(rsInfo.next()) {
	            String tableName = rsInfo.getString(1);
	            tableNames.add(tableName);
	        }    //end for tables
	        
	        Tool.IoUtil.close(rsInfo);
            Tool.IoUtil.close(stmtInfo);
		    
		    if(ToolUtil.isNotEmpty(tableNames)) {
		    	
		    	//将不需要备份表过滤
				SysConfigEntity excludeTables = sysConfigDao.queryByKey("backup_exclude_tables");
				if(ToolUtil.isNotEmpty(excludeTables) && ToolUtil.isNotEmpty(excludeTables.getParamValue())) {
					List<String> excludeTableNames = Arrays.asList(excludeTables.getParamValue().split(StringPool.COMMA));
					tableNames = tableNames.stream().filter(tableName-> !excludeTableNames.contains(tableName)).collect(Collectors.toList());
				}
		    	
		    	/*
		         * 头内容
		         */
		        writer.write("SET NAMES utf8mb4;");
		        writer.newLine();
		        writer.write("SET FOREIGN_KEY_CHECKS = 0;");
		        writer.newLine();
		        writer.flush();

		        /*
		         * 导出表数据
		         */
				for(String tableName : tableNames) { 
					this.dbBackExportTable(conn, tableName, writer, false); 
				} 
		        
		        /*
		         * 导出存储过程信息
		         */
		        stmtInfo = conn.createStatement();
		        rsInfo = stmtInfo.executeQuery(String.format("SELECT `SPECIFIC_NAME` from `INFORMATION_SCHEMA`.`ROUTINES` WHERE `ROUTINE_SCHEMA` = '%s' AND ROUTINE_TYPE = 'PROCEDURE'; ", dbName));
		        while (rsInfo.next()) {
		            String procName = rsInfo.getString(1);
		            
		            /* 存储过程结构 */
		            stmtData = conn.createStatement();
		            rsData = stmtData.executeQuery(String.format("SHOW CREATE PROCEDURE `%s`", procName));
		            if(!rsData.next()) {
		                continue ;
		            }
		            writer.newLine();
		            writer.newLine();
		            writer.write(String.format("/* Procedure structure for procedure `%s` */", procName));
		            writer.newLine();
		            writer.write(String.format("/*!50003 DROP PROCEDURE IF EXISTS  `%s` */;", procName));
		            writer.newLine();
		            writer.write("DELIMITER $$");
		            writer.newLine();
		            writer.append("/*!50003 ").append(rsData.getString(3)).append(" */$$");
		            writer.newLine();
		            writer.write("DELIMITER ;");
		            
		            Tool.IoUtil.close(rsData);
		            Tool.IoUtil.close(stmtData);
		        }
		        
		        /*
		         * 尾内容
		         */
		        writer.newLine();
		        writer.newLine();
		        writer.write("SET FOREIGN_KEY_CHECKS = 1;");
		        writer.flush();
		        writer.close();
		    }
        } finally {
			Tool.IoUtil.close(stmtData);
			Tool.IoUtil.close(rsData);
			Tool.IoUtil.close(stmtInfo);
			Tool.IoUtil.close(rsInfo);
			Tool.IoUtil.close(conn);
		}
    	
    	this.removeBackupFile();
    }

    @Override
    public void removeBackFileByFilename(String filename) {
        SysConfigEntity config = sysConfigDao.queryByKey("backup_file_path");
        String path = Constant.BACKUP_ROOT_PATH;

        if (Optional.ofNullable(config).map(SysConfigEntity::getParamValue).isPresent()) {
            path = config.getParamValue();
        }

        String sqlFilePath = Tool.StrUtil.concat(true, path, File.separator, filename);
        boolean exist = FileUtil.exist(sqlFilePath);
        if (exist) {
            boolean isFile = FileUtil.isFile(sqlFilePath);
            if (isFile) {
                try {
                    // 是文件 则删除
                    boolean del = FileUtil.del(sqlFilePath);
                    log.info(String.format("System backup file delete, filename :%s, result : %s", filename, del));
                } catch (IORuntimeException e) {
                    log.error(e);
                    log.info("System backup file delete error, filename {}", filename);
                }
            }
        } else {
            // 文件不存在
            throw new NZException(RCode.SYS_BACKUP_FILENAME_NOTEXIST);
        }
    }

    /**
     * 根据数据库配置 判断保存文件数量是否超标
     */
    public void removeBackupFile() {
    	// 备份完成后获取文件保存数量设置 将多余文件删除
    	SysConfigEntity backupConfig = sysConfigDao.queryByKey("backup_config");
    	if(ToolUtil.isNotEmpty(backupConfig) && ToolUtil.isNotEmpty(backupConfig.getParamValue())) {
    		String value = backupConfig.getParamValue();
    		JSONObject config = JSON.parseObject(value);
    		Integer retention = (Integer)config.get("retention");
    		if(ToolUtil.isNotEmpty(retention) && !retention.equals(-1)) {
    			SysConfigEntity filePathConfig = sysConfigDao.queryByKey("backup_file_path");
    			String path = Constant.BACKUP_ROOT_PATH;
    			if(ToolUtil.isNotEmpty(filePathConfig) && ToolUtil.isNotEmpty(filePathConfig.getParamValue())) {
    				path = filePathConfig.getParamValue();
    			}
    			boolean directory = FileUtil.isDirectory(path);
    			if(directory) {
    				List<File> loopFiles = FileUtil.loopFiles(path);
    				if(ToolUtil.isNotEmpty(loopFiles)) {
    					if(loopFiles.size() > retention) {
    						Integer diff = loopFiles.size() - retention;
    						for(int i = 0;i < diff; i++) {
    							FileUtil.del(loopFiles.get(i));
    						}
    					}
    				}
    			}
    		}
    	}
    }
    /**
     * 导出表数据
     * @param conn
     * @param tableName
     * @param writer
     * @param bulkFlag   是否将数据放在一起
     * @throws SQLException 
     * @throws IOException 
     */
    private void dbBackExportTable(Connection conn, String tableName, BufferedWriter writer, boolean bulkFlag) throws SQLException, IOException {
        Statement stmt = null;
        ResultSet rs = null;
        /* 表结构 */
        stmt = conn.createStatement();
        rs = stmt.executeQuery(String.format("SHOW CREATE TABLE `%s`", tableName));
        if(!rs.next()) {
            return ;
        }
        writer.newLine();
        writer.newLine();
        writer.write(String.format("/*Table structure for table `%s` */", tableName));
        writer.newLine();
        writer.write(String.format("DROP TABLE IF EXISTS `%s`;", tableName));
        writer.newLine();                
        writer.write(rs.getString(2) + ";");
        writer.newLine();
        Tool.IoUtil.close(rs);
        Tool.IoUtil.close(stmt);
        
        /* 导出表数据 */
        // 先获取记录数
        stmt = conn.createStatement();
        rs = stmt.executeQuery(String.format("SELECT COUNT(1) FROM `%s`", tableName));
        int rowCount = rs.next() ? rs.getInt(1) : 0;
        if(0 >= rowCount) {
            writer.flush();
            return ;
        }
        writer.write(String.format("/*Data for the table `%s` */", tableName));
        writer.newLine();
        
        stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        /**
         * If the fetch size specified is zero, the JDBC driver ignores the value and is free to make its own best guess as to what the fetch size should be.
         * The default value is set by the Statement object that created the result set.
         * The fetch size may be changed at any time.
         * @see #setFetchSize
         * @link https://docs.oracle.com/javase/8/docs/api/java/sql/ResultSet.html#setFetchSize-int-
         */
        // stmt.setFetchSize(Integer.MIN_VALUE);
        stmt.setFetchSize(0);
        stmt.setFetchDirection(ResultSet.FETCH_REVERSE);
        
        Integer start = 0;
        Integer end = exportMaxSize;
        boolean flag = true;
        while(flag) {
        	if(rowCount < exportMaxSize) {
            	end = rowCount;
            }
        	
        	if(end == rowCount) {
        		flag = false;
        	}
        	
            rs = stmt.executeQuery(String.format("SELECT * FROM `%s` limit %d , %d", tableName,start,end));
            int colCount = 0;
            Object colValue = null;
            // 所有数据用","连接
            if(!bulkFlag) {
                while(rs.next()) {
                    colCount = rs.getMetaData().getColumnCount();
                    
                    writer.write(String.format("INSERT INTO `%s` VALUES (", tableName));
                    // 获取表每一列数据
                    for(int j = 0; j < colCount; j ++) {
                        if(j > 0) {
                            writer.write(',');    
                        }
                        colValue = rs.getObject(j + 1);
                        if (null != colValue) {
                            // MariaDbBlob 单独处理，解决 bolo 类型直接 toString 造成结果有误问题
                            if (colValue instanceof MariaDbBlob) {
                                InputStream binaryStream = ((MariaDbBlob) colValue).getBinaryStream();
                                // 使用 0x 标识十六进制字符串，参照  ResultSet API ，binaryStream 转 十六进制 大写 字符串
                                String blobHexStr = Tool.StrUtil.concat(true, "0x", Tool.HexUtil.encodeHexStr(IoUtil.readBytes(binaryStream), false));
                                writer.write(blobHexStr);
                            } else {
                                writer.write(String.format("'%s'", escapeString(colValue.toString())));
                            }
                        } else {
                            writer.write("NULL");
                        }
                    }    //end for one record columns
                    writer.write(");");
                    writer.newLine();
                    writer.flush();
                }    //end for table records
            }
            // 每行数据独立分开
            else {
                ResultSetMetaData rsMetaData = null;
                int counter = 0;
                while(rs.next()) {
                    ++ counter;
                    rsMetaData = rs.getMetaData();
                    colCount = rsMetaData.getColumnCount();

                    // 第一条记录，则列出列名
                    if(1 == counter) {
                        writer.write(String.format("INSERT INTO `%s` (", tableName));
                        for(int i = 0; i < colCount; i ++) {
                            if(i > 0) {
                                writer.write(",");
                            }
                            writer.append('`').append(rsMetaData.getColumnName(i + 1)).append('`');
                        }
                        writer.append(") VALUES ");
                    }
                    // 获取表每一列数据
                    for(int j = 0; j < colCount; j ++) {
                        writer.write((0 >= j) ? '(' : ',');                        
                        colValue = rs.getObject(j + 1);
                        if(null != colValue) {
                            writer.write(String.format("'%s'", this.escapeString(colValue.toString())));
                        } else {
                            writer.write("NULL");
                        }
                    }    //end for one record columns
                    // 是否是最后记录
                    if(rowCount > counter) {
                        writer.write("),");
                    } else {
                        writer.write(");");
                    }
                    writer.flush();
                }    //end for table records
            }
            
            start = end;
            end = end + exportMaxSize;
            if(end > rowCount) {
            	end = rowCount;
            }
        }
        Tool.IoUtil.close(rs);
        Tool.IoUtil.close(stmt);
    }
        
    /**
     * 判断内容是否需要进行转义
     * @param x
     * @return
     */
    public static boolean isNeedEscape(String x) {
        boolean needsHexEscape = false;
        if(StringUtils.isBlank(x)) {
            return needsHexEscape;
        }
        int stringLength = x.length();
        int i = 0;
        do
        {
            if(i >= stringLength)
                break;
            char c = x.charAt(i);
            switch(c)
            {
            case 0: // '\0'
                needsHexEscape = true;
                break;

            case 10: // '\n'
                needsHexEscape = true;
                break;

            case 13: // '\r'
                needsHexEscape = true;
                break;

            case 92: // '\\'
                needsHexEscape = true;
                break;

            case 39: // '\''
                needsHexEscape = true;
                break;

            case 34: // '"'
                needsHexEscape = true;
                break;

            case 26: // '\032'
                needsHexEscape = true;
                break;
            }
            if(needsHexEscape)
                break;
            i++;
        } while(true);
        return needsHexEscape;
    }
    
    /**
     * 对mysql字符进行转义
     * @param x
     * @return
     */
    public static String escapeString(String x) {
        if(StringUtils.isBlank(x)) {
            return x;
        }
        if(!isNeedEscape(x)) {
            return x;
        }
        int stringLength = x.length();
        String parameterAsString = x;
        StringBuffer buf = new StringBuffer((int)((double)x.length() * 1.1000000000000001D));
        // 可以指定结果前后追加单引号：'
        //buf.append('\'');
        for(int i = 0; i < stringLength; i++)
        {
            char c = x.charAt(i);
            switch(c)
            {
            default:
                break;

            case 0: // '\0'
                buf.append('\\');
                buf.append('0');
                continue;

            case 10: // '\n'
                buf.append('\\');
                buf.append('n');
                continue;

            case 13: // '\r'
                buf.append('\\');
                buf.append('r');
                continue;

            case 92: // '\\'
                buf.append('\\');
                buf.append('\\');
                continue;

            case 39: // '\''
                buf.append('\\');
                buf.append('\'');
                continue;

            case 34: // '"'
                buf.append('\\');
                buf.append('"');
                continue;

            case 26: // '\032'
                buf.append('\\');
                buf.append('Z');
                continue;

            }
            buf.append(c);
        }
        // 可以指定结果前后追加单引号：'
        //buf.append('\'');
        parameterAsString = buf.toString();
        return parameterAsString;
    }
    
    
}
