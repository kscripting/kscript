function _in_path() { command -v "$1" >/dev/null 2>&1; }
_in_path kscript && echo "kscript is already installed at $(which kscript)" 1>&2 || {
    function echo_and_eval() { echo "$ $@" 1>&2; eval "$@" 1>&2; }
    _in_path sdk || {
        export SDKMAN_DIR="$HOME/.sdkman" && curl "https://get.sdkman.io" | bash 1>&2 && \
            echo_and_eval source "$SDKMAN_DIR/bin/sdkman-init.sh"
    }

    sdkman_auto_answer=true

    _in_path java || echo_and_eval sdk install java
    _in_path kotlin || echo_and_eval sdk install kotlin
    _in_path gradle || echo_and_eval sdk install gradle
    _in_path kscript || echo_and_eval sdk install kscript
    echo_and_eval source "$SDKMAN_DIR/bin/sdkman-init.sh"
}
