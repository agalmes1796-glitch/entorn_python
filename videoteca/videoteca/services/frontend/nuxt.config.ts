// Microservi "frontend" de la Videoteca (Nuxt 3).
//
// Aquesta aplicació només peticiona les dades a través del *gateway*:
// l'endpoint /api/movies d'aquest servidor crida a GET {NUXT_GATEWAY_URL}/api/movies.
// La URL del gateway és una configuració de servidor (no s'exposa al navegador).
export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  ssr: true,

  devtools: { enabled: false },

  runtimeConfig: {
    // Només visible des del servidor Nuxt (no passa al client).
    // Per defecto assumeix que el gateway (Spring Cloud Gateway) escolta al
    // port 8090 del host (ver docker-compose.yaml: 127.0.0.1:8090:8080).
    // En Docker, el compose la sobrescriu amb http://gateway:8080.
    gatewayUrl: process.env.NUXT_GATEWAY_URL || 'http://localhost:8090',
  },

  app: {
    head: {
      htmlAttrs: { lang: 'ca' },
      title: 'Videoteca',
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'Catàleg de pel·lícules — Videoteca' },
      ],
      link: [{ rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }],
    },
  },
})
