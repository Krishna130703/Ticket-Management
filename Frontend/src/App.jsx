import { BrowserRouter, Link, Route, Routes } from 'react-router-dom';
import CreateTicketPage from './pages/CreateTicketPage.jsx';
import TicketDetailPage from './pages/TicketDetailPage.jsx';
import TicketListPage from './pages/TicketListPage.jsx';

export default function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <nav className="top-nav">
          <Link to="/" className="brand">Ticket Management</Link>
          <div className="nav-links">
            <Link to="/">Tickets</Link>
            <Link to="/tickets/new">Create</Link>
          </div>
        </nav>
        <main className="container">
          <Routes>
            <Route path="/" element={<TicketListPage />} />
            <Route path="/tickets/new" element={<CreateTicketPage />} />
            <Route path="/tickets/:ticketId" element={<TicketDetailPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
