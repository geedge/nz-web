package com.nis.modules.sys.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.ModelDao;
import com.nis.modules.asset.entity.ModelWithMib;
import com.nis.modules.sys.dao.SnmpMibDao;
import com.nis.modules.sys.entity.MmMib;
import com.nis.modules.sys.entity.SnmpMib;
import com.nis.modules.sys.service.SnmpMibService;
import com.nis.modules.sys.shiro.ShiroUtils;
import net.percederberg.mibble.Mib;
import net.percederberg.mibble.MibLoader;
import net.percederberg.mibble.MibLoaderException;
import net.percederberg.mibble.MibValueSymbol;
import net.percederberg.mibble.snmp.SnmpIndex;
import net.percederberg.mibble.snmp.SnmpObjectType;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
            if (null != snmpMib.getModels() && !"".equals(snmpMib.getModels())) {
                List<ModelWithMib> modelList = modelDao.selectByModels(
                        Arrays.asList(snmpMib.getModels().split(",")).stream().mapToInt(Integer::parseInt).toArray());
                if (modelList.size() > 0) {
                    snmpMib.setModelsDetail(modelList);
                }
            }
        }

        page.setRecords(mibs);
        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateSnmpMib(SnmpMib snmpMib, MultipartFile multipartFile) {
        snmpMib.setUpdateBy(ShiroUtils.getUserId());
        snmpMib.setUpdateAt(new Date());

        if (snmpMib.getId() == null) {
            // 添加
            this.parserMibFile(multipartFile, snmpMib);
            this.save(snmpMib);
        } else {
            // 修改
            if (multipartFile != null) {
                this.parserMibFile(multipartFile, snmpMib);
            }
            this.updateById(snmpMib);
        }
    }

    @Override
    public String getMibTrees(String[] ids, String[] models) {
        List<SnmpMib> snmpMibs = snmpMibDao.getMibTreesByIdsAndModels(ids, models);

        List<SnmpMib> mibList = null;
        // 过滤 models
        if (ToolUtil.isNotEmpty(models)) {
            mibList = new ArrayList<>();

            List<String> modelList = Arrays.asList(models);

            List<String> modelIds;
            for (SnmpMib snmpMib : snmpMibs) {
                if (StringUtils.isNotEmpty(snmpMib.getModels())) {
                    modelIds = Arrays.asList(snmpMib.getModels().split(","));
                    for (String id : modelIds) {
                        if (modelList.contains(id)) {
                            mibList.add(snmpMib);
                            break;
                        }
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(mibList)) {
                return this.mibTree(mibList);
            }
            return null;
        }
        if (CollectionUtils.isNotEmpty(snmpMibs)) {
            return this.mibTree(snmpMibs);
        }
        return null;
    }

    /*public String mibTree(List<SnmpMib> mibList) {
        JSONObject jsonObject = new JSONObject();
        for (SnmpMib snmpMib: mibList) {
            jsonObject.putAll(JSONObject.parseObject(snmpMib.getTree(), Map.class));
        }
        return jsonObject.toJSONString();
    }*/

    public String mibTree(List<SnmpMib> mibList) {
        Map resultData = new HashMap();
        for (SnmpMib snmpMib : mibList) {
            resultData.putAll(JSONObject.parseObject(snmpMib.getTree(), Map.class));
        }
        return JSONObject.toJSONString(resultData);
    }

    /**
     * 解析 mib文件
     *
     * @return
     * @throws IOException
     * @throws MibLoaderException
     */
    public void parserMibFile(MultipartFile multipartFile, SnmpMib snmpMib) {
        String name = multipartFile.getOriginalFilename();
        snmpMib.setFileName(name);

        try {
            snmpMib.setContent(multipartFile.getBytes());
        } catch (IOException e) {
            logger.error("transfer mib file error", e);
        }

        // 加载器
        MibLoader ml = new MibLoader();

        // 加载 mib 文件
        Mib mib = null;

        // 临时文件
        File mibFile = null;

        try {
            mibFile = new File(Constant.TEMP_PATH + File.separator + "tempMibFile" + UUID.randomUUID());
            multipartFile.transferTo(mibFile.getAbsoluteFile());
            mib = ml.load(mibFile);
        } catch (IOException e) {
            logger.error("mib文件获取失败", e);
        } catch (MibLoaderException e) {
            logger.error("mib文件解析失败", e);
        } finally {
            if (mibFile != null && mibFile.exists()) {
                mibFile.delete();
            }
        }
        // 经过解析后mib为空
        if(mib == null){
            // 解析失败
            throw new NZException(RCode.SNMP_MIB_PARSING_FAILED);
        }
        // 设置mib文件名称
        String mibName = mib.getName();
        snmpMib.setName(mibName);

        // 最终解析的数据集合
        List<MmMib> list = new ArrayList<>();

        // 获取 mib 文件中所有的对象
        Collection c = mib.getAllSymbols();

        SnmpObjectType snmpObjectType = null;
        MmMib mo = null;
        MibValueSymbol parent = null;

        String syntax = null, access = null, status = null, description = null, type = null, parentValue = null;
        boolean isTable;
        ArrayList<SnmpIndex> snmpIndices = new ArrayList();
        List<String> index;

        // 遍历解析 获取所需数据
        Iterator it = c.iterator();
        while (it.hasNext()) {
            index = new ArrayList<>();

            Object obj = it.next();
            // MibValueSymbol 是 MibSymbol字类 拥有更详细的信息
            if (obj instanceof MibValueSymbol) {
                mo = new MmMib();

                MibValueSymbol mvs = (MibValueSymbol) obj;
                if (mvs.getType() instanceof SnmpObjectType) {
                    snmpObjectType = (SnmpObjectType) mvs.getType();
                }

                if (snmpObjectType != null) {
                    syntax = snmpObjectType.getSyntax().getName();
                    access = snmpObjectType.getAccess().toString();
                    status = snmpObjectType.getStatus().toString();
                    description = snmpObjectType.getDescription();
                    snmpIndices = snmpObjectType.getIndex();
                    for (SnmpIndex snmpIndex : snmpIndices) {
                        index.add(snmpIndex.toString());
                    }
                }

                parent = mvs.getParent();
                if (parent != null) {
                    parentValue = parent.getValue().toString();
                }

                isTable = mvs.isTable();

                if (isTable) {
                    mo.setType(Constant.MIB_TABLE_TYPE);
                } else if (com.nis.common.utils.StringUtils.isNotEmpty(syntax) && Constant.MIB_ENTRY_SYNTAX.equals(syntax.toUpperCase())) {
                    mo.setType(Constant.MIB_ENTRY_TYPE);
                }

                type = mvs.getType().getName();
                if (com.nis.common.utils.StringUtils.isNotEmpty(type) && com.nis.common.utils.StringUtils.isEmpty(mo.getType())) {
                    if (type.toUpperCase().contains(Constant.MIB_IDENTIFIER_TYPE)) {
                        mo.setType(Constant.MIB_IDENTIFIER_TYPE);
                    } else {
                        mo.setType(Constant.MIB_OBJECT_TYPE);
                    }
                }

                mo.setName(mvs.getName());
                mo.setObjectID(mvs.getValue().toString());
                mo.setPid(parentValue);
                mo.setSyntax(syntax);
                mo.setAccess(access);
                mo.setStatus(status);
                mo.setDescription(description);
                mo.setIndex(index);

                list.add(mo);
            }
        }

        // tree
        System.out.println(list.size());
        String treeFormat = this.getTreeFormat(list, mibName);
        snmpMib.setTree(treeFormat);
    }

    public String getTreeFormat(List<MmMib> mibs, String mibName) {
        // 找到顶级节点
        Map map = new HashMap();
        for (MmMib mmMib : mibs) {
            map.put(mmMib.getObjectID(), mmMib);
        }


        List<MmMib> superMibs = new ArrayList<>();
        for (MmMib mmMib : mibs) {
            Object o = map.get(mmMib.getPid());
            if (o == null) {
                superMibs.add(mmMib);
            }
        }

        Map mibMap = new HashMap();
        for (MmMib mmMib : superMibs) {
            // 递归实现树结构
            List<MmMib> mmMibs = handleDataFormat(mibs, mmMib.getObjectID());
            mmMib.setSubTree(mmMibs);
        }
        mibMap.put(mibName, superMibs);

        return JSON.toJSONString(mibMap);
    }

    public List<MmMib> handleDataFormat(List<MmMib> mibs, String parentId) {
        List<MmMib> resultList = new ArrayList<>();
        for (MmMib object : mibs) {
            String oid = object.getObjectID();
            String pid = object.getPid();
            if (parentId.equals(pid)) {
                List<MmMib> c_node = handleDataFormat(mibs, oid);
                object.setSubTree(c_node);
                resultList.add(object);
            }
        }
        return resultList;
    }
}
