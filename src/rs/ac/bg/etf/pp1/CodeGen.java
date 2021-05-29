package rs.ac.bg.etf.pp1;

import java.util.Stack;

import rs.ac.bg.etf.pp1.ast.AddopAdd;
import rs.ac.bg.etf.pp1.ast.AddopMinusTermSingle;
import rs.ac.bg.etf.pp1.ast.AddopTerms;
import rs.ac.bg.etf.pp1.ast.Colon;
import rs.ac.bg.etf.pp1.ast.CondFactNoRelop;
import rs.ac.bg.etf.pp1.ast.CondFactRelop;
import rs.ac.bg.etf.pp1.ast.DesignatorArray;
import rs.ac.bg.etf.pp1.ast.DesignatorStatementDecrement;
import rs.ac.bg.etf.pp1.ast.DesignatorStatementIncrement;
import rs.ac.bg.etf.pp1.ast.DesignatorStmtAssignExp;
import rs.ac.bg.etf.pp1.ast.ExprTernar;
import rs.ac.bg.etf.pp1.ast.FactorArrayExpr;
import rs.ac.bg.etf.pp1.ast.FactorBoolConst;
import rs.ac.bg.etf.pp1.ast.FactorCharAdd;
import rs.ac.bg.etf.pp1.ast.FactorCharConst;
import rs.ac.bg.etf.pp1.ast.FactorDesignator;
import rs.ac.bg.etf.pp1.ast.FactorNewType;
import rs.ac.bg.etf.pp1.ast.FactorNumConst;
import rs.ac.bg.etf.pp1.ast.MethodDecl;
import rs.ac.bg.etf.pp1.ast.MethodVoidName;
import rs.ac.bg.etf.pp1.ast.MulopDiv;
import rs.ac.bg.etf.pp1.ast.MulopFactors;
import rs.ac.bg.etf.pp1.ast.MulopMul;
import rs.ac.bg.etf.pp1.ast.OptExprYes;
import rs.ac.bg.etf.pp1.ast.OptParenActParsNo;
import rs.ac.bg.etf.pp1.ast.PrintOptNumConstYes;
import rs.ac.bg.etf.pp1.ast.PrintStatement;
import rs.ac.bg.etf.pp1.ast.ReadStatement;
import rs.ac.bg.etf.pp1.ast.Relop;
import rs.ac.bg.etf.pp1.ast.RelopDiff;
import rs.ac.bg.etf.pp1.ast.RelopGe;
import rs.ac.bg.etf.pp1.ast.RelopGt;
import rs.ac.bg.etf.pp1.ast.RelopLe;
import rs.ac.bg.etf.pp1.ast.RelopLt;
import rs.ac.bg.etf.pp1.ast.ReturnExprStatement;
import rs.ac.bg.etf.pp1.ast.ReturnStatement;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGen extends VisitorAdaptor {
	private int mainPc;
	private Stack<Integer> jumpOverThen = new Stack<>();
	private Stack<Integer> jumpOverElse =  new Stack<>();
	
	public int getMainPc() {
		return mainPc;
	}
	
	@Override
	public void visit(MethodVoidName methodVoidName) {
		methodVoidName.obj.setAdr(Code.pc);
		if("main".equalsIgnoreCase(methodVoidName.getMethName())) {
			mainPc = Code.pc;
		}
		Code.put(Code.enter);
		Code.put(0);
		Code.put(methodVoidName.obj.getLocalSymbols().size());
	}
	
	@Override
	public void visit(MethodDecl methodDecl) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	@Override
	public void visit(ReturnExprStatement returnExprStatement) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}	
	
	@Override
	public void visit(ReturnStatement returnStatement) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	@Override
	public void visit(FactorNumConst factorNumConst) {
		Code.loadConst(factorNumConst.getValue());
	}	
	
	@Override
	public void visit(FactorCharConst factorCharConst) {
		Code.loadConst(factorCharConst.getValue());
	}	
	
	@Override
	public void visit(FactorBoolConst factorBoolConst) {
		Code.loadConst(factorBoolConst.getValue()? 1 : 0);
	}

	@Override
	public void visit(FactorDesignator factorDesignator) {
		if(factorDesignator.getOptParenActPars() instanceof OptParenActParsNo) {
			Code.load(factorDesignator.getDesignator().obj);
		}
	}
	
	@Override
	public void visit(FactorNewType factorNewType) {
		if(factorNewType.getOptExpr() instanceof OptExprYes) {
			Code.put(Code.newarray);
			if(factorNewType.getType().struct.equals(Tab.charType)) {
				Code.put(0);
			} else {
				Code.put(1);
			}
		}
	}
	
	@Override
	public void visit(FactorCharAdd faCharAdd) {     // 68  -65    (3 + 26 + broj mod 26) mod 26+ 65
		Code.put(Code.enter);
		Code.put(2);
		Code.put(2);
		
		Code.put(Code.load_n);
		Code.loadConst('A');
		Code.put(Code.sub);
		Code.loadConst(26);
		Code.put(Code.add);
		Code.put(Code.load_1);
		Code.loadConst(26);
		Code.put(Code.rem);
		Code.put(Code.add);
		Code.loadConst(26);
		Code.put(Code.rem);
		Code.loadConst('A');
		Code.put(Code.add);
		
		Code.put(Code.exit);

		
//		Code.loadConst(26);
//		Code.put(Code.rem);
//		Code.put(Code.add);
//		
//		Code.put(Code.dup);
//		Code.loadConst('A');
//		Code.putFalseJump(Code.lt, 0);
//		int adr = Code.pc-2;
//		
//		Code.loadConst(26);
//		Code.put(Code.add);
//		
//		
//		Code.fixup(adr);
//		
//		Code.put(Code.dup);
//		Code.loadConst('Z');
//		Code.putFalseJump(Code.gt, 0);
//		adr = Code.pc-2;
//		
//		Code.loadConst(26);
//		Code.put(Code.sub);
//		
//		Code.fixup(adr);
	}
