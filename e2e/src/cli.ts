import * as pty from 'node-pty';
import { Terminal } from 'xterm-headless';
import { sleep } from './utils';

export interface CliOptions {
  command: string;
  options?: string[];
  keyWait?: number;
  cols?: number;
  rows?: number;
}

export class Cli {
  private isDisposed: boolean = false;
  private pty: pty.IPty | undefined;
  private term: Terminal | undefined;
  private keyWait: number = 500;
  private cols: number = 80;
  private rows: number = 20;

  constructor(private options: CliOptions) {
    if (options.keyWait) {
      this.keyWait = options.keyWait;
    }
    if (options.cols) {
      this.cols = options.cols;
    }
    if (options.rows) {
      this.rows = options.rows;
    }
  }

  public run(): void {
    this.pty = pty.spawn(this.options.command, this.options.options || [], {
      name: 'xterm-256color',
      cols: this.cols,
      rows: this.rows
    });
    this.term = new Terminal({
      cols: this.cols,
      rows: this.rows
    });
    this.pty.onData(data => {
      this.term?.write(data);
    });
  }

  public screen(): string[] {
    const l = this.term?.buffer.active.length || 0;
    const ret: string[] = [];
    for (let index = 0; index < l; index++) {
      const line = this.term?.buffer.active.getLine(index)?.translateToString();
      if (line) {
        ret.push(line);
      }
    }
    return ret;
  }

  public async keyText(data: string, wait?: number): Promise<Cli> {
    this.pty?.write(data);
    await this.doWait(wait);
    return this;
  }

  public async keyUp(wait?: number): Promise<Cli> {
    this.pty?.write('\x1BOA');
    await this.doWait(wait);
    return this;
  }

  public async keyDown(wait?: number): Promise<Cli> {
    this.pty?.write('\x1BOB');
    await this.doWait(wait);
    return this;
  }

  public async keyEnter(wait?: number): Promise<Cli> {
    this.pty?.write('\x0D');
    await this.doWait(wait);
    return this;
  }

  public dispose(): void {
    if (this.isDisposed) {
      return;
    }
    this.pty?.kill();
    this.term?.dispose();
    this.isDisposed = true;
  }

  private async doWait(wait?: number): Promise<void> {
    await sleep(wait || this.keyWait);
  }
}
