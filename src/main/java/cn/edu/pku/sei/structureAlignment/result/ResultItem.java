package cn.edu.pku.sei.structureAlignment.result;

/**
 * Created by oliver on 2018/3/28.
 */
public class ResultItem {
    public int exampleCount;
    public int completelyMatch;
    public int partlyMatch;
    public int wronglyMatch;
    public int noMatch;

    public ResultItem(){
        exampleCount = 0;
        completelyMatch = 0;
        partlyMatch = 0;
        wronglyMatch = 0 ;
        noMatch = 0;
    }
}
