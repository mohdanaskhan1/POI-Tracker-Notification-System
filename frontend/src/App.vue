<template>
  <div v-if="!isLoggedIn">
    <Login @loggedIn="onLoggedIn" />
  </div>
  <div v-else>
    <header style="padding: 12px; background: #2f855a; color: white; display: flex; justify-content: space-between; align-items: center;">
      <h1 style="margin:0; font-size: 18px;">Location-Based POI Detection</h1>
      <button @click="logout" style="padding: 8px 12px; background: #c53030; color: white; border: none; cursor: pointer;">Logout</button>
    </header>
    <div v-if="serviceDownBanner" style="background:#FEEBC8; color:#7B341E; padding:8px 12px; font-size:14px; border-bottom:1px solid #FBD38D;">
      Live POI service is temporarily unavailable. We’ll keep tracking and notify when it’s back.
    </div>
    <main style="height: calc(100vh - 56px); display:flex; flex-direction: column;">
      <section style="padding: 8px; display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
        <button @click="requestPermissions" :disabled="consentGranted" style="padding:8px 12px;">Grant Permissions</button>
        <button @click="testNotify" style="padding:8px 12px;">Test Notification</button>
        <button @click="toggleVisits" style="padding:8px 12px;">{{ showVisits ? 'Hide' : 'Show' }} Visits</button>
        <label>Radius (m)
          <input type="number" v-model.number="radius" min="50" max="1000" step="50" style="width:100px; margin-left:6px;">
        </label>
        <label><input type="checkbox" value="fuel" v-model="categories"> Fuel</label>
        <label><input type="checkbox" value="restaurant" v-model="categories"> Restaurants</label>
        <label><input type="checkbox" value="shopping_mall" v-model="categories"> Malls</label>
        <button @click="toggleTracking" style="padding:8px 12px;">{{ tracking ? 'Stop' : 'Start' }} Tracking</button>
        <span v-if="error" style="color:#c53030;">{{ error }}</span>
      </section>
      <MapView
        :radius="radius"
        :categories="categories"
        :tracking="tracking"
        @notify="handleNotify"
        @nearbyOk="handleNearbyOk"
        @visit="handleVisit"
        @error="handleMapError"
        style="flex:1;"
      />
      <section v-if="showVisits" style="padding:8px; border-top:1px solid #eee; max-height:240px; overflow:auto;">
        <div style="font-weight:600; margin-bottom:6px;">Recent Visits</div>
        <div v-if="visits.length===0" style="font-size:13px;">No visits</div>
        <ul style="list-style:none; padding:0; margin:0;">
          <li v-for="v in visits" :key="v.deviceId+':' + (v.poi?.id||'')" style="padding:6px 0; border-bottom:1px solid #f0f0f0; font-size:13px;">
            <span v-if="v.poi">{{ v.poi.name }} • {{ v.poi.category }} • {{ Math.round(v.poi.distanceMeters||0) }} m</span>
            <span v-else>Unknown</span>
          </li>
        </ul>
      </section>
    </main>
    <div style="position:fixed; bottom:12px; left:12px; display:flex; flex-direction:column; gap:8px; z-index:9999; max-height:60vh; width:300px; overflow-y:auto;">
      <div v-for="t in toasts" :key="t.id" style="background:#2f855a; color:white; padding:10px 12px; border-radius:8px; box-shadow:0 2px 8px rgba(0,0,0,0.2); min-width:220px;">
        <div style="font-weight:600;">{{ t.title }}</div>
        <div style="font-size:13px;">{{ t.body }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import MapView from './components/MapView.vue'
import Login from './components/Login.vue'
import { isCapacitorNative, startBackgroundTracking, stopBackgroundTracking } from './native/background.js'
import { getApiBaseUrl } from './utils/api'

const isLoggedIn = ref(false)
const consentGranted = ref(false)
const tracking = ref(false)
const radius = ref(300)
const categories = ref(['fuel','restaurant','shopping_mall'])
const error = ref('')
let bgWatcher = null
const toasts = ref([])
const showVisits = ref(false)
const visits = ref([])
const serviceDownBanner = ref(false)
let firstErrorAt = 0
let lastErrorAt = 0

onMounted(() => {
  if (localStorage.getItem('userId')) {
    isLoggedIn.value = true;
  }
})

function onLoggedIn() {
  isLoggedIn.value = true;
}

function logout() {
  localStorage.removeItem('userId');
  isLoggedIn.value = false;
  if (tracking.value) {
    toggleTracking(); // Stop tracking on logout
  }
}

function showToast(title, body) {
  const id = Date.now() + Math.random()
  toasts.value.push({ id, title, body })
  setTimeout(() => {
    const idx = toasts.value.findIndex(x => x.id === id)
    if (idx >= 0) toasts.value.splice(idx, 1)
  }, 4000)
}

function toggleVisits() {
  showVisits.value = !showVisits.value
  if (showVisits.value) fetchVisits()
}

async function fetchVisits() {
  try {
    const { default: axios } = await import('axios')
    const API_BASE = getApiBaseUrl()
    const userId = localStorage.getItem('userId')
    const res = await axios.get(`${API_BASE}/api/visits/recent?limit=20`, {
      headers: { 'X-User-Id': userId }
    })
    visits.value = Array.isArray(res.data) ? res.data : []
  } catch (_) {}
}

function requestPermissions() {
  error.value = ''
  const reqs = []
  if (Notification && Notification.permission !== 'granted') {
    reqs.push(Notification.requestPermission())
  }
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(() => {}, () => {})
  }
  Promise.all(reqs).then(() => {
    consentGranted.value = true
  })
}

