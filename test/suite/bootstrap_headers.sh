f=/tmp/echo_stdin_args.kts
cp "${PROJECT_DIR}/test/resources/echo_stdin_args.kts" $f

# ensure script works as is
assert 'echo stdin | '$f' --foo bar' "stdin | script --foo bar"

# add bootstrap header
assert 'kscript --add-bootstrap-header '$f ''

# ensure adding it again raises an error
assert_raises 'kscript --add-bootstrap-header '$f 1

# ensure scripts works with header, including stdin
assert 'echo stdin | '$f' --foo bar' "stdin | script --foo bar"

# ensure scripts works with header invoked with explicit `kscript`
assert 'echo stdin | kscript '$f' --foo bar' "stdin | script --foo bar"

rm $f
