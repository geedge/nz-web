package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.SoftwareTypeDao;
import com.nis.modules.asset.entity.SoftwareAsset;
import com.nis.modules.asset.entity.SoftwareType;
import com.nis.modules.asset.service.SoftwareAssetService;
import com.nis.modules.asset.service.SoftwareTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Software Type Service Implement
 *
 * @author shizhendong
 * @date 2023-08-10
 */
@Service
public class SoftwareTypeServiceImpl extends ServiceImpl<SoftwareTypeDao, SoftwareType> implements SoftwareTypeService {

    private static final Log log = Log.get();

    @Autowired
    private SoftwareAssetService softwareAssetService;

    @Override
    public SoftwareType querySoftwareTypeInfo(Integer id) {
        SoftwareType softwareType = this.getById(id);
        ValidateUtils.is(softwareType).notNull(RCode.SOFTWARE_TYPE_NOT_EXIST);

        Object softwareTypeParams = softwareType.getParams();
        if (Tool.ObjectUtil.isNotEmpty(softwareTypeParams)) {
            try {
                softwareType.setParams(JSONUtil.toBean(StrUtil.utf8Str(softwareTypeParams), Map.class));
            } catch (Exception e) {
                log.error(e, "[querySoftwareTypeInfo] [json format error] [id: {}]", id);
            }
        }
        return softwareType;
    }

    @Override
    public PageUtils querySoftwareTypePage(Map<String, Object> params) {
        String ids = (String) params.get("ids");
        String name = (String) params.get("name");
        String category = (String) params.get("category");

        IPage<SoftwareType> result = this.page(
                new Query(SoftwareType.class).getPage(params),
                new LambdaUpdateWrapper<SoftwareType>()
                        .in(Tool.StrUtil.isNotEmpty(ids), SoftwareType::getId, Tool.StrUtil.split(ids, ","))
                        .like(Tool.StrUtil.isNotEmpty(name), SoftwareType::getName, Tool.StrUtil.likeEscape(name))
                        .like(Tool.StrUtil.isNotEmpty(category), SoftwareType::getCategory, Tool.StrUtil.likeEscape(category)));

        for (SoftwareType softwareType : result.getRecords()) {
            Object softwareTypeParams = softwareType.getParams();
            if (Tool.ObjectUtil.isNotEmpty(softwareTypeParams)) {
                try {
                    softwareType.setParams(JSONUtil.toBean(StrUtil.utf8Str(softwareTypeParams), Map.class));
                } catch (Exception e) {
                    log.error(e, "[querySoftwareTypePage] [json format error] [id: {}]", softwareType.getId());
                }
            }
        }

        return new PageUtils(result);
    }

    @Override
    public Integer saveOrUpdateSoftwareType(SoftwareType softwareType) {
        // name duplicate
        String name = softwareType.getName();
        SoftwareType tempSoftwareTypeForValidateNameDuplicate = this.getOne(new LambdaQueryWrapper<SoftwareType>()
                .eq(SoftwareType::getName, name)
                .ne(ObjectUtil.isNotNull(softwareType.getId()), SoftwareType::getId, softwareType.getId())
                .last("limit 1"));
        if (ToolUtil.isNotEmpty(tempSoftwareTypeForValidateNameDuplicate)) {
            throw new NZException(RCode.SOFTWARE_TYPE_NAME_DUPLICATE);
        }

        // params
        if (ObjectUtil.isNotEmpty(softwareType.getParams())) {
            softwareType.setParams(JSONUtil.toJsonStr(softwareType.getParams()));
        }

        Integer id = softwareType.getId();
        if (ObjectUtil.isNull(id)) {
            softwareType.setCts(System.currentTimeMillis());
        } else {
            SoftwareType tempSoftwareTypeForJudge = this.getById(id);
            if (ObjectUtil.isNull(tempSoftwareTypeForJudge)) {
                softwareType.setCts(System.currentTimeMillis());
            }
        }

        // uts
        softwareType.setUts(System.currentTimeMillis());

        // save or update
        this.saveOrUpdate(softwareType);
        return softwareType.getId();
    }

    /**
     * get software type ref object info
     *
     * @param type
     * @return
     */
    private Map<Object, Object> getSoftwareTypeRefObjectInfo(SoftwareType type) {
        List refObjectInfoList = Tool.ListUtil.list(true);

        List<SoftwareAsset> typeRefSoftwareAssetList = softwareAssetService.list(new LambdaQueryWrapper<SoftwareAsset>().eq(SoftwareAsset::getTypeId, type.getId()));
        for (SoftwareAsset softwareAsset : typeRefSoftwareAssetList) {
            Map<Object, Object> map = Tool.MapUtil.builder()
                    .put("id", softwareAsset.getId())
                    .put("name", softwareAsset.getName())
                    .put("type", "software")
                    .build();
            refObjectInfoList.add(map);
        }

        Map<Object, Object> typeRefObjectInfo = Tool.MapUtil.builder()
                .put("id", type.getId())
                .put("name", type.getName())
                .put("relates", refObjectInfoList)
                .build();
        return typeRefObjectInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R deleteSoftwareType(Integer force, List<Integer> idList) {
        List<SoftwareType> delTypeList = this.list(new LambdaQueryWrapper<SoftwareType>().in(SoftwareType::getId, idList));
        if (Tool.CollUtil.isEmpty(delTypeList)) {
            return R.ok().putData("list", Tool.ListUtil.empty());
        }

        // validate build-in data
        SoftwareType findFirstBuildInType = delTypeList.stream().filter(t -> ObjectUtil.equal(t.getBuildIn(), 1)).findFirst().orElse(null);
        if (ObjectUtil.isNotNull(findFirstBuildInType)) {
            throw new NZException(RCode.SOFTWARE_TYPE_BUILTIN_CANNOT_DELETED);
        }

        List<Map> resultList = Tool.ListUtil.list(true);
        for (SoftwareType type : delTypeList) {
            Map<Object, Object> typeRefObjectInfo = this.getSoftwareTypeRefObjectInfo(type);
            resultList.add(typeRefObjectInfo);
        }

        int relatesNum = resultList.stream().mapToInt(value -> Tool.CollUtil.size(value.get("relates"))).sum();
        if (force == 0 && relatesNum > 0) {
            return R.error(RCode.SOFTWARE_TYPE_RELOBJ_CAN_NOT_REMOVE).putData("list", resultList);
        }

        if (force == 1) {
            // type
            this.removeByIds(idList);

            // software asset
            softwareAssetService.remove(new LambdaQueryWrapper<SoftwareAsset>().in(SoftwareAsset::getTypeId, idList));
            return R.ok().putData("list", resultList);
        }
        return R.error(RCode.ERROR);
    }

}
