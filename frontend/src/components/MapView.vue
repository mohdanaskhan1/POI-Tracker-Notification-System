<template>
  <div ref="mapEl" style="height:100%; width:100%;"></div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import apiClient from '../utils/api'
import { haversineMeters } from '../utils/geo'
import { getApiBaseUrl } from '../utils/api'

const API_BASE = getApiBaseUrl()

const props = defineProps({
  radius: { type: Number, default: 300 },
  categories: { type: Array, default: () => ['fuel','restaurant','shopping_mall'] },
  tracking: { type: Boolean, default: false }
})
const emit = defineEmits(['notify','error','visit','nearbyOk'])

const mapEl = ref(null)
let map
let userMarker
let userCircle
let watchId = null
let poiLayer = null
const notifiedIds = new Set()
const distanceThreshold = 50
let lastFetch = null
let fetchTimeout = null

const deviceId = (() => {
  let id = localStorage.getItem('poi_device_id')
  if (!id) {
    id = 'dev-' + Math.random().toString(36).substring(2, 15)
    localStorage.setItem('poi_device_id', id)
  }
  return id
})()

function initMap() {
  map = L.map(mapEl.value).setView([0, 0], 2)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap contributors'
  }).addTo(map)
  poiLayer = L.layerGroup().addTo(map)
}

function startTracking() {
  if (!navigator.geolocation) return
  if (watchId != null) return
  watchId = navigator.geolocation.watchPosition(onPosition, onError, {
    enableHighAccuracy: true,
    maximumAge: 5000,
    timeout: 10000
  })
}

function stopTracking() {
  if (watchId != null) {
    navigator.geolocation.clearWatch(watchId)
    watchId = null
  }
}

async function onPosition(pos) {
  const lat = pos.coords.latitude
  const lon = pos.coords.longitude
  if (!userMarker) {
    userMarker = L.marker([lat, lon])
    userMarker.addTo(map)
    map.setView([lat, lon], 16)
  } else {
    userMarker.setLatLng([lat, lon])
  }
  if (!userCircle) {
    userCircle = L.circle([lat, lon], { radius: props.radius, color: '#2b6cb0' })
    userCircle.addTo(map)
  } else {
    userCircle.setLatLng([lat, lon])
    userCircle.setRadius(props.radius)
  }
  fetchNearby(lat, lon)
  throttledReportLocation(lat, lon)
}

function onError(err) { console.error('geolocation error', err) }

async function doFetchNearby(lat, lon) {
  const categories = props.categories.join(',')
  const url = `${API_BASE}/api/pois/nearby?lat=${lat}&lon=${lon}&radius=${props.radius}&categories=${encodeURIComponent(categories)}`
  try {
    const res = await apiClient.get(url)
    renderPois(lat, lon, res.data || [])
    lastFetch = { lat, lon }
    emit('nearbyOk')
  } catch (e) { console.error('fetchNearby error', e); emit('error', 'Nearby places temporarily unavailable. Retrying automatically.') }
}

let lastReport = null
let reportTimeout = null
function throttledReportLocation(lat, lon) {
  if (lastReport && haversineMeters(lat, lon, lastReport.lat, lastReport.lon) < distanceThreshold / 2) return
  if (reportTimeout) clearTimeout(reportTimeout)
  reportTimeout = setTimeout(() => {
    reportLocation(lat, lon)
    lastReport = { lat, lon }
  }, 1000)
}

async function reportLocation(lat, lon) {
  const categories = props.categories.join(',')
  try {
    const res = await apiClient.post(`${API_BASE}/api/locations/report`, {
      deviceId,
      latitude: lat,
      longitude: lon,
      radius: props.radius,
      categories
    })
    if (res.data && res.data.poi) {
      const p = res.data.poi
      const isEntered = res.data.entered
      
      if (isEntered) {
        if (!notifiedIds.has(p.id)) {
          notifiedIds.add(p.id)
          emit('notify', { id: p.id, name: p.name, category: p.category, distance: p.distanceMeters })
          emit('visit', res.data)
        }
      } else {
        // If we are no longer 'entered', remove from notifiedIds so we can notify again on re-entry
        notifiedIds.delete(p.id)
      }
    } else {
      // No POI nearby at all, clear all notifications
      notifiedIds.clear()
    }
  } catch (e) { console.error('reportLocation error', e) }
}

function fetchNearby(lat, lon) {
  if (lastFetch && haversineMeters(lat, lon, lastFetch.lat, lastFetch.lon) < distanceThreshold) return
  if (fetchTimeout) clearTimeout(fetchTimeout)
  fetchTimeout = setTimeout(() => {
    doFetchNearby(lat, lon)
  }, 300)
}

function renderPois(lat, lon, pois) {
  poiLayer.clearLayers()
  pois.forEach(p => {
    const d = p.distanceMeters != null ? p.distanceMeters : haversineMeters(lat, lon, p.latitude, p.longitude)
    L.circleMarker([p.latitude, p.longitude], {
      radius: 8,
      color: '#2f855a',
      fillColor: '#68d391',
      fillOpacity: 0.8
    }).bindPopup(`${p.name} • ${p.category} • ${Math.round(d)} m`)
      .addTo(poiLayer)
  })
}

onMounted(() => {
  initMap()
  if (props.tracking) startTracking()
})

onBeforeUnmount(() => {
  stopTracking()
})

watch(() => props.tracking, v => {
  if (v) startTracking()
  else stopTracking()
})

watch(() => [props.radius, props.categories], () => {
  if (userMarker && userMarker.getLatLng()) {
    const ll = userMarker.getLatLng()
    doFetchNearby(ll.lat, ll.lng)
  }
}, { deep: true })
</script>

<style scoped>
</style>
