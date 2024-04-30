package uk.gov.hmcts.darts.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;

class TreeTest {

    @Test
    void testTreeWithAndWithoutAntecedent() {
        Tree<CustomTreeNode> tree = new Tree<>();
        CustomTreeNode treeNode1NoAntecedent = new CustomTreeNode(1, null);
        CustomTreeNode treeNode2NoAntecedent = new CustomTreeNode(2, null);
        CustomTreeNode treeNode3AntecedantOf2 = new CustomTreeNode(3, Integer.toString(2));
        CustomTreeNode treeNode4AntecedantOf3 = new CustomTreeNode(4, Integer.toString(3));
        CustomTreeNode treeNode5AntecedantOf2 = new CustomTreeNode(5, Integer.toString(2));

        tree.addNode(treeNode1NoAntecedent);
        tree.addNode(treeNode2NoAntecedent);
        tree.addNode(treeNode3AntecedantOf2);
        tree.addNode(treeNode4AntecedantOf3);
        tree.addNode(treeNode5AntecedantOf2);

        Collection<CustomTreeNode> lowestLevel = tree.getLowestLevelDescendants();
        Assertions.assertTrue(lowestLevel.contains(treeNode1NoAntecedent));
        Assertions.assertTrue(lowestLevel.contains(treeNode4AntecedantOf3));
        Assertions.assertTrue(lowestLevel.contains(treeNode5AntecedantOf2));

        // we dont expect the following to be returned they themselves are antecedents of other nodes
        Assertions.assertFalse(lowestLevel.contains(treeNode2NoAntecedent));
        Assertions.assertFalse(lowestLevel.contains(treeNode3AntecedantOf2));
    }

    static class CustomTreeNode implements TreeNode {

        private final Integer id;
        private final String atecedant;

        public CustomTreeNode(Integer id, String atecedant) {
            this.id = id;
            this.atecedant = atecedant;
        }

        @Override
        public Integer getId() {
            return id;
        }

        @Override
        public String getAntecedent() {
            return atecedant;
        }
    }
}