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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        ValidateUtils.is(ids).notNull(RCode.IPAM_IP_ID_ISNULL);
        ipamIpService.delIpamIps(Arrays.asList(ids));
        return R.ok();
    }

    @GetMapping("/template")
    public void getTemplateByLanguage(HttpServletResponse response,
                                      @RequestParam(required = false, defaultValue = "en") String language,
                                      @RequestParam(required = false, defaultValue = "1") String format) throws IOException {

        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(format)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }

        ipamIpService.getTemplateByLanguage(response,format);
    }

    @PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.IPAMIP)
    public R importIpamIpData(@RequestParam(value = "file") MultipartFile excelFile, HttpServletRequest request, @RequestParam Map<String, Object> params) throws IOException {
        String language = request.getHeader(Constant.LANGUAGE);
        ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);
        // 校验 LANGUAGE
        if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        Map info = ipamIpService.importIpamIpData(excelFile, params);
        return R.ok(info);
    }

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.IPAMIP)
    public void export(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
        Object format = params.get("format");
        String formatStr = format == null ? "1" : format.toString();
        // 校验 FORMAT
        List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
        if (!supportImportingFileTypes.contains(formatStr)) {
            throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
        }
        params.put("format", formatStr);

        ipamIpService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.IPAMIP)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);
        ipamIpService.cancelImport(seq);
        return R.ok();
    }

}
