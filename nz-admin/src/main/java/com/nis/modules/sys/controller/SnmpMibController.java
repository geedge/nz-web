package com.nis.modules.sys.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.module.service.ModuleService;
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
import java.util.HashMap;
import java.util.List;
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

    @Autowired
    private ModuleService moduleService;

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

    /**
     * 执行snmp相关命令
     *
     * @param params
     * @return
     */
    @PostMapping("/browser")
    public R browser(@RequestBody Map<String, Object> params) {
        String operation = (String) params.get("operation");
        Integer version = (Integer) params.get("version");
        String host = (String) params.get("host");
        Integer port = (Integer) params.get("port");
        String oid = (String) params.get("oid");
        ValidateUtils.is(operation).notNull(RCode.SNMP_MIB_OPERATION_ISNULL)
                .and(host).notNull(RCode.SNMP_MIB_HOST_ISNULL).ip(RCode.SNMP_MIB_HOST_FORMAT)
                .and(port).notNull(RCode.SNMP_MIB_PORT_ISNULL).port(RCode.SNMP_MIB_PORT_INVALIDE)
                .and(version).notNull(RCode.SNMP_MIB_VERSION_ISNULL)
                .and(oid).notNull(RCode.SNMP_MIB_OID_ISNULL);

        if (Constant.SnmpOperationType.getOperationType(operation.toLowerCase()) == null) {
            throw new NZException(RCode.SNMP_MIB_OPERATION_INVALIDE);
        }

        if (!Constant.SNMPV2.equals(version) && !Constant.SNMPV3.equals(version)) {
            throw new NZException(RCode.SNMP_MIB_VERSION_INVALIDE);
        }

        String value = (String) params.get("value");
        if (Constant.SnmpOperationType.SET.getValue().equals(operation)) {
            ValidateUtils.is(value).notNull(RCode.SNMP_MIB_VALUE_ISNULL);
        }

        String community = (String) params.get("community");

        Map<String, String> auth = (Map) params.get("auth");
        // v3时需要的用户信息
        if (Constant.SNMPV3.equals(version)) {
            // 校验参数是否正确
            if (ToolUtil.isEmpty(auth)) {
                throw new NZException(RCode.SNMP_MIB_AUTH_ISNULL);
            } else {
                moduleService.ValidateAuthForSnmpV3(auth.toString());
            }
        }

        List<Map> resultData = null;
        try {
            if (Constant.SnmpOperationType.GET.getValue().equals(operation)) {
                resultData = SnmpUtil.snmpGetOrGetNext(host, port, version, community, oid, auth, operation);
            } else if (Constant.SnmpOperationType.GETNEXT.getValue().equals(operation)) {
                resultData = SnmpUtil.snmpGetOrGetNext(host, port, version, community, oid, auth, operation);
            } else if (Constant.SnmpOperationType.WALK.getValue().equals(operation)) {
                resultData = SnmpUtil.snmpWalk(host, port, version, community, oid, auth);
            } else if (Constant.SnmpOperationType.SET.getValue().equals(operation)) {
                R r = SnmpUtil.setPDU(host, port, version, community, oid, auth, value);
                return r;
            }
        } catch (IOException e) {
            R.error(RCode.SNMP_MIB_BROWSER_FAILED);
        }

        if (resultData == null) {
            return R.error(RCode.SNMP_MIB_BROWSER_TIMEOUT);
        }

        Map map = new HashMap();
        map.put("list", resultData);
        return R.ok(map);
    }

}
