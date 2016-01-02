package jCompiler;

import java.util.ArrayList;
import java.util.List;

/* 		OBJECT-ORIENTED RECOGNIZER FOR SIMPLE EXPRESSIONS
expr    -> term   (+ | -) expr | term
term    -> factor (* | /) term | factor
factor  -> int_lit | '(' expr ')'     
*/


public class Parser {
	public static void main(String[] args) {
		System.out.println("Enter an expression, end with 'end'\n");
		Lexer.lex();
		new Program();
		Code.output();
	}
}


class Program {  //Program -> decls stmts end
	 Decls d;
	 Stmts s;
	 char op;
	 
	 public Program(){
		 if(Lexer.nextToken == Token.KEY_INT || Lexer.nextToken == Token.KEY_BOOL){
			 d = new Decls();
			 Lexer.lex();
		 }
		 if(Lexer.nextToken == Token.ID || Lexer.nextToken == Token.KEY_IF || Lexer.nextToken == Token.KEY_FOR ) {
			 s= new Stmts();
		 }
		 if (Lexer.nextToken == Token.KEY_END){
			 Code.gen(Code.endcode());
		 }
	 }	 
}

class Decls { // decls -> int idlist ';'
	IdList il;
	
	public Decls() {
		Lexer.lex();
		il = new IdList();
	}
}

class IdList { // idList -> id [',' idList] 
	public static ArrayList<Character> variable = new ArrayList<Character>();
	IdList idl;
	
	public IdList(){
		if(Lexer.nextToken == Token.ID){
			variable.add(Lexer.ident);
			Lexer.lex();
		}
		if(Lexer.nextToken == Token.COMMA){
			Lexer.lex();
			idl = new IdList();
		}
	}
}

class Stmts  { //stmts -> stmt [ stmts ]
	Stmt s;
	Stmts n;
	
	public Stmts() {
		s = new Stmt();
		if(Lexer.nextToken == Token.ID || Lexer.nextToken == Token.KEY_IF || Lexer.nextToken == Token.KEY_FOR || Lexer.nextToken == Token.LEFT_BRACE || Lexer.nextToken == Token.RIGHT_BRACE ) {
			if(Lexer.nextToken == Token.RIGHT_BRACE){
				Lexer.lex();
				return;
			}
				
			n = new Stmts();
		}
	}
}

class Stmt { //stmt -> assign ';'| cmpd | cond | loop
	Assign a;
	Cmpd c;
	Cond d;
	Loop l;
	char op;
	
	public Stmt() {
			switch(Lexer.nextToken) {
			case Token.ID: // assign
				a = new Assign();
				Lexer.lex();
				break;
		    case Token.LEFT_BRACE: // '{'
		    	c = new Cmpd();// skip over '}'
		    	break;
		    case Token.KEY_IF:
		    	Lexer.lex();
		    	d = new Cond();
		    	break;
		    case Token.KEY_FOR:
		    	Lexer.lex();
		    	l = new Loop();
		    default:
		    	return;
		}
		
	}
}

class Cmpd  { // cmpd->{ stmts }
	Stmts s;
	
	public Cmpd() {
			Lexer.lex();
			s = new Stmts();
		}
	}

class Cond  { //cond -> if '(' rexp ')' stmt [ else stmt ]
	Rexpr r;
	Stmt s1;
	Stmt s2;

	public Cond() {
		Code.writetoifforstack("if");
		if( Lexer.nextToken == Token.LEFT_PAREN) {
			Lexer.lex();
			r = new Rexpr();
			Lexer.lex(); // skip over ')'
			s1 = new Stmt();
			if(Lexer.nextToken == Token.KEY_ELSE){
				Code.gen(Code.displayGoto());
				Lexer.lex();
				s2 = new Stmt();	
				Code.updategotoelse();
			}
			else
				Code.updateIf();
		}
	}
}

class Loop{ //loop -> for '(' [assign] ';' [rexp] ';' [assign] ')' stmt
	Assign a1;
	Assign a2;
	Rexpr r;
	Stmt s;
	boolean assignPresent = false;
	
	public Loop()  {
		Code.ifForStack.add("for");
		if( Lexer.nextToken == Token.LEFT_PAREN) {
			Lexer.lex();
			if (Lexer.nextToken == Token.ID)
				a1 = new Assign();
			if (Lexer.nextToken == Token.SEMICOLON){
				Lexer.lex();
				Code.addForBeginPosition();
				r = new Rexpr();
			}
			if (Lexer.nextToken == Token.SEMICOLON)
				Lexer.lex();
			if (Lexer.nextToken == Token.ID) {
				assignPresent = true;
				Code.arraycode();
				a2 = new Assign();
				Code.arraycode();
				Code.removeAssignCode();
				Lexer.lex(); //skip over ')'
			}
			else {
				Lexer.lex();
			}
			s = new Stmt();
			// put in add assign Code.gen(Code.displayGotoFor());
		}
		if(assignPresent)
			Code.addAssignCode();
		Code.gen(Code.displayGotoFor());
	}
}


