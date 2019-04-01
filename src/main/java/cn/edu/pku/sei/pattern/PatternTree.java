package cn.edu.pku.sei.pattern;

import cn.edu.pku.sei.structureAlignment.alignment.NodeComparator;
import cn.edu.pku.sei.structureAlignment.tree.CodeStructureTree;
import cn.edu.pku.sei.structureAlignment.tree.MatchedNode;
import cn.edu.pku.sei.structureAlignment.tree.node.Node;
import cn.edu.pku.sei.structureAlignment.tree.node.NodeType;
import cn.edu.pku.sei.util.SetOperator;
import sun.nio.cs.ext.MacThai;

import java.util.*;

/**
 * Created by kvirus on 2019/3/30 15:52
 * Email @ caoyingkui@pku.edu.cn
 * <p>
 * |   *******    **     **      **     **
 * |  **            *   *        **  **
 * |  **              *          ***
 * |  **              *          **  **
 * |   *******        *          **     **
 */
public class PatternTree {
    public PatternTree parent;
    public int hash;

    //region <field for UNIQUE >
    public Node root;
    public ArrayList<PatternTree> children;
    //endregion

    //region <field for MULTI>
    public ArrayList<PatternTree> candidates;
    public List<Set<Integer>> comments;

    /**
     * 该构造函数，只用于构造Multi类型节点
     * @param type
     * @param parent
     */
    public PatternTree(NodeType type, PatternTree parent) {
        assert(type == NodeType.PATTERN_MULTI);

        init();

        root = new Node(NodeType.PATTERN_MULTI, "MULTI", 0);
        this.parent = parent;

        candidates = new ArrayList<>();
        comments = new ArrayList<>();
    }

    /**
     * 该构造函数，只用于构造Unique类型节点
     * @param codeTree
     * @param parent
     */
    public PatternTree(CodeStructureTree codeTree, PatternTree parent){
        init();
        this.parent = parent;
        children = new ArrayList<>();
        if (codeTree.root != null) {
            root = copyRoot(codeTree.root);
            for (CodeStructureTree child: codeTree.getChildren()) {
                this.children.add(new PatternTree(child, this));
            }
        }
    }

    public boolean addCandidate(PatternTree candidate, Set<Integer> comment) {
        assert(root.type == NodeType.PATTERN_MULTI);
        candidates.add(candidate);

        Set<Integer> set = new HashSet<>();
        for (Integer i: comment)
            set.add(i);
        comments.add(set);
        return true;
    }

    public boolean compare(PatternTree codeTree) {
        if (root.type != codeTree.root.type)
            return false;
        if (root.type != codeTree.root.type) return false;

        if(root.type == NodeType.CODE_MethodInvocation) {
            String method1 = getMethodName(this);
            String method2 = getMethodName(codeTree);
            return method1.equals(method2);
        } else if ( children.size() == codeTree.children.size()) {
            for (int i = 0; i < children.size(); i++) {
                PatternTree tree1 = children.get(i).root.type == NodeType.PATTERN_MULTI ? null : children.get(i);
                PatternTree tree2 = codeTree.children.get(i).root.type == NodeType.PATTERN_MULTI ? null : codeTree.children.get(i);

                if (tree1 != null && tree2 != null && tree1.compare(tree2))
                    return true;
            }
        }
        return false;
    }

    private Node copyRoot(Node root) {
        Node node = new Node(root.type, new String(root.getContent()), root.id);
        node.setDisplayContent(new String(root.getDisplayContent()));
        node.matchedCodeNodeList = new ArrayList<>();
        for (MatchedNode m: root.matchedCodeNodeList) {
            if (m.similarity >= 1) {
                MatchedNode temp = new MatchedNode(m.codeTreeID, node, m.textTreeID, m.textNode, m.similarity);
                node.matchedCodeNodeList.add(temp);
            }
        }
        node.maxSimilarity = Math.min(root.maxSimilarity, 1);
        for (String al: root.alternatives)
            node.alternatives.add(al);
        return node;
    }

