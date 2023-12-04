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
import com.nis.modules.asset.dao.SoftwareAssetDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.SoftwareAccSession;
import com.nis.modules.asset.entity.SoftwareAsset;
import com.nis.modules.asset.entity.SoftwareType;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.SoftwareAccSessionService;
import com.nis.modules.asset.service.SoftwareAssetService;
import com.nis.modules.asset.service.SoftwareTypeService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private SoftwareTypeService softwareTypeService;

    @Autowired
    private SoftwareAccSessionService softwareAccSessionService;

    @Override
    public SoftwareAsset querySoftwareAssetInfo(Integer id) {
        SoftwareAsset softwareAsset = this.getById(id);
        ValidateUtils.is(softwareAsset).notNull(RCode.SOFTWARE_ASSET_NOT_EXIST);

        AssetAsset refAsset = assetService.getById(softwareAsset.getAssetId());
        if (Tool.ObjectUtil.isNotNull(refAsset)) {
            softwareAsset.setAsset(refAsset);
        }

        SoftwareType refType = softwareTypeService.getById(softwareAsset.getTypeId());
        if (Tool.ObjectUtil.isNotNull(refType)) {
            softwareAsset.setType(refType);
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

        Integer typeId = softwareAsset.getTypeId();
        if (typeId != null && typeId != -1) {
            SoftwareType type = softwareTypeService.getById(typeId);
            ValidateUtils.is(type).notNull(RCode.SOFTWARE_TYPE_NOT_EXIST);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer saveOrUpdateSoftwareAsset(SoftwareAsset softwareAsset) {
        // validate software asset
        this.validateSoftwareAsset(softwareAsset);

        // set default value
        softwareAsset.setAssetId(ObjectUtil.defaultIfNull(softwareAsset.getAssetId(), -1));
        softwareAsset.setTypeId(ObjectUtil.defaultIfNull(softwareAsset.getTypeId(), -1));
        softwareAsset.setParams(ObjectUtil.defaultIfNull(softwareAsset.getParams(), StrUtil.EMPTY_JSON));

        // params
        if (ObjectUtil.isNotEmpty(softwareAsset.getParams())) {
            softwareAsset.setParams(JSONUtil.toJsonStr(softwareAsset.getParams()));
        }

        Integer id = softwareAsset.getId();
        if (ObjectUtil.isNull(id)) {
            softwareAsset.setCid(ShiroUtils.getUserId().intValue());
            softwareAsset.setCts(System.currentTimeMillis());
        } else {
            SoftwareAsset tempSoftwareAssetForJudge = this.getById(id);
            if (ObjectUtil.isNull(tempSoftwareAssetForJudge)) {
                softwareAsset.setCid(ShiroUtils.getUserId().intValue());
                softwareAsset.setCts(System.currentTimeMillis());
            } else {
                // 判断名称是否变更
                String oldName = tempSoftwareAssetForJudge.getName();
                if (!StrUtil.equals(oldName, softwareAsset.getName())) {
                    // acc session said
                    softwareAccSessionService.update(new LambdaUpdateWrapper<SoftwareAccSession>()
                            .set(SoftwareAccSession::getSaid, softwareAsset.getName())
                            .eq(SoftwareAccSession::getSaid, oldName));
                }
            }
        }

        // uid&uts
        softwareAsset.setUid(ShiroUtils.getUserId().intValue());
        softwareAsset.setUts(System.currentTimeMillis());

        // save or update
        this.saveOrUpdate(softwareAsset);
        return softwareAsset.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        // acc session
        List<String> delNameList = delSoftwareAssetList.stream().map(SoftwareAsset::getName).toList();
        softwareAccSessionService.remove(new LambdaQueryWrapper<SoftwareAccSession>().in(SoftwareAccSession::getSaid, delNameList));

        return R.ok().putData("list", resultList);
    }

    @Override
    public List<SoftwareAsset> getSoftwareAssetListByTypeName(String... typeNames) {
        if (Tool.ArrayUtil.isEmpty(typeNames)) {
            return Tool.ListUtil.empty();
        }

        List<SoftwareType> softwareTypeList = softwareTypeService.list(new LambdaQueryWrapper<SoftwareType>().in(SoftwareType::getName, typeNames));
        Integer[] idArr = softwareTypeList.stream().map(SoftwareType::getId).toArray(Integer[]::new);
        if (Tool.ArrayUtil.isEmpty(idArr)) {
            return Tool.ListUtil.empty();
        }

        return this.getSoftwareAssetListByTypeId(idArr);
    }

    @Override
    public List<SoftwareAsset> getSoftwareAssetListByTypeId(Integer... typeIds) {
        if (Tool.ArrayUtil.isEmpty(typeIds)) {
            return Tool.ListUtil.empty();
        }

        List<SoftwareAsset> softwareAssetList = this.list(new LambdaQueryWrapper<SoftwareAsset>().in(SoftwareAsset::getTypeId, typeIds));
        if (Tool.CollUtil.isNotEmpty(softwareAssetList)) {
            for (SoftwareAsset softwareAsset : softwareAssetList) {
                AssetAsset refAsset = assetService.getById(softwareAsset.getAssetId());
                if (Tool.ObjectUtil.isNotNull(refAsset)) {
                    softwareAsset.setAsset(refAsset);
                }
                SoftwareType refType = softwareTypeService.getById(softwareAsset.getTypeId());
                if (Tool.ObjectUtil.isNotNull(refType)) {
                    softwareAsset.setType(refType);
                }
            }
        }
        return softwareAssetList;
    }


}
