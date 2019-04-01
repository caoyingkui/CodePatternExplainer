
import java.util.List;

public class sample{
    public static List<Integer> list = new ArrayList<>();

    public static void main(String[] args) {
        int a = 1;
        List<Integer> list = new ArrayList<>();
        list.add(a);
        //sample.list.add(a);
        //int b = list.get(0);
        //int c = bar(list).bar(list).get(0);
    }
    public static int foo(int a){return a;}

    public static List<Integer> bar(List<Integer> list){
        return list.clone();
    }

}