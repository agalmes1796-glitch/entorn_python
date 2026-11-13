<template>
  <section class="movie-list" aria-live="polite">
    <!-- Estat de càrrega -->
    <div v-if="loading" class="movie-list__state" role="status">
      Carregant pel·lícules…
    </div>

    <!-- Estat d'error (gateway no disponible, etc.) -->
    <div v-else-if="error" class="movie-list__state movie-list__state--error" role="alert">
      <p class="movie-list__state-title">No s'ha pogut carregar el llistat de pel·lícules.</p>
      <p class="movie-list__state-detail">{{ errorMessage }}</p>
      <button type="button" class="movie-list__retry" @click="onRetry">
        Torna-ho a provar
      </button>
    </div>

    <!-- Estat buit -->
    <div v-else-if="movies.length === 0" class="movie-list__state">
      Encara no hi ha pel·lícules al catàleg.
    </div>

    <!-- Llista de pel·lícules -->
    <ul v-else class="movie-list__grid">
      <MovieCard v-for="movie in movies" :key="movie.id" :movie="movie" />
    </ul>
  </section>
</template>

<script setup lang="ts">
import type { Movie } from '~/types'

const props = defineProps<{
  movies: Movie[]
  loading?: boolean
  error?: Error | null
  refresh?: () => Promise<void> | void
}>()

const errorMessage = computed(() => {
  if (!props.error) {
    return ''
  }
  if (typeof props.error === 'string') {
    return props.error
  }
  return props.error.message || 'Error desconegut'
})

async function onRetry() {
  await props.refresh?.()
}
</script>

<style scoped>
.movie-list__state {
  padding: 2rem;
  text-align: center;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius);
  color: var(--color-text-muted);
}

.movie-list__state--error {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.movie-list__state-title {
  font-weight: 600;
  margin: 0 0 0.25rem;
}

.movie-list__state-detail {
  font-size: 0.9em;
  opacity: 0.8;
  margin: 0 0 1rem;
  word-break: break-word;
}

.movie-list__retry {
  border: 1px solid var(--color-danger);
  background: transparent;
  color: var(--color-danger);
  border-radius: var(--radius);
  padding: 0.5rem 1rem;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 120ms ease, color 120ms ease;
}

.movie-list__retry:hover {
  background: var(--color-danger);
  color: #fff;
}

.movie-list__grid {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
}
</style>
