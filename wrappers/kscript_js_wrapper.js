#!/usr/bin/env node
const { spawn } = require('child_process');
const path = require('path');

const jarPath = path.join(__dirname, 'kscript.jar');
const args = process.argv.slice(2);

const kscriptProcess = spawn('java', ['-jar', jarPath, ...args], { stdio: 'inherit' });

kscriptProcess.on('close', (code) => {
  process.exit(code);
});
