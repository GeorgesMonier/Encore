import { useEffect } from 'react';
import Icon from './Icon.jsx';

let activeModalCount = 0;

export default function Modal({ children, onClose, wide = false, label }) {
  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    activeModalCount += 1;
    document.body.classList.add('modal-open');

    return () => {
      document.removeEventListener('keydown', onKeyDown);
      activeModalCount -= 1;
      if (activeModalCount === 0) document.body.classList.remove('modal-open');
    };
  }, [onClose]);

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className={`modal-panel ${wide ? 'modal-wide' : ''}`} role="dialog" aria-modal="true" aria-label={label}>
        <button className="modal-close" aria-label="Cerrar" onClick={onClose}><Icon name="close" size={20} /></button>
        {children}
      </section>
    </div>
  );
}
