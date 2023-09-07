package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.SoftwareAssetDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.SoftwareAsset;
import com.nis.modules.asset.entity.SoftwareType;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.SoftwareAssetService;
import com.nis.modules.asset.service.SoftwareTypeService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Software Asset Service Implement
 *
 * @author shizhendong
 * @date 2023-08-10
 */
@Service
public class SoftwareAssetServiceImpl extends ServiceImpl<SoftwareAssetDao, SoftwareAsset> implements SoftwareAssetService {

    private static final Log log = Log.get();

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private MonitorProjectService projectService;

    @Autowired
    private SoftwareTypeService softwareTypeService;

    @Override
    public SoftwareAsset querySoftwareAssetInfo(Integer id) {
        SoftwareAsset softwareAsset = this.getById(id);
        ValidateUtils.is(softwareAsset).notNull(RCode.SOFTWARE_ASSET_NOT_EXIST);

        AssetAsset refAsset = assetService.getById(softwareAsset.getAssetId());
        if (Tool.ObjectUtil.isNotNull(refAsset)) {
            Map<Object, Object> refAssetMap = Tool.MapUtil.builder()
                    .put("id", refAsset.getId())
                    .put("name", refAsset.getName())
                    .build();
            softwareAsset.setAsset(refAssetMap);
        }

        MonitorProject refProject = projectService.getById(softwareAsset.getProjectId());
        if (Tool.ObjectUtil.isNotNull(refProject)) {
            Map<Object, Object> refProjectMap = Tool.MapUtil.builder()
                    .put("id", refProject.getId())
                    .put("name", refProject.getName())
                    .build();
            softwareAsset.setProject(refProjectMap);
        }

        SoftwareType refType = softwareTypeService.getById(softwareAsset.getTypeId());
        if (Tool.ObjectUtil.isNotNull(refType)) {
            Map<Object, Object> refTypeMap = Tool.MapUtil.builder()
                    .put("id", refType.getId())
                    .put("name", refType.getName())
                    .put("category", refType.getCategory())
                    .build();
            softwareAsset.setType(refTypeMap);
        }

        Object softwareAssetParams = softwareAsset.getParams();
        if (Tool.ObjectUtil.isNotEmpty(softwareAssetParams)) {
            try {
                softwareAsset.setParams(JSONUtil.toBean(StrUtil.utf8Str(softwareAssetParams), LinkedHashMap.class));
            } catch (Exception e) {
                log.error(e, "[querySoftwareAssetInfo] [json format error] [id: {}]", id);
            }
        }
        return softwareAsset;
    }

    @Override
    public PageUtils querySoftwareAssetPage(Map<String, Object> params) {
        IPage page = new Query(SoftwareAsset.class).getPage(params);
        List<SoftwareAsset> softwareAssetList = this.getBaseMapper().querySoftwareAssetInfoList(page, params);

        for (SoftwareAsset softwareAsset : softwareAssetList) {
            Object softwareAssetParams = softwareAsset.getParams();
            if (Tool.ObjectUtil.isNotEmpty(softwareAssetParams)) {
                try {
                    softwareAsset.setParams(JSONUtil.toBean(StrUtil.utf8Str(softwareAssetParams), LinkedHashMap.class));
                } catch (Exception e) {
                    log.error(e, "[querySoftwareAssetPage] [json format error] [id: {}]", softwareAsset.getId());
                }
            }
        }

        page.setRecords(softwareAssetList);
        return new PageUtils(page);
    }

    /**
     * validate software asset
     *
     * @param softwareAsset
     */
    private void validateSoftwareAsset(SoftwareAsset softwareAsset) {
        // name duplicate
        String name = softwareAsset.getName();
        SoftwareAsset tempSoftwareAssetForValidateNameDuplicate = this.getOne(new LambdaQueryWrapper<SoftwareAsset>()
                .eq(SoftwareAsset::getName, name)
                .ne(ObjectUtil.isNotNull(softwareAsset.getId()), SoftwareAsset::getId, softwareAsset.getId())
                .last("limit 1"));
        if (ToolUtil.isNotEmpty(tempSoftwareAssetForValidateNameDuplicate)) {
            throw new NZException(RCode.SOFTWARE_ASSET_NAME_DUPLICATE);
        }

        Integer assetId = softwareAsset.getAssetId();
        if (assetId != null && assetId != -1) {
            AssetAsset asset = assetService.getById(assetId);
            ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);
        }

        Integer projectId = softwareAsset.getProjectId();
        if (projectId != null && projectId != -1) {
            MonitorProject project = projectService.getById(projectId);
            ValidateUtils.is(project).notNull(RCode.PROJECT_NOTFOUND);
        }

        Integer typeId = softwareAsset.getTypeId();
        if (typeId != null && typeId != -1) {
            SoftwareType type = softwareTypeService.getById(typeId);
            ValidateUtils.is(type).notNull(RCode.SOFTWARE_TYPE_NOT_EXIST);
        }
    }

    @Override
    public Integer saveOrUpdateSoftwareAsset(SoftwareAsset softwareAsset) {
        // validate software asset
        this.validateSoftwareAsset(softwareAsset);

        // set default value
        softwareAsset.setAssetId(ObjectUtil.defaultIfNull(softwareAsset.getAssetId(), -1));
        softwareAsset.setProjectId(ObjectUtil.defaultIfNull(softwareAsset.getProjectId(), -1));
        softwareAsset.setTypeId(ObjectUtil.defaultIfNull(softwareAsset.getTypeId(), -1));

        // params
        if (ObjectUtil.isNotEmpty(softwareAsset.getParams())) {
            softwareAsset.setParams(JSONUtil.toJsonStr(softwareAsset.getParams()));
        }

        Integer id = softwareAsset.getId();
        if (ObjectUtil.isNull(id)) {
            softwareAsset.setCts(System.currentTimeMillis());
        } else {
            SoftwareAsset tempSoftwareAssetForJudge = this.getById(id);
            if (ObjectUtil.isNull(tempSoftwareAssetForJudge)) {
                softwareAsset.setCts(System.currentTimeMillis());
            }
        }

        // uts
        softwareAsset.setUts(System.currentTimeMillis());

        // save or update
        this.saveOrUpdate(softwareAsset);
        return softwareAsset.getId();
    }

    @Override
    public R deleteSoftwareAsset(Integer force, List<Integer> idList) {
        List<SoftwareAsset> delSoftwareAssetList = this.list(new LambdaQueryWrapper<SoftwareAsset>().in(SoftwareAsset::getId, idList));
        if (Tool.CollUtil.isEmpty(delSoftwareAssetList)) {
            return R.ok().putData("list", Tool.ListUtil.empty());
        }

        // validate build-in data
        SoftwareAsset findFirstBuildInSoftwareAsset = delSoftwareAssetList.stream().filter(t -> ObjectUtil.equal(t.getBuildIn(), 1)).findFirst().orElse(null);
        if (ObjectUtil.isNotNull(findFirstBuildInSoftwareAsset)) {
            throw new NZException(RCode.SOFTWARE_ASSET_BUILTIN_CANNOT_DELETED);
        }

        List<Map> resultList = Tool.ListUtil.list(true);
        for (SoftwareAsset softwareAsset : delSoftwareAssetList) {
            Map<Object, Object> typeRefObjectInfo = Tool.MapUtil.builder()
                    .put("id", softwareAsset.getId())
                    .put("name", softwareAsset.getName())
                    .put("relates", Tool.ListUtil.empty())
                    .build();
            resultList.add(typeRefObjectInfo);
        }

        // remove
        this.removeByIds(idList);

        return R.ok().putData("list", resultList);
    }


}
