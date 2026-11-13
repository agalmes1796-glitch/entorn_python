<template>
  <main class="page">
    <header class="page__header">
      <h1 class="page__title">Videoteca</h1>
      <p class="page__subtitle">Catàleg de pel·lícules</p>
    </header>

    <MovieList
      :movies="data ?? []"
      :loading="pending"
      :error="error"
      :refresh="refresh"
    />
  </main>
</template>

<script setup lang="ts">
// Carrega les pel·lícules cridant al nostre endpoint /api/movies,
// que a la vegada crida al gateway.
const { data, pending, error, refresh } = useMovies()
</script>

<style scoped>
.page {
  max-width: 960px;
  margin: 0 auto;
  padding: 2.5rem 1.25rem 4rem;
}

.page__header {
  margin-bottom: 2rem;
}

.page__title {
  font-size: 2.25rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 0;
}

.page__subtitle {
  color: var(--color-text-muted);
  margin: 0.25rem 0 0;
}
</style>
