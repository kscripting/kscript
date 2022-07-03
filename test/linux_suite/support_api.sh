## make sure that one-liners include support-api
assert 'echo "foo${NL}bar" | kscript -t "stdin.print()"' $'foo\nbar'
assert 'echo "foo${NL}bar" | kscript -t "lines.print()"' $'foo\nbar'
#echo "$'foo\nbar' | kscript 'lines.print()'

assert_statement 'echo "foo${NL}bar" | kscript -s --text "lines.split().select(1, 2, -3)"' "" "[ERROR] Can not mix positive and negative selections" 1
