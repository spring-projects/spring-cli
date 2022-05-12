import * as os from 'os';
import * as path from 'path';

export const tempDir = path.join(__dirname, 'springup', 'temp');
export const isWindows = os.platform() === 'win32';
export const cliPathRelative = isWindows
  ? '..\\build\\native\\nativeCompile\\spring-up.exe'
  : '../build/native/nativeCompile/spring-up';
export const cliPath = path.resolve(cliPathRelative);
export const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));