    public boolean equals(PatternTree anotherTree) {
        int size = children.size();
        if (    size        != anotherTree.children.size()  ||
                root.type   != anotherTree.root.type )
            return false;
        if (size == 0) {
            return root.getContent().equals(anotherTree.root.getContent());
        } else {
            for (int i = 0; i < size; i++) {
                PatternTree tree1 = children.get(i);
                PatternTree tree2 = anotherTree.children.get(i);
                if (!tree1.equals(tree2)) return false;
            }
        }
        return true;
    }

    public List<PatternTree> getAllLeafNodes(){
        List<PatternTree> ans = new ArrayList<>();
        if (children.size() == 0) {
            ans.add(this);
        } else {
            for (PatternTree child: children) {
                ans.addAll(child.getAllLeafNodes());
            }
        }
        return ans;
    }

    private String getMethodName(PatternTree codeTree) {
        int size = codeTree.children.size();
        PatternTree last = codeTree.children.get(size - 1);
        if (last.root.type == NodeType.CODE_MethodInvocation) {
            return getMethodName(last);
        } else {
            PatternTree first = codeTree.children.get(0);
            return first.root.getContent();
        }
    }

    private Map<Integer, Replacement> generateReplacement(PatternTree candidate, PatternTree codeTree) {
        if (!isSameTypeLeafNode(candidate, codeTree))
            return null;

        Map<Integer, Replacement> replacements = new HashMap<>();
        if (candidate.root.matchedCodeNodeList.size() > 0) {
            for (MatchedNode matchedNode : candidate.root.matchedCodeNodeList) {
                int treeID = matchedNode.textTreeID;
                int nodeID = matchedNode.textNode.id;
                String replace = codeTree.root.getContent();

                if (!replacements.containsKey(treeID)) {
                    replacements.put(treeID, new Replacement(treeID));
                }
                replacements.get(treeID).addReplacement(nodeID, replace);
            }
        }

        return replacements;
    }

    private Map<Integer, Replacement> generateReplacement(List<PatternTree> candidates, PatternTree codeTree) {
        Map<Integer, Replacement> res = new HashMap<>();
        for (int i = 0; i <candidates.size(); i ++) {
            PatternTree candidate = candidates.get(i);
            Map<Integer, Replacement> temp = generateReplacement(candidate, codeTree);
            for (Integer treeId: temp.keySet()) {
                if (res.containsKey(treeId)) res.get(treeId).addAll(temp.get(treeId));
                else res.put(treeId, temp.get(treeId));
            }
        }

        return  res.size() == 0 ? null : res;
    }


    /**
     * 判断两个节点是否为同一种类型的叶子节点
     * @param tree1
     * @param tree2
     * @return true, 当tree1和tree2都为叶子节点时，且两者的类型完全一致，例如tree1为整型，tree2也为整型
     */
    private boolean isSameTypeLeafNode(PatternTree tree1, PatternTree tree2) {
        return tree1.root.type == tree2.root.type &&
                tree1.children.size() == 0 &&
                tree2.children.size() == 0;
    }

    private boolean isSameTypeNonLeafNode(PatternTree tree1, PatternTree tree2) {
        return tree1.root.type == tree2.root.type &&
                tree1.children.size() > 0 &&
                tree2.children.size() > 0;

    }

    private void init() {
        hash = generateHash();
    }

