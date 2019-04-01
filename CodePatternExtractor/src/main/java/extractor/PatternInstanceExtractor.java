package extractor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import dataStructure.PatternInstance;
import dataStructure.PatternInstanceLine;
import utils.Config;
import utils.DirExplorer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PatternInstanceExtractor {
    //Singleton
    private static PatternInstanceExtractor instance;

    PatternInstanceExtractor() {
    }

    public static PatternInstanceExtractor getInstance() {
        if (instance == null) {
            instance = new PatternInstanceExtractor();
        }
        return instance;
    }

    //Data
    private ArrayList<String> patternApis;
    private List<PatternInstance> patternInstances;
    private DirExplorer corpusDirExplorer;

    //Initialization
    private void initializeSymbolSolver() {
        try {
            TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
            System.out.println(System.getProperty("user.dir"));
            TypeSolver jarTypeSolver = new JarTypeSolver("./CodePatternExtractor/poi/poi-3.17.jar");
            TypeSolver jarTypeSolver2 = new JarTypeSolver("./CodePatternExtractor/poi/poi-ooxml-3.17.jar");
            TypeSolver combinedTypeSolver = new CombinedTypeSolver(reflectionTypeSolver, jarTypeSolver, jarTypeSolver2);

            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
            JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void loadPatternApis(String patternFilePath) {
        patternApis = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(patternFilePath));
            String patternAndFrequency = reader.readLine();
            String pattern = patternAndFrequency.substring(0, patternAndFrequency.lastIndexOf(" :"));
            for (String methodCall : pattern.split(" ")) {
                patternApis.add(methodCall);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void initializeExtractor(String patternFilePath) {
        initializeSymbolSolver();

        loadPatternApis(patternFilePath);
        patternInstances = new ArrayList<>();
        corpusDirExplorer = new DirExplorer(
                (level, path, file) -> file.getName().endsWith("java"),
                new extractorVisitor()
        );
    }

    //The only exposed method
    public List<PatternInstance> extract(String patternFilePath, String corpusDirPath) {
        initializeExtractor(patternFilePath);

        System.out.println("------------------------------------");
        System.out.println("Start Extraction Process");
        System.out.println("------------------------------------");
        corpusDirExplorer.explore(new File(corpusDirPath));
        System.out.println("------------------------------------");
        System.out.println("Extraction Process Done");
        System.out.println("------------------------------------");

        return patternInstances;
    }

    private class extractorVisitor implements DirExplorer.FileHandler {
        private int currentFocusApi;
        private PatternInstance patternInstance;

        @Override
        public void handle(int level, String path, File file) {

            try {
                int last = instance.patternInstances.size();

                //System.out.println("Parsing File : " + file.getName());
                CompilationUnit cu = JavaParser.parse(file);
                cu.accept(
                        new VoidVisitorAdapter<Object>() {
                            public void visit(MethodDeclaration n, Object arg) {

                                //System.out.println(n.getDeclarationAsString());
                                currentFocusApi = 0;
                                patternInstance = new PatternInstance();
                                super.visit(n, arg);
                            }

                            public void visit(MethodCallExpr n, Object arg) {
                                //System.out.println(n.toString());
                                super.visit(n, arg);
                                if (!n.getName().toString().equals(instance.patternApis.get(currentFocusApi))) {
                                    return;
                                }
                                if (currentFocusApi == 0 && n.getBegin().isPresent())
                                        patternInstance.beginLine = n.getBegin().get().line;

                                currentFocusApi++;

                                patternInstance.addLine(new PatternInstanceLine(n));
                                if (currentFocusApi == instance.patternApis.size()) {
                                    if (n.getBegin().isPresent()) {
                                        patternInstance.endLine = n.getBegin().get().line;
                                    }
                                    patternInstance.sourceFile = file.getAbsolutePath();
                                    instance.patternInstances.add(patternInstance);
                                    patternInstance.getComments();

                                    patternInstance = new PatternInstance();
                                    currentFocusApi = 0;
                                }
                            }

                            public void visit(LineComment n, Object arg) {
                                // System.out.println(n.toString());
                                super.visit(n, arg);
                            }

                            public void visit(BlockComment n, Object arg) {
                                //System.out.println(n.toString());
                                super.visit(n, arg);
                            }
                        }
                        , null);

//                for (int i = last; i < instance.patternInstances.size(); i++) {
//                    PatternInstance ins = instance.patternInstances.get(i);
//                    List<String> comments = ins.getComments();
//                    if (comments.size() > 0 ){
//                        System.out.println(ins.sourceFile);
//                        for (String comment: comments){
//                            System.out.println(comment);
//                            System.out.println();
//                        }
//                    }
//                }

            } catch (Exception e) {
                if(Config.debug()){
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        PatternInstanceExtractor extractor = getInstance();
        List<PatternInstance> instances = extractor.extract("./CodePatternExtractor/data/fill_color/pattern", "E:\\Intellij workspace\\SCSMiner-master\\repo\\repo");
        instances = PatternInstanceFilter.filter(instances);
        for (PatternInstance instance: instances) {
            instance.getComments();
            //List<String> comments = instance.getComments();
            /*if (comments.size() > 0) {
                System.out.println(instance.sourceFile);
                for (String comment : comments) {
                    System.out.println(comment);
                }
                System.out.println("");
            }*/
        }
    }
}
