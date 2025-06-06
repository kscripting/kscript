import subprocess
import sys
import os

def main():
    jar_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'kscript.jar')
    command = ['java', '-jar', jar_path] + sys.argv[1:]

    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = process.communicate()

    if stdout:
        print(stdout.decode())
    if stderr:
        print(stderr.decode(), file=sys.stderr)

    sys.exit(process.returncode)

if __name__ == '__main__':
    main()
