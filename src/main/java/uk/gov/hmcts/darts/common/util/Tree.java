package uk.gov.hmcts.darts.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tree<T extends TreeNode> {

    public Map<Integer, T> nodeList = new HashMap<>();

    public void addNode(T t) {
        nodeList.put(t.getId(), t);
    }

    public void addNode(Collection<T> nodeCollection) {
        nodeCollection.forEach(this::addNode);
    }

    public Collection<T> getLowestLevelDescendants() {
        List<T> topLevelNodes = getTopLevelNodes();
        List<T> lowestNodes = new ArrayList<>();

        // gets the lowest level top nodes
        for (T node : topLevelNodes) {
            Map<String, T>  antecendantMap = getNodesWithAntecendantMap();

            // if the top level node is not an antecedent then this is the lowest level
            if (antecendantMap.get(node.getId().toString()) == null) {
                lowestNodes.add(node);
            }
        }

        List<T> lowestLevelWithAntecedent = getNodeWhichIsNotAnAntecedent();
        lowestNodes.addAll(lowestLevelWithAntecedent);
        return lowestNodes;
    }

    public List<T> getTopLevelNodes() {
        final List<T> topLevelList = new ArrayList<>();
        nodeList.values().forEach((node) -> {
            if (!node.doesHaveAntecedent()) {
                topLevelList.add(node);
            }
        });

        return topLevelList;
    }

    public List<T> getNodeWhichIsNotAnAntecedent() {
        final List<T> lowestLevelNodes = new ArrayList<>();
        Map<String, T>  atecendantMap = getNodesWithAntecendantMap();
        for (T node : atecendantMap.values()) {
            // if this node is not an attendant to any other node this is the lowest level
            if (atecendantMap.get(node.getId().toString()) == null) {
                lowestLevelNodes.add(node);
            }
        }

        return lowestLevelNodes;
    }

    private Map<String, T> getNodesWithAntecendantMap() {
        final Map<String, T> atecedantMap = new HashMap<>();
        nodeList.values().forEach((node) -> {
            if (node.doesHaveAntecedent()) {
                atecedantMap.put(node.getAntecedent(), node);
            }
        });

        return atecedantMap;
    }
}