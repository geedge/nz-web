package com.nis.modules.sys.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.dao.SysFileDao;
import com.nis.modules.sys.entity.SysFileChunkEntity;
import com.nis.modules.sys.entity.SysFileEntity;
import com.nis.modules.sys.service.SysFileChunkService;
import com.nis.modules.sys.service.SysFileService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileDao, SysFileEntity> implements SysFileService {

    @Autowired
    private SysFileChunkService sysFileChunkService;

    @Autowired
    private SysFileDao sysFileDao;

    private static Log log = Log.get();

    @SuppressWarnings("unchecked")
    @Override
    public SysFileEntity querySysFileInfo(Integer id) {
        ValidateUtils.is(id).notNull(RCode.SYS_FILE_ID_ISNULL);
        return this.getById(id);
    }

    @Override
    public PageUtils querySysFilePage(Map<String, Object> params) {
        IPage page = new Query(SysFileEntity.class).getPage(params);
        List<SysFileEntity> issueList = sysFileDao.querySysFileList(page, params);
        page.setRecords(issueList);
        return new PageUtils(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileEntity sysFileUpload(MultipartFile file) throws IOException {
        ValidateUtils.is(file.getOriginalFilename()).notNull(RCode.SYS_FILE_ISNULL);
        String filename = file.getOriginalFilename();
        String fileType = filename.substring(filename.lastIndexOf(".") + 1);
        byte[] fileBytes = file.getBytes();
        // 构建 sys_file 信息
        SysFileEntity sysFileEntity = new SysFileEntity();
        sysFileEntity.setUuid(CommonUtils.uuid());
        sysFileEntity.setName(filename);
        sysFileEntity.setType(fileType);
        if(Tool.StrUtil.isBlank(fileType)){
            sysFileEntity.setType("application/octet-stream");
        }
        sysFileEntity.setSize(file.getSize());
        sysFileEntity.setMd5(DigestUtils.md5Hex(fileBytes));
        sysFileEntity.setTs(System.currentTimeMillis());
        // 暂不实现 s3，file 存储
        sysFileEntity.setStorage(1);
        sysFileEntity.setMeta("{}");
        sysFileEntity.setPath("");
        this.save(sysFileEntity);
        // 数据保存
        switch (sysFileEntity.getStorage()){
            case 1: // DB
                SysFileChunkEntity sysFileChunk = new SysFileChunkEntity();
                sysFileChunk.setFileId(sysFileEntity.getId());
                sysFileChunk.setChunk(fileBytes);
                try{
                    sysFileChunkService.save(sysFileChunk);
                } catch (Exception e){
                    if(e instanceof DataAccessResourceFailureException) {
                        log.error("文件大小超过 max_allowed_packet 设置", e);
                        throw new NZException(RCode.SYS_FILE_BIG_ERROR);
                    }else {
                        throw new NZException(RCode.ERROR);
                    }
                }
                break;
            case 2:; // S3
            case 3:; // file
        }
        return sysFileEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sysFileDownload(String uuid, HttpServletResponse response) throws IOException {
        ValidateUtils.is(uuid).notNull(RCode.SYS_FILE_UUID_ISNULL);
        SysFileEntity downloadFile = this.getOne(new LambdaQueryWrapper<SysFileEntity>().eq(SysFileEntity::getUuid, uuid));
        if (ToolUtil.isEmpty(downloadFile)) {
            throw new NZException(RCode.SYS_FILE_NOTEXIST);
        }
        switch (downloadFile.getStorage()){
            case 1: // DB
                SysFileChunkEntity sysFileChunk = sysFileChunkService.getOne(new LambdaQueryWrapper<SysFileChunkEntity>().eq(SysFileChunkEntity::getFileId, downloadFile.getId()));
                ResponseUtil.downloadFile(response, downloadFile.getName(), sysFileChunk.getChunk());
                break;
            case 2:; // S3
            case 3:; // file
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSysFile(Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.SYS_FILE_ID_ISNULL);
        List<Integer> idList = Arrays.asList(ids);
        this.removeByIds(idList);
        // 删除数据块
        sysFileChunkService.remove(new LambdaQueryWrapper<SysFileChunkEntity>().in(SysFileChunkEntity::getFileId, idList));
    }

}
