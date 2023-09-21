package com.nis.modules.sys.entity;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.shiro.ShiroUtils;
import lombok.Data;

import java.io.Serializable;

/**
 * sys backup log entity
 *
 * @author shizhendong
 * @date 2023-09-19
 */
@Data
@TableName("sys_backup_log")
public class SysBackupLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Integer id;

    /**
     * 用户ID
     */
    private Integer uid;

    /**
     * 操作类型 1：备份 2：还原 3：删除备份文件
     */
    private Integer type;


    /**
     * 文件名称
     */
    private String filename;

    /**
     * 状态 1：成功  0：失败
     */
    private Integer state;

    /**
     * 错误信息
     */
    private String errorMsg;


    /**
     * 时间戳
     */
    private Long ts;

    public void setFilename(String filename) {
        this.filename = Tool.StrUtil.trim(filename);
    }

    /**
     * build basic log entity
     *
     * @param type
     * @return
     */
    public static SysBackupLog buildBasicLogEntity(Integer type) {
        SysBackupLog blog = new SysBackupLog();
        blog.setType(type);

        blog.setState(1);
        blog.setErrorMsg("");
        blog.setTs(System.currentTimeMillis());

        // uid
        SysUserEntity userEntity = ShiroUtils.getUserEntity();
        Integer uid = ObjectUtil.isNull(userEntity) ? -1 : userEntity.getId().intValue();
        blog.setUid(uid);
        return blog;
    }

}
