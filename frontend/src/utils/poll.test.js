import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { startPolling } from './poll';

describe('startPolling', () => {
	beforeEach(() => vi.useFakeTimers());
	afterEach(() => vi.useRealTimers());

	it('stops calling once the check returns true', async () => {
		const check = vi.fn().mockResolvedValueOnce(false).mockResolvedValue(true);
		startPolling({ check, intervalMs: 100, maxMs: 1000 });
		await vi.advanceTimersByTimeAsync(500);
		expect(check).toHaveBeenCalledTimes(2);
	});

	it('gives up after the time limit and says so', async () => {
		const check = vi.fn().mockResolvedValue(false);
		const onTimeout = vi.fn();
		startPolling({ check, intervalMs: 100, maxMs: 250, onTimeout });
		await vi.advanceTimersByTimeAsync(1000);
		expect(check).toHaveBeenCalledTimes(2);
		expect(onTimeout).toHaveBeenCalledTimes(1);
	});

	it('skips calls while the tab is hidden, but keeps going', async () => {
		let visible = false;
		const check = vi.fn().mockResolvedValue(true);
		startPolling({ check, intervalMs: 100, maxMs: 1000, isVisible: () => visible });
		await vi.advanceTimersByTimeAsync(300);
		expect(check).not.toHaveBeenCalled();
		visible = true;
		await vi.advanceTimersByTimeAsync(100);
		expect(check).toHaveBeenCalledTimes(1);
	});

	it('keeps going after a failed call', async () => {
		const check = vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValue(true);
		startPolling({ check, intervalMs: 100, maxMs: 1000 });
		await vi.advanceTimersByTimeAsync(300);
		expect(check).toHaveBeenCalledTimes(2);
	});

	it('does nothing more after being stopped', async () => {
		const check = vi.fn().mockResolvedValue(false);
		const stop = startPolling({ check, intervalMs: 100, maxMs: 1000 });
		await vi.advanceTimersByTimeAsync(100);
		stop();
		await vi.advanceTimersByTimeAsync(500);
		expect(check).toHaveBeenCalledTimes(1);
	});
});
