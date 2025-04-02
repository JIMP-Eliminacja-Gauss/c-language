grammar Expr;

prog:	expr+ EOF ;

expr: (varDeclaration | arithmeticExpression | inputOutputExpression | arrayAssignement) SEMICOLON;

arithmeticExpression: additiveExpression;

additiveExpression: multiplicativeExpression (ADDITIVE_OPERATOR multiplicativeExpression)?;

multiplicativeExpression: expressionFactor (MULTIPLICATIVE_OPERATOR expressionFactor)?;

expressionFactor: INT_VALUE | FLOAT_VALUE | ID | arrayValueByIndex;

booleanExpression: booleanDisjunctionExpression;

booleanDisjunctionExpression: booleanConjunctionExpression (OR booleanConjunctionExpression)?;

booleanConjunctionExpression: booleanEqualityExpression (AND booleanEqualityExpression)?;

booleanEqualityExpression: unaryExpression ((XAND | XOR) unaryExpression)?;

unaryExpression: (NEG)* (BOOL_VALUE | ID);

inputOutputExpression: READ '(' ID? ')'                                             #read
    | PRINT '(' (ID | arithmeticExpression | booleanExpression | STRING_VALUE) ')'  #print;

varDeclaration: floatDeclaration | intDeclaration | arrayDeclaration | boolDeclaration;

floatDeclaration: FLOAT ID floatAssignement?;

intDeclaration: INT ID intAssignement?;

arrayDeclaration: ARRAY (FLOAT | INT) ID (arrayInitialization | arrayMalloc)?;

boolDeclaration: BOOL ID boolAssignement?;

floatAssignement: ASSIGN (FLOAT_VALUE | arithmeticExpression | arrayValueByIndex);

intAssignement: ASSIGN (INT_VALUE | arithmeticExpression | arrayValueByIndex);

arrayInitialization: ASSIGN '{' arrayValues (',' arrayValues)* '}';

arrayValueByIndex: ID '[' arrayIndex ']';

arrayIndex: INT_VALUE | ID | arithmeticExpression;

arrayAssignement: arrayValueByIndex ASSIGN (INT_VALUE | FLOAT_VALUE | arithmeticExpression | arrayIndex);

boolAssignement: ASSIGN (BOOL_VALUE | booleanExpression);

arrayValues: INT_VALUE | FLOAT_VALUE | arithmeticExpression;

arrayMalloc: ASSIGN '[' INT_VALUE ']';


SEMICOLON: ';';
ASSIGN: '=';
READ: 'read';
PRINT: 'print';
FLOAT: 'double';
INT: 'int';
BOOL: 'bool';
AND: '&&';
XAND: '==';
XOR: '!=';
OR: '||';
NEG: '!';
ARRAY: 'array';
MATRIX: 'matrix';

INT_VALUE     : [0-9]+ ;
FLOAT_VALUE: ([0-9]+[.][0-9]*|[0-9]*[.][0-9]+);
BOOL_VALUE: 'true' | 'false';
ID: [a-zA-Z_]+[a-zA-Z_0-9]*;
STRING_VALUE: '"' ~["]* '"';
ADDITIVE_OPERATOR: [+-];
MULTIPLICATIVE_OPERATOR: [*/];
WS : [ \t\r\n]+ -> skip;