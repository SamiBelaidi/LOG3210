package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.*;

public class PrintMachineCodeVisitor implements ParserVisitor {

    private PrintWriter m_writer = null;

    private Integer REG = 256; // default register limitation
    private ArrayList<String> RETURNED = new ArrayList<String>(); // returned variables from the return statement
    private ArrayList<MachLine> CODE   = new ArrayList<MachLine>(); // representation of the Machine Code in Machine lines (MachLine)

    private ArrayList<String> MODIFIED = new ArrayList<String>(); // could be use to keep which variable/pointer are modified while going through the intermediate code
    private ArrayList<String> REGISTERS = new ArrayList<String>();; // map to get the registers

    private HashMap<String,String> OPMap; // map to get the operation name from it's value
    public PrintMachineCodeVisitor(PrintWriter writer) {
        m_writer = writer;

        OPMap = new HashMap<>();
        OPMap.put("+", "ADD");
        OPMap.put("-", "MIN");
        OPMap.put("*", "MUL");
        OPMap.put("/", "DIV");
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        // Visiter les enfants
        node.childrenAccept(this, null);

        compute_LifeVar();   // Life variables computation from the backward visit of CODE
        compute_NextUse();   // Next-Use computation from the backward visit of CODE
        print_machineCode(); // generate the machine code from the forward visit of CODE

        return null;
    }

    @Override
    public Object visit(ASTNumberRegister node, Object data) {
        REG = ((ASTIntValue) node.jjtGetChild(0)).getValue(); // get the limitation of register
        return null;
    }

    @Override
    public Object visit(ASTReturnStmt node, Object data) {
        for(int i = 0; i < node.jjtGetNumChildren(); i++) {
            RETURNED.add(((ASTIdentifier) node.jjtGetChild(i)).getValue()); // returned values
        }

        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants
        String assign = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left   = (String) node.jjtGetChild(1).jjtAccept(this, null);
        String right  = (String) node.jjtGetChild(2).jjtAccept(this, null);
        String op     = node.getOp();

        // TODOx: Modify CODE to add the needed MachLine.
        //       here the type of Assignment is "assigned = left op right"
        MachLine machline = new MachLine(op, assign, left, right);
        CODE.add(machline);
        return null;
    }

    @Override
    public Object visit(ASTAssignUnaryStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants
        String assign = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String right  = (String) node.jjtGetChild(1).jjtAccept(this, null);

        // TODOx: Modify CODE to add the needed MachLine.
        //       here the type of Assignment is "assigned = - right"
        //       suppose the left part to be the constant #O
        String op = "-";
        String left = "#0";
        MachLine machline = new MachLine(op, assign, left, right);
        CODE.add(machline);

        return null;
    }