    /**
     * match函数用来将codeTree代表的一行代码与代码模式中的一行进行匹配，
     * 匹配的结果是满足的comment的id集合
     *
     * strict为真的话，表示需要节点类型相同，值也相同，不要是对针对叶子节点为数值、字符串类型的节点
     * 为假的话，表示只需要匹配节点类型就可以，值是否相同，可适当放宽。
     * @return 如何是null的话，表示codeTree和当前的代码行不匹配
     *          如何使返回结果的size是0的话，表示codeTree和该模式完全匹配
     *          如何size大于0的话，表示当前的代码行与集合中的所代表的comment所对应的代码是一致的。
     */
    public Map<Integer, Replacement> match(PatternTree codeTree, boolean strict) {
        if (root.type == NodeType.PATTERN_MULTI) {
            Map<Integer, Replacement> replacements = new HashMap<>();
            for (int i = 0;i < candidates.size(); i++) {
                if (candidates.get(i).equals(codeTree)) {
                    for (Integer c: comments.get(i))
                        replacements.put(c, null);
                    return replacements;
                }
            }

            //如果没有完全匹配成功的candidate,则从中选出是否存在合适的选项
            replacements = generateReplacement(candidates, codeTree);
            return replacements;
        } else if (isSameTypeNonLeafNode(this, codeTree)) {
            Map<Integer, Replacement> res = new HashMap<>();
            int size = children.size();
            for (int i = 0; i < size; i++) {
                PatternTree tree1 = children.get(i);
                PatternTree tree2 = codeTree.children.get(i);
                Map<Integer, Replacement> temp = tree1.match(tree2, strict);
                if (temp == null) return null; // 某个节点信息不一致

                for (Integer treeID: temp.keySet()) {
                    if (!res.containsKey(treeID))
                    if (!res.containsKey(treeID))
                        res.put(treeID, new Replacement(treeID));
                    res.get(treeID).addAll(temp.get(treeID));
                }
            }
            return res;
        } else if (isSameTypeLeafNode(this, codeTree)) {
            //节点完全一致
            if (this.root.getContent().equals(codeTree.root.getContent()))
                return new HashMap<>();

            return generateReplacement(this, codeTree);
        } else {
            return null;
        }
    }

    public PatternTree merge(PatternTree codeTree, Set<Integer> comments1, int comment2) {
        if (root.type == NodeType.PATTERN_MULTI) {
            for (PatternTree candidate: candidates) {
                if (candidate.equals(codeTree)) {
                    return null;
                }
            }

            codeTree.parent = this.parent;
            Set<Integer> comments2 = new HashSet<>();
            comments2.add(comment2);
            this.addCandidate(codeTree, comments2);
            return null; // 不需要修改父亲节点的信息。
        }

        if (root.type != codeTree.root.type ||
                (root.type == NodeType.CODE_StringLiteral && !root.getContent().equals(codeTree.root.getContent()))||
                (this.children.size() != codeTree.children.size() ) ||
                (children.size() == 0 && NodeComparator.compare(root, codeTree.root) == 0)
        ) {
            PatternTree mTree = new PatternTree(NodeType.PATTERN_MULTI, this.parent);
            mTree.addCandidate(this, comments1);
            codeTree.parent = this.parent;

            Set<Integer> comments2 = new HashSet<>();
            comments2.add(comment2);
            mTree.addCandidate(codeTree, comments2);
            return mTree;
        } else {
            if (this.children.size() > 0) {
                int size = this.children.size();
                for (int i = 0; i < size; i++) {
                    PatternTree child1 = this.children.get(i);
                    PatternTree child2 = codeTree.children.get(i);
                    PatternTree child = child1.merge(child2, comments1, comment2);
                    if (child != null) {
                        children.remove(i);
                        children.add(i, child);
                    }
                }
            }
        }

        return null;
    }

    public void print(int depth) {
        for (int i = 0; i < depth; i++)
            System.out.print("\t");
        if (this.root.type == NodeType.PATTERN_MULTI){
            System.out.println("MultiTree:");
            for (PatternTree candidate: candidates) {
                candidate.print(depth + 1);
            }
        } else {
            System.out.println((children.size() == 0 ? root.getContent() : root.type + ":") );
            for (PatternTree child: children) {
                child.print(depth + 1);
            }
        }
    }

    private int generateHash() {
        return this.hashCode();
    }
}
