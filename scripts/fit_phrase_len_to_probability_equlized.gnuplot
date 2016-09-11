# Use load of gnuplot
f(x) = a*b**x+c

fit f(x) "< awk <phrases.txt '$1 > 5 && $2 < 0.001 && $1 < 30 { a[$1] += $2; b[$1]+=1} END {for(i=6; i<20; i++) print i,  a[i]/b[i] }'" using 1:2 via a,b,c;

plot "< awk <phrases.txt '$1 > 5 && $2 < 0.001 && $1 < 30 { print $0} '" using 1:($2/f($1)),  f(x);
