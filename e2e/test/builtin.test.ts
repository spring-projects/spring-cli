import * as fs from 'fs';
import { rmRF, mkdirP } from '@actions/io';
import 'jest-extended';
import waitForExpect from 'wait-for-expect';
import { tempDir, cliPath } from '../src/utils';
import { Cli } from '../src/cli';
import { waitForExpectTimeout, waitForExpectInterval } from '../src/constans';

describe('builtin commands', () => {
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

  it('version returns info', async () => {
    cli = new Cli({
      command: cliPath,
      options: ['version']
    });

    cli.run();

    const expected = [expect.stringMatching('Build Version'), expect.stringMatching('Git Short Commit Id')];
    await waitForExpect(async () => {
      const screen = cli.screen();
      expect(screen).toEqual(expect.arrayContaining(expected));
    });
  });
});
