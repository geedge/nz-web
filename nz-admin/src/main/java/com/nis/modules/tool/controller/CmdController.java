package com.nis.modules.tool.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.controller.AbstractController;
import com.nis.modules.tool.entity.PingEntity;
import com.nis.modules.tool.service.CmdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.UnknownHostException;

@RestController
@RequestMapping("/cmd")
public class CmdController extends AbstractController {

    @Autowired
    private CmdService cmdService;

    @PostMapping("/ping")
    public R ping(@RequestBody PingEntity pingEntity) throws UnknownHostException {
        ValidateUtils.is(pingEntity.getIps()).notNull(RCode.ERROR);

        return R.ok().put("list", cmdService.ping(pingEntity));
    }

    @PostMapping("/traceroute")
    public R traceroute(@RequestBody PingEntity pingEntity) throws UnknownHostException {
        ValidateUtils.is(pingEntity.getIps()).notNull(RCode.ERROR);

        return R.ok().put("list", cmdService.traceroute(pingEntity));
    }

}
