import { useEffect } from 'react';

const metadata = {
  home: {
    title: 'Encore — Conciertos para sentirlos en primera fila',
    description: 'Encuentra conciertos en directo cerca de ti. Descubre artistas, salas y fechas, y reserva tus entradas con Encore.',
  },
  privacy: {
    title: 'Política de privacidad — Encore',
    description: 'Consulta cómo Encore trata tus datos personales, los datos de cuenta y las preferencias de cookies.',
  },
  terms: {
    title: 'Términos y condiciones — Encore',
    description: 'Consulta las condiciones de uso de Encore, las reservas de entradas y el proceso de compra.',
  },
  profile: {
    title: 'Mi perfil — Encore',
    description: 'Gestiona tu perfil, tus datos de cuenta y tu método de pago de Encore.',
  },
  'not-found': {
    title: 'Página no encontrada — Encore',
    description: 'La página que buscas no está disponible. Vuelve a Encore para encontrar conciertos y entradas.',
  },
};

function setMeta(selector, attribute, value) {
  const element = document.querySelector(selector);
  if (element) element.setAttribute(attribute, value);
}

function removeMeta(selector, attribute) {
  document.querySelector(selector)?.removeAttribute(attribute);
}

export default function useDocumentMetadata(page) {
  useEffect(() => {
    const pageMetadata = metadata[page] ?? metadata.home;
    document.title = pageMetadata.title;
    setMeta('meta[name="description"]', 'content', pageMetadata.description);
    setMeta('meta[property="og:title"]', 'content', pageMetadata.title);
    setMeta('meta[property="og:description"]', 'content', pageMetadata.description);
    setMeta('meta[name="twitter:title"]', 'content', pageMetadata.title);
    setMeta('meta[name="twitter:description"]', 'content', pageMetadata.description);
    setMeta('meta[name="robots"]', 'content', page === 'profile' || page === 'not-found' ? 'noindex, nofollow' : 'index, follow');

    const siteUrl = import.meta.env.VITE_SITE_URL?.replace(/\/+$/, '');
    if (siteUrl) {
      const canonicalUrl = `${siteUrl}${window.location.pathname}`;
      setMeta('meta[property="og:url"]', 'content', canonicalUrl);
      setMeta('link[rel="canonical"]', 'href', canonicalUrl);
    } else {
      removeMeta('meta[property="og:url"]', 'content');
      removeMeta('link[rel="canonical"]', 'href');
    }
  }, [page]);
}
