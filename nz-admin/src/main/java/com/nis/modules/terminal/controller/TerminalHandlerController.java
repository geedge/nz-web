package com.nis.modules.terminal.controller;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.WebSocketSession;

import com.nis.common.exception.NZException;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.terminal.config.TerminalClient;
import com.nis.modules.terminal.config.TerminalContext;
import com.nis.modules.terminal.config.TerminalOneProcessor;
import com.nis.modules.terminal.config.TerminalSession;
import com.nis.modules.terminal.entity.Account;

@Controller
@RequestMapping("/terminal")
public class TerminalHandlerController {
	
	private Logger logger = LoggerFactory.getLogger(TerminalHandlerController.class);
	
	@Autowired
    private TerminalOneProcessor terminalOneProcessor;
	
	@Value("${nezha.uploadTempPath}")
    private String configUploadPath;
	
	@RequestMapping(value = "resize", method = RequestMethod.POST)
    @ResponseBody
    public R resize(String token, Integer cols, Integer rows, Integer width, Integer height) throws Exception {
        return terminalOneProcessor.resize(token, cols, rows, width, height);
    }
	
	@RequestMapping(value = "upload", method = RequestMethod.POST)
    @ResponseBody
    public R upload(HttpSession httpSession, String token, @RequestParam(value = "file", required = false) MultipartFile file, String path) throws Exception {
		String rootPath=null;
        try{
            File temp=new File("");
            rootPath = temp.getAbsolutePath();
            logger.debug("项目根目录["+rootPath+"]");
        }catch (Exception e){
            e.printStackTrace();
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
            tempFile.delete();
            return terminalOneProcessor.upload(token, tempFile, path + "/" + file.getOriginalFilename(), file.getSize());
        } catch (Exception e) {
            e.printStackTrace();
            throw new NZException(RCode.SYS_ERROR);
        }
    }
	
}
