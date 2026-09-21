/** An error a mock handler throws to produce an HTTP error response. */
export class HttpError extends Error {
	constructor(status, message) {
		super(message);
		this.status = status;
	}
}

/** A non-200 success response (e.g. 201 Created, 204 No Content). */
export class MockResponse {
	constructor(status, data = null) {
		this.status = status;
		this.data = data;
	}
}

export const created = (data) => new MockResponse(201, data);
export const noContent = () => new MockResponse(204);

export const badRequest = (message) => new HttpError(400, message);
export const forbidden = (message = 'You are not allowed to do that.') => new HttpError(403, message);
export const notFound = (message = 'Not found.') => new HttpError(404, message);
export const conflict = (message) => new HttpError(409, message);
