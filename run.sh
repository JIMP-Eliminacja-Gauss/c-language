mvn package
mvn -q exec:java -Dexec.mainClass="org.example.Main" -Dexec.args="przyklad.PL" 2>/dev/null > przyklad.ll
lli przyklad.ll

#
#   a) Bytecode dla LLVM
#      $ llvm-as przyklad.ll
#      $ lli przyklad.bc
#
#   b) Kod maszynowy
#      $ llc przyklad.ll
#      $ clang przyklad.s
#
#   c) Interpretacja
#      $ lli przyklad.ll