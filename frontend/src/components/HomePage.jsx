import AuthDialog from './AuthDialog.jsx';
import EventCatalog from './EventCatalog.jsx';
import EventDialog from './EventDialog.jsx';
import Footer from './Footer.jsx';
import Header from './Header.jsx';
import Hero from './Hero.jsx';
import OrdersDialog from './OrdersDialog.jsx';
import useHomePage from '../hooks/useHomePage.js';

export default function HomePage({ onOpenCookieSettings, theme, onToggleTheme }) {
  const page = useHomePage();

  return (
    <>
      <Header
        city={page.city || 'Todos los lugares'}
        user={page.user}
        onLoginClick={() => page.setAuthOpen(true)}
        onOrdersClick={() => page.setOrdersOpen(true)}
        theme={theme}
        onToggleTheme={onToggleTheme}
      />
      <main>
        <Hero query={page.query} onQueryChange={page.setQuery} onSearch={page.scrollToEvents} />
        <EventCatalog
          events={page.visibleEvents}
          loading={page.loading}
          error={page.error}
          hasActiveFilters={Boolean(page.query.trim() || page.city)}
          onRetry={page.reloadEvents}
          onClearFilters={page.clearFilters}
          onSelectEvent={page.setSelectedEvent}
          query={page.query}
          onQueryChange={page.setQuery}
          city={page.city}
          onCityChange={page.setCity}
          cities={page.cities}
          citiesError={page.citiesError}
          onRetryCities={page.loadCities}
        />
        <section className="gig-note" aria-label="Encore, música en directo">
          <span className="gig-note-number">01 / ENCORE</span>
          <p>La música se vive.<br /><span>Las entradas las ponemos nosotros.</span></p>
          <button className="gig-note-link" onClick={page.scrollToEvents}>Encuentra tu próximo concierto <span>↗</span></button>
          <span className="gig-note-sunburst" aria-hidden="true">✳</span>
        </section>
      </main>
      <Footer onOpenCookieSettings={onOpenCookieSettings} />
      {page.selectedEvent && (
        <EventDialog
          event={page.selectedEvent}
          user={page.user}
          onClose={() => page.setSelectedEvent(null)}
          onRequestAuth={() => page.setAuthOpen(true)}
          onPaymentSuccess={page.showPaymentSuccess}
        />
      )}
      {page.authOpen && <AuthDialog onClose={() => page.setAuthOpen(false)} onLogin={page.handleLogin} />}
      {page.ordersOpen && <OrdersDialog onClose={() => page.setOrdersOpen(false)} />}
      {page.toast && <div className="toast" role="status">{page.toast}</div>}
    </>
  );
}
