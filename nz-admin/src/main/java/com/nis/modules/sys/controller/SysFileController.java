package com.nis.modules.sys.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.service.SysFileService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 文件管理  controller
 */
@RestController
@RequestMapping("/file")
public class SysFileController {

    @Autowired
    private SysFileService sysFileService;

    /**
     * 文件详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSFILE)
    public R querySysFileInfo(@PathVariable("id") Integer id) {
        return R.ok(sysFileService.querySysFileInfo(id));
    }

    /**
     * 文件请求列表
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SYSFILE)
    public R querySysFilePage(@RequestParam Map<String, Object> params) {
        return R.ok(sysFileService.querySysFilePage(params));
    }

    /**
     * 上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.SYSFILE)
    @RequiresPermissions({"sysFile:upload"})
    public R sysFileUpload(@RequestParam("file") MultipartFile file) throws IOException {
        return R.ok(sysFileService.sysFileUpload(file));
    }

    /**
     * 下载
     * @param uuid
     * @return
     */
    @PostMapping("/download/{uuid}")
    @RequiresPermissions({"sysFile:download"})
    public void sysFileDownload(@PathVariable("uuid") String uuid, HttpServletResponse response) throws IOException {
        sysFileService.sysFileDownload(uuid, response);
    }

    /**
     * 删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.SYSFILE)
    @RequiresPermissions({"sysFile:delete"})
    public R deleteSysFile(@RequestParam Integer... ids) {
        sysFileService.deleteSysFile(ids);
        return R.ok();
    }

}
