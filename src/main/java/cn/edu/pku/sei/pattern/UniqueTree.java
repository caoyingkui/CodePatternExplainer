package cn.edu.pku.sei.pattern;

import cn.edu.pku.sei.structureAlignment.CodeLineRelation.CodeLineRelationGraph;
import cn.edu.pku.sei.structureAlignment.tree.CodeStructureTree;
import cn.edu.pku.sei.structureAlignment.tree.node.Node;
import cn.edu.pku.sei.structureAlignment.tree.node.NodeType;
import cn.edu.pku.sei.util.SetOperator;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.*;

/**
 * Created by kvirus on 2019/3/30 16:50
 * Email @ caoyingkui@pku.edu.cn
 * <p>
 * |   *******    **     **      **     **
 * |  **            *   *        **  **
 * |  **              *          ***
 * |  **              *          **  **
 * |   *******        *          **     **
 */
public class UniqueTree {
//    public Node root;
//    public ArrayList<PatternTree> children;
//    public int hash = generateHash();
//
//    public UniqueTree(CodeStructureTree codeTree){
//        children = new ArrayList<>();
//        if (codeTree.root != null) {
//            root = codeTree.root.copy();
//            for (CodeStructureTree child: codeTree.getChildren()) {
//                this.children.add(new UniqueTree(child));
//            }
//        }
//    }
//
//    public boolean compare(UniqueTree codeTree) {
//        if (root.type != codeTree.root.type) return false;
//
//        if(root.type == NodeType.CODE_MethodInvocation) {
//            String method1 = getMethodName(this);
//            String method2 = getMethodName(codeTree);
//            return method1.equals(method2);
//        } else if (root.type == codeTree.root.type && children.size() == codeTree.children.size()) {
//            for (int i = 0; i < children.size(); i++) {
//                UniqueTree tree1 = children.get(i) instanceof UniqueTree ? (UniqueTree) (children.get(i)) : null;
//                UniqueTree tree2 = codeTree.children.get(i) instanceof UniqueTree ? (UniqueTree) (codeTree.children.get(i)) : null;
//
//                if (tree1 != null && tree2 != null && tree1.compare(tree2))
//                    return true;
//            }
//        }
//        return false;
//    }
//
//    private String getMethodName(UniqueTree codeTree) {
//        int size = codeTree.children.size();
//        UniqueTree last = (UniqueTree)codeTree.children.get(size - 1);
//        if (last.root.type == NodeType.CODE_MethodInvocation) {
//            return getMethodName(last);
//        } else {
//            UniqueTree first = (UniqueTree)codeTree.children.get(0);
//            return first.root.getContent();
//        }
//    }
//
//    public PatternTree merge(UniqueTree codeTree, Set<Integer> comments1, int comment2) {
//
//    }
//
//
//
//
//
//
//
//    public static void main(String[] args) {
//        CodeLineRelationGraph graph = new CodeLineRelationGraph().build("" +
//                "CellStyle style_yen = book.createCellStyle();\n" +
//                "        ExcelService.setBorder(style_yen, BorderStyle.THIN);\n" +
//                "        style_percent.setDataFormat(format.getFormat(\"\\\"\\\\\\\"#,##0;\\\"\\\\\\\"-#,##0\"));" +
//                "style_percent.setDataFormat(format.getFormat(\"0.0%\"));");
//
//        List<CodeStructureTree> trees = graph.getCodeLineTrees();
//
//        UniqueTree tree1 = new UniqueTree(trees.get(2));
//        UniqueTree tree2 = new UniqueTree(trees.get(3));
//
//    }

}
