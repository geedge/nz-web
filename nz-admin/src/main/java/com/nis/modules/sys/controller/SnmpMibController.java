package com.nis.modules.sys.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.SnmpMib;
import com.nis.modules.sys.service.SnmpMibService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
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

    /**
     * mib文件查询
     *
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.MIB)
    @RequiresPermissions({"sys:mib:list"})
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
    @RequiresPermissions({"sys:mib:save"})
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
    @RequiresPermissions({"sys:mib:update"})
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
    @RequiresPermissions({"sys:mib:delete"})
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
    @RequiresPermissions({"sys:mib:download"})
    public void download(Integer id, HttpServletResponse response) {
        ValidateUtils.is(id).notNull(RCode.SNMP_MIB_ID_ISNULL);

        SnmpMib snmpMib = snmpMibService.getById(id);
        if (snmpMib != null) {
            String fileName = snmpMib.getFileName();

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
    @RequiresPermissions({"sys:mib:tree"})
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
    @RequiresPermissions({"sys:mib:browser"})
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
        String type = (String) params.get("type");
        if (Constant.SnmpOperationType.SET.getValue().equals(operation)) {
            ValidateUtils.is(value).notNull(RCode.SNMP_MIB_VALUE_ISNULL)
                    .and(type).notNull(RCode.SNMP_MIB_TYPE_ISNULL);

            if(!Constant.SNMP_SETVLAUE_TYPE.contains(type)){
                throw new NZException(RCode.SNMP_MIB_TYPE_INVALIDE);
            }
        }

        String community = (String) params.get("community");

        Map<String, String> auth = (Map) params.get("auth");
        // v3时需要的用户信息
        if (Constant.SNMPV3.equals(version)) {
            // 校验参数是否正确
            if (ToolUtil.isEmpty(auth)) {
                throw new NZException(RCode.SNMP_MIB_AUTH_ISNULL);
            } else {
                SnmpUtil.ValidateAuthForSnmpV3(auth);
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

        if (CollectionUtils.isEmpty(resultData)){
            return R.error(RCode.SNMP_MIB_BROWSER_TIMEOUT);
        }

        if (CollectionUtils.isNotEmpty(resultData)) {
            // 获取当前所有的mib tree
            Map<String, String> oidWithNameMap = snmpMibService.getAllTreeNodeOidAndNameMap();

            String resultOid;
            String name, str;
            for (Map map : resultData) {
                resultOid = map.get("oid").toString();
                str = resultOid;
                // 处理对应关系
                name = oidWithNameMap.get(resultOid);

                // name 为null 截取之后再次获取
                if (StringUtils.isEmpty(name)) {
                    // 从最后开始截取 目前暂定截取最后一个 .
                    for (int i = 0; i < Constant.OIDNAME_TRUNCATION_INDEX; i++) {
                        resultOid = resultOid.substring(0, resultOid.lastIndexOf("."));
                    }

                    name = oidWithNameMap.get(resultOid);
                    if (StringUtils.isNotEmpty(name)) {
                        str = str.replaceAll(resultOid, "");
                        name += str;
                    }
                }

                map.put("name", name);
            }
        }

        Map map = new HashMap();
        map.put("list", resultData);
        return R.ok(map);
    }

}
