string classes = "------------Obsługa klas------------";
print(classes);

cls HelloClass {
    string name;
    int age;
    double zzz;

    int age() {
        return 10;
    }
};

HelloClass h = new;
h.age = 20;
int classTest = h.age();
int classTest2 = h.age;
print(classTest);
print(classTest2);