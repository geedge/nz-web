package com.nis.modules.sys.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.SnmpMib;
import com.nis.modules.sys.service.SnmpMibService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;

/**
 * mib文件 controller
 *
 * @author shizhendong
 * @date 2020-03-09
 */
@RestController
@RequestMapping("/mib")
public class SnmpMibController extends AbstractController {

    @Autowired
    private SnmpMibService snmpMibService;

    /**
     * mib文件查询
     *
     * @param params
     * @return
     */
    @GetMapping
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = snmpMibService.queryPage(params);
        return R.ok(page);
    }

    /**
     * 保存
     *
     * @param snmpMib
     * @param file
     * @return
     */
    @PostMapping
    @SysLog(operation = OperationEnum.ADD,type = TypeEnum.MIB)
    public R save(SnmpMib snmpMib, @RequestParam(value = "file", required = false) MultipartFile file) {
        ValidateUtils.is(file).notNull(RCode.SNMP_MIB_FILE_ISNULL);

        snmpMibService.saveOrUpdateSnmpMib(snmpMib, file);
        return R.ok();
    }


    /**
     * 修改
     *
     * @param snmpMib
     * @param file
     * @return
     */
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.MIB)
    public R update(SnmpMib snmpMib, @RequestParam(value = "file", required = false) MultipartFile file) {
        ValidateUtils.is(snmpMib.getId()).notNull(RCode.SNMP_MIB_ID_ISNULL);

        snmpMibService.saveOrUpdateSnmpMib(snmpMib, file);
        return R.ok();
    }

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE,type = TypeEnum.MIB)
    public R delete(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.SNMP_MIB_ID_ISNULL);

        snmpMibService.removeByIds(Arrays.asList(ids));
        return R.ok();
    }


    /**
     * 下载
     *
     * @param id
     * @param response
     */
    @GetMapping("/download")
    public void download(Integer id, HttpServletResponse response) {
        ValidateUtils.is(id).notNull(RCode.SNMP_MIB_ID_ISNULL);

        SnmpMib snmpMib = snmpMibService.getById(id);
        if (snmpMib != null) {
            String fileName = snmpMib.getFileName();

            InputStream is = null;
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(response.getOutputStream());

                response.setCharacterEncoding("utf-8");
                response.setContentType("multipart/form-data");
                response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
                response.setHeader("Access-Control-Expose-Headers","Content-Disposition");

                os.write(snmpMib.getContent());
                os.flush();
            } catch (Exception e) {
                logger.error("snmp mib file download error {}", e.getMessage());
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            try {
                String msg = "snmp mib cannot be found.";
                OutputStream outputStream = response.getOutputStream();
                outputStream.write(msg.getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 查询mib tree
     *
     * @param ids
     * @param models
     * @return
     */
    @GetMapping("/tree")
    public R mibTree(String[] ids, String[] models) {
        String mibTrees = snmpMibService.getMibTrees(ids, models);
        return R.ok((Object) mibTrees);
    }

}