class Rexpr{ // rexpr-> expr ('<' | '>' | '==' | '!= ') expr
	Expr e1;
	Expr e2;
	int op;
	
	public Rexpr() {
		e1 = new Expr();
		if (Lexer.nextToken == Token.LESSER_OP || Lexer.nextToken == Token.GREATER_OP || Lexer.nextToken == Token.EQ_OP || Lexer.nextToken == Token.NOT_EQ)  {
			op = Lexer.nextToken;
			Lexer.lex();
			e2 = new Expr();
			Code.gen(Code.opcode(op));
		}
	}
}


class Assign    { //assign -> id = expr 
	Expr e;
	char i;
	char op;
	
	public Assign()  {
		if (Lexer.nextToken == Token.ID) {
			i = Lexer.ident;
			Lexer.lex();
		}
		if (Lexer.nextToken == Token.ASSIGN_OP)  {
			op = Lexer.nextChar;
			Lexer.lex();
			e = new Expr();
			Code.gen(Code.assigncode(i));
		}
	}	
}


class Expr   { // expr -> term (+ | -) expr | term
	Term t;
	Expr e;
	char op;

	public Expr() {
		t = new Term();
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			e = new Expr();
			Code.gen(Code.opcode(op));
		}
	}
}

class Term    { // term -> factor (* | /) term | factor
	Factor f;
	Term t;
	char op;

	public Term() {
		f = new Factor();
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			t = new Term();
			Code.gen(Code.opcode(op));
			}
	}
}

class Factor { // factor -> number | '(' expr ')'
	Expr e;
	int i;

	public Factor() {
		switch (Lexer.nextToken) {
		case Token.ID:
			char ch = Lexer.ident;
			i = IdList.variable.indexOf(ch);
			Code.gen(Code.loadcode(i));
			Lexer.lex();
			break;
		case Token.INT_LIT: // number
			i = Lexer.intValue;
			Code.gen(Code.intcode(i));
			Lexer.lex();
			break;
		case Token.LEFT_PAREN: // '('
			Lexer.lex();
			e = new Expr();
			Lexer.lex(); // skip over ')'
			break;
		default:
			break;
		}
	}
}


class Code {
	static String[] code = new String[1000]; //change
	static int codeptr = 0;
	public static int[] lineNumbers = new int[1000];
	public static int[] assarr = new int[1000];
	public static int lineptr = 0; 
	public static int assarrptr = 0; 
	public static int lineValue = 0;
	public static List<Integer> ifPositions = new ArrayList<Integer>();
	public static List<Integer> forPositions = new ArrayList<Integer>();
	public static List<Integer> forBeginPositions = new ArrayList<Integer>();
	public static List<Integer> gotoPositions = new ArrayList<Integer>();
	public static List<String> ifForStack = new ArrayList<String>();
	static List<List<String>> assignCodes = new ArrayList<List<String>>(); 
	
	public static void arraycode() {
		assarr[assarrptr] = codeptr;
		assarrptr++;
	}
	
	public static void updateIf() {
		if(ifPositions.size() != 0) {
			if(ifForStack.get(ifForStack.size()-1).equalsIgnoreCase("if")){
				code[ifPositions.get(ifPositions.size()-1)] += " " + (lineValue);
				ifPositions.remove(ifPositions.size()-1);
				ifForStack.remove(ifForStack.size()-1);
			}
		}
		
	}

	public static void addAssignCode() {
		int j = assignCodes.size()-1;
		List<String> assignCode = new ArrayList<String>();
		assignCode = assignCodes.get(j);
		for(int i=0;i <= assignCode.size()-1;i++) {
			lineNumbers[lineptr] = lineValue;
			if(assignCode.get(i).contains("store") || assignCode.get(i).contains("load") ) {
				if(assignCode.get(i).contains("1") || assignCode.get(i).contains("3") || assignCode.get(i).contains("2") )
					lineValue++;
				else
					lineValue += 2;
			}
			else if( assignCode.get(i).contains("bipush") ) {
				lineValue += 2;
			}
			else if( assignCode.get(i).contains("sipush") ) {
				lineValue += 3;
			}
			else
				lineValue++; 
			lineptr++;
			code[codeptr] = assignCode.get(i);
			codeptr++;
		}
		assignCodes.remove(assignCodes.size()-1);
	}

	public static void removeAssignCode() {
		List<String> assignCode = new ArrayList<String>();
		for(int i = assarr[0];i<assarr[1];i++) {
			assignCode.add(code[i].toString());
		}
		assignCodes.add(assignCode);
		lineptr = assarr[0];
		lineValue -= 6; //Change
		codeptr = assarr[0];
		assarrptr = 0;
	}
	
	public static void gen(String s) {
		code[codeptr] = s;
		codeptr++;
	}

