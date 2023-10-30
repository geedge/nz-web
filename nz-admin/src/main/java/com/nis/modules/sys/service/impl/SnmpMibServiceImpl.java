package com.nis.modules.sys.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.AssetBrand;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.entity.AssetTypeConf;
import com.nis.modules.asset.service.AssetBrandService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.asset.service.AssetTypeConfService;
import com.nis.modules.sys.dao.SnmpMibDao;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.MmMib;
import com.nis.modules.sys.entity.SnmpMib;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SnmpMibService;
import com.nis.modules.sys.shiro.ShiroUtils;
import net.percederberg.mibble.Mib;
import net.percederberg.mibble.MibLoader;
import net.percederberg.mibble.MibLoaderException;
import net.percederberg.mibble.MibValueSymbol;
import net.percederberg.mibble.snmp.SnmpIndex;
import net.percederberg.mibble.snmp.SnmpObjectType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("snmpMibService")
public class SnmpMibServiceImpl extends ServiceImpl<SnmpMibDao, SnmpMib> implements SnmpMibService {

    private static Log logger = Log.get();

    @Autowired
    private SnmpMibDao snmpMibDao;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private AssetModelService assetModelService;

    @Autowired
    private AssetTypeConfService assetTypeConfService;

    @Autowired
    private AssetBrandService brandService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String ids = (String) params.get("ids");
        if (StrUtil.isNotBlank(ids)) {
            List<String> listIds = Arrays.asList(ids.split(","));
            params.put("ids", listIds);
        }
        IPage page = new Query(SnmpMib.class).getPage(params);

        List<SnmpMib> mibs = snmpMibDao.queryList(page, params);

        // 获取所有 model
        List<AssetModel> assetModelList = assetModelService.list();
        Map<Integer, AssetModel> modelsDetailIdAndEntity = assetModelList.stream().collect(Collectors.toMap(AssetModel::getId, Function.identity()));

        // 获取所有的 asset type
        List<AssetTypeConf> typeConfs = assetTypeConfService.list();
        Map<Integer, AssetTypeConf> assetTypeConfMap = typeConfs.stream().collect(Collectors.toMap(AssetTypeConf::getId, Function.identity()));

        List<AssetBrand> brands = brandService.list();
        Map<Integer, AssetBrand> brandMap = brands.stream().collect(Collectors.toMap(AssetBrand::getId, Function.identity()));


        // 处理 modelDetail 信息
        List<AssetModel> modelsDetail = null;
        for (SnmpMib snmpMib : mibs) {
            snmpMib.setContent(null);
            if (ToolUtil.isNotEmpty(snmpMib.getModels())) {
                modelsDetail = new ArrayList<>();
                int[] ints = Arrays.asList(snmpMib.getModels().split(",")).stream().mapToInt(Integer::parseInt).toArray();
                for (int i = 0; i < ints.length; i++) {
                    AssetModel assetModel = modelsDetailIdAndEntity.get(ints[i]);
                    if (Tool.ObjectUtil.isNotEmpty(assetModel)) {
                        assetModel.setBrand(brandMap.get(assetModel.getBrandId()));
                        assetModel.setType(assetTypeConfMap.get(assetModel.getTypeId()));
                        modelsDetail.add(assetModel);
                    }
                }
                snmpMib.setModelsDetail(modelsDetail);
            }
        }
        page.setRecords(mibs);
        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateSnmpMib(SnmpMib snmpMib, MultipartFile multipartFile) {
        snmpMib.setName(StrUtil.trim(snmpMib.getName()));
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
        // 将 保存成功的 mib 上传到 generator 上，以提供生成 snmp.yml 使用
        this.copyMibFileForGenerator(multipartFile);
    }

    private void copyMibFileForGenerator(MultipartFile mibFile) {
        if (mibFile != null) {
            // 取出generator路径
            SysConfigEntity entity = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_GENERATOR_PATH));
            String mibsPath = entity.getParamValue() + File.separator + "mibs";
            try {
                File dest = new File(mibsPath + File.separator + mibFile.getOriginalFilename());
                FileUtils.copyInputStreamToFile(mibFile.getInputStream(), dest);
            } catch (IOException e) {
                logger.error(String.format("拷贝mib文件到%s下失败，失败信息是:%s", mibsPath, e));
            }
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

    public String mibTree(List<SnmpMib> mibList) {
        Map resultData = new HashMap();
        for (SnmpMib snmpMib : mibList) {
            resultData.putAll(JSONObject.parseObject(snmpMib.getTree(), Map.class));
        }

        // 默认排序 a-z
        Map<String, Object> sortMap = new TreeMap<>(new Comparator<String>() {
            public int compare(String obj1, String obj2) {
                return obj1.compareTo(obj2);
            }
        });
        sortMap.putAll(resultData);
        return JSONObject.toJSONString(sortMap);
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
            logger.error("transfer mib file error",e);
        }

        // 加载器
        MibLoader ml = new MibLoader();

        // 加载 mib 文件
        Mib mib = null;

        // 临时文件
        File mibFile = null;

