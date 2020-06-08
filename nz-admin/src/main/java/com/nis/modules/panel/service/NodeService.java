package com.nis.modules.panel.service;

import com.nis.modules.panel.entity.Node;

import java.util.List;
import java.util.Map;

public interface NodeService {

    boolean validatePrevAndNext(Map<Integer, Node> idAndNodeMap, Node node);

    List<Node> modify(Map<Integer, Node> idAndPanelMap, Node panel, Node oldPanel);
}
