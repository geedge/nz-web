package com.nis.modules.tool.service;

import com.nis.modules.tool.entity.PingEntity;

import java.net.UnknownHostException;
import java.util.List;

public interface CmdService {

	List ping(PingEntity pingEntity) throws UnknownHostException;

	List traceroute(PingEntity pingEntity) throws UnknownHostException;
}
