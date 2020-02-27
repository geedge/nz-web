package com.nis.modules.panel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Objects;

@Data
@TableName("chart_element")
public class ChartElement {

    private Integer id;
    /**
     *图表id
     * */
    private Integer chartId;
    /**
     * promql表达式
     * */
    private String expression;
    /**
     * 表达式类型
     * normal：简单表达式，可页面解析，如：up{asset="192.168.40.40",module="kafka"}
     *
     * expert：用户输入表达式，解析困难，直接在页面显示
     * */
    private String type;

    /**
     * 图表legend表达式
     */
    private String legend;

    @Override
    public boolean equals(Object compareObj){
        if(this ==  compareObj){
            return true;
        }

        if(compareObj == null || compareObj.getClass() != this.getClass()){
            return false;
        }

        ChartElement compareElement=(ChartElement)compareObj;

        if(compareElement.getId()==null && this.getId()==null){
            return this ==  compareObj;
        }

        if(compareElement.getId()==this.getId()){
            return true;
        }else{
            return false;
        }

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getId());
    }

    public static void main(String[] args) {
        ChartElement ce1=new ChartElement();
        ce1.setId(2);
        ChartElement ce2=new ChartElement();
        ce2.setId(2);

        System.out.println(ce1.equals(ce2));
        System.out.println(ce2.equals(ce1));
    }
}
