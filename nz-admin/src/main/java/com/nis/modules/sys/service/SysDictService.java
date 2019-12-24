/**

 *

 *

 */

package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.entity.SysDictEntity;

import com.nis.common.utils.PageUtils;

import java.util.List;
import java.util.Map;

/**
 * 数据字典
 *

 */
public interface SysDictService extends IService<SysDictEntity> {

    PageUtils queryPage(Map<String, Object> params);
    
    List<SysDictEntity> queryList(Map<String, Object> params);

	boolean delete(String ids);
	
	boolean update(SysDictEntity dict);
	
	void updateLangCache();

    /**
     *  name type 下 value 值不能重复
     * @param id
     * @param name   字典名称
     * @param type   字典类型
     * @param value  字典值
     * @return
     */
    boolean dictValueCheck(Long id, String name, String type, String value);

    Integer queryMaxTypeCode(String type);

    void checkSysDicValue(String type, String value, RCode errCode);

    void checkSysDicCode(String type, Integer code, RCode errCode);

    List<SysDictEntity> querySysDicByType(String type);

}

