<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import CountUp from './fx/CountUp.vue'
import { badgeImg, badgeQuote, badgeTitle, loadBadges } from '../badges'
import { coffees, dayLabel } from '../format'
import { gsap, reducedMotion } from '../motion'

/*
  Баджовете в профила: колко кафета остават до следващия (лента за прогрес)
  и колекцията – получените са цветни с датата, а следващите са сиви и заключени.
*/
const data = ref(null)
const error = ref('')
const root = ref(null)

const next = computed(() => data.value?.badges.find((b) => b.code === data.value.next) ?? null)
const earnedCount = computed(() => data.value?.badges.filter((b) => b.earnedAt).length ?? 0)
const progress = computed(() => {
  if (!data.value || !next.value) return 1
  const from = data.value.progressFrom
  return Math.min(1, Math.max(0, (data.value.coffees - from) / (next.value.threshold - from)))
})

onMounted(async () => {
  try {
    data.value = await loadBadges()
  } catch {
    error.value = 'Баджовете не можаха да се заредят. Опитай пак след малко.'
    return
  }
  await nextTick()
  animateIn()
})

function animateIn() {
  const el = root.value
  if (!el) return
  const bar = el.querySelector('.progress-fill')
  if (reducedMotion()) {
    if (bar) bar.style.transform = `scaleX(${progress.value})`
    return
  }
  gsap
    .timeline()
    .fromTo(bar, { scaleX: 0 }, { scaleX: progress.value, duration: 1.3, ease: 'power3.out' }, 0.2)
    .from(el.querySelectorAll('.badge-card'), { y: 18, autoAlpha: 0, scale: 0.9, duration: 0.5, stagger: 0.08, ease: 'back.out(1.7)', clearProps: 'all' }, 0.1)
    .from(el.querySelectorAll('.badge-card.is-earned .badge-img'), { rotation: -30, scale: 0.4, duration: 0.7, stagger: 0.08, ease: 'back.out(2.2)', clearProps: 'transform' }, 0.25)
}
</script>

<template>
  <section ref="root" class="panel shelf" aria-labelledby="badges-title">
    <div class="shelf-head">
      <h2 id="badges-title">Баджове</h2>
      <span v-if="data" class="shelf-count">{{ earnedCount }} от {{ data.badges.length }}</span>
    </div>

    <p v-if="error" class="form-error" role="alert">{{ error }}</p>
    <template v-else-if="data">
      <!-- прогрес до следващия бадж -->
      <div class="next" :class="{ 'is-done': !next }">
        <img v-if="next" :src="badgeImg(next.code)" alt="" class="next-img" width="64" height="64" />
        <div class="next-text">
          <p class="next-total">
            Изпи общо <strong><CountUp :value="data.coffees" :duration="1.2" /></strong>&nbsp;{{ data.coffees === 1 ? 'кафе' : 'кафета' }}
          </p>
          <p v-if="next" class="next-left">
            Още <strong>{{ coffees(data.remaining) }}</strong> до „{{ badgeTitle(next.code) }}“
          </p>
          <p v-else class="next-left">Събра всички баджове. Пълна колекция!</p>
          <div
            class="progress"
            role="progressbar"
            :aria-valuemin="data.progressFrom"
            :aria-valuemax="next ? next.threshold : data.coffees"
            :aria-valuenow="data.coffees"
            :aria-label="next ? `${data.coffees} от ${next.threshold} кафета` : 'Всички баджове са събрани'"
          >
            <span class="progress-fill"></span>
          </div>
          <p v-if="next" class="progress-scale" aria-hidden="true">
            <span>{{ data.progressFrom }}</span><span>{{ next.threshold }}</span>
          </p>
        </div>
      </div>

      <!-- колекцията -->
      <ul class="badge-grid">
        <li v-for="b in data.badges" :key="b.code" class="badge-card" :class="{ 'is-earned': b.earnedAt }">
          <span class="badge-img-wrap">
            <img :src="badgeImg(b.code)" alt="" class="badge-img" width="96" height="96" draggable="false" />
            <svg v-if="!b.earnedAt" class="badge-lock" viewBox="0 0 24 24" aria-hidden="true">
              <rect x="5" y="10.5" width="14" height="10" rx="2" />
              <path d="M8 10.5V8a4 4 0 0 1 8 0v2.5" />
            </svg>
          </span>
          <span class="badge-title">{{ badgeTitle(b.code) }}</span>
          <span class="badge-threshold">{{ b.threshold }} кафета</span>
          <span v-if="b.earnedAt" class="badge-quote">„{{ badgeQuote(b.code) }}“</span>
          <span class="badge-meta">
            {{ b.earnedAt ? `получен ${dayLabel(b.earnedAt).toLowerCase()}` : 'още заключен' }}
          </span>
        </li>
      </ul>
    </template>
    <div v-else class="shelf-loading" aria-busy="true">
      <span class="skeleton" style="height: 64px"></span>
      <span class="skeleton" style="height: 120px"></span>
    </div>
  </section>
