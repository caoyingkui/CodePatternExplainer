package cn.edu.pku.sei.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by kvirus on 2019/3/31 9:19
 * Email @ caoyingkui@pku.edu.cn
 * <p>
 * |   *******    **     **      **     **
 * |  **            *   *        **  **
 * |  **              *          ***
 * |  **              *          **  **
 * |   *******        *          **     **
 */
public class SetOperator<E>  {
    public HashSet<E> set;

    public SetOperator() {
        set = new HashSet();
    }

    public boolean add(E ele) {
        set.add(ele);
        return true;
    }


    public void union(Set<E> anotherSet) {
        set = union(set, anotherSet);
    }

    public HashSet<E> union(Set<E> set1, Set<E> set2) {
        HashSet<E> ans = new HashSet<>();
        ans.addAll(set1);
        ans.addAll(set2);
        return ans;
    }

    public void intersection(Set<E> anotherSet) {
        set = intersection(set, anotherSet);
    }

    public HashSet<E> intersection(Set<E> set1, Set<E> set2) {
        HashSet<E> ans = new HashSet<>();
        for (E ele: set1) {
            if (set2.contains(ele))
                ans.add(ele);
        }
        return ans;
    }

    public void difference(Set<E> anotherSet) {
        set = difference(set, anotherSet);
    }

    public HashSet<E> difference(Set<E> set1, Set<E> set2) {
        HashSet<E> ans = new HashSet<>();
        for (E ele: set1) {
            if (!set2.contains(ele))
                ans.add(ele);
        }

        for (E ele: set2) {
            if (!set1.contains(ele))
                ans.add(ele);
        }
        return ans;
    }
}
