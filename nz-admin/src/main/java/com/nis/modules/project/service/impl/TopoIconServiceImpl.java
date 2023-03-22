package com.nis.modules.project.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.project.dao.TopoIconDao;
import com.nis.modules.project.entity.TopoIcon;
import com.nis.modules.project.service.TopoIconService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service("topoIconService")
public class TopoIconServiceImpl extends ServiceImpl<TopoIconDao, TopoIcon> implements TopoIconService {

    private static final Log log = Log.get();

    @Autowired
    private TopoIconDao topoIconDao;

    @Override
    public List<String> queryUnitList(String name) {
        List<TopoIcon> topoIcons = topoIconDao.selectList(new LambdaQueryWrapper<TopoIcon>().like(Tool.StrUtil.isNotEmpty(name), TopoIcon::getName, Tool.StrUtil.likeEscape(name)));
        List<String> unitNameList = topoIcons.stream().map(TopoIcon::getUnit).sorted().distinct().collect(Collectors.toList());

        List<String> resultList = Tool.ListUtil.list(true);

        Iterator<String> iterator = unitNameList.iterator();
        while (iterator.hasNext()) {
            String unitName = iterator.next();
            // default index = 0
            if (StrUtil.equalsIgnoreCase("default", unitName)) {
                resultList.add(unitName);
                iterator.remove();
            }
        }
        resultList.addAll(unitNameList);
        return resultList;
    }

    @Override
    public void renameUnit(String oldUnitName, String newUnitName) {
        LambdaUpdateWrapper updateWrapper = new LambdaUpdateWrapper<TopoIcon>()
                .set(TopoIcon::getUnit, newUnitName)
                .eq(TopoIcon::getUnit, oldUnitName);

        // update unit value
        this.update(updateWrapper);
    }

    @Override
    public void deleteIconByUnit(String unit) {
        this.remove(new LambdaQueryWrapper<TopoIcon>().eq(TopoIcon::getUnit, unit));
    }


    @Override
    public List<TopoIcon> queryIconList(Map<String, Object> params) {
        List<TopoIcon> topoIcons = topoIconDao.queryIconList(params);
        return topoIcons;
    }

