grammar Expr;

prog:	expr+ EOF ;

expr: (varDeclaration | arithmeticExpression | inputOutputExpression | arrayDeclaration | arrayAssignement | function | ifStatement) SEMICOLON;

function: returnType ID '(' argsDeclaration (',' argsDeclaration)* ')' '{' functionBlock '}';

functionBlock: blockStmt*;

ifStatement: IF '(' ID ')' '{' ifBlock '}' (elseStatement)?;

ifBlock: blockStmt*;

elseStatement: ELSE '{' blockStmt* '}';

argsDeclaration: (FLOAT | INT | BOOL | STRING) ID;

blockStmt: (varDeclaration | arithmeticExpression | inputOutputExpression | arrayAssignement | ifStatement | returnStmt) SEMICOLON;

returnStmt: RETURN (ID | INT_VALUE | FLOAT_VALUE | STRING_VALUE | BOOL_VALUE | arithmeticExpression | booleanExpression)?;

returnType: INT | FLOAT | BOOL | STRING | VOID;

arithmeticExpression: additiveExpression+;

additiveExpression: multiplicativeExpression (ADDITIVE_OPERATOR multiplicativeExpression)*;

multiplicativeExpression: expressionFactor (MULTIPLICATIVE_OPERATOR expressionFactor)*;

expressionFactor: INT_VALUE
                | FLOAT_VALUE
                | ID;

booleanExpression: booleanDisjunctionExpression;

booleanDisjunctionExpression: booleanConjunctionExpression (OR booleanConjunctionExpression)*;

booleanConjunctionExpression: booleanEqualityExpression (AND booleanEqualityExpression)*;

booleanEqualityExpression: unaryExpression ((XAND | XOR) unaryExpression)? |
                         arithmeticExpression (XAND | XOR) arithmeticExpression ;

unaryExpression: (NEG)* (BOOL_VALUE | ID);

inputOutputExpression: READ '(' ID? ')'                                                                 #read
                    | PRINT '(' (ID | arithmeticExpression | booleanExpression | STRING_VALUE) ')'      #print;

varDeclaration: floatDeclaration | intDeclaration | boolDeclaration | stringDeclaration;

floatDeclaration: FLOAT ID floatAssignement?;

intDeclaration: INT ID intAssignement?;

arrayDeclaration: ARRAY (FLOAT | INT) ID (arrayInitialization | arrayMalloc)?;

boolDeclaration: BOOL ID boolAssignement?;

stringDeclaration: STRING ID stringAssignement?;

floatAssignement: ASSIGN (FLOAT_VALUE | arithmeticExpression | arrayValueByIndex);

intAssignement: ASSIGN (INT_VALUE | arithmeticExpression | arrayValueByIndex);

arrayInitialization: ASSIGN '{' arrayValues (',' arrayValues)* '}';

arrayValueByIndex: ID '[' arrayIndex ']';

arrayIndex: INT_VALUE | ID | arithmeticExpression;

arrayAssignement: arrayValueByIndex ASSIGN (INT_VALUE | FLOAT_VALUE | arithmeticExpression | arrayIndex);

boolAssignement: ASSIGN (BOOL_VALUE | booleanExpression);

stringAssignement: ASSIGN STRING_VALUE;

arrayValues: INT_VALUE | FLOAT_VALUE | arithmeticExpression;

arrayMalloc: ASSIGN '[' INT_VALUE ']';


SEMICOLON: ';';
ASSIGN: '=';
READ: 'read';
PRINT: 'print';
FLOAT: 'double';
INT: 'int';
BOOL: 'bool';
VOID: 'void';
STRING: 'string';
AND: '&&';
XAND: '==';
XOR: '!=';
OR: '||';
NEG: '!';
ARRAY: 'array';
MATRIX: 'matrix';
RETURN: 'return';
IF: 'if';
ELSE: 'else';

INT_VALUE     : [0-9]+ ;
FLOAT_VALUE: ([0-9]+[.][0-9]*|[0-9]*[.][0-9]+);
BOOL_VALUE: 'true' | 'false';
ID: [a-zA-Z_]+[a-zA-Z_0-9]*;
STRING_VALUE: '"' ~["]* '"';
ADDITIVE_OPERATOR: [+-];
MULTIPLICATIVE_OPERATOR: [*/];
WS : [ \t\r\n]+ -> skip;