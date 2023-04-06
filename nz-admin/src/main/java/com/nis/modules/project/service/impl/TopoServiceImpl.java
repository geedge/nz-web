package com.nis.modules.project.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.dao.TopoDao;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.entity.Topo;
import com.nis.modules.project.entity.TopoIcon;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.project.service.TopoIconService;
import com.nis.modules.project.service.TopoService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


@Service("topoService")
public class TopoServiceImpl extends ServiceImpl<TopoDao, Topo> implements TopoService {

    private static final Log log = Log.get();

    @Autowired
    private TopoIconService iconService;

    @Autowired
    private MonitorProjectService projectService;

    @Override
    public Map queryTopoByProjectId(Integer projectId) {
        Topo topo = this.getOne(new QueryWrapper<Topo>().lambda().eq(Topo::getProjectId, projectId));
        if (ToolUtil.isEmpty(topo) || ToolUtil.isEmpty(topo.getTopo())) {
            return null;
        }
        return JSONObject.parseObject(topo.getTopo().toString(), Map.class);
    }

    @Override
    public void updateProjectTopo(Integer projectId, String topoConfig) {
        // 校验是否存在改 project
        MonitorProject project = projectService.getById(projectId);
        if (project == null) {
            throw new NZException(RCode.PROJECT_NOTFOUND);
        }

        Topo topo = this.getOne(new QueryWrapper<Topo>().lambda().eq(Topo::getProjectId, projectId));
        topo = ToolUtil.isEmpty(topo) ? new Topo() : topo;
        topo.setProjectId(projectId);
        topo.setUpdateAt(new Date());
        topo.setUpdateBy(ShiroUtils.getUserId().intValue());
        topo.setTopo(topoConfig);
        this.saveOrUpdate(topo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTopoConfigNodeAndLineByModules(List<MonitorModule> modules) {
        Set<Integer> projectIds = modules.stream().map(MonitorModule::getProjectId).collect(Collectors.toSet());
        List<Topo> topos = this.list(new QueryWrapper<Topo>().lambda().in(Topo::getProjectId, projectIds));
        if (CollectionUtils.isEmpty(topos)) {
            return;
        }
        Map<Integer, Object> topoProjectIdAndConfigMap = topos.stream().collect(Collectors.toMap(Topo::getProjectId, Topo::getTopo));

        // module 是批量删除，删除的或许不属于同一个 project
        Map<Integer, List<MonitorModule>> projectIdAndMoudles = modules.stream().collect(Collectors.groupingBy(MonitorModule::getProjectId));

        Object topoConfig;
        Map<String, JSONArray> configMap;
        List<Integer> moduleIds;
        for (Map.Entry<Integer, List<MonitorModule>> entry : projectIdAndMoudles.entrySet()) {
            topoConfig = topoProjectIdAndConfigMap.get(entry.getKey());
            if (ToolUtil.isEmpty(topoConfig)) {
                continue;
            }

            moduleIds = entry.getValue().stream().map(MonitorModule::getId).collect(Collectors.toList());
            configMap = JSONObject.parseObject(topoConfig.toString(), Map.class);
            configMap.put("pens", this.removeModuleEntity(configMap.get("pens"), moduleIds));
            topoProjectIdAndConfigMap.put(entry.getKey(), JSONObject.toJSONString(configMap));
        }

        Date now = new Date();
        Integer userId = ShiroUtils.getUserId().intValue();
        for (Topo topo : topos) {
            topo.setTopo(topoProjectIdAndConfigMap.get(topo.getProjectId()).toString());
            topo.setUpdateBy(userId);
            topo.setUpdateAt(now);
        }
        this.saveOrUpdateBatch(topos);
    }


    private JSONArray removeModuleEntity(JSONArray pens, List<Integer> moduleIds) {
        Map<String, Object> node, nodeData;
        Integer moduleId, type;
        Iterator<Object> iterator = pens.iterator();
        while (iterator.hasNext()) {
            node = (Map<String, Object>) iterator.next();
            type = (Integer) node.get("type");
            // type 为 0 是 module 节点，只处理 节点
            if (!type.equals(0)) {
                continue;
            }

            nodeData = (Map<String, Object>) node.get("data");
            moduleId = (Integer) nodeData.get("moduleId");
            if (moduleIds.contains(moduleId)) {
                iterator.remove();
            }
        }
        return pens;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topoImport(Integer projectId, Map topoConfigMap, List<TopoIcon> icons) {
        // save Icons For Topo Import
        List<TopoIcon> topoIcons = this.saveIconsForTopoImport(icons);

        // pens
        List<Map> pens = (List) topoConfigMap.getOrDefault("pens", Tool.ListUtil.list(true));
        for (Map<String, Object> pen : pens) {
            Object imageObj = pen.get("image");
            String imageStr = StrUtil.toStringOrNull(imageObj);
            if (Tool.ObjectUtil.isNull(imageStr)) {
                continue;
            }

            // get iconName by file path. example: /png/2.5D-智慧城市/马路1.svg
            String iconName = FilenameUtils.getBaseName(imageStr);
            TopoIcon icon = topoIcons.stream().filter(topoIcon -> StrUtil.equals(topoIcon.getName(), iconName)).findFirst().orElse(null);
            if (Tool.ObjectUtil.isNotNull(icon)) {
                Integer iconId = icon.getId();

                pen.put("imageId", iconId);
                pen.put("image", StrUtil.concat(true, "/monitor/project/topo/icon/", iconId.toString(), "/0"));
            }
        }
        // set pens
        topoConfigMap.put("pens", pens);

        Topo topology = this.getOne(new LambdaQueryWrapper<Topo>().eq(Topo::getProjectId, projectId));
        if (Tool.ObjectUtil.isNull(topology)) {
            // save
            Topo topo = new Topo();
            topo.setProjectId(projectId);
            topo.setUpdateAt(new Date());
            topo.setUpdateBy(ShiroUtils.getUserId().intValue());
            topo.setTopo(Tool.JSONUtil.toJsonStr(topoConfigMap));
            this.save(topo);
        } else {
            // update
            LambdaUpdateWrapper<Topo> updateWrapper = new LambdaUpdateWrapper<Topo>()
                    .set(Topo::getUpdateAt, new Date())
                    .set(Topo::getUpdateBy, ShiroUtils.getUserId().intValue())
                    .set(Topo::getTopo, Tool.JSONUtil.toJsonStr(topoConfigMap))
                    .eq(Topo::getId, topology.getId());
            this.update(updateWrapper);
        }
    }

    /**
     * save Icons For Topo Import
     *
     * @param icons
     * @return
     */
    private List<TopoIcon> saveIconsForTopoImport(List<TopoIcon> icons) {
        List<TopoIcon> topoImagesSource = Tool.ListUtil.list(true);

        // distinct
        icons = icons.stream().distinct().collect(Collectors.toList());
        for (TopoIcon icon : icons) {
            // 先在数据库中查找
            List<TopoIcon> queryIconsByName = iconService.list(new LambdaQueryWrapper<TopoIcon>()
                    .eq(TopoIcon::getUnit, icon.getUnit())
                    .eq(TopoIcon::getName, icon.getName()));

            TopoIcon iconInDb = queryIconsByName.stream().filter(topoIconInDb -> StrUtil.equals(topoIconInDb.getBytes(), icon.getBytes())).findFirst().orElse(null);
            if (iconInDb != null) {
                // 能在数据库中找到相同的 icon （unit,name,bytes），则引用数据库中的 icon 即可
                topoImagesSource.add(iconInDb);
            } else {
                // validate name
                List<TopoIcon> list = iconService.list(new LambdaUpdateWrapper<TopoIcon>().eq(TopoIcon::getUnit, icon.getUnit()).eq(TopoIcon::getName, icon.getName()));
                if (Tool.CollUtil.isNotEmpty(list)) {
                    log.error("[saveIconsForTopoImport] [name duplicate] [icon: {}]", Tool.JSONUtil.toJsonStr(icon));
                    throw new NZException(RCode.PROJECT_ICON_NAME_DUPLICATE);
                }

                Integer width = icon.getWidth();
                Integer height = icon.getHeight();
                if (width == null || height == null) {
                    String bytes = icon.getBytes();
                    byte[] data = Base64.getDecoder().decode(bytes);
                    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);) {
                        BufferedImage read = ImageIO.read(byteArrayInputStream);
                        icon.setWidth(read.getWidth());
                        icon.setHeight(read.getHeight());
                    } catch (Exception e) {
                        log.error(e, "[saveIconsForTopoImport] [get picture width and height error] [icon: {}]", Tool.JSONUtil.toJsonStr(icon));
                    }
                }

                // save
                iconService.save(icon);

                topoImagesSource.add(icon);
            }
        }
        return topoImagesSource;
    }

    @Override
    public void topoExport(Integer projectId, HttpServletResponse response) {
        MonitorProject project = projectService.getById(projectId);
        ValidateUtils.is(project).notNull(RCode.PROJECT_NOTFOUND);

        Topo topology = this.getOne(new LambdaQueryWrapper<Topo>().eq(Topo::getProjectId, projectId));
        JSONObject resultJsonObject = new JSONObject();
        resultJsonObject.put("topo", new JSONObject());
        resultJsonObject.put("icons", new JSONArray());

        if (Tool.ObjectUtil.isNotNull(topology)) {
            String topoConfJsonStr = topology.getTopo().toString();
            resultJsonObject.put("topo", Tool.JSONUtil.parseObj(topoConfJsonStr));

            JSONObject jsonObject = JSONObject.parseObject(topoConfJsonStr);
            List<Map> pens = (List) jsonObject.getOrDefault("pens", Tool.ListUtil.list(true));

            List<String> iconIdListUsedInTopo = Tool.ListUtil.list(true);
            for (Map<String, Object> pen : pens) {
                Object imageObj = pen.get("image");
                String imageStr = StrUtil.toStringOrNull(imageObj);
                if (Tool.ObjectUtil.isNull(imageStr)) {
                    continue;
                }

                String iconId = Tool.ReUtil.getGroup1("/monitor/project/topo/icon/(\\d+)/0", imageStr);
                if (Tool.ObjectUtil.isNotEmpty(iconId)) {
                    iconIdListUsedInTopo.add(iconId);
                }
            }
            if (Tool.CollUtil.isNotEmpty(iconIdListUsedInTopo)) {
                List<TopoIcon> iconList = iconService.list(new LambdaUpdateWrapper<TopoIcon>().in(TopoIcon::getId, iconIdListUsedInTopo));
                resultJsonObject.put("icons", iconList);
            }
        }

        File tmplFile = null;
        try {
            tmplFile = FileUtil.createTempFile("Topology", ".json", true);

            // write bytes
            FileUtil.writeUtf8String(Tool.JSONUtil.toJsonPrettyStr(resultJsonObject), tmplFile);

            String absolutePath = tmplFile.getAbsolutePath();
            String fileName = FilenameUtils.getName(absolutePath);
            Tool.ResponseUtil.downloadFile(response, fileName, FileUtil.readBytes(tmplFile));
        } catch (Exception e) {
            log.error(e, "[topoExport] [error] [projectId: {}]", projectId);
        } finally {
            FileUtil.del(tmplFile);
        }
    }

}
