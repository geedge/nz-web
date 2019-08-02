package com.nis.modules.mission.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class FileUtil {
	/**
	 * 上传文件时，判断该文件是否已存在，如存在，则在后面加入时间戳
	 * 
	 * @param fileName
	 *            单纯的文件名
	 */
	public static String addTimeTagForUploadFileName(String fileName) {
		Calendar calendar = new GregorianCalendar();
		long timestamp = calendar.getTimeInMillis();
		// 去掉后缀的文件名
		String fielType = "";
		if (fileName.lastIndexOf(".") != -1) {
			fielType = fileName.substring(fileName.lastIndexOf("."));
			fileName = fileName.substring(0, fileName.lastIndexOf("."));
		}
		fileName += "_" + timestamp;
		fileName += fielType;
		return fileName;
	}
	/**
	 * 去掉文件名后的时间戳
	 * 
	 * @param fileName
	 *            带时间戳的文件名
	 */
	public static String removeTimeTagForDownLoad(String fileName) {
		if (fileName.contains("_")) {
			String timeTag = fileName.substring(fileName.lastIndexOf("_"),
					fileName.lastIndexOf("."));
			fileName = fileName.replace(timeTag, "");
		}
		return fileName;
	}
	
	public long getFileSizes(File f) throws Exception {// 取得文件大小
		long s = 0;
		if (f.exists()) {
			FileInputStream fis = null;
			fis = new FileInputStream(f);
			s = fis.available();
		} else {
			f.createNewFile();
			System.out.println("文件不存在");
		}
		return s;
	}

	// 递归
	public long getFileSize(File f) throws Exception// 取得文件夹大小
	{
		long size = 0;
		File flist[] = f.listFiles();
		for (int i = 0; i < flist.length; i++) {
			if (flist[i].isDirectory()) {
				size = size + getFileSize(flist[i]);
			} else {
				size = size + flist[i].length();
			}
		}
		return size;
	}

	public String FormetFileSize(long fileS) {// 转换文件大小
		DecimalFormat df = new DecimalFormat("#.00");
		String fileSizeString = "";
		if (fileS < 1024) {
			fileSizeString = df.format((double) fileS) + "B";
		} else if (fileS < 1048576) {
			fileSizeString = df.format((double) fileS / 1024) + "K";
		} else if (fileS < 1073741824) {
			fileSizeString = df.format((double) fileS / 1048576) + "M";
		} else {
			fileSizeString = df.format((double) fileS / 1073741824) + "G";
		}
		return fileSizeString;
	}

	public long getlist(File f) {// 递归求取目录文件个数
		long size = 0;
		File flist[] = f.listFiles();
		size = flist.length;
		for (int i = 0; i < flist.length; i++) {
			if (flist[i].isDirectory()) {
				size = size + getlist(flist[i]);
				size--;
			}
		}
		return size;

	}
	/**
	 * 获取目录下所有指定后缀名的文件
	 * @param file
	 * @return
	 */
	public  File[] getDirectoryArray(String filepath, final String suffix) {
		File file = new File(filepath);
		if (!file.isDirectory()) {
			new Exception(file.getAbsolutePath() + " Not A Directory");
		}
		return file.listFiles(new FilenameFilter() {
			// 使用匿名内部类重写accept方法;
			public boolean accept(File dir, String name) {
				if (new File(dir, name).isDirectory()) {
					return false;
				} else
					return name.endsWith(suffix); // 过滤出注定后缀名称的文件
			}
		});
	}
	
	//根据给定的目录产生所有的文件对象数组
	public static File[] listAllFilesEndWith(File file,String suffix) {
		File[] files = new File[0];
		List<File> fileGroup = new ArrayList<File>();
		try {
			generateAllFilesEndWith(file,suffix,fileGroup);
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return fileGroup.toArray(files);
	}
	
	public static void generateAllFilesEndWith(File file,String suffix,List<File> fileGroup) {
		try {
			if(file.exists()&&file.isDirectory()) {
				File[] fileList = file.listFiles();
				for(File f:fileList) {
					if(f.exists()) {//文件夹作为节点
						if(f.isDirectory()) {
							generateAllFilesEndWith(f,suffix,fileGroup);
						}else if(isRightSuffix(f,suffix)) {
							fileGroup.add(f);
						}
					} 
				}
			}else {
				fileGroup.add(file);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	//文件后缀名是否符合指定字符串
	public static boolean isRightSuffix(File file,String suffix) {
		boolean canRead = false;
		String fileName = file.getName();
		String extensionName = fileName.substring(fileName.lastIndexOf(".")+1);
		if(file.isFile()&&suffix.equals(extensionName)) {
			canRead = true; 
		}
		return canRead;
	}
	
	public static Calendar getModifiedTime(File f){          
		   Calendar cal = Calendar.getInstance();           
		   long time = f.lastModified();           
//		   SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");                  
		   cal.setTimeInMillis(time);           
//		   Date date = cal.getTime();
//		   System.out.println("修改时间[2] " + formatter.format(cal.getTime()));              //输出：修改时间[2]    2009-08-17 10:32:38
		   
		   return cal;
	}   
	
	
	 static void main(String[] args) {

		// File file = new
		// File("E:/tomacat/apache-tomcat-6.0.14/webapps/share/attached/image/share/20111018/1318925123906.jpg");
		// FileInputStream fis = null;
		// try{
		// fis = new FileInputStream(file);
		// System.out.println("文件size:"+String.valueOf((double)fis.available()/1024)+"K");
		// // fileSizeString = df.format((double) fileS / 1048576) + "M";
		// 
		// }catch(IOException e1){
		// System.out.println("IO出错！");
		// }

		FileUtil g = new FileUtil();
		long startTime = System.currentTimeMillis();
		try {
			long l = 0;
			String path = "D:\testSize";
			File ff = new File(path);
			if (ff.isDirectory()) { // 如果路径是文件夹的时候
				System.out.println("文件个数 " + g.getlist(ff));
				System.out.println("目录");
				l = g.getFileSize(ff);
				System.out.println(path + "目录的大小为：" + g.FormetFileSize(l));
			} else {
				System.out.println(" 文件个数 1");
				System.out.println("文件");
				l = g.getFileSizes(ff);
				System.out.println(path + "文件的大小为：" + g.FormetFileSize(l));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("总共花费时间为：" + (endTime - startTime) + "毫秒...");
	}
	 
	 public static void copy(File src, File dst, int bufferSize) {
			try {
				long start  =  System.currentTimeMillis();
				InputStream in = null;
				OutputStream out = null;
				try {
					in = new BufferedInputStream(new FileInputStream(src),	bufferSize);
					out = new BufferedOutputStream(new FileOutputStream(dst),	bufferSize);
					byte[] buffer = new byte[bufferSize];
					
					
					int length = -1;
					while((length = in.read(buffer)) != -1)
					{
						out.write(buffer, 0, length);
					}
					
					/***** 原先的 *****/
					/*
					while (in.read(buffer) > 0) {
						out.write(buffer);
					}
					*/
					/**********/
					
					out.flush();
					System.out.println("上传文件耗时："+(System.currentTimeMillis()-start)+"毫秒！");
				} finally {
					if (null != in) {
						in.close();
					}
					if (null != out) {
						out.close();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("上传文件时出错！！！");
			}
		}
		
}
