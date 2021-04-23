package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;


/**
 * Created: 19-02-15
 * Last Changed: 20-10-6
 * Author: Félix Brunet & Doriane Olewicki
 * <p>
 * Description: Ce visiteur explore l'AST et génère un code intermédiaire.
 */

public class IntermediateCodeGenFallVisitor implements ParserVisitor {

    //le m_writer est un Output_Stream connecter au fichier "result". c'est donc ce qui permet de print dans les fichiers
    //le code généré.
    private final PrintWriter m_writer;

    public IntermediateCodeGenFallVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    public HashMap<String, VarType> SymbolTable = new HashMap<>();
    private int id = 0;
    private int label = 0;

    /*
    génère une nouvelle variable temporaire qu'il est possible de print
    À noté qu'il serait possible de rentrer en conflit avec un nom de variable définit dans le programme.
    Par simplicité, dans ce tp, nous ne concidérerons pas cette possibilité, mais il faudrait un générateur de nom de
    variable beaucoup plus robuste dans un vrai compilateur.
     */
    private String genId() {
        return "_t" + id++;
    }

    //génère un nouveau Label qu'il est possible de print.
    private String genLabel() {
        return "_L" + label++;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        String next = genLabel();
        node.childrenAccept(this, next);
        m_writer.println(next);
        return null;
    }

    /*
    Code fournis pour remplir la table de symbole.
    Les déclarations ne sont plus utile dans le code à trois adresse.
    elle ne sont donc pas concervé.
     */
    @Override
    public Object visit(ASTDeclaration node, Object data) {
        ASTIdentifier id = (ASTIdentifier) node.jjtGetChild(0);
        VarType t;
        if (node.getValue().equals("bool")) {
            t = VarType.Bool;
        } else {
            t = VarType.Number;
        }
        SymbolTable.put(id.getValue(), t);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        for (int i = 0; i < node.jjtGetNumChildren() - 1; i++) {
            String next = genLabel();
            node.jjtGetChild(i).jjtAccept(this, next);
            m_writer.println(next);
        }
        node.jjtGetChild(node.jjtGetNumChildren() - 1).jjtAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    /*
    le If Stmt doit vérifier s'il à trois enfants pour savoir s'il s'agit d'un "if-then" ou d'un "if-then-else".
     */
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        // if seulement sans else
        if (node.jjtGetNumChildren() == 2) {
            BoolLabel b = new BoolLabel("fall", data.toString());
            node.jjtGetChild(0).jjtAccept(this, b);
            node.jjtGetChild(1).jjtAccept(this, data);
        } else {
            BoolLabel b = new BoolLabel("fall", genLabel());
            node.jjtGetChild(0).jjtAccept(this, b);
            node.jjtGetChild(1).jjtAccept(this, data);
            m_writer.println("goto " + data);
            m_writer.println(b.lFalse);
            node.jjtGetChild(2).jjtAccept(this, data);
        }

        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        String lBegin = genLabel();
        BoolLabel boolData = new BoolLabel("fall", data.toString());
        m_writer.println(lBegin);
        node.jjtGetChild(0).jjtAccept(this, boolData);
        node.jjtGetChild(1).jjtAccept(this, lBegin);
        m_writer.println("goto " + lBegin);
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        String identifier = ((ASTIdentifier) node.jjtGetChild(0)).getValue();

        if (SymbolTable.get(identifier) == VarType.Number) {
            String value = node.jjtGetChild(1).jjtAccept(this, data).toString();
            m_writer.println(identifier + " = " + value);

        } else {
            String lTrue = "fall";
            String lFalse = genLabel();
            BoolLabel boolData = new BoolLabel(lTrue, lFalse);
            node.jjtGetChild(1).jjtAccept(this, boolData);
            m_writer.println(identifier + " = 1");
            m_writer.println("goto " + data.toString());
            m_writer.println(lFalse);
            m_writer.println(identifier + " = 0");
        }
        return null;
    }


