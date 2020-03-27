package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.asset.dao.ModelDao;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.entity.ModelWithMib;
import com.nis.modules.asset.service.ModelService;
import com.nis.modules.sys.dao.SnmpMibDao;
import com.nis.modules.sys.entity.SnmpMib;
import com.nis.modules.sys.service.SnmpMibService;
import com.nis.modules.sys.shiro.ShiroUtils;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service("snmpMibService")
public class SnmpMibServiceImpl extends ServiceImpl<SnmpMibDao, SnmpMib> implements SnmpMibService {

    private static Logger logger = LoggerFactory.getLogger(SnmpMibServiceImpl.class);

    @Autowired
    private SnmpMibDao snmpMibDao;
    @Autowired
    private ModelDao modelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SnmpMib> page = new Query<SnmpMib>().getPage(params);

        List<SnmpMib> mibs = snmpMibDao.queryList(page, params);

        for (SnmpMib snmpMib : mibs) {
            snmpMib.setContent(null);
            if(null!=snmpMib.getModels()&&!"".equals(snmpMib.getModels())){
                List<ModelWithMib> modelList = modelDao.selectByModels(
                        Arrays.asList(snmpMib.getModels().split(",")).stream().mapToInt(Integer::parseInt).toArray());
                if(modelList.size()>0){
                    snmpMib.setModelsDetail(modelList);
                }
            }
        }

        page.setRecords(mibs);
        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateSnmpMib(SnmpMib snmpMib, MultipartFile file) {
        snmpMib.setUpdateBy(ShiroUtils.getUserId());
        snmpMib.setUpdateAt(new Date());

        if (snmpMib.getId() == null) {
            // 添加
            this.handleMibName(snmpMib, file);
            this.save(snmpMib);
        } else {
            // 修改
            if (file != null) {
                this.handleMibName(snmpMib, file);
            }
            this.updateById(snmpMib);
        }
    }

    private void handleMibName(SnmpMib snmpMib, MultipartFile file) {
        // 文件名称默认文件名取消后缀
        String name = file.getOriginalFilename();
        snmpMib.setFileName(name);
        if (StringUtils.isEmpty(snmpMib.getName())) {
            if (name.contains(".")) {
                String defaultName = name.substring(0, name.lastIndexOf("."));
                snmpMib.setName(defaultName);
            } else {
                snmpMib.setName(name);
            }
        }

        try {
            snmpMib.setContent(file.getBytes());
        } catch (IOException e) {
            logger.error("transfer mib certificate error", e);
        }
    }
}
