import Icon from './Icon.jsx';

export default function SearchFilters({ query, onQueryChange, city, onCityChange, cities, citiesError, onRetryCities }) {
  return (
    <form className="catalog-search" role="search" onSubmit={(event) => event.preventDefault()}>
      <Icon name="search" size={18} />
      <label className="visually-hidden" htmlFor="concert-search">Buscar conciertos</label>
      <input
        id="concert-search"
        type="search"
        name="search"
        autoComplete="off"
        maxLength={120}
        value={query}
        onChange={(event) => onQueryChange(event.target.value)}
        placeholder="Artista, concierto o sala"
      />
      {query && <button type="button" className="clear-search" onClick={() => onQueryChange('')}>Borrar</button>}
      <label className="search-location">
        <Icon name="pin" size={15} />
        <span className="visually-hidden">Filtrar por lugar</span>
        <select aria-label="Filtrar conciertos por lugar" value={city} onChange={(event) => onCityChange(event.target.value)}>
          <option value="">Todos los lugares</option>
          {cities.map((name) => <option key={name} value={name}>{name}</option>)}
        </select>
      </label>
      {citiesError && <span className="city-filter-error" role="status">{citiesError} <button type="button" onClick={() => void onRetryCities()}>Reintentar</button></span>}
    </form>
  );
}
