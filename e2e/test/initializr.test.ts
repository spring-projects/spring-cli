import * as fs from 'fs';
import * as path from 'path';
import { rmRF, mkdirP } from '@actions/io';
import 'jest-extended';
import waitForExpect from 'wait-for-expect';
import { tempDir, cliPath, sleep } from '../src/utils';
import { Cli } from '../src/cli';
import { waitForExpectTimeout, waitForExpectInterval } from '../src/constans';

describe('initializr non-interactive commands', () => {
  let cli: Cli;

  beforeEach(async () => {
    waitForExpect.defaults.timeout = waitForExpectTimeout;
    waitForExpect.defaults.interval = waitForExpectInterval;
    await rmRF(tempDir);
    await mkdirP(tempDir);
    expect(fs.existsSync(cliPath)).toBe(true);
  }, 300000);

  afterEach(async () => {
    try {
      await rmRF(tempDir);
    } catch {
      console.log('Failed to remove test directories');
    }
    cli?.dispose();
  }, 100000);

  it('create maven project', async () => {
    const demoDir = path.join(tempDir, 'demo');
    const demoDirWin = demoDir.replace(/\\/g, '\\\\');
    cli = new Cli({
      command: cliPath,
      options: [
        'initializr new',
        `--path ${demoDirWin}`,
        '--project maven-project',
        '--language java',
        '--boot-version 2.6.4',
        '--version 0.0.1-SNAPSHOT',
        '--group com.example',
        '--artifact demo',
        '--name demo',
        '--description Demo',
        '--package-name com.example.demo',
        '--dependencies camel,derby',
        '--packaging jar',
        '--java-version 11'
      ]
    });

    cli.run();

    const buildFile = path.join(demoDir, 'pom.xml');
    await waitForExpect(async () => {
      expect(fs.existsSync(buildFile)).toBe(true);
    });
  }, 20000);

  it('create gradle project', async () => {
    const demoDir = path.join(tempDir, 'demo');
    const demoDirWin = demoDir.replace(/\\/g, '\\\\');
    cli = new Cli({
      command: cliPath,
      options: [
        'initializr new',
        `--path ${demoDirWin}`,
        '--project gradle-project',
        '--language java',
        '--boot-version 2.6.4',
        '--version 0.0.1-SNAPSHOT',
        '--group com.example',
        '--artifact demo',
        '--name demo',
        '--description Demo',
        '--package-name com.example.demo',
        '--dependencies camel,derby',
        '--packaging jar',
        '--java-version 11'
      ]
    });

    cli.run();

    const buildFile = path.join(demoDir, 'build.gradle');
    await waitForExpect(async () => {
      expect(fs.existsSync(buildFile)).toBe(true);
    });
  }, 20000);
});

describe('initializr interactive commands', () => {
  let cli: Cli;

  beforeEach(async () => {
    waitForExpect.defaults.timeout = waitForExpectTimeout;
    waitForExpect.defaults.interval = waitForExpectInterval;
    await rmRF(tempDir);
    await mkdirP(tempDir);
    expect(fs.existsSync(cliPath)).toBe(true);
  }, 300000);

  afterEach(async () => {
    try {
      await rmRF(tempDir);
    } catch {
      console.log('Failed to remove test directories');
    }
    cli?.dispose();
  }, 100000);

  it('create maven project', async () => {
    const demoDir = path.join(tempDir, 'demo');
    cli = new Cli({
      command: cliPath,
      options: ['initializr new']
    });

    cli.run();

    const expected = [expect.stringMatching('Path')];
    await waitForExpect(async () => {
      const screen = cli.screen();
      expect(screen).toEqual(expect.arrayContaining(expected));
    });

    // path
    await cli.keyText(demoDir);
    await cli.keyEnter();
    // project
    await cli.keyDown();
    await cli.keyEnter();
    // language
    await cli.keyDown();
    await cli.keyEnter();
    // boot
    await cli.keyEnter();
    // version
    await cli.keyEnter();
    // group
    await cli.keyEnter();
    // artifact
    await cli.keyEnter();
    // name
    await cli.keyEnter();
    // description
    await cli.keyEnter();
    // package
    await cli.keyEnter();
    // dependencies
    await cli.keyEnter();
    // packaging
    await cli.keyEnter();
    // java
    await cli.keyUp();
    await cli.keyEnter();

    const buildFile = path.join(demoDir, 'pom.xml');
    await waitForExpect(async () => {
      expect(fs.existsSync(buildFile)).toBe(true);
    });
    await waitForExpect(async () => {
      const content = fs.readFileSync(buildFile).toString();
      expect(content).toContain('<java.version>17</java.version>');
    });
  }, 20000);

  it('create gradle project', async () => {
    const demoDir = path.join(tempDir, 'demo');
    cli = new Cli({
      command: cliPath,
      options: ['initializr new']
    });

    cli.run();

    const expected = [expect.stringMatching('Path')];
    await waitForExpect(async () => {
      const screen = cli.screen();
      expect(screen).toEqual(expect.arrayContaining(expected));
    });

    // path
    await cli.keyText(demoDir);
    await cli.keyEnter();
    // project
    await cli.keyEnter();
    // language
    await cli.keyDown();
    await cli.keyEnter();
    // boot
    await cli.keyEnter();
    // version
    await cli.keyEnter();
    // group
    await cli.keyEnter();
    // artifact
    await cli.keyEnter();
    // name
    await cli.keyEnter();
    // description
    await cli.keyEnter();
    // package
    await cli.keyEnter();
    // dependencies
    await cli.keyEnter();
    // packaging
    await cli.keyEnter();
    // java
    await cli.keyUp();
    await cli.keyEnter();

    const buildFile = path.join(demoDir, 'build.gradle');
    await waitForExpect(async () => {
      expect(fs.existsSync(buildFile)).toBe(true);
    });
    await waitForExpect(async () => {
      const content = fs.readFileSync(buildFile).toString();
      expect(content).toContain('sourceCompatibility = \'17\'');
    });
  }, 20000);
});
