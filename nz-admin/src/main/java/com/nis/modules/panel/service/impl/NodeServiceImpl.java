package com.nis.modules.panel.service.impl;

import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.modules.panel.entity.Node;
import com.nis.modules.panel.service.NodeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class NodeServiceImpl implements NodeService {

    @Override
    public boolean validatePrevAndNext(Map<Integer, Node> idAndNodeMap, Node node) {
        // 如果没有改变位置
        Node entity = idAndNodeMap.get(node.getId());
        if (entity.getPrev().equals(node.getPrev()) && entity.getNext().equals(node.getNext())) {
            return true;
        }

        Integer prev = node.getPrev();
        Integer next = node.getNext();

        // 移动至开头的
        if (Constant.START_PREV_VALUE.equals(prev)) {
            // 校验 next 参数值是否正确
            Node oldFirstPanel = idAndNodeMap.get(next);
            if (oldFirstPanel == null || !Constant.START_PREV_VALUE.equals(oldFirstPanel.getPrev())) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }
        } else if (Constant.END_NEXT_VALUE.equals(next)) {
            // 校验 prev 参数是否正确
            Node oldLastPanel = idAndNodeMap.get(prev);
            if (oldLastPanel == null || !Constant.END_NEXT_VALUE.equals(oldLastPanel.getNext())) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }
        } else {
            // 移动到中间的
            Node oldMiddlePrev = idAndNodeMap.get(prev);
            Node oldMiddleNext = idAndNodeMap.get(next);
            if (oldMiddlePrev == null || oldMiddleNext == null) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }

            // 判断是否连续
            if (!oldMiddlePrev.getNext().equals(oldMiddleNext.getId())) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }
        }
        return false;
    }

    @Override
    public List<Node> modify(Map<Integer, Node> idAndNodeMap, Node node, Node oldNode) {
        // 最后需要更改的实体集合
        List<Node> updateNodes = new ArrayList<>();
        Integer id = node.getId();

        // 开头往后挪
        if (Constant.START_PREV_VALUE.equals(oldNode.getPrev()) && !Constant.START_PREV_VALUE.equals(node.getPrev())) {
            // 新的开头
            Node newFirstIndex = idAndNodeMap.get(oldNode.getNext());
            newFirstIndex.setPrev(Constant.START_PREV_VALUE);

            // 第一种 移动到末尾
            if (Constant.END_NEXT_VALUE.equals(node.getNext())) {
                // 新的倒数第二位
                Node newCountdownSecond = idAndNodeMap.get(node.getPrev());
                newCountdownSecond.setNext(id);

                updateNodes.add(newCountdownSecond);
            } else {
                // 新位置的前一个 重新设置 后
                Node newPrev = idAndNodeMap.get(node.getPrev());
                newPrev.setNext(id);
                // 新位置的后一个 重新设置 前
                Node newNext = idAndNodeMap.get(node.getNext());
                newNext.setPrev(id);
                updateNodes.add(newPrev);
                updateNodes.add(newNext);
            }
            updateNodes.add(node);
            updateNodes.add(newFirstIndex);
        }

        // 末尾往前挪
        if (Constant.END_NEXT_VALUE.equals(oldNode.getNext()) && !Constant.END_NEXT_VALUE.equals(node.getNext())) {

            // 新的最后一位
            Node newLastIndex = idAndNodeMap.get(oldNode.getPrev());
            newLastIndex.setNext(Constant.END_NEXT_VALUE);
            // 第一种 末尾移动到了开头
            if (Constant.START_PREV_VALUE.equals(node.getPrev())) {
                // 新的第二位
                Node newSecond = idAndNodeMap.get(node.getNext());
                newSecond.setPrev(id);

                updateNodes.add(newSecond);
            } else {
                // 第二种 不是移动到开始 而是移动到中间位置 即 有前有后 重新设置 前后值
                Node newPrev = idAndNodeMap.get(node.getPrev());
                newPrev.setNext(id);
                Node newNext = idAndNodeMap.get(node.getNext());
                newNext.setPrev(id);
                updateNodes.add(newPrev);
                updateNodes.add(newNext);
            }

            updateNodes.add(node);
            updateNodes.add(newLastIndex);
        }


        // 中间 挪位置
        if (!Constant.START_PREV_VALUE.equals(oldNode.getPrev()) && !Constant.END_NEXT_VALUE.equals(oldNode.getNext())) {
            boolean flag = false;

            // 原来位置的前后
            Node oldPrevChart = idAndNodeMap.get(oldNode.getPrev());
            Node oldNextChart = idAndNodeMap.get(oldNode.getNext());
            // 第一种 移动到了开头
            if (Constant.START_PREV_VALUE.equals(node.getPrev())) {
                // 新的第二位
                Node newSecondIndex = idAndNodeMap.get(node.getNext());
                newSecondIndex.setPrev(id);

                if (oldPrevChart.getId().equals(newSecondIndex.getId())) {
                    flag = true;
                    newSecondIndex.setNext(oldNextChart.getId());
                    oldNextChart.setPrev(newSecondIndex.getId());
                } else {
                    oldPrevChart.setNext(oldNextChart.getId());
                    oldNextChart.setPrev(oldPrevChart.getId());
                }

                updateNodes.add(node);
                updateNodes.add(newSecondIndex);
                updateNodes.add(oldNextChart);
                if (!flag) {
                    updateNodes.add(oldPrevChart);
                }
            } else if (Constant.END_NEXT_VALUE.equals(node.getNext())) {
                // 第二种移动到末尾
                // 新的倒数第二位
                Node newCountdownSecond = idAndNodeMap.get(node.getPrev());
                newCountdownSecond.setNext(id);

                // 原来位置的前后
                if (oldNextChart.getId().equals(newCountdownSecond.getId())) {
                    flag = true;
                    newCountdownSecond.setPrev(oldPrevChart.getId());
                    oldPrevChart.setNext(newCountdownSecond.getId());
                } else {
                    oldPrevChart.setNext(oldNextChart.getId());
                    oldNextChart.setPrev(oldPrevChart.getId());
                }

                updateNodes.add(node);
                updateNodes.add(newCountdownSecond);
                updateNodes.add(oldPrevChart);

                if (!flag) {
                    updateNodes.add(oldNextChart);
                }
            } else {
                // 第三种 中间调整到中间
                Node newPrevChart = idAndNodeMap.get(node.getPrev());
                Node newNextChart = idAndNodeMap.get(node.getNext());

                oldPrevChart.setNext(oldNextChart.getId());
                oldNextChart.setPrev(oldPrevChart.getId());

                newNextChart.setPrev(id);

                // 换位前后面的 和 换位后前面的是同一个
                if (oldNextChart.getId().equals(newPrevChart.getId())) {
                    flag = true;
                    oldNextChart.setNext(id);
                } else {
                    newPrevChart.setNext(id);
                }

                updateNodes.add(oldPrevChart);
                updateNodes.add(oldNextChart);
                updateNodes.add(newNextChart);

                if (!flag) {
                    updateNodes.add(newPrevChart);
                }
                updateNodes.add(node);
            }
        }
        return updateNodes;
    }
}