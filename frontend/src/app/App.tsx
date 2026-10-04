import { Link } from 'react-router';

export function App() {
  return (
    <div>
      <header>
        <h1>Where does my team need attention today?</h1>
        <nav aria-label="Primary navigation">
          <Link to="/">Today</Link>
          {' | '}
          <Link to="/system-health">System health</Link>
        </nav>
      </header>
    </div>
  );
}
