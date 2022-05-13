import * as os from 'os';
import * as path from 'path';

export const tempDir = path.join(__dirname, 'spring-cli', 'temp');
export const isWindows = os.platform() === 'win32';
export const cliPathRelative = isWindows
  ? '..\\..\\build\\native\\\nativeCompile\spring-cli.exe'
  : '../../build/native/nativeCompile/spring-cli';
export const jarPathRelative = isWindows
  ? '..\\..\\build\\libs\\spring-cli-0.0.1-SNAPSHOT.jar'
  : '../../build/libs/spring-cli-0.0.1-SNAPSHOT.jar';
export const cliPath = path.resolve(cliPathRelative);
export const jarPath = path.resolve(jarPathRelative);
export const nativeDesc = 'native';
export const jarDesc = 'jar';
export const jarCommand = isWindows ? 'java.exe' : 'java';
export const nativeCommand = cliPath;
export const jarOptions = ['-jar', jarPath];
export const waitForExpectDefaultTimeout = 30000;
export const waitForExpectDefaultInterval = 2000;
export const testTimeout = 120000;
