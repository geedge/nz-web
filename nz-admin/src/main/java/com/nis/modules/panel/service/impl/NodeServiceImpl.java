package com.nis.modules.panel.service.impl;

import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.modules.panel.entity.Node;
import com.nis.modules.panel.service.NodeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class NodeServiceImpl implements NodeService {

    private Integer firstIndex = 0;
    private Integer lastIndex = -1;

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
        if (firstIndex.equals(prev)) {
            // 校验 next 参数值是否正确
            Node oldFirstPanel = idAndNodeMap.get(next);
            if (oldFirstPanel == null || !firstIndex.equals(oldFirstPanel.getPrev())) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }
        } else if (lastIndex.equals(next)) { // 移动至末尾的
            // 校验 prev 参数是否正确
            Node oldLastPanel = idAndNodeMap.get(prev);
            if (oldLastPanel == null || !lastIndex.equals(oldLastPanel.getNext())) {
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

        // 判断旧位置是否为第一位 并且移动了位置
        if (firstIndex.equals(oldNode.getPrev()) && !firstIndex.equals(node.getPrev())) {
            // 新的第一位
            Node newFirstIndex = idAndNodeMap.get(oldNode.getNext());
            newFirstIndex.setPrev(firstIndex);

            // 第一种 移动到末尾
            if (lastIndex.equals(node.getNext())) {
                // 新的倒数第二位
                Node newCountdownSecond = idAndNodeMap.get(node.getPrev());
                newCountdownSecond.setNext(id);

                updateNodes.add(newCountdownSecond);
            } else {
                // 移动后 前面的
                Node newPrev = idAndNodeMap.get(node.getPrev());
                // 移动后 后面的
                Node newNext = idAndNodeMap.get(node.getNext());
                newPrev.setNext(id);
                newNext.setPrev(id);
                updateNodes.add(newPrev);
                updateNodes.add(newNext);
            }
            updateNodes.add(node);
            updateNodes.add(newFirstIndex);
        }

        // 原来是末尾 并且移动了位置
        if (lastIndex.equals(oldNode.getNext()) && !lastIndex.equals(node.getNext())) {

            // 新的最后一位
            Node newLastIndex = idAndNodeMap.get(oldNode.getPrev());
            newLastIndex.setNext(lastIndex);
            // 第一种 末尾移动到了首位
            if (firstIndex.equals(node.getPrev())) {
                // 新的第二位
                Node newSecond = idAndNodeMap.get(node.getNext());
                newSecond.setPrev(id);

                updateNodes.add(newSecond);
            } else {
                // 第二种 不是移动到开始 而是移动到中间位置 即 有前有后
                // 移动后 前面的
                Node newPrev = idAndNodeMap.get(node.getPrev());
                // 移动后 后面的
                Node newNext = idAndNodeMap.get(node.getNext());
                newPrev.setNext(id);
                newNext.setPrev(id);
                updateNodes.add(newPrev);
                updateNodes.add(newNext);
            }

            updateNodes.add(node);
            updateNodes.add(newLastIndex);
        }


        // 原来是中间的情况
        if (!firstIndex.equals(oldNode.getPrev()) && !lastIndex.equals(oldNode.getNext())) {
            boolean flag = false;

            // 原来位置的前后
            Node oldPrevChart = idAndNodeMap.get(oldNode.getPrev());
            Node oldNextChart = idAndNodeMap.get(oldNode.getNext());
            // 第一种 移动到了开头
            if (firstIndex.equals(node.getPrev())) {
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
            } else if (lastIndex.equals(node.getNext())) {
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
