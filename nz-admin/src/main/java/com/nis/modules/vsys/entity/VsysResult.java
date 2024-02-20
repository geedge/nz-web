package com.nis.modules.vsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("vsys_result")
public class VsysResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 任务id
     */
    private Integer taskId;

    /**
     * 资产id
     */
    private Integer assetId;

    /**
     * vsys id
     */
    private Integer vsysId;

    /**
     * 任务类型
     * 1：add
     * 2：update
     * 3：delete
     */
    private Integer type;

    /**
     * 状态
     * 0 未执行
     * 1 执行中
     * 2 执行失败
     * 3 执行成功
     */
    private Integer state;

    /**
     * 执行信息
     */
    private String msg;

    /**
     * sf ruleSet chain 三个接口请求参数记录
     */
    private String reqParam;

    /**
     * 执行第几次	默认：1
     */
    private Integer num;

    /**
     * 时间戳
     */
    private Long ts;

    public void setMsg(String msg) {
        // 超长截断
        this.msg = Tool.StrUtil.subWithLength(msg, 0, Constant.MYSQL_TEXT_MAXLENGTH);
    }
}
