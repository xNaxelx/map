. .env/bin/activate

./annotate.py 0 & ./annotate.py 1 & ./annotate.py 2 &./annotate.py 3 
./annotate.py 4 & ./annotate.py 5 & ./annotate.py 6 
./annotate.py 7 & ./annotate.py 8 & ./annotate.py 9

find corpus/annotated/ -name '*0.txt' -exec cp {} ../noescape-generator/text/0 \;