        try {
            mibFile = new File(Constant.TEMP_PATH + File.separator + "tempMibFile" + UUID.randomUUID());
            // multipartFile.transferTo(mibFile.getAbsoluteFile());
            FileUtils.copyInputStreamToFile(multipartFile.getInputStream(), mibFile);
            mib = ml.load(mibFile);
            logger.info("mib name {}", mib != null ? mib.getName(): "mib 解析异常");
        } catch (IOException e) {
            logger.error("mib文件获取失败",e);
        } catch (MibLoaderException e) {
            logger.error("mib文件解析失败",e);
        } finally {
            if (mibFile != null && mibFile.exists()) {
                boolean delete = mibFile.delete();
                logger.debug("mib file delete: {}", delete);
            }
        }

        // 经过解析后 mib 为空  解析失败
        if(ObjectUtil.isEmpty(mib)){
            throw new NZException(RCode.SNMP_MIB_PARSING_FAILED);
        }
        // 设置mib文件名称
        String mibName = mib.getName();
        snmpMib.setName(mibName);

        // 最终解析的数据集合
        List<MmMib> resultList = new ArrayList<>();

        // 获取 mib 文件中所有的对象
        Collection c = mib.getAllSymbols();

        MmMib mo = null;
        List<String> index;
        MibValueSymbol parent = null;
        SnmpObjectType snmpObjectType = null;
        ArrayList<SnmpIndex> snmpIndices = new ArrayList();
        String syntax = null, access = null, status = null, description = null, type = null, parentValue = null;

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

                if (mvs.isTable()) {
                    mo.setType(Constant.MIB_TABLE_TYPE);
//                } else if (StringUtils.isNotEmpty(syntax) && Constant.MIB_ENTRY_SYNTAX.equals(syntax.toUpperCase())) {
                } else if (StringUtils.isNotEmpty(syntax) && StrUtil.equals(Constant.MIB_ENTRY_SYNTAX,syntax.toUpperCase())) {
                    mo.setType(Constant.MIB_ENTRY_TYPE);
                }

                type = mvs.getType().getName();
                if (StringUtils.isNotEmpty(type) && StringUtils.isEmpty(mo.getType())) {
                	String upperCase = type.toUpperCase();                	
                    if (StrUtil.contains(upperCase, Constant.MIB_IDENTIFIER_TYPE)) {
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

                resultList.add(mo);
            }
        }

        // tree
        snmpMib.setTree(this.getTreeFormat(resultList, mibName));
    }

    public String getTreeFormat(List<MmMib> mibs, String mibName) {
        // 找到顶级节点
        Map map = new HashMap();
        for (MmMib mmMib : mibs) {
            map.put(mmMib.getObjectID(), mmMib);
        }


        List<MmMib> superMibs = new ArrayList<>();
        for (MmMib mmMib : mibs) {
            if (ToolUtil.isEmpty(map.get(mmMib.getPid()))) {
                superMibs.add(mmMib);
            }
        }

        Map mibMap = new HashMap();
        for (MmMib mmMib : superMibs) {
            // 递归实现树结构
            mmMib.setSubTree(this.handleDataFormat(mibs, mmMib.getObjectID()));
        }
        mibMap.put(mibName, superMibs);

        return JSON.toJSONString(mibMap);
    }

    public List<MmMib> handleDataFormat(List<MmMib> mibs, String parentId) {
        List<MmMib> resultList = new ArrayList<>();
        String oid;
        String pid;
        for (MmMib object : mibs) {
            oid = object.getObjectID();
            pid = object.getPid();
            if(StrUtil.equals(parentId,pid)) {
                object.setSubTree(this.handleDataFormat(mibs, oid));
                resultList.add(object);
            }
        }
        return resultList;
    }


    @Override
    public Map<String, String> getAllTreeNodeOidAndNameMap() {
        List<SnmpMib> mibList = this.list();
        List<String> treeList = mibList.stream().map(SnmpMib::getTree).collect(Collectors.toList());

        Map map;
        List<MmMib> values = new ArrayList<>();
        for (String str : treeList) {
            map = JSONObject.parseObject(str, Map.class);
            values.addAll(JSONArray.parseArray(JSONArray.toJSONString(new ArrayList<JSONArray>(map.values()).get(0)), MmMib.class));
        }

        Map<String, String> oidWithNameMap = new HashMap<>();

        List<MmMib> subTree;
        Map<String, String> collectMap;

        for (MmMib mmMib : values) {
            oidWithNameMap.put(mmMib.getObjectID(), mmMib.getName());
            // 如果存在子树 则继续
            if (CollectionUtils.isNotEmpty(mmMib.getSubTree())) {
                subTree = this.getSubTree(mmMib.getSubTree());
                collectMap = subTree.stream().collect(Collectors.toMap(MmMib::getObjectID, MmMib::getName));
                oidWithNameMap.putAll(collectMap);
            }
        }

        return oidWithNameMap;
    }


    public List<MmMib> getSubTree(List<MmMib> list) {
        List<MmMib> resultList = new ArrayList<>();
        for (MmMib mmMib : list) {
            resultList.add(mmMib);
            if (CollectionUtils.isNotEmpty(mmMib.getSubTree())) {
                resultList.addAll(this.getSubTree(mmMib.getSubTree()));
            }
        }
        return resultList;
    }
}