</template>

<style scoped>
.shelf { display: grid; gap: 1.1rem; }
.shelf-head { display: flex; align-items: baseline; justify-content: space-between; gap: 1rem; }
.shelf-head h2 { margin: 0; }
.shelf-count { color: var(--muted); font-size: var(--step--1); font-variant-numeric: tabular-nums; }

.next { display: flex; gap: 0.9rem; align-items: center; }
.next-img {
  flex: none;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  filter: grayscale(1) brightness(0.75);
  opacity: 0.7;
  box-shadow: 0 4px 10px -4px rgba(43, 31, 24, 0.4);
}
.next-text { flex: 1; min-width: 0; display: grid; gap: 0.3rem; }
.next-total, .next-left { margin: 0; }
.next-total strong { font-family: var(--font-hand); font-size: 1.6rem; color: var(--marker); line-height: 1; margin-right: 0.15em; }
.next-left { color: var(--muted); font-size: var(--step--1); }
.next-left strong { color: var(--ink); }

.progress {
  position: relative;
  height: 10px;
  margin-top: 0.3rem;
  border-radius: 999px;
  background: var(--board);
  box-shadow: inset 0 0 0 1px var(--rule);
  overflow: hidden;
}
.progress-fill {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(90deg, #A0612E, #D9A15B 55%, #F2CF7A);
  transform-origin: left center;
  transform: scaleX(v-bind(progress));
}
.is-done .progress-fill { background: linear-gradient(90deg, #6D4BD8, #B37BF0, #79D1F2); }
.progress-scale {
  display: flex;
  justify-content: space-between;
  margin: 0;
  color: var(--muted);
  font-size: 0.75rem;
  font-variant-numeric: tabular-nums;
}

.badge-grid {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}
@media (min-width: 560px) { .badge-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); } }
.badge-card {
  display: grid;
  justify-items: center;
  gap: 0.2rem;
  padding: 0.9rem 0.5rem 0.8rem;
  border-radius: 14px;
  background: var(--board);
  text-align: center;
}
.badge-img-wrap { position: relative; display: block; margin-bottom: 0.3rem; }
.badge-img {
  display: block;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  box-shadow: 0 3px 3px rgba(43, 31, 24, 0.25), 0 10px 18px -8px rgba(43, 31, 24, 0.45);
}
.badge-card:not(.is-earned) .badge-img {
  filter: grayscale(1) brightness(0.8) contrast(0.9);
  opacity: 0.45;
  box-shadow: none;
}
.badge-lock {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 30px;
  height: 30px;
  padding: 5px;
  border-radius: 50%;
  background: var(--surface);
  box-shadow: 0 1px 4px rgba(43, 31, 24, 0.25);
  fill: none;
  stroke: var(--muted);
  stroke-width: 2;
  stroke-linecap: round;
}
.badge-title { font-weight: 700; font-size: var(--step--1); }
.badge-threshold {
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--muted);
}
.badge-quote {
  margin: 0.2rem 0 0.1rem;
  font-size: 0.75rem;
  font-style: italic;
  line-height: 1.35;
  color: var(--ink);
}
.badge-meta { color: var(--muted); font-size: 0.75rem; line-height: 1.3; }
.badge-card:not(.is-earned) .badge-title { color: var(--muted); }

.shelf-loading { display: grid; gap: 0.75rem; }
</style>
