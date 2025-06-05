#!/usr/bin/env node
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

function runKscriptTest(kscriptFile, expectedOutputSubstring) {
    return new Promise((resolve) => { // Removed reject, will resolve with true/false
        console.log(`Running Node.js kscript test: ${kscriptFile}...`);

        const projectRoot = path.resolve(__dirname, '..');
        const jarPath = path.join(projectRoot, 'wrappers', 'kscript.jar');
        const wrapperScriptPath = path.join(projectRoot, 'wrappers', 'kscript_js_wrapper.js');
        const kscriptFilePath = path.join(projectRoot, 'examples', kscriptFile);

        if (!fs.existsSync(jarPath)) {
            console.error(`ERROR: kscript.jar not found at ${jarPath}`);
            console.log(`Node.js wrapper test for ${kscriptFile} SKIPPED due to missing kscript.jar.`);
            resolve(false);
            return;
        }

        if (!fs.existsSync(wrapperScriptPath)) {
            console.error(`ERROR: Node.js wrapper script not found at ${wrapperScriptPath}`);
            resolve(false);
            return;
        }

        if (!fs.existsSync(kscriptFilePath)) {
            console.error(`ERROR: Test kscript file not found at ${kscriptFilePath}`);
            resolve(false);
            return;
        }

        console.log(`Executing command: node ${wrapperScriptPath} ${kscriptFilePath}`);
        const kscriptProcess = spawn('node', [wrapperScriptPath, kscriptFilePath], {
            stdio: ['pipe', 'pipe', 'pipe'],
            timeout: 60000 // 60 seconds timeout, increased for kscript compilation
        });

        let stdoutData = '';
        let stderrData = '';

        kscriptProcess.stdout.on('data', (data) => { stdoutData += data.toString(); });
        kscriptProcess.stderr.on('data', (data) => { stderrData += data.toString(); });

        // Timeout handling is implicitly done by spawn's timeout option, which emits 'error' with err.code 'ETIMEDOUT'
        // or kills the process and it closes with a signal code.

        kscriptProcess.on('close', (code, signal) => {
            stdoutData = stdoutData.trim();
            stderrData = stderrData.trim();

            if (stdoutData) console.log(`Stdout:\n${stdoutData}`);
            if (stderrData) console.error(`Stderr:\n${stderrData}`);

            if (signal) { // Process was killed, e.g. by timeout
                console.error(`Test FAILED for ${kscriptFile} due to signal: ${signal}`);
                resolve(false);
            } else if (code === 0 && stdoutData.includes(expectedOutputSubstring)) {
                console.log(`Test PASSED for ${kscriptFile}!`);
                resolve(true);
            } else {
                console.error(`Test FAILED for ${kscriptFile}. Exit code: ${code}`);
                if (!stdoutData.includes(expectedOutputSubstring)) {
                    console.error(`Expected substring '${expectedOutputSubstring}' not found in stdout.`);
                }
                resolve(false);
            }
        });

        kscriptProcess.on('error', (err) => {
            // This 'error' event usually means the process could not be spawned or was killed due to timeout.
            if (err.code === 'ETIMEDOUT') {
                 console.error(`Test TIMEOUT for ${kscriptFile}`);
            } else {
                 console.error(`Test ERRORED for ${kscriptFile}: ${err.message}`);
            }
            // Ensure stderrData from the process is also logged if available
            if (stderrData.trim()) console.error(`Stderr (on error event):\n${stderrData.trim()}`);
            resolve(false);
        });
    });
}

async function main() {
    console.log("Starting Node.js wrapper tests...");
    // Similar to the Python test, this version is stricter and expects kscript.jar to be present.
    // If java and kscript.jar are not functional, the wrapper will likely forward an error from java,
    // which will be caught as a test failure.

    const test1Success = await runKscriptTest('test_wrapper.kts', 'kscript wrapper test successful!');
    const test2Success = await runKscriptTest('test_kotlin2_feature.kts', 'Kotlin 2.x feature (value class) test successful!');

    if (test1Success && test2Success) {
        console.log("All Node.js wrapper tests PASSED!");
        process.exit(0);
    } else {
        console.error("One or more Node.js wrapper tests FAILED or were SKIPPED.");
        process.exit(1);
    }
}

main();
