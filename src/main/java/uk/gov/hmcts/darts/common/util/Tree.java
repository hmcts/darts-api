package uk.gov.hmcts.darts.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"PMD.ShortClassName"})
public class Tree<T extends TreeNode> extends HashMap<Integer, T> {

    public void addNode(T node) {
        put(node.getId(), node);
    }

    public void addNode(Collection<T> nodeCollection) {
        nodeCollection.forEach(this::addNode);
    }

    public Optional<T> getParent(T node) {
        Optional<T> parentNode = Optional.empty();
        if (node.doesHaveAntecedent()) {
            return Optional.of(get(Integer.valueOf(node.getAntecedent())));
        }

        return parentNode;
    }

    public List<T> getChildren(T node) {
        List<T> children = new ArrayList<>();
        if (node.doesHaveAntecedent()) {
            children = getNodesWithAntecendantMap().get(node.getId().toString());
        }

        return children;
    }

    public boolean isLeaf(T node) {
        return getNodesWithAntecendantMap().get(node.getId().toString()) == null;
    }

    public Collection<T> getLowestLevelDescendants() {
        List<T> topLevelNodes = getTopLevelNodes();
        List<T> lowestNodes = new ArrayList<>();

        // gets the lowest level top nodes
        for (T node : topLevelNodes) {
            Map<String, List<T>>  antecendantMap = getNodesWithAntecendantMap();

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
        values().forEach((node) -> {
            if (!node.doesHaveAntecedent()) {
                topLevelList.add(node);
            }
        });

        return topLevelList;
    }

    public List<T> getNodeWhichIsNotAnAntecedent() {
        final List<T> lowestLevelNodes = new ArrayList<>();
        Map<String, List<T>>  atecendantMap = getNodesWithAntecendantMap();
        for (List<T> nodeLst : atecendantMap.values()) {
            // if this node is not an attendant to any other node this is the lowest level
            for (T node : nodeLst) {
                if (atecendantMap.get(node.getId().toString()) == null) {
                    lowestLevelNodes.add(node);
                }
            }
        }

        return lowestLevelNodes;
    }

    private Map<String, List<T>> getNodesWithAntecendantMap() {
        final Map<String, List<T>> atecedantMap = new HashMap<>();
        values().forEach((node) -> {
            if (node.doesHaveAntecedent() && !atecedantMap.containsKey(node.getAntecedent())) {
                List<T> nodeList = new ArrayList<>();
                nodeList.add(node);
                atecedantMap.put(node.getAntecedent(), nodeList);
            } else if (node.doesHaveAntecedent()) {
                atecedantMap.get(node.getAntecedent()).add(node);
            }
        });

        return atecedantMap;
    }
}