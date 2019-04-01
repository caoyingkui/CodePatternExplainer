package dataStructure;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserParameterDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;
import dataStructure.CreationPath.*;
import utils.Config;
import utils.LastNameFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatternInstanceLine implements Cloneable {
    public enum Mode {ASSIGN, METHODCALL, ERROR}

    public Mode mode;
    private boolean callerExist;

    private String MethodSignature;
    private String MethodName;
    private String MethodLocation;
    private List<String> MethodParameterTypes = new ArrayList<>();
    private int MethodParameterNumber;

    private List<String> MethodParameters = new ArrayList<>();
    private String MethodCaller;

    private String AssignTarget;
    private String AssignTargetType;

    public List<CreationPath> creationPaths = new ArrayList<>();

    public List<String> pathSerialize(){
        ArrayList<String> holeInstance = new ArrayList<>();
        if (mode == Mode.ASSIGN) {
            holeInstance.add(AssignTarget);
        }
        for (CreationPath creationPath:creationPaths){
            holeInstance.add(creationPath.toString());
        }
        return holeInstance;
    }


    public List<String> serialize() {
        ArrayList<String> holeInstance = new ArrayList<>();
        if (mode == Mode.ASSIGN) {
            holeInstance.add(AssignTarget);
        }
        if (callerExist) {
            holeInstance.add(MethodCaller);
        }
        holeInstance.addAll(MethodParameters);
        return holeInstance;
    }

    public int deserialize(List<String> stream, int startPoint) {
        if (mode != Mode.METHODCALL) {
            AssignTarget = stream.get(startPoint++);
        }
        if (callerExist) {
            MethodCaller = stream.get(startPoint++);
        }
        MethodParameters = new ArrayList<>();
        for (int i = 0; i < MethodParameterNumber; i++) {
            MethodParameters.add(stream.get(startPoint++));
        }
        return startPoint;
    }

    public List<String> typeSerialize() {
        ArrayList<String> types = new ArrayList<>();
        if (mode == Mode.ASSIGN) {
            types.add(AssignTargetType);
        }
        if (callerExist) {
            types.add(MethodLocation);
        }
        types.addAll(MethodParameterTypes);
        return types;
    }

    public List<String> getSymbols() {
        ArrayList<String> pairs = new ArrayList<>();
        if (mode == Mode.ASSIGN) {
            int lastDotPosition = AssignTargetType.lastIndexOf(".");
            if (lastDotPosition != -1) {
                pairs.add(AssignTargetType.substring(lastDotPosition + 1) + "  :  " + AssignTargetType);
            } else {
                pairs.add(AssignTargetType + "  :  " + AssignTargetType);
            }
        }
        pairs.add(MethodName + "  :  " + MethodSignature);
        return pairs;
    }


    private void fillSignature(String signature) {
        MethodSignature = signature;
        String[] temp = signature.split("\\(");
        int lastDotPosition = temp[0].lastIndexOf(".");
        MethodLocation = temp[0].substring(0, lastDotPosition);
        MethodName = temp[0].substring(lastDotPosition + 1);
        temp = temp[1].split("\\)|, ");
        MethodParameterNumber = temp.length;
        for (String parameter : temp) {
            MethodParameterTypes.add(parameter);
        }
    }

    private CreationPath parse(Node focusNode){
        CreationPath creationPath = new CreationPath();
        while (focusNode != null){
            if (focusNode instanceof MethodCallExpr){
                MethodCallExpr methodCallExpr = (MethodCallExpr) focusNode;
                ResolvedMethodDeclaration resolvedMethodDeclaration = methodCallExpr.resolve();

                creationPath.addNode(new CreationPathMethodNode(
                        resolvedMethodDeclaration.getQualifiedSignature(),
                        methodCallExpr.getNameAsString()
                ));

                Optional<Expression> caller = methodCallExpr.getScope();
                if (caller.isPresent()){
                    focusNode = caller.get();
                } else {
                    break;
                }
            } else if(focusNode instanceof ObjectCreationExpr || focusNode instanceof FieldAccessExpr){
                creationPath.addNode(new CreationPathOthersNode( focusNode.toString()));
                break;
            } else if (focusNode instanceof NameExpr){
                ResolvedValueDeclaration resolvedValueDeclaration = ((NameExpr) focusNode).resolve();
                if (resolvedValueDeclaration instanceof JavaParserSymbolDeclaration){
                    JavaParserSymbolDeclaration symbolDeclaration = ((JavaParserSymbolDeclaration)resolvedValueDeclaration);
                    VariableDeclarator variableDeclarator = (VariableDeclarator)symbolDeclaration.getWrappedNode();
                    Optional<Expression> initializer = variableDeclarator.getInitializer();

                    if (initializer.isPresent()){
                        focusNode = initializer.get();
                    }else {
                        creationPath.addNode(new CreationPathVariableNode(
                                variableDeclarator.getTypeAsString(),
                                variableDeclarator.getNameAsString()
                        ));
                        break;
                    }
                }else if (resolvedValueDeclaration instanceof JavaParserParameterDeclaration){
                    JavaParserParameterDeclaration parameterDeclaration = ((JavaParserParameterDeclaration)resolvedValueDeclaration);
                    Parameter parameter = parameterDeclaration.getWrappedNode();
                    creationPath.addNode(new CreationPathVariableNode(parameter.getTypeAsString(), parameter.getNameAsString()));
                    break;
                }else{
                    break;
                }
            } else {
                creationPath.addNode(new CreationPathOthersNode(focusNode.toString()));
                break;
            }
        }
        return creationPath;
    }

    private void trackCreationPaths(List<Node> fillers){
        for (Node filler : fillers){
            CreationPath path;
            try {
                path = parse(filler);
            }catch (UnsolvedSymbolException e){
                if (Config.debug()){
                    e.printStackTrace();
                }
                path = new CreationPath();
                path.addNode(new CreationPathOthersNode("unsolved"));
                //System.out.println(filler.toString());
            }
            creationPaths.add(path);
        }
    }

    public PatternInstanceLine(MethodCallExpr n) {
        ResolvedMethodDeclaration rn;
        List<Node> trackingFillers = new ArrayList<>();
        try {
            rn = n.resolve();
        } catch (Exception e) {
            if (Config.debug()) {
                System.out.println(e);
            }

            mode = Mode.ERROR;
            return;
        }

        fillSignature(rn.getQualifiedSignature());

        callerExist = n.getScope().isPresent();
        n.getScope().ifPresent(caller -> {
            MethodCaller = caller.toString();
            trackingFillers.add(caller);
        });

        for (Expression e : n.getArguments()) {
            MethodParameters.add(e.toString());
            trackingFillers.add(e);
        }

        trackCreationPaths(trackingFillers);

        n.getParentNode().ifPresent(parent -> {
            if (parent instanceof VariableDeclarator) {
                mode = Mode.ASSIGN;
                AssignTargetType = ((VariableDeclarator) parent).getTypeAsString();
                AssignTarget = ((VariableDeclarator) parent).getNameAsString();
            } else if (parent instanceof AssignExpr) {
                mode = Mode.ASSIGN;
                AssignTargetType = rn.getReturnType().describe();
                AssignTarget = ((AssignExpr) parent).getTarget().toString();
            } else if (parent instanceof ExpressionStmt) {
                mode = Mode.METHODCALL;
            } else {
                mode = Mode.ERROR;
                if (Config.debug()) {
                    System.out.println(
                            "Method call <" + MethodName + ">'s parent node type <"
                                    + parent.getMetaModel() + "> unknown: " + parent.toString()
                    );
                }
                return;
            }
        });
        /*
        if (serialize().size()!=pathSerialize().size()){
            System.out.println(serialize());
            System.out.println(pathSerialize());
        }
        */
    }

    public String getLineSignature() {
        String signature = mode.toString() + " " + callerExist + " " + MethodSignature;
        if (mode != Mode.METHODCALL) {
            signature += " " + AssignTargetType;
        }
        return signature;
    }

    public String toString() {
        String result = "";
        if (mode == Mode.ASSIGN) {
            result += LastNameFinder.getLastName(AssignTargetType) + " ";
            result += AssignTarget + " = ";
        }
        if (callerExist) {
            result += MethodCaller + ".";
        }
        result += MethodName + "(";
        for (String parameter : MethodParameters) {
            result += parameter + ", ";
        }
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }
        result += ");";

        /*
        for (CreationPath creationPath : creationPaths){
            result += "\nPath : " + creationPath.toString();
        }
        */

        return result;
    }

    @Override
    public Object clone() {
        PatternInstanceLine clone = null;
        try {
            clone = (PatternInstanceLine) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        clone.MethodParameters = new ArrayList<>();
        for (String parameter : MethodParameters) {
            clone.MethodParameters.add(parameter);
        }

        clone.MethodParameterTypes = new ArrayList<>();
        for (String parameterType : MethodParameterTypes) {
            clone.MethodParameterTypes.add(parameterType);
        }
        return clone;
    }
}