	public static String displayGotoFor() {
		lineNumbers[lineptr] = lineValue;
		lineValue += 3; 
		lineptr++;
		String gotoString = "goto " + forBeginPositions.get(forBeginPositions.size()-1);
		if(forPositions.size() != 0) {
			if(ifForStack.get(ifForStack.size()-1).equalsIgnoreCase("for")){
				code[forPositions.get(forPositions.size()-1)] += " " + (lineValue);
				forPositions.remove(forPositions.size()-1);
				ifForStack.remove(ifForStack.size()-1);
			}
		}
		forBeginPositions.remove(forBeginPositions.size()-1);
		return gotoString;
	}

	public static void addForBeginPosition() {
		forBeginPositions.add(lineValue);
	}

	public static void writetoifforstack(String iforforstring) {
		ifForStack.add(iforforstring);
	}

	public static void updategotoelse() {
		code[gotoPositions.get(gotoPositions.size()-1)] += (lineValue);
		gotoPositions.remove(gotoPositions.size()-1);
	}

	public static String displayGoto() {
		lineNumbers[lineptr] = lineValue;
		lineValue += 3; 
		lineptr++;
		if(ifPositions.size() != 0) {
			if(ifForStack.get(ifForStack.size()-1).equalsIgnoreCase("if")){
				code[ifPositions.get(ifPositions.size()-1)] += " " + (lineValue);
				ifPositions.remove(ifPositions.size()-1);
				ifForStack.remove(ifForStack.size()-1);
			}
		}
		gotoPositions.add(codeptr);
		return "goto ";
	}
	
	public static String loadcode(int i) {
		if(i<3) {
			lineNumbers[lineptr] = lineValue;
			lineValue++; 
			lineptr++;
			return "iload_" + (i+1);
		}
		else {
			lineNumbers[lineptr] = lineValue;
			lineValue += 2; 
			lineptr++;
			return "iload " + (i+1);
		}
	}
	
	public static String intcode(int i) {
		if (i > 127) {
			lineNumbers[lineptr] = lineValue;
			lineValue += 3; 
			lineptr++;
			return "sipush " + i;
		}
		if (i > 5) {
			lineNumbers[lineptr] = lineValue;
			lineValue += 2; 
			lineptr++;
			return "bipush " + i;
		}
		lineNumbers[lineptr] = lineValue;
		lineValue++; 
		lineptr++;
		return "iconst_" + i;
	}
	
	public static String assigncode(char i){
		if(IdList.variable.indexOf(i) < 3) {
			lineNumbers[lineptr] = lineValue;
			lineValue++; 
			lineptr++;
			return "istore_" + ((IdList.variable.indexOf(i))+1);
		}
		else {
			lineNumbers[lineptr] = lineValue;
			lineValue += 2; 
			lineptr++;
			return "istore " + ((IdList.variable.indexOf(i))+1);
		}
	}
	
	public static String opcode(int op){
		switch(op){
		case Token.EQ_OP : lineNumbers[lineptr] = lineValue;
						   lineValue += 3; 
						   lineptr++;
						   if(ifForStack.get(ifForStack.size()-1).equalsIgnoreCase("if")){
							   ifPositions.add(codeptr);
						   }
						   else {
								forPositions.add(codeptr);   
							   }
						   return "if_icmpne";
		case Token.NOT_EQ : lineNumbers[lineptr] = lineValue;
							lineValue += 3; 
							lineptr++;
							if(ifForStack.get(ifForStack.size()-1).equalsIgnoreCase("if")){
								   ifPositions.add(codeptr);
							   }
							else {
								forPositions.add(codeptr);   
							   }
							return "if_icmpeq";
		case Token.LESSER_OP : lineNumbers[lineptr] = lineValue;
							   lineValue += 3; 
							   lineptr++;
							   if(ifForStack.get(ifForStack.size()-1).equalsIgnoreCase("if")){
								   ifPositions.add(codeptr);
							   	}
							   else {
								forPositions.add(codeptr);   
							   }
							   return "if_icmpge";
		case Token.GREATER_OP : lineNumbers[lineptr] = lineValue;
								lineValue += 3; 
								lineptr++;
								if(ifForStack.get(ifForStack.size()-1).equalsIgnoreCase("if")){
									   ifPositions.add(codeptr);
								   }
								else {
									forPositions.add(codeptr);   
								   }
								return "if_icmple";
		default : return "";
		}
	}
	
	public static String endcode() {
		lineNumbers[lineptr] = lineValue;
		lineValue++; 
		lineptr++;
		return "return";
	}
	
	public static String opcode(char op) {
		switch(op) {
		case '+' : lineNumbers[lineptr] = lineValue;
				   lineValue++; 
				   lineptr++;
				   return "iadd";
		case '-' : lineNumbers[lineptr] = lineValue;
				   lineValue++; 
				   lineptr++;
				   return "isub";
		case '*' : lineNumbers[lineptr] = lineValue;
				   lineValue++; 
				   lineptr++;
				   return "imul";
		case '/' : lineNumbers[lineptr] = lineValue;
				   lineValue++; 
				   lineptr++;
				   return "idiv";
		default: return "";
		}
	}
	
	public static void output() {
		for (int i=0; i<codeptr; i++)
			System.out.println(lineNumbers[i] + ": " + code[i]);
	}
}