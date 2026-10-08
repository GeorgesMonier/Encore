import Icon from './Icon.jsx';

export default function Header({ city, user, onLoginClick, onOrdersClick, theme = 'light', onToggleTheme }) {
  return (
    <header className="site-header">
      <a className="brand" href="/" aria-label="Encore, inicio">
        encore<span className="brand-mark">/</span>
      </a>
      <span className="header-edition">MÚSICA EN DIRECTO, CERCA DE TI</span>
      <div className="header-city" aria-label="Ciudad de la cartelera">
        <Icon name="pin" size={17} />
        <span>{city}</span>
      </div>
      <div className="header-actions">
        <button
          className="theme-toggle"
          type="button"
          onClick={onToggleTheme}
          aria-label={theme === 'dark' ? 'Activar modo claro' : 'Activar modo oscuro'}
          title={theme === 'dark' ? 'Activar modo claro' : 'Activar modo oscuro'}
        >
          <Icon name={theme === 'dark' ? 'sun' : 'moon'} size={17} />
        </button>
        {user ? (
          <>
            <button className="header-link orders-link" onClick={onOrdersClick}>
              <Icon name="ticket" size={17} /><span>Mis entradas</span>
            </button>
            <a className="profile-button" aria-label={`Perfil de ${user.name}`} title={`Perfil de ${user.name}`} href="/perfil">
              <span className="avatar">{user.name?.slice(0, 1)?.toUpperCase() ?? 'U'}</span>
              <span className="profile-name">{user.name?.split(' ')[0]}</span>
            </a>
          </>
        ) : (
          <button className="header-link login-link" onClick={onLoginClick}>
            <Icon name="user" size={18} /><span>Entrar</span>
          </button>
        )}
      </div>
    </header>
  );
}
