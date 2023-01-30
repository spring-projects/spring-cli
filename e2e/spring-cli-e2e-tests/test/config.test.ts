import 'jest-extended';
import waitForExpect from 'wait-for-expect';
import { Cli } from 'spring-shell-e2e';
import {
  nativeDesc,
  jarDesc,
  jarCommand,
  nativeCommand,
  jarOptions,
  waitForExpectDefaultTimeout,
  waitForExpectDefaultInterval,
  testTimeout
} from '../src/utils';

describe('config commands', () => {
  let cli: Cli;
  let command: string;
  let options: string[] = [];

  // config list runs without error test
  const configListDoesNotErrorDesc = 'config list does not error';
  const configListDoesNotErrorCommand = ['config', 'list'];
  const configListDoesNotError = async (cli: Cli) => {
    cli.run();
    await expect(cli.exitCode()).resolves.toBe(0);
  };

  beforeEach(async () => {
    waitForExpect.defaults.timeout = waitForExpectDefaultTimeout;
    waitForExpect.defaults.interval = waitForExpectDefaultInterval;
  }, testTimeout);

  afterEach(async () => {
    cli?.dispose();
  }, testTimeout);

  // fatjar commands
  describe(jarDesc, () => {
    beforeAll(() => {
      command = jarCommand;
      options = jarOptions;
    });

    it(
      configListDoesNotErrorDesc,
      async () => {
        cli = new Cli({
          command: command,
          options: [...options, ...configListDoesNotErrorCommand]
        });
        await configListDoesNotError(cli);
      },
      testTimeout
    );
  });

  // native commands
  describe(nativeDesc, () => {
    beforeAll(() => {
      command = nativeCommand;
      options = [];
    });

    it(
      configListDoesNotErrorDesc,
      async () => {
        cli = new Cli({
          command: command,
          options: [...options, ...configListDoesNotErrorCommand]
        });
        await configListDoesNotError(cli);
      },
      testTimeout
    );
  });
});
