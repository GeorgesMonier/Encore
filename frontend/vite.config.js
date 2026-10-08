import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { loadEnv } from 'vite';

function publicDiscoveryFiles(siteUrl) {
  return {
    name: 'encore-public-discovery-files',
    generateBundle() {
      const robots = ['User-agent: *', 'Allow: /', 'Disallow: /api/'];
      if (siteUrl) {
        robots.push('', `Sitemap: ${siteUrl}/sitemap.xml`);
        const locations = ['/', '/privacidad', '/terminos'].map((path) => {
          const url = new URL(path, `${siteUrl}/`).toString();
          return `  <url><loc>${url}</loc></url>`;
        });
        this.emitFile({
          type: 'asset',
          fileName: 'sitemap.xml',
          source: `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${locations.join('\n')}\n</urlset>\n`,
        });
      }
      this.emitFile({ type: 'asset', fileName: 'robots.txt', source: `${robots.join('\n')}\n` });
    },
  };
}

export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, process.cwd(), 'VITE_SITE_URL');
  const rawSiteUrl = environment.VITE_SITE_URL?.trim().replace(/\/+$/, '');
  if (rawSiteUrl) {
    const siteUrl = new URL(rawSiteUrl);
    if (siteUrl.protocol !== 'https:' && !['localhost', '127.0.0.1'].includes(siteUrl.hostname)) {
      throw new Error('VITE_SITE_URL must use HTTPS for the production website.');
    }
  }

  return {
    plugins: [react(), publicDiscoveryFiles(rawSiteUrl)],
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  };
});
