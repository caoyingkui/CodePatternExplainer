package dataStructure;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import javafx.util.Pair;
import jdk.nashorn.internal.ir.BlockStatement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatternInstance implements Cloneable {


    private List<PatternInstanceLine> lines = new ArrayList<>();
    private String Signature = "";

    //记录该模式来自哪个文件
    public String sourceFile;

    //记录该模式在该文件中的起始行
    public int beginLine;

    //记录该模式在该文件中的终止行
    public int endLine;


    public void addLine(PatternInstanceLine line) {
        lines.add(line);
    }

    public boolean isIllegalPattern() {
        boolean isIllegal = false;
        for (PatternInstanceLine line : lines) {
            if (line.mode == PatternInstanceLine.Mode.ERROR) {
                isIllegal = true;
            }
        }
        return isIllegal;
    }

    public String getSignature() {
        if (Signature != "") {
            return Signature;
        }
        for (PatternInstanceLine line : lines) {
            Signature += line.getLineSignature() + '\n';
        }
        return Signature;
    }

    public List<String> pathSerialize() {
        List<String> fillers = new ArrayList<>();
        for (PatternInstanceLine line : lines) {
            fillers.addAll(line.pathSerialize());
        }
        return fillers;
    }

    public List<String> serialize() {
        List<String> fillers = new ArrayList<>();
        for (PatternInstanceLine line : lines) {
            fillers.addAll(line.serialize());
        }
        return fillers;
    }

    public void deserialize(List<String> fillers) {
        int startPoint = 0;
        for (PatternInstanceLine line : lines) {
            startPoint = line.deserialize(fillers, startPoint);
        }
    }

    public List<String> typeSerialize() {
        List<String> types = new ArrayList<>();
        for (PatternInstanceLine line : lines) {
            types.addAll(line.typeSerialize());
        }
        return types;
    }

    public List<String> getSymbols() {
        List<String> symbols = new ArrayList<>();
        for (PatternInstanceLine line : lines) {
            symbols.addAll(line.getSymbols());
        }
        Set<String> reducedSymbols = new HashSet<>(symbols);
        return new ArrayList<>(reducedSymbols);
    }

    public List<Integer> getIntermediateHoles() {
        int startPos = 0;
        List<Integer> intermediateHoles = new ArrayList<>();
        for (PatternInstanceLine line : lines) {
            if (line.mode == PatternInstanceLine.Mode.ASSIGN) {
                intermediateHoles.add(startPos);
            }
            startPos += line.serialize().size();
        }
        return intermediateHoles;
    }

    public List<String> getComments() {
        List<String> result = new ArrayList<>();
        try {
            List<String> codeLines = new ArrayList<>();
            codeLines.add(""); // 由于javaparser的行号是从1开始的，因此加一行空行，使得javaparser给出的行号，和实际的下标是一致的。
            try {
                BufferedReader reader = new BufferedReader(new FileReader(new File(sourceFile)));
                String line ;
                while((line = reader.readLine()) != null) {
                    codeLines.add(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            CompilationUnit cu = JavaParser.parse(new File(this.sourceFile));
            cu.accept(new VoidVisitorAdapter<Object>() {
                public void visit(MethodDeclaration n, Object arg) {
                    int rangeBegin = 0, rangeEnd = 0;
                    BlockStmt block =  n.getBody().get();
                    int blockBegin = block.getBegin().get().line, blockEnd = block.getEnd().get().line;
                    if (blockBegin <= beginLine && endLine <= blockEnd) {
                        boolean s = false;
                        for (int i = beginLine; i > blockBegin; i -- ) {
                            if (codeLines.get(i).trim().compareTo("") == 0) break;
                            if (s && codeLines.get(i).trim().indexOf("//") != 0) break;
                            if (codeLines.get(i).trim().indexOf("//") == 0) s = true;
                            rangeBegin = i;
                        }

                        for (int i = endLine; i < blockEnd; i++) {
                            if (codeLines.get(i).trim().compareTo("") == 0) break;
                            rangeEnd = i;
                        }

                        super.visit(n, new Pair<Integer, Integer>(rangeBegin, rangeEnd));
                    }


                }
                public void visit(LineComment n, Object arg) {
                    Pair<Integer, Integer> range = (Pair<Integer, Integer>) arg;
                    if (n.getBegin().get().line >= range.getKey() &&
                        n.getEnd().get().line <= range.getValue()) {

                        result.add(n.toString().trim());

                        System.out.println(sourceFile);
                        for (int i = range.getKey() ; i < range.getValue() ; i++) {
                            System.out.println(codeLines.get(i));
                        }
                        System.out.println("");
                    }
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public String toString() {
        String pattern = "";
        for (PatternInstanceLine line : lines) {
            pattern += line.toString() + "\n";
        }
        return pattern;
    }

    @Override
    public Object clone() {
        PatternInstance clone = null;
        try {
            clone = (PatternInstance) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        clone.lines = new ArrayList<>();
        for (PatternInstanceLine line : lines) {
            clone.lines.add((PatternInstanceLine) line.clone());
        }

        return clone;
    }
}
