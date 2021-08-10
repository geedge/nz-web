package com.nis.modules.sys.entity;

import lombok.Data;

/**
 * 临时Token信息
 *

 */
@Data
public class AuthTokenEntity {
	private Long userId;
	private Integer mfaLevel;//非必填
	private String authKey;
	private Integer c;//每更新一次 authToken c 加1，超过10 响应操作频繁，请30分钟后重试

}
