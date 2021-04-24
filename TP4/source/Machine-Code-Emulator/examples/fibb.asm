// unsigned int fib(unsigned int n){
//    unsigned int i = n - 1, a = 1, b = 0, c = 0, d = 1, t;
//    if (n <= 0)
//      return 0;
//    while (i > 0){
//      if (i % 2 == 1){
//        t = d*(b + a) + c*b;
//        a = d*b + c*a;
//        b = t;
//      }
//      t = d*(2*c + d);
//      c = c*c + d*d;
//      d = t;
//      i = i / 2;
//    }
//    return a + b;
//  }

PRINT "Please enter the number of the fibonacci suite to compute:"
INPUT n

//    if (n <= 0)
//      return 0;
LD R0, n
BGTZ R0, validInput
PRINT #0
BR end

validInput:
//    unsigned int i = n - 1, a = 1, b = 0, c = 0, d = 1, t;
DEC R0
ST i, R0
ST a, #1
ST b, #0
ST c, #0
ST d, #1

//    while (i > 0){
beginWhile:
LD R0, i
BLETZ R0, printResult

//      if (i % 2 == 1){
MOD R0, R0, #2
DEC R0
BNETZ R0, afterIf

CLEAR

//        t = d*(b + a) + c*b;
//        a = d*b + c*a;
//        b = t;

// TODO:: PUT THE BLOCK 1 HERE !
// Step 0
LD R0, b
LD R1, a
ADD R2, R0, R1
// Life_IN  : [a, b, c, d, i]
// Life_OUT : [a, b, c, d, i, t0]
// Next_IN  : a:[0], b:[0, 2], c:[2], d:[1]
// Next_OUT : b:[2], c:[2], d:[1], t0:[1]

// Step 1
LD R1, d
MUL R1, R1, R2
// Life_IN  : [a, b, c, d, i, t0]
// Life_OUT : [a, b, c, d, i, t1]
// Next_IN  : b:[2], c:[2], d:[1], t0:[1]
// Next_OUT : b:[2], c:[2], t1:[3]

// Step 2
LD R2, c
MUL R0, R2, R0
// Life_IN  : [a, b, c, d, i, t1]
// Life_OUT : [a, c, d, i, t1, t2]
// Next_IN  : b:[2], c:[2], t1:[3]
// Next_OUT : t1:[3], t2:[3]

// Step 3
ADD R0, R1, R0
// Life_IN  : [a, c, d, i, t1, t2]
// Life_OUT : [a, c, d, t, i]
// Next_IN  : t1:[3], t2:[3]
// Next_OUT : t:[4]

// Step 4
ST t, R0
// Life_IN  : [a, c, d, t, i]
// Life_OUT : [a, b, c, d, t, i]
// Next_IN  : t:[4]
// Next_OUT :

ST b, R0

// TODO:: END THE BLOCK 1 HERE ABOVE !

CLEAR

afterIf:
CLEAR

//      t = d*(2*c + d);
//      c = c*c + d*d;
//      d = t;
//      i = i / 2;

// TODO:: PUT THE BLOCK 2 HERE !
// Step 0
LD R0, c
MUL R1, #2, R0
// Life_IN  : [a, b, c, d, i]
// Life_OUT : [a, b, c, d, i, t0]
// Next_IN  : c:[0, 3], d:[1, 2, 4], i:[7]
// Next_OUT : c:[3], d:[1, 2, 4], i:[7], t0:[1]

// Step 1
LD R2, d
ADD R1, R1, R2
// Life_IN  : [a, b, c, d, i, t0]
// Life_OUT : [a, b, c, d, i, t1]
// Next_IN  : c:[3], d:[1, 2, 4], i:[7], t0:[1]
// Next_OUT : c:[3], d:[2, 4], i:[7], t1:[2]

// Step 2
MUL R1, R2, R1
// Life_IN  : [a, b, c, d, i, t1]
// Life_OUT : [a, b, c, t, d, i]
// Next_IN  : c:[3], d:[2, 4], i:[7], t1:[2]
// Next_OUT : c:[3], d:[4], i:[7], t:[6]

// Step 3
MUL R0, R0, R0
// Life_IN  : [a, b, c, t, d, i]
// Life_OUT : [a, b, t, d, i, t2]
// Next_IN  : c:[3], d:[4], i:[7], t:[6]
// Next_OUT : d:[4], i:[7], t:[6], t2:[5]

// Step 4
MUL R2, R2, R2
// Life_IN  : [a, b, t, d, i, t2]
// Life_OUT : [a, b, t, i, t2, t3]
// Next_IN  : d:[4], i:[7], t:[6], t2:[5]
// Next_OUT : i:[7], t:[6], t2:[5], t3:[5]

// Step 5
ADD R0, R0, R2
// Life_IN  : [a, b, t, i, t2, t3]
// Life_OUT : [a, b, c, t, i]
// Next_IN  : i:[7], t:[6], t2:[5], t3:[5]
// Next_OUT : i:[7], t:[6]

// Step 6
ST c, R0
// Life_IN  : [a, b, c, t, i]
// Life_OUT : [a, b, c, d, t, i]
// Next_IN  : i:[7], t:[6]
// Next_OUT : i:[7]

// Step 7
ST d, R0
LD R0, i
DIV R0, R0, #2
// Life_IN  : [a, b, c, d, t, i]
// Life_OUT : [a, b, c, d, t, i]
// Next_IN  : i:[7]
// Next_OUT :

ST i, R0
ST t, R1

// TODO:: END THE BLOCK 2 HERE ABOVE!




// TODO:: This instruction is just a placeholder to let the code end, remove the code below!

// TODO:: Remove the placeholder above of this line!

CLEAR
BR beginWhile

//    return a + b;
printResult:
LD R0, a
LD R1, b
ADD R0, R0, R1
PRINT R0

end:
PRINT "END"