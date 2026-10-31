/**
 * Calls `check` every `intervalMs` until it returns true, `stop` is called, or `maxMs` has passed (then `onTimeout`).
 * Skips calls while `isVisible` is false, and a failed call just waits for the next one.
 * @returns {() => void} Stops polling.
 */
export function startPolling({ check, intervalMs, maxMs, isVisible = () => true, onTimeout = () => {} }) {
	const startedAt = Date.now();
	let timer = null;
	let stopped = false;

	async function tick() {
		if (Date.now() - startedAt >= maxMs) {
			onTimeout();
			return;
		}
		let done = false;
		if (isVisible()) {
			try {
				done = await check();
			} catch {
				done = false;
			}
		}
		if (!done && !stopped) timer = setTimeout(tick, intervalMs);
	}

	timer = setTimeout(tick, intervalMs);
	return () => {
		stopped = true;
		clearTimeout(timer);
	};
}