    @Override
    public Object visit(ASTAssignDirectStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants
        String assign = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String right  = (String) node.jjtGetChild(1).jjtAccept(this, null);

        // TODOx: Modify CODE to add the needed MachLine.
        //       here the type of Assignment is "assigned = right"
        //       suppose the left part to be the constant #O
        String op = "+";
        String left = "#0";
        MachLine machline = new MachLine(op, assign, left, right);
        CODE.add(machline);

        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, null);
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return "#"+String.valueOf(node.getValue());
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        return (Object) node.getValue();
    }


    private class NextUse {
        // NextUse class implementation: you can use it or redo it you're way
        public HashMap<String, ArrayList<Integer>> nextuse = new HashMap<String, ArrayList<Integer>>();

        // Constructor without parameter
        public NextUse() {}

        // Constructor with parameter
        public NextUse(HashMap<String, ArrayList<Integer>> nextuse) {
            this.nextuse = nextuse;
        }

        // Get function: gets the arraylist of nextuses line numbers for the string s
        public ArrayList<Integer> get(String s) {
            return nextuse.get(s);
        }

        // Add function: add the value i at the next use array for the variable s
        public void add(String s, int i) {
            if (!nextuse.containsKey(s)) {
                nextuse.put(s, new ArrayList<Integer>());
            }
            nextuse.get(s).add(i);
        }

        // To string function
        public String toString() {
            String buff = "";
            boolean first = true;
            for (String k : set_ordered(nextuse.keySet())) {
                if (! first) {
                    buff +=", ";
                }
                Collections.sort(nextuse.get(k));
                buff += k + ":" + nextuse.get(k);
                first = false;
            }
            return buff;
        }

        // Clone function
        public Object clone() {
            return new NextUse((HashMap<String, ArrayList<Integer>>) nextuse.clone());
        }
    }


    private class MachLine {
        String OP;
        String ASSIGN;
        String LEFT;
        String RIGHT;

        public HashSet<String> REF = new HashSet<String>();
        public HashSet<String> DEF = new HashSet<String>();

        public HashSet<String> Life_IN  = new HashSet<String>();
        public HashSet<String> Life_OUT = new HashSet<String>();

        public NextUse Next_IN  = new NextUse();
        public NextUse Next_OUT = new NextUse();

        // Constructor
        public MachLine(String op, String assign, String left, String right) {
            this.OP = OPMap.get(op);
            this.ASSIGN = assign;

            this.LEFT = left;
            this.RIGHT = right;

            DEF.add(this.ASSIGN);
            if (this.LEFT.charAt(0) != ('#'))
                REF.add(this.LEFT);
            if (this.RIGHT.charAt(0) != ('#'))
                REF.add(this.RIGHT);
        }
        public String toString() {
            String buff = "";
            buff += "// Life_IN  : " +  Life_IN.toString() +"\n";
            buff += "// Life_OUT : " +  Life_OUT.toString() +"\n";
            buff += "// Next_IN  : " +  Next_IN.toString() +"\n";
            buff += "// Next_OUT : " +  Next_OUT.toString() +"\n";
            return buff;
        }
    }

    private void compute_LifeVar() {
        // TODO: Implement LifeVariable algorithm on the CODE array (for basic bloc)
        int lastNode = CODE.size() - 1;
        int firstNode = 0;
        CODE.get(lastNode).Life_OUT.addAll(RETURNED);
        for(int i = lastNode; i >= firstNode; i--){
            final int line = i;
            MachLine it = CODE.get(i);
            if(line < lastNode) {
               it.Life_OUT = CODE.get(i + 1).Life_IN;
            }
            it.Life_IN.addAll(it.Life_OUT);
            it.Life_IN.removeAll(it.DEF);
            it.Life_IN.addAll(it.REF);
        };
    }

    private void compute_NextUse() {
        // TODO: Implement NextUse algorithm on the CODE array (for basic bloc)
        int lastNode = CODE.size() - 1;
        int firstNode = 0;
        for(int i = lastNode; i >= firstNode; i--){
            final int line = i;
            MachLine it = CODE.get(i);
            if(line < lastNode) {
                it.Next_OUT = CODE.get(i + 1).Next_IN;
            }
            for(Map.Entry<String, ArrayList<Integer>> pair : it.Next_OUT.nextuse.entrySet()){
                if(!it.DEF.contains(pair.getKey())){
                    pair.getValue().forEach((v) -> it.Next_IN.add(pair.getKey(), v));
                }
            }
            it.REF.forEach((ref) -> it.Next_IN.add(ref, line));
        };
    }


    // choose_register function: will be used in the print_machineCode function
    // returns the register assigned to var.
    // The assignation is done with respect to the life and next information.
    // The boolean load_if_not_found indicates if the variable needs to be loaded
    // from the memory to be accessible in REGISTERS
    public String choose_register(String var, HashSet<String> life, NextUse next, boolean load_if_not_found) {
        // /!\ TODO this function should generate the LD and ST when needed

        // TODO: if var is a constant (starts with '#'), return var
        if(var.startsWith("#")){
            return var;
        }
        // TODO: if REGISTERS contains var, return "R"+index
        if(REGISTERS.contains(var)){
            final String ans = "R" + REGISTERS.indexOf(var);
            return ans;
        }

        // TODO: if REGISTERS size is not max (<REG), add var to REGISTERS and return "R"+index
        if(REGISTERS.size() < REG){
            REGISTERS.add(var);
            if(load_if_not_found) {
                m_writer.print("LD ");
            }
            final String ans = "R" + REGISTERS.indexOf(var);
            return ans;
        }
        // TODO: if REGISTERS has max size,
        //          1) put var in space of an other variable which is not used anymore
        //          or
        //          2) put var in space of var which as the largest next-use
        if(REGISTERS.size() == REG){
            int largest = -1;
            String chosenOne = "";

            for(String it : REGISTERS) {
                // TODO: 1) UNUSED
                if(!life.contains(it)){
                    final String ans = "R" + REGISTERS.indexOf(it);
                    REGISTERS.set(REGISTERS.indexOf(it), var);
                    if(load_if_not_found) {
                        m_writer.print("LD ");
                    }
                    return ans;
                }

                // TODO: 2) OLDER
                if(next.nextuse.containsKey(it)){
                    final int lastNode = next.nextuse.get(it).size() - 1;
                    if(next.nextuse.get(it).get(lastNode) >= largest){
                        largest = next.nextuse.get(it).get(lastNode);
                        chosenOne = it;
                    }
                } else {
                    final String ans = "R" + REGISTERS.indexOf(it);
                    setRegister(REGISTERS.indexOf(it), var);
                    if(load_if_not_found) {
                        m_writer.print("LD ");
                    }
                    return ans;
                }
            }
            if (largest == -1){
                m_writer.print("PROBLEME -1");
                return "";
            }
            final String ans = "R" + REGISTERS.indexOf(chosenOne);
            setRegister(REGISTERS.indexOf(chosenOne), var);
            if(load_if_not_found) {
                m_writer.print("LD ");
            }
            return ans;
        }
        return null;
    }

    public void print_machineCode() {
        final int BREAK = 4;
        // TODO: Print the machine code (this function needs to be change)
        for (int i = 0; i < CODE.size(); i++) { // print the output
            if(i == BREAK){
                m_writer.print("");
            }
            MachLine it = CODE.get(i);
            boolean loadLeft = !REGISTERS.contains(it.LEFT);
            boolean loadRight = !REGISTERS.contains(it.RIGHT);
            m_writer.println("// Step " + i);
            String left  = choose_register(
                    it.LEFT,
                    it.Life_IN,
                    it.Next_IN,
                    loadLeft
            );
            if(loadLeft){
                if(!left.startsWith("#")){
                    final String ans = left + ", "+ it.LEFT;
                    m_writer.println(ans);
                }
            }

            String right  = choose_register(
                    it.RIGHT,
                    it.Life_IN,
                    it.Next_IN,
                    loadRight
            );
            if(loadRight){
                if(!right.startsWith("#")){
                    final String ans = right + ", "+ it.RIGHT;
                    m_writer.println(ans);
                }
            }
                String assign = choose_register(it.ASSIGN,it.Life_OUT,it.Next_OUT,!true);
            if(!left.equals("#0") || REGISTERS.size() != REG){
                m_writer.print(it.OP + " "+ assign);
                m_writer.println(", "+ left + ", " + right);
            }
            m_writer.println(it);

            MODIFIED.add(it.ASSIGN);
        }

        for(String it : REGISTERS){
            if(MODIFIED.contains(it) && RETURNED.contains(it)){
                final String ans = "ST "+ it + ", R" + REGISTERS.indexOf(it);
                m_writer.println(ans);
            }
        }
    }


    public List<String> set_ordered(Set<String> s) {
        // function given to order a set in alphabetic order
        List<String> list = new ArrayList<String>(s);
        Collections.sort(list);
        return list;
    }

    // TODO: add any class you judge necessary, and explain them in the report. GOOD LUCK!

    public void setRegister(int i, String var) {
        if(MODIFIED.contains(REGISTERS.get(i))){
            final String ans = "ST "+ REGISTERS.get(i) + ", R" + i;
            m_writer.println(ans);
        }
        REGISTERS.set(i, var);
    }
}