function toggleTracking() {
  if (!consentGranted.value) requestPermissions()
  tracking.value = !tracking.value
  if (isCapacitorNative()) {
    if (tracking.value) {
      startBackgroundTracking({ radius: radius.value, categories: categories.value }).then(w => bgWatcher = w)
    } else {
      stopBackgroundTracking(bgWatcher)
      bgWatcher = null
    }
  }
}

async function triggerNotification(title, body) {
  if (typeof Notification !== 'undefined' && Notification.permission === 'default') {
    try { await Notification.requestPermission() } catch (_) {}
  }
  let shown = false
  if (!shown && typeof navigator !== 'undefined' && navigator.serviceWorker) {
    try {
      const reg = await navigator.serviceWorker.ready
      await reg.showNotification(title, { body, tag: 'poi', renotify: true, requireInteraction: true, silent: false, icon: '/icons/poi.svg', badge: '/icons/poi-badge.svg' })
      shown = true
    } catch (e) { console.error('SW showNotification error', e) }
  }
  if (!shown && typeof Notification !== 'undefined' && Notification.permission === 'granted') {
    try {
      new Notification(title, { body, tag: 'poi', renotify: true, requireInteraction: true, silent: false, icon: '/icons/poi.svg', badge: '/icons/poi-badge.svg' })
      shown = true
    } catch (e) { console.error('Notification API error', e) }
  }
  if (!shown) {
    alert(`${title}\n${body}`)
  }
  showToast(title, body)
}

async function handleNotify(payload) {
  const title = `Welcome to ${payload.name}`
  const body = `${payload.category} • ${Math.round(payload.distance)} m`
  await triggerNotification(title, body)
}

async function testNotify() {
  await triggerNotification('Test Notification', 'This is a test message')
}

function handleMapError(msg) {
  showToast('Notice', msg)
  const now = Date.now()
  if (!firstErrorAt) firstErrorAt = now
  lastErrorAt = now
  if (now - firstErrorAt > 150000) {
    serviceDownBanner.value = true
  }
}

function handleVisit(v) {
  if (v && v.entered) {
    visits.value.unshift(v)
    if (visits.value.length > 20) visits.value.pop()
  }
}

function handleNearbyOk() {
  serviceDownBanner.value = false
  firstErrorAt = 0
  lastErrorAt = 0
}
</script>

<style>
body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; }
label { user-select: none; }
</style>
