import { Link, Route, Routes } from 'react-router';
import { TodayPage } from '../today/TodayPage';
import { CaseReviewPage } from '../cases/CaseReviewPage';

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
      <Routes>
        <Route path="/" element={<TodayPage />} />
        <Route path="/cases/:caseId" element={<CaseReviewPage />} />
        <Route path="/system-health" element={<p>System health</p>} />
      </Routes>
    </div>
  );
}
