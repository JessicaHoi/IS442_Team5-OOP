/** Importing each handler module registers its routes with the mock router. */
import './handlers/auth';
import './handlers/students';
import './handlers/matches';
import './handlers/connections';
import './handlers/groups';
import './handlers/admin';

export { findRoute } from './router';
