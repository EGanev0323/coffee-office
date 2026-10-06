<script setup>
import { onMounted, ref, watch } from 'vue'
import QRCode from 'qrcode'

/* Етикет с QR код за кафемашината – сканираш и кафето се отбелязва. */
const url = ref(`${window.location.origin}/drink`)
const svg = ref('')

async function render() {
  try {
    svg.value = await QRCode.toString(url.value, {
      type: 'svg',
      margin: 1,
      errorCorrectionLevel: 'M',
      color: { dark: '#2B1F18', light: '#FFFFFF' }
    })
  } catch {
    svg.value = ''
  }
}

function printLabel() {
  window.print()
}

watch(url, render)
onMounted(render)
</script>

<template>
  <div class="qr-admin">
    <section class="panel">
      <h2>QR код за кафемашината</h2>
      <p class="hint">
        Принтирай етикета и го залепи на машината. Колегите го сканират с камерата на телефона
        и кафето им се отбелязва веднага, с възможност за отмяна.
      </p>
      <div class="field url-field">
        <label for="qr-url">Адрес в QR кода</label>
        <input id="qr-url" v-model.trim="url" />
        <p class="hint">
          Трябва да е адресът, през който колегите отварят системата от телефона
          (например Tailscale Funnel адресът), а не вътрешен IP.
        </p>
      </div>
      <button class="btn btn-primary" @click="printLabel">Принтирай етикета</button>
    </section>

    <div class="qr-label">
      <p class="qr-title">Кафе дъската</p>
      <div class="qr-code" v-html="svg"></div>
      <p class="qr-text">Налей си кафе и сканирай</p>
      <p class="qr-url">{{ url }}</p>
    </div>
  </div>
</template>

<style scoped>
.qr-admin { display: grid; gap: 1.5rem; justify-items: start; }
.qr-admin .panel { display: grid; gap: 1rem; width: 100%; }
.url-field { max-width: 560px; }
.qr-admin .panel .btn { justify-self: start; }

.qr-label {
  width: 8cm;
  padding: 0.5cm 0.6cm 0.4cm;
  background: #FFFFFF;
  border: 3px solid var(--marker);
  border-radius: 12px;
  text-align: center;
  color: var(--ink);
}
.qr-title {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 2rem;
  line-height: 1;
}
.qr-code { margin: 0.3cm auto 0.2cm; width: 6cm; }
.qr-code :deep(svg) { display: block; width: 100%; height: auto; }
.qr-text {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 1.5rem;
  color: var(--marker);
  line-height: 1.1;
}
.qr-url { font-size: 0.6rem; color: var(--muted); margin-top: 0.2cm; overflow-wrap: anywhere; }
</style>

<style>
/* При принтиране – само етикетът */
@media print {
  body * { visibility: hidden !important; }
  .qr-label, .qr-label * { visibility: visible !important; }
  .qr-label { position: fixed; left: 1cm; top: 1cm; }
}
</style>