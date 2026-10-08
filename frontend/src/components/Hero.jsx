import Icon from './Icon.jsx';
import { optimizeImageUrl } from '../utils/events.js';

export default function Hero({ query, onQueryChange, onSearch }) {
  return (
    <section className="hero">
      <img
        className="hero-image"
        src={optimizeImageUrl('https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 1800)}
        alt=""
        fetchPriority="high"
        decoding="async"
      />
      <div className="hero-grain" />
      <div className="hero-content">
        <span className="eyebrow"><span className="eyebrow-bar" /> AGENDA ABIERTA · 2026</span>
        <h1>Que tiemble<br />el <span>suelo.</span></h1>
        <p>Conciertos que se sienten mucho<br className="desktop-break" /> después del último acorde.</p>
        <form className="hero-search" onSubmit={(event) => { event.preventDefault(); onSearch(); }}>
          <Icon name="search" size={20} />
          <input
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder="Busca un artista, sala o ciudad"
            aria-label="Buscar conciertos"
          />
          <button type="submit" aria-label="Buscar conciertos"><span>Buscar</span><Icon name="arrow" size={17} /></button>
        </form>
      </div>
      <div className="hero-caption"><span>SONIDO ALTO.</span><span>RECUERDOS MÁS ALTOS.</span></div>
      <div className="hero-sticker" aria-hidden="true"><span>LIVE</span><span>LOUD</span><b>✳</b></div>
    </section>
  );
}
