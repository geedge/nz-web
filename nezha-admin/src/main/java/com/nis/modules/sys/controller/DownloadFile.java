package com.nis.modules.sys.controller;

import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@RestController
@RequestMapping("/sys/")
public class DownloadFile {
    @GetMapping("/download")
    @ResponseBody
    @ApiOperation(value="文件下载")
    public String download(HttpServletRequest request, HttpServletResponse response){
        String filePath=request.getParameter("filePath");
        String fileName=request.getParameter("fileName");
        String rootPath=null;
        try{
            rootPath= ResourceUtils.getURL("classpath:").getPath();
        }catch (Exception e){
            e.printStackTrace();
        }
        File file = new File(rootPath+filePath);
        if(!file.exists()){
            throw new NZException(RCode.SYS_DOWNLOAD_NO_FILE);
        }
        try {
            fileName = new String(fileName.getBytes("GBK"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength((int) file.length());
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        byte[] buff = new byte[1024];
        BufferedInputStream bis = null;
        OutputStream os = null;
        try {
            os = response.getOutputStream();
            bis = new BufferedInputStream(new FileInputStream(file));
            int i = 0;
            while ((i = bis.read(buff)) != -1) {
                os.write(buff, 0, i);
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