    @Override
    public Integer saveTopoIcon(TopoIcon icon, MultipartFile file) {
        String filename = file.getOriginalFilename();
        String fileType = FilenameUtils.getExtension(filename);
        // validate file type
        if (Tool.BooleanUtil.negate(StrUtil.equalsAnyIgnoreCase(fileType, "jpg", "jpeg", "png", "gif"))) {
            throw new NZException(RCode.PROJECT_TOPOICON_FILETYPE_ERROR);
        }

        // name
        List<TopoIcon> list = this.list(new LambdaUpdateWrapper<TopoIcon>().eq(TopoIcon::getUnit, icon.getUnit()).eq(TopoIcon::getName, StrUtil.trim(icon.getName())));
        if (Tool.CollUtil.isNotEmpty(list)) {
            throw new NZException(RCode.PROJECT_ICON_NAME_DUPLICATE);
        }

        Integer width = icon.getWidth();
        Integer height = icon.getHeight();
        if (width == null || height == null) {
            try {
                log.info("[saveTopoIcon] [width and height has null val, get value from the stream] [width: {}] [height: {}]", width, height);
                BufferedImage read = ImageIO.read(file.getInputStream());
                width = read.getWidth();
                height = read.getHeight();
                log.info("[saveTopoIcon] [get value from the stream finshed] [width: {}] [height: {}]", width, height);
            } catch (IOException e) {
                log.error(e, "[saveTopoIcon] [get picture width and height error] [icon: {}]", Tool.JSONUtil.toJsonStr(icon));
            }
        }

        TopoIcon topoIcon = new TopoIcon();
        topoIcon.setName(StrUtil.trim(icon.getName()));
        topoIcon.setType(fileType);
        topoIcon.setUnit(icon.getUnit());
        topoIcon.setWidth(width == null ? -1 : width);
        topoIcon.setHeight(height == null ? -1 : height);
        try {
            // file bytes
            topoIcon.setBytes(Base64.getEncoder().encodeToString(file.getBytes()));
            this.save(topoIcon);
        } catch (Exception e) {
            if (e instanceof TransientDataAccessResourceException) {
                if (e.getMessage().contains("PacketTooBigException")) {
                    throw new NZException(RCode.PROJECT_ICON_BIG_ERROR);
                }
            }
            log.error(e, "[saveTopoIcon] [error] [icon: {}]", Tool.JSONUtil.toJsonStr(icon));
            throw new NZException(RCode.ERROR);
        }
        return topoIcon.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeIcons(Integer[] ids) {
        List<Integer> idList = Arrays.asList(ids);
        this.removeByIds(idList);
    }

    @Override
    public void downloadIconFileById(Integer id, Integer base64, HttpServletResponse response) {
        TopoIcon icon = this.getById(id);
        if (ToolUtil.isEmpty(icon)) {
            throw new NZException(RCode.PROJECT_ICON_NOTFOUND);
        }

        base64 = base64 == null ? 0 : base64;
        if (base64 != 0 && base64 != 1) {
            throw new NZException(RCode.PROJECT_ICON_BASE64_VALUEERROR);
        }
        // 1: base64格式  0：非base64格式
        byte[] bytes = base64 == 0 ? Base64.getDecoder().decode(icon.getBytes()) : icon.getBytes().getBytes();

        try (OutputStream os = new BufferedOutputStream(response.getOutputStream());) {
            if (base64 == 0) {
                String contentType = StrUtil.equals("jpg", icon.getType()) ? "jpeg" : icon.getType().toLowerCase();
                response.setContentType("image/" + contentType);
            } else if (base64 == 1) {
                response.setContentType("text/plain");
            }
            response.setHeader("Content-Disposition", "inline");
            response.setHeader("width", String.valueOf(icon.getWidth()));
            response.setHeader("height", String.valueOf(icon.getHeight()));
            response.setHeader("Access-Control-Expose-Headers", "width,height");
            // 缓存12小时
            response.setHeader("Cache-Control", "max-age=" + 12 * 60 * 60 + ", must-revalidate");
//            response.setDateHeader("Expires", System.currentTimeMillis() + 12 * 60 * 60 * 1000);
            os.write(bytes);
            os.flush();
        } catch (IOException e) {
            log.error(String.format("拓扑图图标预览失败，参数是：%s,错误信息是：%s", id, e));
        }
    }

    @Override
    public void downloadIconFile(String unit, String name, Integer base64, HttpServletResponse response) {
        List<TopoIcon> list = this.list(new LambdaUpdateWrapper<TopoIcon>().eq(TopoIcon::getUnit, unit).eq(TopoIcon::getName, name));
        if (Tool.CollUtil.isEmpty(list)) {
            throw new NZException(RCode.PROJECT_ICON_NOTFOUND);
        }

        if (list.size() > 1) {
            log.warn("[downloadIconFile] [Query multiple icons by criteria, and return to the first one] [query unit: {}] [query name: {}] [size: {}]", unit, name, list.size());
        }

        base64 = Tool.ObjectUtil.defaultIfNull(base64, 0);
        if (!Tool.ListUtil.of(0, 1).contains(base64)) {
            throw new NZException(RCode.PROJECT_ICON_BASE64_VALUEERROR);
        }

        // 取第一个相应，防止 unit 下存在同名图标
        TopoIcon icon = list.get(0);

        byte[] bytes = base64 == 0 ? Base64.getDecoder().decode(icon.getBytes()) : icon.getBytes().getBytes();
        try (OutputStream os = new BufferedOutputStream(response.getOutputStream());) {
            if (base64 == 0) {
                String contentType = StrUtil.equals("jpg", icon.getType()) ? "jpeg" : icon.getType().toLowerCase();
                response.setContentType("image/" + contentType);
            } else if (base64 == 1) {
                response.setContentType("text/plain");
            }
            response.setHeader("Content-Disposition", "inline");
            response.setHeader("width", String.valueOf(icon.getWidth()));
            response.setHeader("height", String.valueOf(icon.getHeight()));
            response.setHeader("Access-Control-Expose-Headers", "width,height");
            // 缓存 12 小时
            response.setHeader("Cache-Control", "max-age=" + 12 * 60 * 60 + ", must-revalidate");

            os.write(bytes);
            os.flush();
        } catch (IOException e) {
            log.error(e, "[downloadIconFile] [error] [name: {}] [unit: {}]", name, unit);
        }
    }
}
