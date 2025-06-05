import subprocess
import sys
import os

def run_kscript_test(kscript_file, expected_output_substring):
    print(f"Running Python kscript test: {kscript_file}...")

    # Determine paths relative to this test script's location
    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
    jar_path = os.path.join(project_root, 'wrappers', 'kscript.jar')
    wrapper_script_path = os.path.join(project_root, 'wrappers', 'kscript_py_wrapper.py')
    kscript_file_path = os.path.join(project_root, 'examples', kscript_file)

    # Check if kscript.jar exists BEFORE attempting to run
    if not os.path.exists(jar_path):
        print(f"ERROR: kscript.jar not found at {jar_path}", file=sys.stderr)
        print(f"Python wrapper test for {kscript_file} SKIPPED due to missing kscript.jar.")
        return False # Indicate failure/skip

    if not os.path.exists(wrapper_script_path):
        print(f"ERROR: Python wrapper script not found at {wrapper_script_path}", file=sys.stderr)
        return False

    if not os.path.exists(kscript_file_path):
        print(f"ERROR: Test kscript file not found at {kscript_file_path}", file=sys.stderr)
        return False

    command = [sys.executable, wrapper_script_path, kscript_file_path]
    print(f"Executing command: {' '.join(command)}")

    process = None # Initialize process to None
    try:
        process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        stdout, stderr = process.communicate(timeout=60) # Increased timeout to 60s for kscript compilation

        decoded_stdout = stdout.decode().strip()
        decoded_stderr = stderr.decode().strip()

        if decoded_stdout:
            print(f"Stdout:\n{decoded_stdout}")
        if decoded_stderr:
            print(f"Stderr:\n{decoded_stderr}", file=sys.stderr)

        if process.returncode == 0 and expected_output_substring in decoded_stdout:
            print(f"Test PASSED for {kscript_file}!")
            return True
        else:
            print(f"Test FAILED for {kscript_file}. Exit code: {process.returncode}", file=sys.stderr)
            if not (expected_output_substring in decoded_stdout):
                 print(f"Expected substring '{expected_output_substring}' not found in stdout.", file=sys.stderr)
            return False
    except subprocess.TimeoutExpired:
        print(f"Test TIMEOUT for {kscript_file}", file=sys.stderr)
        if process:
            process.kill()
            # Try to communicate again to get any remaining output
            stdout, stderr = process.communicate()
            decoded_stdout = stdout.decode().strip()
            decoded_stderr = stderr.decode().strip()
            if decoded_stdout:
                print(f"Stdout (on timeout):\n{decoded_stdout}")
            if decoded_stderr:
                print(f"Stderr (on timeout):\n{decoded_stderr}", file=sys.stderr)
        return False
    except Exception as e:
        print(f"Test ERRORED for {kscript_file}: {e}", file=sys.stderr)
        return False

def main():
    print("Starting Python wrapper tests...")
    # Assuming kscript.jar might not be present or Java not runnable in a pure 'unit test' like environment for the wrapper
    # The run_kscript_test function now checks for kscript.jar.
    # For a true end-to-end test, Java and a working kscript.jar (from a build) are required.
    # The previous version of this test script had more complex logic to "partially pass"
    # if kscript.jar or java was missing. This version is stricter and expects kscript.jar to be present
    # and for the java command within the wrapper to be attempted. If kscript.jar is missing, it's a skip/fail.
    # If java fails to run kscript.jar (e.g. ClassNotFound), that's a failure of the kscript execution itself.

    test1_success = run_kscript_test('test_wrapper.kts', 'kscript wrapper test successful!')
    test2_success = run_kscript_test('test_kotlin2_feature.kts', 'Kotlin 2.x feature (value class) test successful!')

    if test1_success and test2_success:
        print("All Python wrapper tests PASSED!")
        sys.exit(0)
    else:
        print("One or more Python wrapper tests FAILED or were SKIPPED.")
        sys.exit(1)

if __name__ == '__main__':
    main()
