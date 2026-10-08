export default function LegalContact({ kind = 'privacidad' }) {
  const address = import.meta.env.VITE_CONTACT_EMAIL?.trim();
  if (!address) return <strong>privacy@encore.com</strong>;
  return <a href={`mailto:${encodeURIComponent(address)}`}>{address}</a>;
}
