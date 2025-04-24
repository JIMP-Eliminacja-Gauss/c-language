string intDouble = "------------Obsluga zmiennych: calkowite, rzeczywiste------------";
print(intDouble);
int int1 = 5;
double double1 = 3.14;
print(int1);
print(double1);

string readStep = "------------Wejscie-wyjscie------------";
print(readStep);
string read1;
read(read1);
print(read1);


string arithmeticStep = "------------Podstawowe operacje arytmetyczne------------";
print(arithmeticStep);
int addResult = int1 + 10;
int addResult2 = 10 + 15;
double addResult3 = double1 + 4.1;
double addResult4 = 5.2 + 3.4;
int complex = 5 + addResult2 * 2;
print(addResult);
print(addResult2);
print(addResult3);
print(addResult4);
print(complex);

int multResult = int1 * 5;
int multResult2 = 10 * 15;
double multResult3 = double1 * 2.5;
double multResult4 = 1.5 * 1.5;
print(multResult);
print(multResult2);
print(multResult3);
print(multResult4);

string arrayStep = "------------Obsluga zmiennych tablicowych------------";
print(arrayStep);
array int array1 = {5, 8, 12};
int test = array1[1];
int test2 = array1[2];
print(test);
print(test2);


string booleanStep = "------------Obsluga wartosci logicznych z short-circuit------------";
print(booleanStep);
bool x = false && 1/0==0;
print(x);

bool y = true || 10 / 0 != 0;
print(y);

bool z = true || false && true || false;
print(z);

bool o = false && false || true;
print(o);