    @Override
    public Object visit(ASTExpr node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    //Expression arithmétique
    /*
    Les expressions arithmétique add et mult fonctionne exactement de la même manière. c'est pourquoi
    il est plus simple de remplir cette fonction une fois pour avoir le résultat pour les deux noeuds.

    On peut bouclé sur "ops" ou sur node.jjtGetNumChildren(),
    la taille de ops sera toujours 1 de moins que la taille de jjtGetNumChildren
     */
    public Object codeExtAddMul(SimpleNode node, Object data, Vector<String> ops) {
        if (ops.size() == 0) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            String id = genId();
            String id1 = (String) node.jjtGetChild(0).jjtAccept(this, data);
            String id2 = (String) node.jjtGetChild(1).jjtAccept(this, data);
            m_writer.println(id + " = " + id1 + " " + ops.get(0) + " " + id2);
            return id;
        }

    }

    @Override
    public Object visit(ASTAddExpr node, Object data) {
        return codeExtAddMul(node, data, node.getOps());
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        return codeExtAddMul(node, data, node.getOps());
    }

    //UnaExpr est presque pareil au deux précédente. la plus grosse différence est qu'il ne va pas
    //chercher un deuxième noeud enfant pour avoir une valeur puisqu'il s'agit d'une opération unaire.
    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        if (node.getOps().size() == 0) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            String id = node.jjtGetChild(0).jjtAccept(this, data).toString();
            for (int i = 0; i < node.getOps().size(); i++) {
                String id2 = genId();
                m_writer.println(id2 + " = - " + id);
                id = id2;
            }
            return id;
        }
    }

    //expression logique
    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        if (node.jjtGetNumChildren() == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            if (node.getOps().get(0).equals("&&")) {
                BoolLabel b1 = new BoolLabel("fall", ((BoolLabel) data).lFalse.equals("fall") ? genLabel() : ((BoolLabel) data).lFalse);
                BoolLabel b2 = new BoolLabel(((BoolLabel) data).lTrue, ((BoolLabel) data).lFalse);
                node.jjtGetChild(0).jjtAccept(this, b1);
                node.jjtGetChild(1).jjtAccept(this, b2);
                if (((BoolLabel) data).lFalse.equals("fall"))
                    m_writer.println(b1.lFalse);
            } else {
                BoolLabel b1 = new BoolLabel(((BoolLabel) data).lTrue.equals("fall") ? genLabel() : "fall", ((BoolLabel) data).lTrue);
                BoolLabel b2 = new BoolLabel(((BoolLabel) data).lTrue, ((BoolLabel) data).lFalse);
                node.jjtGetChild(0).jjtAccept(this, b1);
                node.jjtGetChild(1).jjtAccept(this, b2);
                if (((BoolLabel) data).lTrue.equals("fall"))
                    m_writer.println(b1.lTrue);
            }

            return null;
        }
    }


    @Override
    public Object visit(ASTCompExpr node, Object data) {
        if (node.jjtGetNumChildren() < 2) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        BoolLabel tmpData = (BoolLabel) data;

        if (!tmpData.lTrue.equals("fall") && !tmpData.lFalse.equals("fall")) {
            String id1 = node.jjtGetChild(0).jjtAccept(this, data).toString();
            String id2 = node.jjtGetChild(1).jjtAccept(this, data).toString();
            m_writer.println("if " + id1 + " " + node.getValue() + " " + id2 + " goto " + tmpData.lTrue);
            m_writer.println("goto " + ((BoolLabel) data).lFalse);
        } else if (!tmpData.lTrue.equals("fall")) {
            String id1 = node.jjtGetChild(0).jjtAccept(this, data).toString();
            String id2 = node.jjtGetChild(1).jjtAccept(this, data).toString();
            m_writer.println("if " + id1 + " " + node.getValue() + " " + id2 + " goto " + tmpData.lTrue);
        } else if (!tmpData.lFalse.equals("fall")) {
            String id1 = node.jjtGetChild(0).jjtAccept(this, data).toString();
            String id2 = node.jjtGetChild(1).jjtAccept(this, data).toString();
            m_writer.println("ifFalse " + id1 + " " + node.getValue() + " " + id2 + " goto " + tmpData.lFalse);
        } else {
            m_writer.println("error");
        }

        return null;
    }


    /*
    Même si on peut y avoir un grand nombre d'opération, celle-ci s'annullent entre elle.
    il est donc intéressant de vérifier si le nombre d'opération est pair ou impaire.
    Si le nombre d'opération est pair, on peut simplement ignorer ce noeud.
     */
    @Override
    public Object visit(ASTNotExpr node, Object data) {
        if (node.getOps().size() == 0) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        BoolLabel b1 = new BoolLabel(((BoolLabel) data).lFalse, ((BoolLabel) data).lTrue);
        for (int i = 1; i < node.getOps().size(); i++) {
            b1 = new BoolLabel(b1.lFalse, b1.lTrue);
        }
        return node.jjtGetChild(0).jjtAccept(this, b1);
    }

    @Override
    public Object visit(ASTGenValue node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
//        return null;
    }

    /*
    BoolValue ne peut pas simplement retourné sa valeur à son parent contrairement à GenValue et IntValue,
    Il doit plutôt généré des Goto direct, selon sa valeur.
     */
    @Override
    public Object visit(ASTBoolValue node, Object data) {
        BoolLabel tmpData = (BoolLabel) data;
        if (node.getValue()) {
            if (!tmpData.lTrue.equals("fall"))
                m_writer.println("goto " + tmpData.lTrue);
        } else {
            if (!tmpData.lFalse.equals("fall"))
                m_writer.println("goto " + tmpData.lFalse);
        }

        return null;
    }


    /*
    si le type de la variable est booléenne, il faudra généré des goto ici.
    le truc est de faire un "if value == 1 goto Label".
    en effet, la structure "if valeurBool goto Label" n'existe pas dans la syntaxe du code à trois adresse.
     */
    @Override
    public Object visit(ASTIdentifier node, Object data) {
        if (SymbolTable.get(node.getValue()) == VarType.Bool) {
            BoolLabel tmpData = (BoolLabel) data;

            if (!tmpData.lTrue.equals("fall") && !tmpData.lFalse.equals("fall")) {
                m_writer.println("if " + node.getValue() + " == 1 goto " + tmpData.lTrue);
                m_writer.println("goto " + ((BoolLabel) data).lFalse);
            } else if (!tmpData.lTrue.equals("fall")) {
                m_writer.println("if " + node.getValue() + " == 1 goto " + tmpData.lTrue);
            } else if (!tmpData.lFalse.equals("fall")) {
                m_writer.println("ifFalse " + node.getValue() + " == 1 goto " + tmpData.lFalse);
            } else {
                m_writer.println("error");
            }

            return null;
        } else {
            return node.getValue();
        }
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return Integer.toString(node.getValue());
    }


    @Override
    public Object visit(ASTSwitchStmt node, Object data) {
        HashMap<String, String> switchMap = new LinkedHashMap<>();

        String test = genLabel();
        String next = data.toString();
        String t = node.jjtGetChild(0).jjtAccept(this, data).toString();

        m_writer.println("goto " + test);

        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            String label = genLabel();
            m_writer.println(label);

            String caseValue = node.jjtGetChild(i).jjtAccept(this, data).toString();

            switchMap.put(caseValue, label);

            m_writer.println("goto " + next);
        }
        m_writer.println(test);
        switchMap.forEach((caseValue, label) -> {
            if (caseValue.equals("default"))
                m_writer.println("goto " + label);
            else
                m_writer.println("if " + t + " == " + caseValue + " goto " + label);
        });

        return null;
    }

    @Override
    public Object visit(ASTCaseStmt node, Object data) {
        String caseValue = node.jjtGetChild(0).jjtAccept(this, data).toString();
        node.jjtGetChild(1).jjtAccept(this, data);

        return caseValue;
    }

    @Override
    public Object visit(ASTDefaultStmt node, Object data) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            node.jjtGetChild(i).jjtAccept(this, data);
        }
        return "default";
    }

    //des outils pour vous simplifier la vie et vous enligner dans le travail
    public enum VarType {
        Bool,
        Number
    }

    //utile surtout pour envoyé de l'informations au enfant des expressions logiques.
    private class BoolLabel {
        public String lTrue = null;
        public String lFalse = null;

        public BoolLabel(String t, String f) {
            lTrue = t;
            lFalse = f;
        }
    }


}
