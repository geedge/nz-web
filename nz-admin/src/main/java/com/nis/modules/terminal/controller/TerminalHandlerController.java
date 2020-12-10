package com.nis.modules.terminal.controller;

import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.terminal.config.TerminalOneProcessor;
import com.nis.modules.terminal.entity.TerminalConfigDto;
import com.nis.modules.terminal.service.TerminalLogService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.Map;

@Controller
@RequestMapping("/terminal")
public class TerminalHandlerController {
	
	private Logger logger = LoggerFactory.getLogger(TerminalHandlerController.class);
	
	@Autowired
    private TerminalOneProcessor terminalOneProcessor;
	@Autowired
    private TerminalLogService terminalLogService;
	
	@Value("${nezha.uploadTempPath}")
    private String configUploadPath;
	
	@RequestMapping(value = "resize", method = RequestMethod.POST)
    @ResponseBody
    public R resize(ServletRequest req, @RequestBody TerminalConfigDto dto) throws Exception {
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
	
	@RequestMapping(value = "upload", method = RequestMethod.POST)
    @ResponseBody
    @RequiresPermissions("terminal:upload")
    public R upload(HttpSession httpSession, String uuid, @RequestParam(value = "file", required = false) MultipartFile file, String path) throws Exception {
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
            File temp=new File("");
            rootPath = temp.getAbsolutePath();
            logger.info("项目根目录["+rootPath+"]");
        }catch (Exception e){
        	logger.error("error info :",e);
        }

        String tmpPath=rootPath+configUploadPath;
        File uploadDir=new File(tmpPath);
        if(!uploadDir.exists()){
            uploadDir.mkdirs();
        }else{//存在同名文件/文件夹
            if(!uploadDir.isDirectory()){//是一个同名文件
                uploadDir.delete();
                uploadDir.mkdirs();
            }
        }

        File tempFile = new File(tmpPath, file.getOriginalFilename());
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
        } catch (Exception e) {
        	logger.error("error info :",e);
            throw new NZException(RCode.SYS_ERROR);
        }
    }
	
	@RequestMapping(value = "download", method = RequestMethod.POST)
    @ResponseBody
    @RequiresPermissions("terminal:download")
    public void download(HttpSession httpSession,HttpServletResponse res, @RequestBody Map<String,String> param) throws Exception {
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
        } catch (Exception e) {
        	logger.error("error info :",e);
            throw new NZException(RCode.SYS_ERROR);
        }
    }

    @GetMapping(value = "/log")
    @ResponseBody
    @RequiresPermissions({"terminal:log:list"})
    public R queryPage(@RequestParam Map<String, Object> params) throws Exception {
        PageUtils page = terminalLogService.queryPage(params);
        return R.ok(page);
    }

	
}
