package com.nis.modules.project.service.impl;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import com.nis.common.utils.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.project.dao.TopoIconDao;
import com.nis.modules.project.entity.TopoIcon;
import com.nis.modules.project.service.TopoIconService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

@Service("topoIconService")
public class TopoIconServiceImpl extends ServiceImpl<TopoIconDao, TopoIcon> implements TopoIconService {

    private static Log log = Log.get();

    @Autowired
    private TopoIconDao topoIconDao;

    @Override
    public List<TopoIcon> queryIconList(Map<String, Object> params) {
        List<TopoIcon> topoIcons = topoIconDao.queryIconList(params);
        return topoIcons;
    }

    @Override
    public Integer saveTopoIcon(TopoIcon icon, MultipartFile file) throws IOException{
        // 校验文件类型
        this.validateIconFileType(file);
        String filename = file.getOriginalFilename();
        String fileType = filename.substring(filename.lastIndexOf(".") + 1);

        Integer width = icon.getWidth();
        Integer height = icon.getHeight();
        if (width == null || height == null) {
            try {
                BufferedImage read = ImageIO.read(file.getInputStream());
                width = read.getWidth();
                height = read.getHeight();
            } catch (IOException e) {
                log.error("获取图片宽高信息失败" + JSONObject.toJSONString(icon), e);
            }
        }

        TopoIcon topoIcon = new TopoIcon();
        topoIcon.setName(icon.getName());
        topoIcon.setType(fileType);
        topoIcon.setUnit(icon.getUnit());
        topoIcon.setBytes(Base64.getEncoder().encodeToString(file.getBytes()));
        topoIcon.setWidth(width == null ? -1 : width);
        topoIcon.setHeight(height == null ? -1 : height);
        try {
			this.save(topoIcon);
		} catch (Exception e) {
			if(e instanceof TransientDataAccessResourceException) {
				if(e.getMessage().contains("PacketTooBigException")) {
					throw new NZException(RCode.PROJECT_ICON_BIG_ERROR);
				}
			}else {
				log.error("保存图片信息失败" + JSONObject.toJSONString(icon), e);
				throw new NZException(RCode.ERROR);
			}
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
            response.setHeader("Cache-Control","max-age=" + 12 * 60 * 60 + ", must-revalidate");
//            response.setDateHeader("Expires", System.currentTimeMillis() + 12 * 60 * 60 * 1000);
            os.write(bytes);
            os.flush();
        } catch (IOException e) {
            log.error(String.format("拓扑图图标预览失败，参数是：%s,错误信息是：%s", id, e));
        }
    }


    private void validateIconFileType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (!filename.contains(".")) {
            throw new NZException(RCode.PROJECT_TOPOICON_FILETYPE_ERROR);
        }

//        String fileType = filename.substring(filename.lastIndexOf(".") + 1);
//        fileType = fileType.toLowerCase();
        String fileType = StrUtil.sub(filename, StrUtil.lastIndexOf(filename, ".", filename.length(), false)+1,filename.length());
        if(!StrUtil.equals(fileType, "jpg",true) && !StrUtil.equals(fileType, "jpeg",true) &&
        		!StrUtil.equals(fileType, "png",true) &&
        		!StrUtil.equals(fileType, "gif",true)) {
        	throw new NZException(RCode.PROJECT_TOPOICON_FILETYPE_ERROR);
        }
		/*switch (fileType){
		    case "jpg":break;
		    case "jpeg":break;
		    case "png":break;
		    case "gif":break;
		    default:
		        throw new NZException(RCode.PROJECT_TOPOICON_FILETYPE_ERROR);
		}*/
    }

	@Override
	public List<String> queryTopoIconUnitName(String name) {
        List<TopoIcon> topoIcons = topoIconDao.selectList(new LambdaQueryWrapper<TopoIcon>().like(Tool.StrUtil.isNotEmpty(name), TopoIcon::getName, Tool.StrUtil.likeEscape(name)));
        List<String> result = topoIcons.stream().map(TopoIcon::getUnit).distinct().collect(Collectors.toList());
		return result;
	}

    @Override
    public void deleteTopoIconByUnit(String unit) {
        this.remove(new LambdaQueryWrapper<TopoIcon>().eq(TopoIcon::getUnit, unit));
    }
}
