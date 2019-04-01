package cn.edu.pku.sei.pattern;

import cn.edu.pku.sei.structureAlignment.tree.CodeStructureTree;
import cn.edu.pku.sei.structureAlignment.tree.TextStructureTree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kvirus on 2019/3/31 19:13
 * Email @ caoyingkui@pku.edu.cn
 * <p>
 * |   *******    **     **      **     **
 * |  **            *   *        **  **
 * |  **              *          ***
 * |  **              *          **  **
 * |   *******        *          **     **
 */
public class Replacement {
    int commentID;
    Map<Integer, String> replacement;

    public Replacement(int commentID) {
        this.commentID = commentID;
        replacement = new HashMap<>();
    }

    public boolean addReplacement(int key, String value) {
        replacement.put(key, value);
        return true;
    }

    public boolean addAll(Replacement re) {
        if (re != null)
            replacement.putAll(re.replacement);
        return true;
    }

    public String generate(TextStructureTree textTree) {
        StringBuilder builder = new StringBuilder();
        generate(textTree, builder);
        return builder.toString();
    }

    private void generate(TextStructureTree textTree, StringBuilder builder) {
        List<TextStructureTree> children = textTree.getChildren();
        if (children.size() == 0) {
            int id = textTree.getId();
            if (replacement.containsKey(id)) builder.append(replacement.get(id)).append(" ");
            else builder.append(textTree.getContent()).append(" ");
        } else {
            for (TextStructureTree child: children) {
                generate(child, builder);
            }
        }
    }
}
