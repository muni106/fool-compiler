push 0
push 5
push 2
add
push function1
lfp
push 1
lfp
stm
ltm
ltm
push -3
add
lw
js
push 1
beq label4
push 10
b label5
label4:
lfp
push -2
add
lw
label5:
print
halt

function0:
cfp
lra
lfp
push 2
add
lw
lfp
push 3
lfp
lw
lw
push -2
add
lw
beq label0
push 0
b label1
label0:
push 1
label1:
lfp
lw
lw
stm
ltm
ltm
push -3
add
lw
js
stm
pop
sra
pop
pop
pop
sfp
ltm
lra
js

function1:
cfp
lra
push function0
lfp
push 1
add
lw
push 1
beq label2
push 0
b label3
label2:
lfp
push 3
push 2
lfp
stm
ltm
ltm
push -2
add
lw
js
label3:
stm
pop
sra
pop
pop
sfp
ltm
lra
js