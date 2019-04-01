package cn.edu.pku.sei.summarization;

import cn.edu.pku.sei.structureAlignment.alignment.NodeComparator;
import cn.edu.pku.sei.structureAlignment.tree.CodeStructureTree;
import cn.edu.pku.sei.structureAlignment.tree.MatchedNode;
import cn.edu.pku.sei.structureAlignment.tree.TextStructureTree;
import cn.edu.pku.sei.structureAlignment.tree.TreeUtil;
import cn.edu.pku.sei.structureAlignment.tree.node.Node;
import cn.edu.pku.sei.structureAlignment.util.DoubleValue;
import cn.edu.pku.sei.structureAlignment.util.Matrix;
import javafx.util.Pair;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class Summarization {
    private List<CodeStructureTree> codeTrees;
    private List<TextStructureTree> textTrees;

    // code term frequency
    private Map<String, Integer> codeTf;

    //text term frequency
    private Map<String, Integer> textTf;

    public Summarization(){
        ;
    }

    public Summarization(String code, String description) {
        codeTrees = TreeUtil.constructCodeTrees(code);
        textTrees = TreeUtil.constructTextTrees(description);
    }

    public Summarization(String code, List<String> sentences) {
        codeTrees = TreeUtil.constructCodeTrees(code);
        textTrees = TreeUtil.constructTextTrees(sentences);
    }

    public Summarization setCode(String code) {
        codeTrees = TreeUtil.constructCodeTrees(code);
        if (textTrees != null) textTrees.forEach(tree -> tree.clean());
        return this;
    }

    public Summarization setText(List<String> sentences) {
        textTrees = TreeUtil.constructTextTrees(sentences);
        if (codeTrees != null) codeTrees.forEach(tree -> tree.clean());
        return this;
    }

    /**
     * 获取每个一棵textTree树的，对应的每一棵代码树的集合
     * @param textTree
     * @return 返回值是一个map，key是一个code树的id，value则是，该棵树上可以对应到text数的某个节点
     */
    public Map<Integer , List<MatchedNode>> getMatchedNode(TextStructureTree textTree){
        Map<Integer , List<MatchedNode>> result = new HashMap<>();
        List<MatchedNode> matchedNodeList = textTree.root.matchedCodeNodeList;

        // 获取当前节点root对应的节点集合
        matchedNodeList.stream().forEach( node -> {
            int codeTreeID = node.codeTreeID;
            if(result.containsKey(codeTreeID)) {
                result.get(codeTreeID).add(node);
            } else {
                List<MatchedNode> matchedNodes = new ArrayList< >();
                matchedNodes.add(node);
                result.put(codeTreeID, matchedNodes);
            }
        });

        //递归收集子节点的对应节点集合的信息
        textTree.getChildren().stream().forEach( child -> {
            Map<Integer , List<MatchedNode>> temp = getMatchedNode(child);
            for(Integer codeTreeNum : temp.keySet()){
                if (result.containsKey(codeTreeNum)) result.get(codeTreeNum).addAll(temp.get(codeTreeNum));
                else result.put(codeTreeNum , temp.get(codeTreeNum));
            }
        });
        return result;
    }

    public Matrix<DoubleValue> getMatrix() {
        return match(codeTrees, textTrees);
    }

    public List<String> getMainDescription(int topN) {
        Matrix<DoubleValue> matrix = match(codeTrees, textTrees);
        matrix.print(0.0);
        List<Integer> sentences = matrix.getTopNCollumn(topN);

        return sentences.stream().map(sentence -> textTrees.get(sentence).getContent()).collect(toList());
    }

    // match
    public Matrix<DoubleValue> match(List<CodeStructureTree> codeTrees, List<TextStructureTree> textTrees) {
        tryToMatchLeafNode();
        tryToMatchNonleafNode();

        Matrix<DoubleValue> matrix = new Matrix<>(codeTrees.size(), textTrees.size(), new DoubleValue(0));

        textTrees.stream().
            flatMap(textTree -> {
                int textTreeID = textTrees.indexOf(textTree);
                // 获取这个text树，所对应的code树，以及该code树上的节点集合
                // key为一棵code树的id，value为该code树上的节点集合
                Map<Integer, List<MatchedNode>> alignment = getMatchedNode(textTree);
                List<Pair<Pair<Integer, Integer>, List<MatchedNode>>> list = new ArrayList<>();

                alignment.keySet().forEach(codeTreeID ->{
                    Pair<Integer, Integer> treePair = new Pair<>(codeTreeID, textTreeID);
                    List<MatchedNode> matchedNodes = alignment.get(codeTreeID);
                    list.add(new Pair<>(treePair, matchedNodes));
                });
                return list.stream();
            }).forEach(pair -> {
                int codeTreeID = pair.getKey().getKey(), textTreeID = pair.getKey().getValue();
                List<MatchedNode> matchedNodes = pair.getValue();
                Matrix<DoubleValue> matrixTemp = new Matrix<>(codeTrees.get(codeTreeID).getEndIndex() + 1, textTrees.get(textTreeID).getEndIndex() + 1, new DoubleValue(0));
                matchedNodes.forEach(matchedNode -> {
                    double m1 = 0, n1 = 0, m2 = 0, n2 = 0;
                    for (MatchedNode node: matchedNode.codeNode.matchedCodeNodeList) {
                        n1 ++;
                        if (node.textTreeID == textTreeID) m1 ++;
                    }

                    for (MatchedNode node: matchedNode.textNode.matchedCodeNodeList) {
                        n2 ++;
                        if (node.codeTreeID == codeTreeID) m2 ++;
                    }
                    double sim = 0.5 * matchedNode.similarity * (m1 / n1 + m2 / n2);
                    String log = matchedNode.logInfo + "  --- 0.5 * " + matchedNode.similarity + " * ( " + m1 + "/" + n1 + "+" + m2 + "/" + n2 + ")";
                    matrixTemp.setValue(matchedNode.codeNode.getId() , matchedNode.textNode.getId(), sim);
                    matrixTemp.setLogInfo(matchedNode.codeNode.getId() , matchedNode.textNode.getId(), log );
                });

                Map<Pair<Integer, Integer>, Double> match = new HashMap<>();
                double sim = matrixTemp.similarity(match);
                matrix.setValue(codeTreeID, textTreeID, sim);
                String log = "";
                if(sim > 0){
                    for(Pair<Integer , Integer> matchPair : match.keySet()){
                        log += "\t" + matrixTemp.getLogInfo(matchPair.getKey(), matchPair.getValue()) + "\n";
                    }
                }
            });

        return matrix;
    }

    void firstBackPropagationForPruning(TextStructureTree textTree , Set<Integer> parentHasBeenMatchedToTheseTrees){
        if(parentHasBeenMatchedToTheseTrees.size() > 0){
            Iterator<MatchedNode> iterator = textTree.root.matchedCodeNodeList.iterator();
            while(iterator.hasNext()){
                //去除了一个！
                if(parentHasBeenMatchedToTheseTrees.contains(iterator.next().codeTreeID)){
                    iterator.remove();
                }
            }
        }


        List<TextStructureTree> children = textTree.getChildren();
        if(children.size() > 0){
            for(TextStructureTree child : children) {
                Set<Integer> treeSet = new HashSet<>();
                treeSet.addAll(parentHasBeenMatchedToTheseTrees);
                for (MatchedNode matchedNode :textTree.root.matchedCodeNodeList ){
                    treeSet.add(matchedNode.codeTreeID);
                }
                firstBackPropagationForPruning(child, treeSet);
            }
        }
    }

    public static void matchNodes(int codeTreeID, int textTreeID, List<Node> codeLeafNodes, List<Node> textLeafNodes){
        for(Node textNode : textLeafNodes){
            for(Node codeNode : codeLeafNodes){
                if(codeNode.isPunctuation())
                    continue;
                double compareResult = NodeComparator.compare(codeNode, textNode);

                if(compareResult > 0 ){ //&& compareResult >= textNode.maxSimilarity){
                    MatchedNode newNode = new MatchedNode(codeTreeID , codeNode , textTreeID , textNode, compareResult);
                    newNode.logInfo = codeNode.getContent() + " " + textNode.getContent() + " : " + compareResult + " " + codeNode.getId() + " " + textNode.getId() ;

                    textNode.addMatchedNode(newNode);
                    codeNode.addMatchedNode(newNode);
                }
            }
        }
    }



    private void tryToMatchLeafNode( ) {
        //WN.extend(codeTrees , textTrees);
        List<List<Node>> codeTreeLeafNodes = new ArrayList<>();
        for (CodeStructureTree codeTree : codeTrees) {
            codeTreeLeafNodes.add(codeTree.getAllLeafNodes());
        }

        List<List<Node>> textTreeLeafNodes = new ArrayList<>();
        for (TextStructureTree textTree : textTrees) {
            textTreeLeafNodes.add(textTree.getAllLeafNodes());
        }

        int codeLine = 0, codeLineCount = codeTrees.size();
        int textLine = 0, textLineCount = textTrees.size();


        for (textLine = 0; textLine < textLineCount; textLine++) {
            List<Node> textLeafNodes = textTreeLeafNodes.get(textLine);
            for (codeLine = 0; codeLine < codeLineCount; codeLine++) {
                System.out.println(codeLine );
                List<Node> codeLeafNodes = codeTreeLeafNodes.get(codeLine);
                matchNodes(codeLine, textLine, codeLeafNodes, textLeafNodes);
            }
        }
    }

    private void tryToMatchNonleafNode(){
        for(int textTreeID = 0 ; textTreeID < textTrees.size() ; textTreeID ++){
            TextStructureTree tree = textTrees.get(textTreeID);
            tryToMatchNonleafNode(textTreeID, tree);
        }
        for(TextStructureTree textTree : textTrees){
            firstBackPropagationForPruning(textTree , new HashSet<>());
        }
    }

    List<MatchedNode> tryToMatchNonleafNode(int textTreeID, TextStructureTree textTree){
        List<MatchedNode> result = new ArrayList<>();
        List<TextStructureTree> children = textTree.getChildren();
        if(children.size() == 0){
            for(MatchedNode node : textTree.root.matchedCodeNodeList){
                if(node.similarity > 1){
                    result.add(node);
                }
            }
            return result;
        }else{
            int treeOccurTimes[] = new int[codeTrees.size()];
            for(int i = 0; i < codeTrees.size() ; i ++)
                treeOccurTimes[i] = 0;

            Map<Integer, Set<MatchedNode>> nodesFromSameTree = new HashMap<>();

            for(TextStructureTree child : children){
                List<MatchedNode> matchedNodes = tryToMatchNonleafNode(textTreeID, child);

                //在当前child子树中出现了那些树的节点。
                Set<Integer> codeTreeNums = new HashSet<>();
                matchedNodes.stream().
                        filter(matchedNode -> {
                            int codeTreeID = matchedNode.codeTreeID;
                            if (!nodesFromSameTree.containsKey(codeTreeID)) nodesFromSameTree.put(codeTreeID, new HashSet<>());
                            return true;
                        }).forEach(matchedNode -> {
                            codeTreeNums.add(matchedNode.codeTreeID);
                            nodesFromSameTree.get(matchedNode.codeTreeID).add(matchedNode);
                        });

                codeTreeNums.stream().forEach(codeTreeNum -> treeOccurTimes[codeTreeNum]++);
            }

            Pair<Map<Integer, Integer> , Double> mergeNodes = getTextMergeNode(nodesFromSameTree, treeOccurTimes);

            if(mergeNodes != null){
                Map<Integer, Integer> nodes = mergeNodes.getKey();
                double sim = mergeNodes.getValue();
                Node textNode = textTree.root;

                for(int codeTreeID : nodes.keySet()){
                    int codeNodeID = nodes.get(codeTreeID);
                    Node codeNode = codeTrees.get(codeTreeID).getNode(codeNodeID);
                    MatchedNode newNode = new MatchedNode(codeTreeID , codeNode , textTreeID, textNode , sim);
                    String logInfo = "";
                    for(MatchedNode subNode : nodesFromSameTree.get(codeTreeID)){
                        logInfo += " (" + subNode.logInfo + ")";
                    }
                    logInfo += (" : " + sim );

                    newNode.logInfo = logInfo;
                    textNode.addMatchedNode(newNode);
                    codeNode.addMatchedNode(newNode);

                    result.add(newNode);
                }

                for(Integer codeTreeNum : nodesFromSameTree.keySet()){
                    //codeTreeNum这棵树的节点以及被merge,所以来自这个树的节点就不要了。
                    if(nodes.containsKey(codeTreeNum))
                        continue;
                    for(MatchedNode matchedNode: nodesFromSameTree.get(codeTreeNum))
                        result.add(matchedNode);
                }

                return result;
            }else{
                for(int codeTreeNum : nodesFromSameTree.keySet()){
                    result.addAll(nodesFromSameTree.get(codeTreeNum));
                }
                return result;
            }
        }
    }

    Pair<Map<Integer, Integer>, Double> getTextMergeNode(Map<Integer, Set<MatchedNode>> nodes, int[] treeOccurTimes){
        List<Integer> mostSimilarTrees = new ArrayList<>();

        Optional<Pair<Integer, Double>> pair = nodes.keySet().stream().
                filter(codeTreeID -> treeOccurTimes[codeTreeID] > 1).
                map(codeTreeID -> {
                    Set<MatchedNode> matchedNodes = nodes.get(codeTreeID);
                    double sim = matchedNodes.stream().
                            map(matchedNode -> matchedNode.similarity).
                            reduce(0.0, (max_sim, nodeSim) -> max_sim > nodeSim ? max_sim : nodeSim );
                    sim += 0.2 * matchedNodes.size();
                    return new Pair<Integer, Double> (codeTreeID, sim);
                }).
                reduce((max, cur) -> {
                    if (max.getValue() > cur.getValue()) return max;
                    if (max.getValue() < cur.getValue()) mostSimilarTrees.clear();
                    mostSimilarTrees.add(cur.getKey());
                    return cur;
                });

        if (!pair.isPresent()) return null;
        double maxSim = pair.get().getValue();
        Map<Integer, Integer> mergeNodes = new HashMap<>();
        mostSimilarTrees.forEach(codeTreeID -> {
            List<Integer> nodeList = nodes.get(codeTreeID).stream().map(matchedNode -> matchedNode.codeNode.getId()).collect(toList());
            int parentID = codeTrees.get(codeTreeID).findCommonParents(new HashSet<>(nodeList));
            mergeNodes.put(codeTreeID, parentID);
        });
        return mergeNodes.size() > 0 ? new Pair<>(mergeNodes , maxSim) : null;
    }

    public static void main(String[] args) {
        String description =
                "It would be useful if Solr supported Lucene's capability to do partial optimizes.\n" +
                        "The associated method on the IndexWriter is http://lucene.apache.org/java/2_3_2/api/core/org/apache/lucene/index/IndexWriter.html#optimize(int,%20boolean) and the variations there-in";
        String code = "SolrCore core = h.getCore();\n" +
                "\n" +
                "    UpdateHandler updater = core.getUpdateHandler();\n" +
                "    AddUpdateCommand cmd = new AddUpdateCommand();\n" +
                "    cmd.overwriteCommitted = true;\n" +
                "    cmd.overwritePending = true;\n" +
                "    cmd.allowDups = false;\n" +
                "    //add just under the merge factor, so no segments are merged\n" +
                "    //the merge factor is 1000 and the maxBufferedDocs is 2, so there should be 500 segments (498 segs each w/ 2 docs, and 1 segment with 1 doc)\n" +
                "    for (int i = 0; i < 999; i++) {\n" +
                "      // Add a valid document\n" +
                "      cmd.doc = new Document();\n" +
                "      cmd.doc.add(new Field(\"id\", \"id_\" + i, Field.Store.YES, Field.Index.UN_TOKENIZED));\n" +
                "      cmd.doc.add(new Field(\"subject\", \"subject_\" + i, Field.Store.NO, Field.Index.TOKENIZED));\n" +
                "      updater.addDoc(cmd);\n" +
                "    }\n" +
                "\n" +
                "    CommitUpdateCommand cmtCmd = new CommitUpdateCommand(false);\n" +
                "    updater.commit(cmtCmd);\n" +
                "\n" +
                "    String indexDir = core.getIndexDir();\n" +
                "    assertNumSegments(indexDir, 500);\n" +
                "\n" +
                "    //now do an optimize\n" +
                "    cmtCmd = new CommitUpdateCommand(true);\n" +
                "    cmtCmd.maxOptimizeSegments = 250;\n" +
                "    updater.commit(cmtCmd);\n" +
                "    assertNumSegments(indexDir, 250);\n" +
                "\n" +
                "    cmtCmd.maxOptimizeSegments = -1;\n" +
                "    try {\n" +
                "      updater.commit(cmtCmd);\n" +
                "      assertTrue(false);\n" +
                "    } catch (IllegalArgumentException e) {\n" +
                "    }\n" +
                "    cmtCmd.maxOptimizeSegments = 1;\n" +
                "    updater.commit(cmtCmd);\n" +
                "    assertNumSegments(indexDir, 1);";
        Summarization summarization = new Summarization(code, description);
        summarization.getMainDescription(2).forEach(sentence -> System.out.println(sentence));
        int a = 2;
    }
}