//	treba novi operator da se napravi u sledecem obliku
//	designator @ expr, i primenjuje se samo na nizove
//	recimo niz@2 treba da sabere dva simetricna elementa niza - drugi otpozadi i drugi spreda
	
	// brojis pristupe bilo kom elementu niza sa #niz
	
	// |a:b:c| = mid(a,b,c)
	
	@Override
	public void visit(FactorArrayExpr factorArrayExpr) {
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.load(factorArrayExpr.getDesignator().obj);
		Code.put(Code.load_n);
		Code.put(Code.aload);
		
		Code.load(factorArrayExpr.getDesignator().obj);   
		
		Code.put(Code.dup);
		Code.put(Code.arraylength);
		Code.put(Code.load_n);
		Code.put(Code.sub);
		Code.loadConst(1);
		Code.put(Code.sub);
		
		Code.put(Code.aload);
		
		Code.put(Code.add);
		
		Code.put(Code.exit);
	}
	
	@Override
	public void visit(AddopTerms addopTerms) {
		if(addopTerms.getAddop() instanceof AddopAdd) {
			Code.put(Code.add);
		} else {
			Code.put(Code.sub);
		}
	}
	
	@Override
	public void visit(MulopFactors mulopFactors) {
		if(mulopFactors.getMulop() instanceof MulopMul)
			Code.put(Code.mul);
		else if(mulopFactors.getMulop() instanceof MulopDiv)
				Code.put(Code.div);
			else
				Code.put(Code.rem);
	}
	
	@Override
	public void visit(AddopMinusTermSingle addopMinusTermSingle) {
		Code.put(Code.neg);
	}
	
	@Override
	public void visit(DesignatorStmtAssignExp designatorStmtAssignExp) {
		Code.store(designatorStmtAssignExp.getDesignator().obj);
	}
	
	@Override
	public void visit(DesignatorStatementDecrement designatorStatementDecrement) {
		Obj desigObj = designatorStatementDecrement.getDesignator().obj;
		if(desigObj.getKind() == Obj.Elem)
			Code.put(Code.dup2);
		
		Code.load(desigObj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(desigObj);
	}
	
	@Override
	public void visit(DesignatorStatementIncrement designatorStatementIncrement) {
		Obj desigObj = designatorStatementIncrement.getDesignator().obj;
		if(desigObj.getKind() == Obj.Elem)
			Code.put(Code.dup2);
		
		Code.load(desigObj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(desigObj);
	}
	
	@Override
	public void visit(PrintStatement printStatement) {
		if(printStatement.getPrintOptNumConst() instanceof PrintOptNumConstYes) {
			PrintOptNumConstYes numConst = (PrintOptNumConstYes) printStatement.getPrintOptNumConst();
			Code.loadConst(numConst.getValue());
		} else {
			Code.loadConst(1);
		}
		if(printStatement.getExpr().struct.equals(Tab.charType)) {
			Code.put(Code.bprint);
		} else {
			Code.put(Code.print);
		}
	}
	
	@Override
	public void visit(ReadStatement readStatement) {
		if(readStatement.getDesignator().obj.getType().equals(Tab.charType)) {
			Code.put(Code.bread);
		} else {
			Code.put(Code.read);
		}
		Code.store(readStatement.getDesignator().obj);
	}
	
	@Override
	public void visit(DesignatorArray designatorArray) {
		Code.load(designatorArray.getDesignator().obj);
	}
	
	@Override
	public void visit(CondFactRelop condFactRelop) {
		int op = Code.eq;
		Relop relop = condFactRelop.getRelop();
		if(relop instanceof RelopDiff)
			op = Code.ne;
		if(relop instanceof RelopGt) 
			op = Code.gt;
		if(relop instanceof RelopGe)
			op = Code.ge;
		if(relop instanceof RelopLt)
			op = Code.lt;
		if(relop instanceof RelopLe)
			op = Code.le;
		
		Code.putFalseJump(op,0);
		jumpOverThen.push(Code.pc-2);
	}
	
	@Override
	public void visit(CondFactNoRelop condFactNoRelop) {
		Code.loadConst(1);
		Code.putFalseJump(Code.eq, 0);
		jumpOverThen.push(Code.pc-2);
	}
	
	@Override
	public void visit(Colon colon) {
		Code.putJump(0);
		jumpOverElse.push(Code.pc-2);
		Code.fixup(jumpOverThen.pop());
	}
	
	@Override
	public void visit(ExprTernar exprTernar) {
		Code.fixup(jumpOverElse.pop());
	}
}
