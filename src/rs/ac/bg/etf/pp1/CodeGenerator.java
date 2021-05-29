package rs.ac.bg.etf.pp1;

import java.util.Stack;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {
	
	private int mainPc;
	private Stack<Integer> jumpOverThen = new Stack<>();
	private Stack<Integer> jumpOverElse =  new Stack<>();
	public int getMainPc() {
		return mainPc;
	}
	
	@Override
	public void visit(MethodVoidName methodTypeName) {
		methodTypeName.obj.setAdr(Code.pc);
		if ("main".equalsIgnoreCase(methodTypeName.getMethName())) {
			mainPc = Code.pc;
		}
		
		// Generate the entry.
		Code.put(Code.enter);
		Code.put(0);
		Code.put(methodTypeName.obj.getLocalSymbols().size());
	}
	
	@Override
	public void visit(MethodTypeAndName methodTypeName) {
		methodTypeName.obj.setAdr(Code.pc);
	
		// Generate the entry.
		Code.put(Code.enter);
		Code.put(0);
		Code.put(methodTypeName.obj.getLocalSymbols().size());
	}
	
	@Override
	public void visit(MethodDecl MethodDecl) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	@Override
	public void visit(ReturnExprStatement ReturnExpr) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	@Override
	public void visit(ReturnStatement ReturnNoExpr) {
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
		Code.loadConst(factorBoolConst.getValue() ? 1 : 0); 
	}
	
	@Override
	public void visit(FactorDesignator factorDesignator) {
		if(factorDesignator.getOptParenActPars() instanceof OptParenActParsNo)
			Code.load(factorDesignator.getDesignator().obj);
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
	public void visit(AddopTerms addopTerms) {
		if(addopTerms.getAddop() instanceof AddopAdd) {
			Code.put(Code.add);			
		} else {
			Code.put(Code.sub);
		}
	}
	
	@Override
	public void visit(MulopFactors mulopFactors) {
		if(mulopFactors.getMulop() instanceof MulopMul) {
			Code.put(Code.mul);			
		} else if(mulopFactors.getMulop() instanceof MulopDiv) {
			Code.put(Code.div);
		} else {
			Code.put(Code.rem);
		}
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
	public void visit(DesignatorStatementIncrement designatorStatementIncrement) {
		Obj desigObj = designatorStatementIncrement.getDesignator().obj;
		if(desigObj.getKind() == Obj.Elem) {
			Code.put(Code.dup2);
		}
		Code.load(desigObj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(desigObj);
	}
	
	@Override
	public void visit(DesignatorStatementDecrement designatorStatementDecrement) {
		Obj desigObj = designatorStatementDecrement.getDesignator().obj;
		if(desigObj.getKind() == Obj.Elem) {
			Code.put(Code.dup2);
		}
		Code.load(desigObj);
		Code.loadConst(1);
		Code.put(Code.sub);
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
		int op=Code.eq;
		if(condFactRelop.getRelop() instanceof RelopGt) {
			op = Code.gt;
		}
		//dodati else ifove za ostale
		Code.putFalseJump(op, 0);
		jumpOverThen.push(Code.pc - 2); 
	}
	
	@Override
	public void visit(CondFactNoRelop condFactNoRelop) {
		Code.loadConst(1);
		Code.putFalseJump(Code.eq, 0);
		jumpOverThen.push(Code.pc - 2); 
	}
	
	@Override
	public void visit(Colon colon) {
		Code.putJump(0);
		jumpOverElse.push(Code.pc - 2);
		Code.fixup(jumpOverThen.pop());
	}
	
	@Override
	public void visit(ExprTernar exprTernar) {
		Code.fixup(jumpOverElse.pop());
	}
}
