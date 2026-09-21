/** "Aisha Rahman" -> "AR" */
export function initials(name = '') {
	return name
		.split(/\s+/)
		.filter(Boolean)
		.slice(0, 2)
		.map((part) => part[0].toUpperCase())
		.join('');
}

/** Human-friendly relative time, e.g. "3 days ago". */
export function timeAgo(isoString) {
	if (!isoString) return 'Never';
	const seconds = Math.round((Date.now() - new Date(isoString).getTime()) / 1000);
	if (seconds < 60) return 'Just now';
	const units = [
		['year', 31536000],
		['month', 2592000],
		['day', 86400],
		['hour', 3600],
		['minute', 60],
	];
	for (const [unit, size] of units) {
		if (seconds >= size) {
			const count = Math.floor(seconds / size);
			return `${count} ${unit}${count > 1 ? 's' : ''} ago`;
		}
	}
	return 'Just now';
}

/** "1 Mar 2026" */
export function formatDate(isoString) {
	if (!isoString) return '';
	return new Date(isoString).toLocaleDateString('en-SG', { day: 'numeric', month: 'short', year: 'numeric' });
}

/** "Year 2 · Information Systems" */
export function programmeLine(student) {
	return [student.yearOfStudy ? `Year ${student.yearOfStudy}` : '', student.programme].filter(Boolean).join(' · ');
}
