import { useState } from 'react';
import { fallbackImages, formatDate, formatTime, optimizeImageUrl } from '../utils/events.js';
import Icon from './Icon.jsx';

export default function EventCard({ event, index, onClick }) {
  const [imageFailed, setImageFailed] = useState(false);
  const image = optimizeImageUrl(!imageFailed && event.imageUrl ? event.imageUrl : fallbackImages[index % fallbackImages.length], 720);

  return (
    <button className={`event-card event-card-${index % 4}`} onClick={onClick}>
      <div className="card-image-wrap">
        <img
          className="card-image"
          src={image}
          alt={event.name}
          loading="lazy"
          decoding="async"
          width="720"
          height="480"
          onError={() => setImageFailed(true)}
        />
        <span className="card-date">
          {formatDate(event.eventDate)}
          {formatTime(event.eventTime) && ` · ${formatTime(event.eventTime)}`}
        </span>
        <span className="card-image-shade" />
        <span className="card-city">{event.city || 'España'}</span>
        <span className="card-open"><Icon name="arrow" size={18} /></span>
      </div>
      <div className="card-copy">
        <div className="card-meta"><span>EN DIRECTO</span><span>{event.venue || 'SALA POR ANUNCIAR'}</span></div>
        <h3>{event.name}</h3>
        <p>{event.artist || 'Una noche para cantar a pleno pulmón'}</p>
      </div>
    </button>
  );
}
