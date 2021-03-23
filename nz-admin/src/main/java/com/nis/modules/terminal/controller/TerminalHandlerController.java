package com.nis.modules.terminal.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.log.Log;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.common.xss.XssUtil;
import com.nis.modules.terminal.config.TerminalOneProcessor;
import com.nis.modules.terminal.entity.TerminalConfigDto;
import com.nis.modules.terminal.service.TerminalCmdService;
import com.nis.modules.terminal.service.TerminalLogService;
import com.nis.modules.terminal.service.TerminalRecordService;
import com.nis.modules.terminal.service.TerminalSessionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/terminal")
public class TerminalHandlerController {
	
	private Log logger = Log.get();
	
	@Autowired
    private TerminalOneProcessor terminalOneProcessor;
	@Autowired
    private TerminalLogService terminalLogService;

	@Autowired
    private TerminalSessionService terminalSessionService;

	@Autowired
    private TerminalCmdService terminalCmdService;

	@Autowired
    private TerminalRecordService terminalRecordService;
	
	@Value("${nezha.uploadTempPath}")
    private String configUploadPath;
	
	@PostMapping(value = "resize")
    public R resize(ServletRequest req, @RequestBody TerminalConfigDto dto) throws IOException,RuntimeException {
		HttpServletRequest request=(HttpServletRequest) req;
		String token=request.getHeader(Constant.AUTH_TOKEN_CODE);
		Integer cols = dto.getCols();
		Integer height = dto.getHeight();
		Integer rows = dto.getRows();
		Integer width = dto.getWidth();
		if(cols==null||height==null||rows==null||width==null) {
			throw new NZException(RCode.TERMINAL_PARAM_ISNULL);
		}else {
			return terminalOneProcessor.resize(token, cols, rows, width, height);
		}
    }
	
	@PostMapping(value = "upload")
    @RequiresPermissions("terminal:upload")
    public R upload(HttpSession httpSession, String uuid, @RequestParam(value = "file", required = false) MultipartFile file, String path) throws RuntimeException {
		if(StringUtils.isBlank(uuid)) {
			throw new NZException(RCode.TERMINAL_UUID_ISNULL);
		}
		if(StringUtils.isBlank(path)) {
			throw new NZException(RCode.TERMINAL_PATH_ISNULL);
		}
		if(CommonUtils.isEmpty(file)) {
			throw new NZException(RCode.TERMINAL_FILE_ISNULL);
		}
		String rootPath=null;
        try{
            File temp= FileUtil.file("");
            rootPath = temp.getAbsolutePath();
            logger.info("项目根目录["+rootPath+"]");
        }catch (RuntimeException e){
        	logger.error("error info :",e);
        }

        String tmpPath=rootPath+configUploadPath;
        File uploadDir=FileUtil.file(tmpPath);
        if(!uploadDir.exists()){
            uploadDir.mkdirs();
        }else{//存在同名文件/文件夹
            if(!uploadDir.isDirectory()){//是一个同名文件
                uploadDir.delete();
                uploadDir.mkdirs();
            }
        }

        File tempFile = FileUtil.file(tmpPath, file.getOriginalFilename());
	        logger.debug("上传临时文件["+tempFile.getAbsolutePath()+"]");
        try {
            file.transferTo(tempFile);
            if (CommonUtils.isEmpty(path)) {
                path = ".";
            } else {
                if (path.endsWith("/")) {
                    path = path.substring(0, path.lastIndexOf("/"));
                }
            }
            logger.debug("上传目录：["+path+"]");
 //           tempFile.delete();
            return terminalOneProcessor.upload(uuid, tempFile, path + "/" + file.getOriginalFilename(), file.getSize());
        } catch (IOException | IllegalStateException e) {
        	logger.error("error info :",e);
            throw new NZException(RCode.SYS_ERROR);
        }
    }
	
	@PostMapping(value = "download")
    @RequiresPermissions("terminal:download")
    public void download(HttpSession httpSession,HttpServletResponse res, @RequestBody Map<String,String> param) throws RuntimeException {
		String uuid = param.get("uuid");
		String path = param.get("path");
		if(StringUtils.isBlank(uuid)) {
			throw new NZException(RCode.TERMINAL_UUID_ISNULL);
		}
		if(StringUtils.isBlank(path)) {
			throw new NZException(RCode.TERMINAL_PATH_ISNULL);
		}
		try {
			// 判断当前路径是绝对路径还是相对路径
			if(!path.startsWith("/")) {
				path="./"+path;
			}
            terminalOneProcessor.download(uuid,path,res);
        } catch (RuntimeException e) {
        	logger.error("error info :",e);
            throw new NZException(RCode.SYS_ERROR);
        }
    }

    @GetMapping(value = "/log")
    @RequiresPermissions({"terminal:log:list"})
    public R queryPage(@RequestParam Map<String, Object> params) throws RuntimeException {
        params = (Map<String, Object>) XssUtil.filterParameters((Map) params);
        PageUtils page = terminalLogService.queryPage(params);
        return R.ok(page);
    }

    /**
     * terminal审计日志会话查询接口 默认按照 id 倒序
     *
     * @param params
     * @return
     */
    @GetMapping("/session")
    public R querySessionPage(@RequestParam Map<String, Object> params) {
        params = (Map<String, Object>) XssUtil.filterParameters((Map) params);
        PageUtils page = terminalSessionService.queryPage(params);
        return R.ok(page);
    }

    /**
     * terminal审计日志命令查询接口 按照  id asc
     *
     * @param params
     * @return
     */
    @GetMapping("/cmd")
    public R queryCmdPage(@RequestParam Map<String, Object> params) {
        params = (Map<String, Object>) XssUtil.filterParameters((Map) params);
        Map map = terminalCmdService.queryCmdInfos(params);
        return R.ok(map);
    }

    @PutMapping("/kill")
    public R killTerminal(@RequestBody Map<String, String> params) {
        ValidateUtils.is(params.get("uuid")).notNull(RCode.TERMINAL_UUID_ISNULL);

        terminalSessionService.killSession(params.get("uuid"));
        return R.ok();
    }

    @GetMapping("/record")
    public R queryTerminalRecord(@RequestParam Map<String, Object> params) {
        params = (Map<String, Object>) XssUtil.filterParameters((Map) params);
        Map map = terminalRecordService.queryTerminalRecordInfos(params);
        return R.ok(map);
    }
	
}
