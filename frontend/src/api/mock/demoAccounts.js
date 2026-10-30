import { DEMO_PASSWORD } from './seed';
import { resetDb } from './db';

/** Shown on the login page in mock mode so the demo is easy to start. */
export const DEMO_ACCOUNTS = [
	{ label: 'Student', email: 'aisha.rahman@smu.edu.sg', password: DEMO_PASSWORD },
	{ label: 'Administrator', email: 'admin@smu.edu.sg', password: DEMO_PASSWORD },
];

export { resetDb };
