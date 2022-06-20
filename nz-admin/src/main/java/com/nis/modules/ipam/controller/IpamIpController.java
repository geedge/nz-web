package com.nis.modules.ipam.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.ipam.entity.IpamIp;
import com.nis.modules.ipam.service.IpamIpService;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IncompatibleAddressException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/ipam/ip")
public class IpamIpController {

    @Autowired
    private IpamIpService ipamIpService;

    @GetMapping("/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.IPAMIP)
    public R ipamIpInfo(@PathVariable Integer id){

        ValidateUtils.is(id).notNull(RCode.IPAM_IP_ID_ISNULL);
        IpamIp ip = ipamIpService.ipamIpInfo(id);
        return R.ok(ip);

    }

    @GetMapping
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.IPAMIP)
    public R ipamIpList(@RequestParam Map params){

        PageUtils page = ipamIpService.ipamIpList(params);
        return R.ok(page);
    }

    @RequestMapping(method = { RequestMethod.POST })
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.IPAMIP)
    public R ipamIpSave(@RequestBody IpamIp ipamIp) throws IncompatibleAddressException, UnknownHostException {
        Integer id = ipamIpService.saveOrModifyIpamSubnet(ipamIp);
        return R.ok().putData("id", id);
    }

    @RequestMapping(method = { RequestMethod.PUT })
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.IPAMIP)
    public R ipamIpModify(@RequestBody IpamIp ipamIp) throws IncompatibleAddressException, UnknownHostException {
        ValidateUtils.is(ipamIp.getId()).notNull(RCode.IPAM_IP_ID_ISNULL);
        Integer id = ipamIpService.saveOrModifyIpamSubnet(ipamIp);
        return R.ok().putData("id", id);
    }

    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.IPAMIP)
    public R ipamIpDelete(@RequestParam Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.PANEL_ID_ISNULL);
        ipamIpService.delIpamIps(Arrays.asList(ids));
        return R.ok();
    }

    @GetMapping("/template")
    public void getTemplateByLanguage(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String language = request.getHeader(Constant.LANGUAGE);
        String format = request.getHeader("format");
        format = Tool.StrUtil.emptyToDefault(format, "1");

        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        // 校验 FORMAT
        if (!Arrays.asList(Constant.IMPORTFILE_TYPE_INTVALUES).contains(format)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }

        ipamIpService.getTemplateByLanguage(response,format);
    }

    @PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.CHART)
    public R importChartData(MultipartFile excelFile, HttpServletRequest request) throws IOException, AddressStringException, IncompatibleAddressException {
        String language = request.getHeader(Constant.LANGUAGE);
        ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);
        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        Map info = ipamIpService.importIpamIpData(excelFile);
        return R.ok(info);
    }

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.CHART)
    public void export(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
        Object format = params.get("format");
        String formatStr = format == null ? "1" : format.toString();
        // 校验 FORMAT
        if (!Arrays.asList(Constant.IMPORTFILE_TYPE_INTVALUES).contains(formatStr)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }
        params.put("format", formatStr);

        ipamIpService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.CHART)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);
        ipamIpService.cancelImport(seq);
        return R.ok();
    }

}
