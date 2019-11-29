/**

 *

 *
 * 
 */

package com.nis.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.List;

/**
 * 分页工具类
 *
 * @author Mark sunlightcs@gmail.com
 */
public class PageUtils implements Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * 总记录数
	 */
	private int total;
	/**
	 * 每页记录数
	 */
	private int pageSize;
	/**
	 * 总页数
	 */
	private int pages;
	/**
	 * 当前页数
	 */
	private int pageNo;
	/**
	 * 列表数据
	 */
	private List<?> list;
	
	/**
	 * 分页
	 * @param list        列表数据
	 * @param total  总记录数
	 * @param pageSize    每页记录数
	 * @param pageNo    当前页数
	 */
	public PageUtils(List<?> list, int total, int pageSize, int pageNo) {
		this.list = list;
		this.total = total;
		this.pageSize = pageSize;
		this.pageNo = pageNo;
		this.pages = (int)Math.ceil((double)total/pageSize);
	}

	/**
	 * 分页
	 */
	public PageUtils(IPage<?> page) {
		this.list = page.getRecords();
		this.total = (int)page.getTotal();
		this.pageSize = (int)page.getSize();
		this.pageNo = (int)page.getCurrent();
		this.pages = (int)page.getPages();
	}

	public int gettotal() {
		return total;
	}

	public void settotal(int total) {
		this.total = total;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getpages() {
		return pages;
	}

	public void setpages(int pages) {
		this.pages = pages;
	}

	public int getpageNo() {
		return pageNo;
	}

	public void setpageNo(int pageNo) {
		this.pageNo = pageNo;
	}

	public List<?> getList() {
		return list;
	}

	public void setList(List<?> list) {
		this.list = list;
	}
	
}
