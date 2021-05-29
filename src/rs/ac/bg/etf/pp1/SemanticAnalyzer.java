package rs.ac.bg.etf.pp1;
import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.visitors.DumpSymbolTableVisitor;
import rs.etf.pp1.symboltable.visitors.SymbolTableVisitor;

public class SemanticAnalyzer extends VisitorAdaptor {
	static Struct boolType = new Struct(Struct.Bool);
	boolean errorDetected = false;
	Obj currentMethod = null;
	boolean returnFound = false;
	int nVars;
	Struct lastType;

	Logger log = Logger.getLogger(getClass());

	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder("Semanticka greska, ");
		msg.append(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (", na liniji: ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (", na liniji ").append(line);
		log.info(msg.toString());
	}
	
	
	
	
	public void visit(Program program) {		
		nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(program.getProgName().obj);
		Obj main = Tab.currentScope.findSymbol("main");
		if(main != null) {
			if(main.getKind() != Obj.Meth) {
				report_error("main mora biti tipa method", program);
			}
			if(main.getType().getKind() != Struct.None ) {
				report_error("metoda main mora biti tipa void", program);
			}
		} else {
			report_error("mora postojati main metoda u programu", program);
		}
		
		Tab.closeScope();
		
	}

	public void visit(ProgName progName) {
		if(Tab.currentScope.findSymbol(progName.getName()) != null) {
			report_error("simbol " + progName.getName() + " je vec definisan u scope-u", progName);
			progName.obj = Tab.noObj;
		} else {
			progName.obj = Tab.insert(Obj.Prog, progName.getName(), Tab.noType);
		}
		Tab.openScope();     	
	}

	public void visit(MethodDecl methodDecl) {
		if (!returnFound && currentMethod.getType() != Tab.noType) {
			report_error("metoda '" + currentMethod.getName() + "' nema return iskaz", methodDecl);
		}
		
		if(currentMethod.getName().equals(new String("main")) && (methodDecl.getFormPars() instanceof FormParams)) {
			report_error("metoda 'main' ne sme imati formalne parametre", methodDecl);
		}
		
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		
		returnFound = false;
		currentMethod = null;
	}
	
	public void visit(MethodTypeAndName methodTypeName) {
		if( Tab.currentScope.findSymbol(methodTypeName.getMethName()) != null) {
			report_error("metoda sa imenom " + methodTypeName.getMethName() + " je vec definisana u scope-u", methodTypeName);
			currentMethod = Tab.noObj;
		} else {
			currentMethod = Tab.insert(Obj.Meth, methodTypeName.getMethName(), methodTypeName.getType().struct);
			methodTypeName.obj = currentMethod;
		}	
		Tab.openScope();
	}
	
	public void visit(MethodVoidName methodVoidName) {
		if(Tab.currentScope.findSymbol(methodVoidName.getMethName()) != null) {
			report_error("metoda sa imenom " + methodVoidName.getMethName() + " je vec definisana u scope-u", methodVoidName);
			currentMethod = Tab.noObj;
		} else {
			currentMethod = Tab.insert(Obj.Meth, methodVoidName.getMethName(), Tab.noType);
			methodVoidName.obj = currentMethod;
		}
		Tab.openScope();
	}
	
	public void visit(Type type) {
		Obj typeNode = Tab.find(type.getTypeName());
		if (typeNode == Tab.noObj) {
			report_error("nije pronadjen tip " + type.getTypeName() + " u tabeli simbola", null);
			type.struct = Tab.noType;
		} 
		else {
			if (Obj.Type == typeNode.getKind()) {
				type.struct = typeNode.getType();
			} 
			else {
				report_error("'" + type.getTypeName() + "' ne predstavlja tip ", type);
				type.struct = Tab.noType;
			}
		}  
		lastType = type.struct;
	}

	public void visit(ConstNum constNum) {
		if(!Tab.intType.equals(lastType)) {
			report_error("tip nije odgovajuci za deklaraciju konstante", constNum); 
		}
		if(Tab.currentScope.findSymbol(constNum.getName()) != null) {
			report_error("konstanta sa imenom '" + constNum.getName() + "' je vec definisana u scope-u", constNum);
		} else {
			Tab.insert(Obj.Con, constNum.getName(), lastType).setAdr(constNum.getValue());
		}
	}
	
	public void visit(ConstChar constChar) {
		if(!Tab.charType.equals(lastType)) {
			report_error("tip nije odgovajuci za deklaraciju konstante.", constChar); 
		}
		if(Tab.currentScope.findSymbol(constChar.getName()) != null) {
			report_error("Konstanta sa imenom '" + constChar.getName() + "' je vec definisana u scope-u", constChar);
		} else {
			Tab.insert(Obj.Con, constChar.getName(), lastType).setAdr(constChar.getValue());
		}	
	}
	
	public void visit(ConstBool constBool) {
		if(!boolType.equals(lastType)) {
			report_error("tip nije odgovajuci za deklaraciju konstante", constBool); 
		} 
		if(Tab.currentScope.findSymbol(constBool.getName()) != null) {
			report_error("konstanta sa imenom '" + constBool.getName() + "' je vec definisana u scope-u", constBool);
		} else {
			Tab.insert(Obj.Con, constBool.getName(), lastType).setAdr(constBool.getValue()? 1 : 0);
		}
	}
	
	public void visit(VarIdent varIdent) {
		if(Tab.currentScope.findSymbol(varIdent.getVarName()) != null) {
			report_error("promenljiva sa imenom '" + varIdent.getVarName() + "' je vec deklarisana u scope-u", varIdent);
		} else {
			if(varIdent.getVarIdentSB() instanceof VarIdentYesSB) {
				Tab.insert(Obj.Var, varIdent.getVarName(), new Struct(Struct.Array,lastType));
			} else {
				Tab.insert(Obj.Var, varIdent.getVarName(), lastType);
			}
		}
	}
	
	public void visit(ReturnExprStatement returnExpr){
		returnFound = true;
		Struct currMethType = currentMethod.getType();
		if( returnExpr.getExpr().struct.assignableTo(currMethType)) {
			report_error("povratna vrednost i tip metode moraju biti isti", returnExpr);
		}  	     	
	}
	
	public void visit(ReturnStatement returnStatement) {
		Struct currMethType = currentMethod.getType();
		if(!currMethType.equals(Tab.noType)) {
			report_error("metoda mora biti tipa 'void' da ne bi imala povratnu vrednost", returnStatement);
		}
	}
	
	public void visit(ExprAddopTerm exprAddopTerm) {
		exprAddopTerm.struct = exprAddopTerm.getAddopTermList().struct;
	}
	
	public void visit(FactorCharAdd factorCharAdd) {
		factorCharAdd.struct = Tab.charType;
		if(factorCharAdd.getExpr().struct != Tab.charType) {
			report_error("tip prvog parametra mora biti char", factorCharAdd);
			factorCharAdd.struct = Tab.noType;
		}
		if(factorCharAdd.getExpr1().struct != Tab.intType) {
			report_error("tip drugog parametra mora biti int", factorCharAdd);
			factorCharAdd.struct = Tab.noType;
		}
	}
	
	@Override
	public void visit(FactorArrayExpr factorArrayExpr) {
		factorArrayExpr.struct = Tab.intType;
		if(!factorArrayExpr.getDesignator().obj.getType().equals( new Struct(Struct.Array, Tab.intType))) {
			report_error("designator ne predstavlja niz intova", factorArrayExpr);
			factorArrayExpr.struct = Tab.noType;
		}
		if(!factorArrayExpr.getFactor().struct.equals(Tab.intType)) {
			report_error("factor nije tipa int",factorArrayExpr);
			factorArrayExpr.struct = Tab.noType;
		}
	}
	
	public void visit(AddopTerms addExpr) {
		Struct te = addExpr.getAddopTermList().struct;
		Struct t = addExpr.getTerm().struct;
		Boolean d = te.equals(t);
		if (d && te == Tab.intType)
			addExpr.struct = te;
		else {
			report_error("nekompatibilni tipovi u izrazu uz operciju addop", addExpr);
			addExpr.struct = Tab.noType;
		} 
	}
	
	public void visit(AddopMinusTermSingle addopMinusTermSingle) {
		if(addopMinusTermSingle.getTerm().struct == Tab.intType) {
			addopMinusTermSingle.struct = Tab.intType;
		} else {
			report_error("nekompatibilan tip u izrazu negacije", addopMinusTermSingle);
			addopMinusTermSingle.struct = Tab.noType;
		}
	}
	public void visit(AddopTermSingle addopTermSingle) {
		addopTermSingle.struct = addopTermSingle.getTerm().struct;
	}
	
	public void visit(FactorDesignator factorDesignator) {
		factorDesignator.struct = factorDesignator.getDesignator().obj.getType();
	}
	
	public void visit(FactorNumConst factorNumConst) {
		factorNumConst.struct = Tab.intType;
	}
	
	public void visit(FactorCharConst factorCharConst) {
		factorCharConst.struct = Tab.charType;
	}
	
	public void visit(FactorBoolConst factorBoolConst) {
		factorBoolConst.struct = boolType;
	}
	
	public void visit(FactorNewType factorNewType) {
		if(factorNewType.getOptExpr() instanceof OptExprYes) {
			OptExprYes optExprYes = (OptExprYes) factorNewType.getOptExpr();
			if(optExprYes.getExpr().struct.getKind() != Struct.Int) {
				report_error("tip izraza mora biti int", factorNewType);
			}
			factorNewType.struct = new Struct(Struct.Array, lastType);
		}
	}
	
	public void visit(FactorExpr factorExpr) {
		factorExpr.struct = factorExpr.getExpr().struct;
	}
	
	public void visit(MulopFactorSingle mulopFactorSingle) {
		mulopFactorSingle.struct = mulopFactorSingle.getFactor().struct;
	}
	
	public void visit(MulopFactors mulopFactors) {
		Struct term = mulopFactors.getTerm().struct;
		Struct factor = mulopFactors.getFactor().struct;
		Boolean same = term.equals(factor);
		if(same && factor == Tab.intType) {
			mulopFactors.struct = Tab.intType;
		} else {
			report_error("nekompatibilni tipovi u izrazu uz operaciju mulop", mulopFactors);
			mulopFactors.struct = Tab.noType;
		}
	}
	
	public void visit(DesignIdentSingle designIdentSingle){
		Obj obj = Tab.find(designIdentSingle.getName());
		SymbolTableVisitor stv;
		if (obj == Tab.noObj) { 
			report_error("designator sa imenom '"+designIdentSingle.getName()+"' nije deklarisan ", designIdentSingle);
		} else {
			stv = new DumpSymbolTableVisitor();
			if(obj.getKind() == Obj.Con) {
				stv.visitObjNode(obj);
				report_info("Pronadjen simbol  - '" + obj.getName() + "' - " + stv.getOutput(), designIdentSingle);
			}
			if(obj.getKind() == Obj.Var) {
				stv.visitObjNode(obj);
				report_info("Pronadjen simbol  - '" + obj.getName() + "' - " + stv.getOutput(), designIdentSingle);
			}
			//ispitati tipove da li je const var ... i level glob lok.
		}
		designIdentSingle.obj = obj;
	}
	
	@Override
	public void visit(DesignFStopIdent designFStopIdent) {
		report_error("nije podrzano pristupanje polju", designFStopIdent);
		designFStopIdent.obj = Tab.noObj;
	}
	
	public void visit(DesignIndex designIndex) {
		Obj arrayObj = designIndex.getDesignatorArray().getDesignator().obj; 
		designIndex.obj = new Obj(Obj.Elem, "", arrayObj.getType().getElemType());				
		if(arrayObj.getType().getKind() != Struct.Array) {
			report_error("Greska, identifikator ne predstavlja niz", designIndex);
			designIndex.obj = Tab.noObj;
		}
		if(designIndex.getExpr().struct.getKind() != Struct.Int) {
			report_error("Greska, expr mora biti tipa int", designIndex);
			designIndex.obj = Tab.noObj;
		}
		
	}
	
	public void visit(DesignatorStmtAssignExp designatorStmtAssignExp) {
		Obj desigObj = designatorStmtAssignExp.getDesignator().obj;
		int designatorKind = desigObj.getKind();
		Struct exprStruct = designatorStmtAssignExp.getExpr().struct;
		
		if( designatorKind != Obj.Var && designatorKind != Obj.Elem) {
			report_error("Designator '" + desigObj.getName() +"' nije promenljiva.", designatorStmtAssignExp);
		}
		if(!exprStruct.assignableTo(desigObj.getType())) {
				report_error("Greska pri operaciji dodele vrednosti, promenljive su razlicitog tipa: ", designatorStmtAssignExp);
		}
	}
	
	public void visit(DesignatorStatementIncrement designatorStatementIncrement) {
		int designKind = designatorStatementIncrement.getDesignator().obj.getKind();
		int designType = designatorStatementIncrement.getDesignator().obj.getType().getKind();
		
		if( designKind != Obj.Var && designKind != Obj.Elem) {
			report_error("Designator '" + designatorStatementIncrement.getDesignator().obj.getName() +"' nije promenljiva.", designatorStatementIncrement);
		}
		if( designType != Struct.Int) {
				report_error("Greska pri operaciji inkrementiranja, promenljiva nije tipa int: ", designatorStatementIncrement);
		}
	}
	
	public void visit(DesignatorStatementDecrement designatorStatementDecrement) {
		int designKind = designatorStatementDecrement.getDesignator().obj.getKind();
		int designType = designatorStatementDecrement.getDesignator().obj.getType().getKind();
		
		if(designKind != Obj.Var && designKind != Obj.Elem) {
			report_error("Designator '" + designatorStatementDecrement.getDesignator().obj.getName() +"' nije promenljiva.", designatorStatementDecrement);
		}
		if(designType != Struct.Int) {
				report_error("Greska pri operaciji dekrementiranja, promenljiva nije tipa int: ", designatorStatementDecrement);
		}
	}
	
	public void visit(ReadStatement readStatement) {
		int designKind = readStatement.getDesignator().obj.getKind();
		int designType = readStatement.getDesignator().obj.getType().getKind();
		
		if(designKind != Obj.Var && designKind != Obj.Elem) {
			report_error("Designator '" + readStatement.getDesignator().obj.getName() +"' nije promenljiva.", readStatement);
		}
		if(designType != Struct.Int && designType != Struct.Char && designType != Struct.Bool) {
				report_error("Greska pri operaciji dekrementiranja, promenljiva nije tipa int: ", readStatement);
		}
	}

	public void visit(ExprTernar exprTernar) {
		if(!exprTernar.getExpr().struct.equals(exprTernar.getExpr1().struct)) {
			report_error("Greska, 'true' i 'false' opcije ternarnog moraju biti istog tipa ", exprTernar);
			exprTernar.struct = Tab.noType;
		} else {
			exprTernar.struct = exprTernar.getExpr().struct;
		}
	}
	
	public void visit(CondFactRelop condFactRelop) {
		Struct addop1 = condFactRelop.getAddopTermList().struct;
		Struct addop2 = condFactRelop.getAddopTermList1().struct;
		
		if(!addop1.equals( addop2)) {
			report_error("Nekompatibilni tipovi operanada condFact u ternarnom operatoru ", condFactRelop);
		} else {
			if(addop1.getKind() == Struct.Array && !( condFactRelop.getRelop() instanceof RelopSame || condFactRelop.getRelop() instanceof RelopDiff)) {
				report_error("na nizove se moze primeniti samo == i != operatori", condFactRelop);
			}
		}
	}
	
	public void visit(CondFactNoRelop condFactNoRelop) {
		if(condFactNoRelop.getAddopTermList().struct != boolType) {
			report_error("condition nije tipa 'bool' u ternarnom", condFactNoRelop);
		}
	}
	
	public void visit(PrintStatement printStatement) {
		int expType = printStatement.getExpr().struct.getKind();
		if(expType != Struct.Int && expType != Struct.Char && expType != Struct.Bool) {
			report_error("Greska pri operaciji ispisivanja, izraz nije tipa int - char - bool: ", printStatement);
		}
	}
	
	@Override
	public void visit(VarDeclError varDeclError) {
		report_info("Izvrsen oporavak od reske ", varDeclError);
	}
	
	@Override
	public void visit(VarIdentError varIdentError) {
		report_info("Izvrsen oporavak od reske ", varIdentError);
	}
	
	@Override
	public void visit(DesignatorStmtAssignExpError designatorStmtAssignExpError) {
		report_info("Izvrsen oporavak od reske ", designatorStmtAssignExpError);
	}
	public boolean passed() {
		return !errorDetected;
	}
	
}

