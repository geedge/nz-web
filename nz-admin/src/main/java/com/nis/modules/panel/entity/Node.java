package com.nis.modules.panel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Node {

    @TableId
    public Integer id;

    /**
     * 排序字段，前一个元素
     */
    public Integer prev;

    /**
     * 排序字段，后一个元素
     */
    public Integer next;
}
