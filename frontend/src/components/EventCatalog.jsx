import EventCard from './EventCard.jsx';
import Icon from './Icon.jsx';
import SearchFilters from './SearchFilters.jsx';

export default function EventCatalog({
  events,
  loading,
  error,
  hasActiveFilters,
  onRetry,
  onClearFilters,
  onSelectEvent,
  query,
  onQueryChange,
  city,
  onCityChange,
  cities,
  citiesError,
  onRetryCities,
}) {
  return (
    <section className="event-section" id="eventos" aria-busy={loading}>
      <div className="section-heading">
        <div>
          <span className="section-kicker"><span /> EL PRÓXIMO SUBIDÓN ESTÁ AQUÍ</span>
          <h2>Próximos <span>conciertos.</span></h2>
        </div>
        <div className="section-edition">ENTRADAS PARA SENTIRLO EN PRIMERA FILA</div>
      </div>

      <div className="discovery-controls">
        <div className="concert-label"><Icon name="ticket" size={16} /> CONCIERTOS · {(city || 'TODOS LOS LUGARES').toLocaleUpperCase('es')}</div>
        <div className="result-count" role="status" aria-live="polite">
          {loading ? 'BUSCANDO…' : `${events.length} ${events.length === 1 ? 'FECHA' : 'FECHAS'}`}
        </div>
      </div>

      <SearchFilters
        query={query}
        onQueryChange={onQueryChange}
        city={city}
        onCityChange={onCityChange}
        cities={cities}
        citiesError={citiesError}
        onRetryCities={onRetryCities}
      />

      {loading ? (
        <div className="event-grid" aria-label="Cargando conciertos">
          {[1, 2, 3, 4].map((item) => <div className="event-skeleton" key={item} />)}
        </div>
      ) : error ? (
        <div className="empty-state error-state">
          <div className="empty-icon"><Icon name="ticket" size={23} /></div>
          <h3>El escenario está afinando.</h3>
          <p>{error}</p>
          <button className="button button-dark" onClick={() => void onRetry()}>Volver a intentarlo <Icon name="arrow" size={16} /></button>
        </div>
      ) : events.length ? (
        <div className="event-grid">
          {events.map((event, index) => (
            <EventCard key={event.id} event={event} index={index} onClick={() => onSelectEvent(event)} />
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <div className="empty-icon"><Icon name="search" size={23} /></div>
          <h3>{hasActiveFilters ? 'No encontramos conciertos con esos filtros.' : 'El siguiente gran concierto está por anunciarse.'}</h3>
          <p>{hasActiveFilters ? 'Prueba con otro artista, sala o ciudad.' : 'Vuelve pronto. La música no para.'}</p>
          {hasActiveFilters && <button className="button button-dark" onClick={onClearFilters}>Ver todos los conciertos</button>}
        </div>
      )}
    </section>
  );
}
