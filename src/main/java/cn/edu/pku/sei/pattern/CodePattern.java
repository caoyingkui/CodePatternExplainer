package cn.edu.pku.sei.pattern;

import cn.edu.pku.sei.structureAlignment.CodeLineRelation.CodeLineRelationGraph;
import cn.edu.pku.sei.structureAlignment.tree.CodeStructureTree;
import cn.edu.pku.sei.structureAlignment.tree.TextStructureTree;
import cn.edu.pku.sei.util.SetOperator;
import edu.stanford.nlp.simple.Sentence;

import java.util.*;

/**
 * Created by kvirus on 2019/3/30 15:51
 * Email @ caoyingkui@pku.edu.cn
 * <p>
 * |   *******    **     **      **     **
 * |  **            *   *        **  **
 * |  **              *          ***
 * |  **              *          **  **
 * |   *******        *          **     **
 */
public class CodePattern {
    List<PatternTree> codeLines;
    List<TextStructureTree> comments;
    Map<Integer, Set<Integer>> commentMap;



    public CodePattern(String code, String comment) {
        init();
        TextStructureTree textTree = new TextStructureTree(0);
        textTree.construct(new Sentence(comment));
        comments.add(textTree);

        codeLines = new CodeLineRelationGraph().build(code).getPatternTrees(0, textTree);
        for (PatternTree codeLine: codeLines) {
            Set<Integer> set = new HashSet<>();
            set.add(0);
            commentMap.put(codeLine.hash, set);
        }


    }

    public void init() {
        codeLines = new ArrayList<>();
        comments = new ArrayList<>();
        commentMap = new HashMap<>();
    }


    public void add(String code, String comment) {

        TextStructureTree textTree = new TextStructureTree(0);
        textTree.construct(new Sentence(comment));
        comments.add(textTree);
        int commentIndex = comments.size() - 1;

        List<PatternTree> trees = new CodeLineRelationGraph().build(code).getPatternTrees(commentIndex, textTree);
        int last = 0;
        for (PatternTree newTree: trees) {
            int i = 0;
            for (i = 0; i < codeLines.size(); i++) {
                PatternTree codeLine = codeLines.get(i);
                if (codeLine.compare(newTree)) {
                    last = Math.max(last, i);
                    codeLine.merge(newTree, commentMap.get(codeLine.hash), commentIndex);
                    commentMap.get(codeLine.hash).add(commentIndex);
                    break;
                }
            }
            if (i == codeLines.size()) {
                Set<Integer> set = new HashSet<>();
                set.add(commentIndex);
                commentMap.put(newTree.hash, set);
                codeLines.add(last + 1, newTree);
                last ++;
            }
        }
    }

    /**
     * 给定一段代码，为其产生注释
     * @param code 待产生注释的代码
     * @return 产生的注释
     */
    public String generate(String code) {
        List<PatternTree> trees = new CodeLineRelationGraph().build(code).getPatternTrees();
        assert(trees.size() > 0);

        Map<Integer, Replacement> ans = new HashMap<>();
        SetOperator<Integer> candidates = new SetOperator();
        for (int i = 0; i < comments.size(); i++)
            candidates.add(i);

        for (PatternTree tree: trees) {
            Map<Integer, Replacement> temp = findCandidate(tree);
            if (temp == null) continue;
            for (Integer treeID: temp.keySet()) {
                if (!ans.containsKey(treeID))
                    ans.put(treeID, new Replacement(treeID));
                ans.get(treeID).addAll(temp.get(treeID));
            }
            candidates.intersection(temp.keySet());

        }
        StringBuilder builder = new StringBuilder();
        if (candidates.set.size() > 0) {
            for (Integer commentID: candidates.set) {
                if (ans.get(commentID) == null) {
                    builder.append(comments.get(commentID).getContent()).append("\n");
                } else {
                    builder.append(ans.get(commentID).generate(comments.get(commentID))).append("\n");
                }
            }
        }
        return builder.toString();
    }

    public Map<Integer, Replacement> findCandidate(PatternTree tree) {
        for (PatternTree codeLine: codeLines) {
            if (!codeLine.compare(tree)) continue;
            Map<Integer, Replacement> ans = codeLine.match(tree, false);
            if (ans == null) continue;

            if (ans.size() == 0) {
                int index = codeLines.indexOf(codeLine);
                ans.put(index, null);
            }
            return ans;
        }
        return null;
    }

    public static void main(String[] args) {
        CodePattern pattern = new CodePattern(


                                                "\t\tstyle3.setAlignment(XSSFCellStyle.ALIGN_RIGHT);\n" +
                                                        "style3.setVerticalAlignment(XSSFCellStyle.VERTICAL_BOTTOM);"
                                                , "Bottom Right alignment");

        //pattern.add(
          //      "\t\tstyle4.setVerticalAlignment(XSSFCellStyle.TOP);\n", "top alignment");

        for (PatternTree tree: pattern.codeLines) {
            tree.print(0);
        }

        System.out.println(pattern.generate(
                        "\t\tstyle4.setAlignment(XSSFCellStyle.ALIGN_LEFT);\n" +
                                "style3.setVerticalAlignment(XSSFCellStyle.VERTICAL_TOP);"  ));
    }
}
