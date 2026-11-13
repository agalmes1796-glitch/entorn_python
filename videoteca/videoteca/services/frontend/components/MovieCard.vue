<template>
  <li class="movie-card">
    <div class="movie-card__body">
      <h2 class="movie-card__title">{{ movie.titulo }}</h2>

      <dl class="movie-card__meta">
        <div class="movie-card__row">
          <dt>Any</dt>
          <dd>{{ yearLabel }}</dd>
        </div>

        <div class="movie-card__row">
          <dt>Duració</dt>
          <dd>{{ durationLabel }}</dd>
        </div>

        <div class="movie-card__row">
          <dt>Gènere</dt>
          <dd>#{{ movie.genero_id }}</dd>
        </div>
      </dl>
    </div>
  </li>
</template>

<script setup lang="ts">
import type { Movie } from '~/types'

const props = defineProps<{ movie: Movie }>()

const yearLabel = computed(() =>
  props.movie.anio_estreno != null ? String(props.movie.anio_estreno) : '—',
)

const durationLabel = computed(() =>
  props.movie.duracion_minutos != null
    ? `${props.movie.duracion_minutos} min`
    : '—',
)
</script>

<style scoped>
.movie-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  overflow: hidden;
  transition: box-shadow 120ms ease, transform 120ms ease;
}

.movie-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.movie-card__body {
  padding: 1rem 1.1rem 1.15rem;
}

.movie-card__title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0 0 0.85rem;
  line-height: 1.3;
}

.movie-card__meta {
  margin: 0;
  display: grid;
  gap: 0.35rem;
}

.movie-card__row {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  font-size: 0.88rem;
}

.movie-card__row dt {
  color: var(--color-text-muted);
}

.movie-card__row dd {
  margin: 0;
  font-weight: 600;
  text-align: right;
}
</style>
