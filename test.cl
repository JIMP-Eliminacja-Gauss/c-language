bool x = false && 1/0==0;
print(x);

bool y = true || 10 / 0 != 0;
print(y);

bool z = true || false && true || false;
print(z);

bool o = false && false || true;
print(o);
